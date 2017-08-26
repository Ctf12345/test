package com.ctf.ass.server.utils;

import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.GlobalConfig;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/****************** 通讯机制概述 ***************************
 * cmd_queue(本方主动发送的命令的缓存队列)
 * 通讯机制是一问一答。在上一命令没有被处理前，不应该再发送下一命令。
 * 所以需要建立命令队列，将需要发送的命令排队依次发送。
 *
 * waiting_req(对方主动发送的命令的缓存队列)
 * 服务器：在本方命令没有被处理前，对方上发的Req将缓存，暂时不予处理。待收到本方命令的回复后，再处理挂起的req并回复。
 * 客户端：在本方命令没有被处理前，如收到服务器下发的req，将立即处理并回复。
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

    private ChannelHandlerContext ctx;
    private boolean waitClose;             //为true时，待当前命令处理结束后应关闭ctx。该标志用于配合发送TermLogout实现强制退录

    private short cmd_seq_no;              //主动发送的cmd的序列号
    private short req_seq_no;              //被动接收的req的序列号
    private short ind_seq_no;              //被动接收的ind的序列号

    private LinkedList<Cmd> cmd_queue;      //命令队列
    private Message waiting_req;            //等待的请求
    private Cmd currCmd;                    //当前命令
    private Message lastRsp;                //上次请求的回复

    private ScheduledFuture future;         //定时任务，用于超时重发
    private int curr_resend_times;        //当前命令重发次数

    //初始化ctx
    //1.初始化一个CmdUtils对象，将其保存在ctx的attr中。           2.将ctx添加到allCtxs中
    public static void init(ChannelHandlerContext ctx) {
        Attribute<CmdUtils> attrCmdUtils = ctx.channel().attr(CMD_UTILS_KEY);
        attrCmdUtils.setIfAbsent(new CmdUtils(ctx));
    }

    //从ctx中获取CmdUtils对象
    public static CmdUtils get(ChannelHandlerContext ctx) {
        Attribute<CmdUtils> attr = ctx.channel().attr(CMD_UTILS_KEY);
        return attr.get();
    }

    //--------------------------------------------------------------------------------------------
    //私有构造函数
    private CmdUtils(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.waitClose = false;

        this.cmd_seq_no = 0;
        this.req_seq_no = -1;
        this.ind_seq_no = -1;

        this.cmd_queue = new LinkedList<>();
        this.waiting_req = null;
        this.currCmd = null;
        this.lastRsp = null;

        this.future = null;
        this.curr_resend_times = 0;
    }

    //----------------------------------------------------------------------
    //等待关闭 标志
    public boolean getWaitClose() {
        return waitClose;
    }

    public void setWaitClose() {
        cmd_queue.clear();  //为避免负作用，设置等待关闭标志前，需要清除命令队列
        waitClose = true;
    }

    //获取并自增 命令 序列号
    public short getIncCmdSeqNo() {
        short old_seq_no = this.cmd_seq_no;
        this.cmd_seq_no = (short) ((cmd_seq_no + 1) & 0x7FFF);
        return old_seq_no;
    }

    //请求消息 序列号
    public short getMsgSeqNo() {
        return this.req_seq_no;
    }

    public void setMsgSeqNo(short msg_seq_no) {
        this.req_seq_no = msg_seq_no;
    }

    //通知消息 序列号
    public boolean isRepeatedInd(Message ind) {
        if (ind.getSeqNo() == ind_seq_no) {
            return true;
        }

        return false;
    }

    //等待的请求消息
    public void setWaitingRequest(Message message) {
        waiting_req = message;
    }

    public Message getWaitingRequest() {
        Message ret = waiting_req;
        waiting_req = null;
        return ret;
    }

    //获取当前命令
    public Message getCurrCmdMessage() {
        Message message = null;

        if (currCmd != null) {
            message = currCmd.getMessage();
        }

        return message;
    }

    //------------------------------------------------------------------
    //判断消息是否为Indication
    public boolean isIndMessage(Message message) {
        boolean bRet = false;
        short cmd_id_value = message.getCmdId();

        switch (message.getTag()) {
            case Message.MtTag:     //Mt=>Server
                if (cmd_id_value > MtProtoMsg.CMDID.END_SERVER2TERM_VALUE) {
                    bRet = true;
                }
                break;

            case Message.SpTag:     //Sp=>Server
                if (cmd_id_value == SpBinaryMsg.CmdId.CMD_SP_SIM_APDU.value()) {
                    bRet = true;
                }
                break;

            default:
                break;
        }

        return bRet;
    }

    //判断消息是否为Request
    public boolean isReqMessage(Message message) {
        boolean bRet = false;
        short cmd_id_value = message.getCmdId();
        switch (message.getTag()) {
            case Message.MtTag:     //Mt=>Server
            {
                if (cmd_id_value > MtProtoMsg.CMDID.START_TERM2SERVER_VALUE &&
                        cmd_id_value < MtProtoMsg.CMDID.END_TERM2SERVER_VALUE) {  //req
                    bRet = true;
                }
            }
            break;

            case Message.SpTag:     //Sp=>Server
            {
                SpBinaryMsg.CmdId cmdId = SpBinaryMsg.CmdId.valueOf(cmd_id_value);
                if (cmdId != null) {
                    switch (cmdId) {
                        case CMD_SP_LOGIN:
                        case CMD_SP_SIM_INFO:
                        case CMD_SP_SIM_INFO_UPDATE:
                        case CMD_SP_HEARTBEAT: {
                            bRet = true;
                        }
                        break;

                        default:
                            break;
                    }
                }
            }
            break;
        }
        return bRet;
    }

    //判断消息是否为上一命令的Response
    public boolean isCmdRsp(Message rsp) {
        if (rsp != null) {
            if (future != null &&
                    rsp.getCmdId() == currCmd.getMessage().getCmdId() &&
                    rsp.getSeqNo() == currCmd.getMessage().getSeqNo()) {   // the CmdId & SeqNo of rsp should match the cmd
                return true;
            }
        }

        return false;
    }

    //发送回复
    public void sendRsp(Message rsp) {
        this.lastRsp = rsp;
        writeAndFlush_FireExceptOnFail(rsp);
    }

    //检查是否为重复消息，如果是则直接发送上次的回复并返回True；否则返回False
    public boolean procRepeatedRequest(Message req) {
        if (req.getSeqNo() == req_seq_no) {
            sendRsp(lastRsp);
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------
    //对ctx.writeAndFlush的带Exception检测的封装
    private void writeAndFlush_FireExceptOnFail(Object obj) {
        ChannelFuture f = ctx.writeAndFlush(obj);
        f.addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);    //if any error occur in writeAndFlush, will fire exception can caught by 'exceptionCaught'
    }

    //发送当前命令
    private boolean _sendCmd() {
        if (currCmd != null) {
            future = ctx.executor().schedule(this, GlobalConfig.RESEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            writeAndFlush_FireExceptOnFail(currCmd.getMessage());
            return true;
        }

        logger.debug(ctx, "[_sendCmd] No currCmd!");
        return false;
    }

    //发送命令队列中下一命令
    public boolean sendNextCmd() {
        boolean ret = false;
        Cmd cmd = null;

        if (cmd_queue.size() > 0) {
            cmd = cmd_queue.pop();
        }
        currCmd = cmd;
        curr_resend_times = 0;
        if (cmd != null) {
            ret = _sendCmd();
        }

        return ret;
    }

    //从Http触发的命令发送。与sendCmd不同在于，需要记录httpCtx，在收到客户端的回复后，将通过httpCtx对Http请求进行回复
    public boolean httpSendCmd(ChannelHandlerContext httpCtx, Message message) {
        //If channel is closed, ignore
        if (!ctx.channel().isOpen()) {
            return false;
        }

        Cmd cmd = new Cmd(message, false, httpCtx);
        if (future == null) {
            currCmd = cmd;
            curr_resend_times = 0;
            return _sendCmd();
        } else {
            cmd_queue.addFirst(cmd);
            logger.debug(ctx, "[httpSendCmd] insert to cmd_queue!");
            return true;
        }
    }

    //发送消息(不进行超时重发的管理)
    public boolean sendMessage(Message message) {
        if (message != null) {
            writeAndFlush_FireExceptOnFail(message);
            return true;
        }

        return false;
    }

    //发送命令(带超时重发机制)，如果当前有命令正在处理，将插入到后台命令之前
    public boolean sendCmd(Message message) {
        //If channel is closed, ignore
        if (!ctx.channel().isOpen()) {
            logger.warn(ctx, "[sendCmd] channel not isOpen!");
            return false;
        }

        Cmd cmd = new Cmd(message, false);
        if (future == null) {
            currCmd = cmd;
            curr_resend_times = 0;
            return _sendCmd();
        } else {
            int index;
            for (index = 0; index < cmd_queue.size(); index++) {
                if (cmd_queue.get(index).getIsBgCmd()) {
                    break;
                }
            }
            cmd_queue.add(index, cmd);
            logger.debug(ctx, "[sendCmd] insert to cmd_queue!");
            return true;
        }
    }

    //发送后台命令(带超时重发机制)，如果当前有命令正在处理，当插入到命令队列结尾
    public boolean sendBgCmd(Message message) {
        //If channel is closed, ignore
        if (!ctx.channel().isOpen()) {
            return false;
        }

        Cmd cmd = new Cmd(message, true);
        if (future == null) {
            currCmd = cmd;
            curr_resend_times = 0;
            return _sendCmd();
        } else {
            logger.debug(ctx, "[sendBgCmd] append to cmd_queue!");
            cmd_queue.addLast(cmd);
            return false;
        }
    }

    //获取当前命令的httpCtx
    public ChannelHandlerContext getCurrCmdHttpCtx() {
        if (currCmd != null) {
            return currCmd.getHttpCtx();
        }
        return null;
    }

    //取消重发Timer。 A)已收到回复  B)检测到致命错误(将消除消息队列)
    public void cancelSendingCmd(boolean bSucc) {
        logger.debug(ctx, "[cancelSendingCmd] " + bSucc);

        try {
            if (future != null) {
                future.cancel(true);
                future = null;
                curr_resend_times = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!bSucc) {
            cmd_queue.clear();
        }
    }

    //超时，进行命令重发
    @Override
    public void run() {
        if (!ctx.channel().isOpen()) {
            logger.debug(ctx, "ResendCmd failed. ctx closed!!!");
            return;
        }

        if (curr_resend_times < GlobalConfig.RESEND_MAX_TIMES) {
            curr_resend_times++;
            if (!_sendCmd()) {
                logger.debug(ctx, "ResendCmd failed!!!");
            }
        } else {
            logger.warn(ctx, "MaxResendTimes reached!!!");
            ctx.channel().close();
        }
    }

    private static final class Cmd {
        private Message message;                    //要发送的消息
        private boolean isBgCmd;                   //是否为后台命令
        private ChannelHandlerContext httpCtx;      //如果命令来自web请求，则记录http连接的ctx

        Cmd(Message message, boolean bBgCmd) {
            this(message, bBgCmd, null);
        }

        Cmd(Message message, boolean isBgCmd, ChannelHandlerContext httpCtx) {
            this.message = message;
            this.isBgCmd = isBgCmd;
            this.httpCtx = httpCtx;
        }

        Message getMessage() {
            return message;
        }

        boolean getIsBgCmd() {
            return isBgCmd;
        }

        ChannelHandlerContext getHttpCtx() {
            return httpCtx;
        }
    }
}
