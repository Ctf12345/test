package com.ctf.autotest;

import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.struct.Message;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/****************** 通讯机制概述 ***************************
 * cmd_queue(本方主动发送的命令的缓存队列)
 * 通讯机制是一问一答。但模拟测试client可能需要测试乱发命令的情况，故不需要命令队列，仅以最后一个命令为准。
 *
 * lastTermReq(本方收到的服务器发送的的请求)
 *
 * resend & repeated
 * 因网络不稳定，本方发出的命令或对方发出的回复有可能丢失，为保证通讯成功，需要加入超时重发机制。
 * 超时重发由发起方负责，发起方发送命令后，在预定时间内未收到对应的回复，即视为命令超时，需要进行重发。
 * 重发多次后仍未收到回复，则视为通讯异常，发起方将关闭socket连接。
 *
 * 超时重发会导致接收方可能收到重复消息。故消息接收方要保证消息处理的*幂等性*！
 * 每个命令都有一个相对唯一的顺序号，对方的回复也使用该顺序号。
 * 通过顺序号对方可以判断收到的命令是新消息还是重发的命令，如为重发的命令则将直接返回上次处理的结果。
 */
public class CmdUtils implements Runnable {
    private static LogWrapper logger = LogWrapper.getLogger(CmdUtils.class.getName());

    private static final AttributeKey<CmdUtils> CMD_UTILS_KEY = AttributeKey.valueOf("cmd_utils");

    private static final int DEFAULT_RESEND_TIMEOUT_SECONDS = 10;  //默认命令发送超时时长(秒数)
    private static final int DEFAULT_MAX_RESEND_TIMES = 3;          //默认最大重发次数
    private static final int DEFAULT_WAIT_TIMEOUT_SECONDS = 500;      //默认等待超时时长(秒数)

    private static volatile ArrayList<ChannelHandlerContext> allCtxs = new ArrayList<>();

    private ChannelHandlerContext ctx;

    private short req_seq_no;
    private Message lastReq;                //当前/上次本机发给服务器的请求
    private Message lastRsp;                //上次本机发给服务器的回复
    private ScheduledFuture resendFuture;  //定时任务，用于超时重发
    private int resend_timeout_seconds;  //命令超时时长(秒数)
    private int max_resend_times;         //最大命令重发次数
    private int curr_resend_times;        //当前命令重发次数

    //---------------------------------------------------------------------
    private Message lastTermReq;           //上次服务器下发的请求
    private Message lastTermRsp;           //上次服务器下发的回复

    private AssertMessage assertMessage;       //对收到的TermMessage进行脚本断言
    private boolean bWaitReq;
    private volatile WaitResult waitResult;       //等待结果
    private Object waitObject;                     //等待对象，用于wait, notify

    private ScheduledFuture waitFuture;     //定时任务，用于等待超时
    private int wait_timeout_seconds;     //等待超时时长(秒数)

    //初始化ctx
    //1.初始化一个CmdUtils对象，将其保存在ctx的attr中。           2.将ctx添加到allCtxs中
    public static void init(ChannelHandlerContext ctx) {
        Attribute<CmdUtils> attrCmdUtils = ctx.channel().attr(CMD_UTILS_KEY);
        attrCmdUtils.setIfAbsent(new CmdUtils(ctx, DEFAULT_RESEND_TIMEOUT_SECONDS, DEFAULT_MAX_RESEND_TIMES, DEFAULT_WAIT_TIMEOUT_SECONDS));

        allCtxs.add(ctx);
    }

    //从ctx中获取CmdUtils对象
    public static CmdUtils get(ChannelHandlerContext ctx) {
        Attribute<CmdUtils> attr = ctx.channel().attr(CMD_UTILS_KEY);
        return attr.get();
    }

    //按ipAddr从allCtxs中获取ctx
    public static ChannelHandlerContext getCtx(String ipAddr) {
        if (ipAddr != null) {
            for (ChannelHandlerContext tempCtx : allCtxs) {
                if (tempCtx.channel().remoteAddress().toString().equals(ipAddr)) {
                    return tempCtx;
                }
            }
        }

        return null;
    }

    //当ctx断开时，从allCtxs中移除该ctx
    public static synchronized void deInit(ChannelHandlerContext ctx) {
        if (ctx != null) {
            CmdUtils cmdUtils = CmdUtils.get(ctx);
            logger.debug(ctx, "set result diconnn!!!");
            cmdUtils.setWaitResult(WaitResult.WAIT_RESULT_DISCONN);
            for (ChannelHandlerContext foundCtx : allCtxs) {
                if (foundCtx.channel().equals(ctx.channel())) {
                    allCtxs.remove(foundCtx);
                    return;
                }
            }

            logger.debug("DeInit not found :(");
            return;
        } else {
            logger.debug("DeInit ctx is NULL :(");
            return;
        }
    }

    public static void waitAllCtxsDeInit() {
        while(allCtxs.size() > 0) {
            try {

                Thread.sleep(500);
                for (ChannelHandlerContext ctx : allCtxs){
                    ctx.close();
                }
            } catch (InterruptedException e) {

            }
        }
    }

    //--------------------------------------------------------------------------------------------
    //私有构造函数
    private CmdUtils(ChannelHandlerContext ctx, int resend_timeout_seconds, int max_resend_times, int wait_timeout_seconds) {
        this.ctx = ctx;

        this.req_seq_no = 0;

        this.lastTermReq = null;
        this.lastTermRsp = null;

        this.lastReq = null;
        this.lastRsp = null;

        this.assertMessage = null;
        this.assertMessage = null;

        this.resendFuture = null;
        this.resend_timeout_seconds = resend_timeout_seconds;
        this.max_resend_times = max_resend_times;
        this.curr_resend_times = 0;

        this.bWaitReq = false;
        this.waitResult = WaitResult.WAIT_RESULT_UNKNOWN;
        this.waitObject = new Object();

        this.waitFuture = null;
        this.wait_timeout_seconds = wait_timeout_seconds;
    }

    //----------------------------------------------------------------------
    //获取 命令 序列号
    public short getCmdSeqNo() {
        return this.req_seq_no;
    }
    //获取并自增 命令 序列号
    public short getIncCmdSeqNo() {
        short old_seq_no = this.req_seq_no;
        this.req_seq_no = (short)((req_seq_no + 1) & 0x7FFF);
        return old_seq_no;
    }

    public Message getLastTermReq() {
        return lastTermReq;
    }

    public Message getLastReq() {
        return lastReq;
    }

    //发送回复
    public void sendRsp(Message rsp) {
        this.lastRsp = rsp;
        writeAndFlush_FireExceptOnFail(rsp);
    }

    //检查是否为重复消息，如果是则直接发送上次的回复并返回True；否则返回False
    public boolean procRepeatedRequest(Message req) {
        if (lastTermReq != null && lastTermReq.getSeqNo() == req.getSeqNo() && lastRsp != null) {
            sendRsp(lastRsp);
            return true;
        }
        return false;
    }

    //------------------------------------------------------------------
    //判断消息是否为Request
    public boolean isTermReq(Message message) {
        boolean bRet = false;
        short cmd_id_value = message.getCmdId();
        switch(message.getTag()) {
            case Message.ToMtTag:   //Server=>Mt
            {
                if (cmd_id_value > MtProtoMsg.CMDID.START_SERVER2TERM_VALUE &&
                        cmd_id_value < MtProtoMsg.CMDID.END_SERVER2TERM_VALUE) {
                    bRet = true;
                }
            }
            break;

            case Message.ToSpTag:   //Server=>Sp
            {
                SpBinaryMsg.CmdId cmdId = SpBinaryMsg.CmdId.valueOf(cmd_id_value);
                if (cmdId != null)
                    switch(cmdId) {
                        case CMD_SP_SIM_IMG:
                        case CMD_SP_SIM_APDU: {
                            bRet = true;
                        }
                        break;
                    }
            }
            break;
        }
        return bRet;
    }

    //判断消息是否为上一请求的Response
    public boolean isTermRsp(Message rsp) {
        if (rsp != null) {
            if (resendFuture != null &&
                    rsp.getCmdId() == lastReq.getCmdId() &&
                    rsp.getSeqNo() == lastReq.getSeqNo()) {   // the CmdId & SeqNo of rsp should match the cmd
                return true;
            }
        }

        return false;
    }

    //--------------------------------------------------------------------------------
    //对ctx.writeAndFlush的带Exception检测的封装
    private void writeAndFlush_FireExceptOnFail(Object obj) {
        ChannelFuture f = ctx.writeAndFlush(obj);
        f.addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);    //if any error occur in writeAndFlush, will fire exception can caught by 'exceptionCaught'
    }

    //发送命令(可选是否需要超时重发)，如果当前有命令正在处理，将覆盖
    public boolean sendCmd(Message message, boolean bResend) {
        //If channel is closed, ignore
        if (!ctx.channel().isOpen()) {
            logger.warn(ctx, "[sendCmd] channel not isOpen!");
            return false;
        }

        //取消上次命令
        if (resendFuture != null) {
            logger.warn(ctx, "[lastReq] is waiting rsp:" + lastReq + "!Will override");
            cancelSendingCmd();
        }

        //发送当前命令
        lastReq = message;
        if (lastReq != null) {
            if (bResend) {
                resendFuture = ctx.executor().schedule(this, resend_timeout_seconds, TimeUnit.SECONDS);
            }
            writeAndFlush_FireExceptOnFail(lastReq);
            return true;
        }

        logger.debug(ctx, "[_sendCmd] No lastReq!");
        return false;
    }

    //发送命令(不进行超时重发的管理)
    private boolean sendCmd(Message message) {
        return sendCmd(message, true);
    }

    //取消重发Timer
    public void cancelSendingCmd() {
        logger.debug(ctx, "[cancelSendingCmd]");

        try {
            if (resendFuture != null) {
                resendFuture.cancel(true);
                resendFuture = null;
                curr_resend_times = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //超时，进行命令重发
    @Override
    public void run() {
        if (!ctx.channel().isOpen()) {
            logger.debug(ctx, "ResendCmd failed. ctx closed!!!");
            return;
        }

        if (curr_resend_times < max_resend_times) {
            curr_resend_times++;
            if (!sendCmd(lastReq)) {
                logger.debug(ctx, "ResendCmd failed!!!");
            }
        } else {
            logger.warn(ctx, "MaxResendTimes reached!!!");
            ctx.channel().close();
        }
    }

    //=================================================================================
    private void setWaitResult(WaitResult result) {
        waitResult = result;
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    public static String getMsgCmdIdName(Message message) {
        String cmdIdName = null;
        switch (message.getTag()) {
            case Message.MtTag:
            case Message.ToMtTag:
                cmdIdName = MtProtoMsg.CMDID.forNumber(message.getCmdId()).name();
                break;

            case Message.SpTag:
            case Message.ToSpTag:
                cmdIdName = SpBinaryMsg.CmdId.valueOf(message.getCmdId()).name();
                break;
        }
        return cmdIdName;
    }

    //返回是否匹配，ASSERT结果由TestException返回。
    private boolean checkTermMessage(Message message) throws TestException {
        //消息不匹配
        if (assertMessage == null || !assertMessage.getCmdId().equals(getMsgCmdIdName(message))) {
            return false;
        }

        //序列号不匹配
        if (assertMessage.getSeqNo() != null && assertMessage.getSeqNo() != message.getSeqNo()) {
            throw new TestException("assertMessage", "seq_no", assertMessage.getSeqNo(), message.getSeqNo());
        }

        //断言结果
        try {
            if (assertMessage.doAssert(message)) {
                setWaitResult(WaitResult.WAIT_RESULT_ASSERT_PASS);
            } else {
                setWaitResult(WaitResult.WAIT_RESULT_ASSERT_FAIL);
            }
        } catch (TestException e) {
            setWaitResult(WaitResult.WAIT_RESULT_EXCEPTION);
            throw(e);
        }

        return true;
    }

    private boolean checkLastTermMessage() throws TestException {
        boolean bMatch = false;

        Message termMessage;
        String info;
        if (bWaitReq) {
            termMessage = lastTermReq;
            info = "TermReq";
        } else {
            termMessage = lastTermRsp;
            info = "TermRsp";
        }
        logger.debug("before check last " + info + " message");
        if (termMessage != null) {
            logger.debug("check last " + info + " message SUCC!");
            bMatch = checkTermMessage(termMessage);
            logger.debug("after check last " + info + " message");
        } else {
            logger.debug("last " + info + " message is NULL!!!");
        }

        return bMatch;
    }

    private void waitAndAssertTermMessage(AssertMessage assertMessage, boolean bWaitReq) throws TestException {
        //保存参数
        this.assertMessage = assertMessage;
        this.bWaitReq = bWaitReq;

        try {
            //等待req
            waitFuture = ctx.executor().schedule(new Runnable() {
                @Override
                public void run() {
                    setWaitResult(WaitResult.WAIT_RESULT_TIMEOUT);
                }
            }, wait_timeout_seconds, TimeUnit.SECONDS);

            synchronized (waitObject) {
                if (!checkLastTermMessage()) {
                    logger.debug(ctx,"startWaiting...");
                    waitObject.wait();
                    logger.debug(ctx,"[afterWaiting]");

                    if (waitResult == WaitResult.WAIT_RESULT_UNKNOWN) {
                        logger.debug(ctx, "will check last Term message");
                        checkLastTermMessage();
                    } else {
                        logger.debug(ctx, "waitResult:" + waitResult);
                    }
                }
            }
        } catch (InterruptedException e) {

        } finally {
            waitFuture.cancel(false);
            //TODO: find a better way to clear these
            lastTermRsp = null;
        }

        if (waitResult != WaitResult.WAIT_RESULT_ASSERT_PASS) {
            logger.debug(ctx, String.format("wait result NOT pass!!!:cmdUtils:%d, waitObject:%d",
                    this.hashCode(),
                    waitObject.hashCode()
            ));
            throw new TestException("assertMessage", "", "pass", waitResult);
        }

        //reset WaitResult
        waitResult = WaitResult.WAIT_RESULT_UNKNOWN;
    }

    //----------------  PUBLIC -----------------------
    public void sendCmdAndAssertRsp(Message message, boolean bWaitRsp, AssertMessage assertMessage) throws TestException {
        if(sendCmd(message, true)) {
            if (bWaitRsp && assertMessage != null) {
                waitAndAssertTermMessage(assertMessage, false);
            }
        } else {
            throw new TestException("sendCmdAndAssertRsp", "sendCmd", "succ", "fail");
        }
    }

    public void waitAndAssertReq(AssertMessage assertMessage) throws TestException {
        waitAndAssertTermMessage(assertMessage, true);
    }

    public void waitAndAssertRsp(AssertMessage assertMessage) throws TestException {
        waitAndAssertTermMessage(assertMessage, false);
    }

    public void setLastTermRsp(Message rsp) {
        synchronized (waitObject) {
            lastTermRsp = rsp;
            waitObject.notify();
        }
    }

    //请求消息
    public void setLastTermReq(Message req) {
        synchronized (waitObject) {
            lastTermReq = req;
            waitObject.notify();
        }
    }

}
