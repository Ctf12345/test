package com.ctf.autotest;

import com.ctf.ass_codec.utils.Cloud88StorageUtils;
import com.ctf.autotest.MockEnv.MockClientState;
import com.ctf.ass_codec.mt_body.*;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg.CMDID;
import com.ctf.ass_codec.protobuf.MtProtoMsg.LEVEL;
import com.ctf.ass_codec.protobuf.MtProtoMsg.TermLogUploadRequest.UPLOAD;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class MtMsgHandler extends ChannelInboundHandlerAdapter {
    private static LogWrapper logger = LogWrapper.getLogger(MtMsgHandler.class.getName());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CmdUtils.init(ctx);
        MockEnv.pushCtx(ctx);

        logger.debug(ctx, "channelActive");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
        if (object instanceof Message) {
            Message message = (Message) object;
            CmdUtils cmdUtils = CmdUtils.get(ctx);
            MockMt mockMt = MockEnv.getMockMt(ctx);

            if (cmdUtils.isTermRsp(message)) {   //如果是对当前命令的回复消息，则取消命令重发。
                cmdUtils.cancelSendingCmd();
                cmdUtils.setLastTermRsp(message);

                procMsg(ctx, message);
            } else if (cmdUtils.isTermReq(message)) {  //收到请求消息
                logger.debug("TermReq " + mockMt);
                if (!mockMt.getIsAutoRsp()) {    //如果是忙状态，则不响应命令
                    //保存接收到的seq_no，以便回复时使用同样的seq_no
                    CmdUtils.get(ctx).setLastTermReq(message);
                    logger.debug("Mt client isAutoRsp==False! Won't process recvived message:" + message);
                    return;
                }

                if (cmdUtils.procRepeatedRequest(message)) {  //重复消息直接返回上次回复，保证消息的幂等性
                    logger.debug(ctx, "Repeated message:" + message);
                } else {
                    //保存接收到的seq_no，以便回复时使用同样的seq_no
                    CmdUtils.get(ctx).setLastTermReq(message);

                    procMsg(ctx, message);
                }
            } else {
                logger.debug(ctx, "Unexpected message:" + message);
            }
        }
        else {
            logger.warn(ctx, "Not a valid Message! fireChannelRead!!!");
            ctx.fireChannelRead(object);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        MockMt mockMt = MockEnv.getMockMt(ctx);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state() == IdleState.ALL_IDLE) {
                logger.warn(ctx, MockMt.MT_CLIENT_HEARTBEAT_TIMEOUT_SECONDS  + " seconds heartbeat timeout!");
                if (mockMt.getMockClientState() != MockEnv.MockClientState.CLIENT_STATE_CONNECTED && mockMt.getIsAutoHeartBeat()) {    //如果至少已登录，且不忙。才发心跳包
                    mockMt.heartbeat(false, "ERR_OK", "");
                } else {
                    logger.debug(ctx, "Mt client is busy. Won't send heartbeat.");
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn(ctx, "MtMsgHandler channelInactive!");
        ctx.fireChannelInactive();

        MockMt mockMt = MockEnv.getMockMt(ctx);
        if (mockMt != null) {
            mockMt.onClose();
        }

        CmdUtils.deInit(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(ctx, "MtMsgHandler exceptionCaught!", cause.fillInStackTrace());
        cause.printStackTrace();

        ctx.close();
        //ctx.fireExceptionCaught(cause);
    }

    public void procMsg(ChannelHandlerContext ctx, Message message)
    {
        //打印log
        StringBuffer sb = new StringBuffer();
        sb.append("Server=>Mt ");
        sb.append(message);
        logger.debug(ctx, sb.toString());

        // 处理MT相关消息
        CMDID cmd_id = CMDID.valueOf(message.getCmdId());
        switch(cmd_id) {
            //=========  Request  ==========
            case TERMINFO:                  //终端 信息
                procMtTermInfoReq(ctx, message);
                break;

            case TERMREVUIM:               //终端 强制还卡
                procMtTermRevUimReq(ctx, message);
                break;

            case TERMLOGOUT:               //终端 强制退录
                procMtTermLogoutReq(ctx, message);
                break;

            case TERMASSIGNUIM:           //终端 主动分卡
                procMtTermAssignUimReq(ctx, message);
                break;

            case TERMTRACE:               //终端 实时trace控制
                procMtTermTraceReq(ctx, message);
                break;

            case TERMLOGUPLOAD:         //终端 log上传
                procMtTermLogUploadReq(ctx, message);
                break;

            //=========  Response  ==========
            case LOGIN:            //终端 登录
                procMtLoginRsp(ctx, message);
                break;

            case LOGOUT:          //终端 退录
                procMtLogoutRsp(ctx, message);
                break;

            case REQUIM:          //终端 分卡
                procMtReqUimRsp(ctx, message);
                break;

            case REVUIM:          //终端 还卡
                procMtRevUimRsp(ctx, message);
                break;

            case LOGINBYID:       //终端 使用session ID登录
                procMtLoginByIdRsp(ctx, message);
                break;

            case SIMIMG:          //终端  SIM卡镜像
                procMtSimImgRsp(ctx, message);
                break;

            case SIMPOOL:          //终端 发命令给SimPool
                procMtSimPoolRsp(ctx, message);
                break;

            case HEARTBEAT:        //终端 心跳包回复
                procMtHeartBeatRsp(ctx, message);
                break;

            case LOGUPLOADRET:    //终端 日志上传回复
                procMtLogUploadRetRsp(ctx, message);
                break;

            case ALERTS:           //终端 警告信息回复
                procMtAlertRsp(ctx, message);
                break;

            default:
                procMtBadMsg(ctx, message);    //终端 错误消息
                break;
        }
    }

    //===========================  Request  =============================

    //----------------------------------------------------------------------------
    //终端 信息
    //-----------------
    private void procMtTermInfoReq(ChannelHandlerContext ctx, Message message) {
        MtTermInfoReq mtTermInfoReq = (MtTermInfoReq)message.getBody();
        MockMt mockMt = MockEnv.getMockMt(ctx);

        Message rsp = buildMtTermInfoRsp(message.getSeqNo(),
                mockMt.getSerialNo(),
                mockMt.getModel(),
                mockMt.getSwVer(),
                mockMt.getBuildNo(),
                mockMt.getIniVer());
        CmdUtils.get(ctx).sendRsp(rsp);
    }

    private Message buildMtTermInfoRsp(short seq_no, String serialno, String model, String sw_ver, String buildno, String ini_ver) {
        MtTerm term = new MtTerm(serialno, model, buildno, sw_ver, ini_ver, MtProtoMsg.VERSION.PROTO_VALUE, ErrCode.VERSION);
        MtTermInfoRsp mtTermInfoRsp = new MtTermInfoRsp(term);
        return new Message(Message.MtTag, (short)CMDID.TERMINFO_VALUE, seq_no, (byte)0, mtTermInfoRsp);
    }

    //----------------------------------------------------------------------------
    //终端 强制还卡
    //-----------------
    private void procMtTermRevUimReq(ChannelHandlerContext ctx, Message message) {
        MockMt mockMt = MockEnv.getMockMt(ctx);
        MockEnv.MockClientState mockClientState = mockMt.getMockClientState();

        ServerCode errCode = ServerCode.ERR_OK;
        switch(mockClientState) {
            case CLIENT_STATE_ASSIGNED:
                {
                    MtTermRevUimReq mtTermRevUimReq = (MtTermRevUimReq)message.getBody();
                    mtTermRevUimReq.getCause().getCode();
                    int type = mtTermRevUimReq.getCause().getType();

                    //如果不可拒绝
                    if (type == 0) {
                        errCode = ServerCode.ERR_OK;
                    } else {
                        errCode = ServerCode.USER_OPERATING;
                    }
                }
                break;

            case CLIENT_STATE_LOGINED:
                errCode = ServerCode.ERR_OK;
                break;

            case CLIENT_STATE_CONNECTED:
                errCode = ServerCode.ERR_USER_NOT_LOGIN;
                break;
        }

        Message rsp = buildMtTermRevUimRsp(message.getSeqNo(), errCode.value());
        CmdUtils.get(ctx).sendRsp(rsp);
    }

    private Message buildMtTermRevUimRsp(short seq_no, int code) {
        MtCodes result = new MtCodes(code);

        MtTermRevUimRsp mtTermRevUimRsp = new MtTermRevUimRsp(result);
        return new Message(Message.MtTag, (short)CMDID.TERMREVUIM_VALUE, seq_no, (byte)0, mtTermRevUimRsp);
    }

    //----------------------------------------------------------------------------
    //终端 强制退录
    //-----------------
    private void procMtTermLogoutReq(ChannelHandlerContext ctx, Message message) {
        MockMt mockMt = MockEnv.getMockMt(ctx);
        MockEnv.MockClientState mockClientState = mockMt.getMockClientState();

        ServerCode errCode = ServerCode.ERR_OK;
        switch(mockClientState) {
            case CLIENT_STATE_ASSIGNED:
                MockEnv.getMockMt(ctx).clientSyncToState(MockClientState.CLIENT_STATE_LOGINED);
            case CLIENT_STATE_LOGINED:
                MtTermLogoutReq mtTermLogoutReq = (MtTermLogoutReq) message.getBody();
                mtTermLogoutReq.getCause().getCode();
                errCode = ServerCode.ERR_OK;
                break;

            case CLIENT_STATE_CONNECTED:
                errCode = ServerCode.ERR_OK;
                break;
        }

        Message rsp = buildMtTermLogoutRsp(message.getSeqNo(), errCode.value());
        CmdUtils.get(ctx).sendRsp(rsp);
    }

    private Message buildMtTermLogoutRsp(short seq_no, int code) {
        MtCodes result = new MtCodes(code);

        MtTermLogoutRsp mtTermLogoutRsp = new MtTermLogoutRsp(result);
        return new Message(Message.MtTag, (short)CMDID.TERMLOGOUT_VALUE, seq_no, (byte)0, mtTermLogoutRsp);
    }

    //----------------------------------------------------------------------------
    //终端 主动分卡
    //-----------------
    private void procMtTermAssignUimReq(ChannelHandlerContext ctx, Message message) {
        MockMt mockMt = MockEnv.getMockMt(ctx);
        MockEnv.MockClientState mockClientState = mockMt.getMockClientState();
        ServerCode errCode = ServerCode.ERR_OK;

        MtTermAssignUimReq mtTermAssignUimReq = (MtTermAssignUimReq)message.getBody();
        MtUim uim = mtTermAssignUimReq.getUim();
        byte[] sessionId = mtTermAssignUimReq.getSessionId();

        switch(mockClientState) {
            case CLIENT_STATE_ASSIGNED:
                //终端检查本机分卡信息是否与服务器一致，如一致直接回复OK
                if (sessionId.equals(MockEnv.getMockMt(ctx).getSessionId())) {
                    errCode = ServerCode.ERR_OK;
                    break;
                }

                //终端侧还卡，切换到<已登录>状态，再继续处理
                MockEnv.getMockMt(ctx).clientSyncToState(MockClientState.CLIENT_STATE_LOGINED);
            case CLIENT_STATE_LOGINED:
                //如有好的理由，终端可拒绝

                //否则终端应接受
                mockMt.setImsi(uim.getImsi());
                mockMt.cacheImgMd5(uim.getImg_md5());
                mockMt.setSessionId(sessionId);
                errCode = ServerCode.ERR_OK;
                break;

            case CLIENT_STATE_CONNECTED:
                errCode = ServerCode.ERR_USER_NOT_LOGIN;
                break;
        }

        Message rsp = buildMtTermAssignUimRsp(message.getSeqNo(), errCode.value());
        CmdUtils.get(ctx).sendRsp(rsp);
    }

    private Message buildMtTermAssignUimRsp(short seq_no, int code) {
        MtCodes result = new MtCodes(code);

        MtTermAssignUimRsp mtTermAssignUimRsp = new MtTermAssignUimRsp(result);
        return new Message(Message.MtTag, (short)CMDID.TERMASSIGNUIM_VALUE, seq_no, (byte)0, mtTermAssignUimRsp);
    }

    //----------------------------------------------------------------------------
    //终端 实时trace控制
    //-----------------
    private void procMtTermTraceReq(ChannelHandlerContext ctx, Message message) {
        MtTermTraceReq mtTermTraceReq = (MtTermTraceReq)message.getBody();
        boolean bOn = (mtTermTraceReq.getStatus() != 0);
        LEVEL level = mtTermTraceReq.getLevel();

        //保存设置
        MockEnv.getMockMt(ctx).setIsTraceOn(bOn);

        Message rsp = buildMtTermTraceRsp(message.getSeqNo(), ServerCode.ERR_OK.value());
        CmdUtils.get(ctx).sendRsp(rsp);
    }

    private Message buildMtTermTraceRsp(short seq_no, int code) {
        MtCodes result = new MtCodes(code);

        MtTermTraceRsp mtTermTraceRsp = new MtTermTraceRsp(result);
        return new Message(Message.MtTag, (short)CMDID.TERMTRACE_VALUE, seq_no, (byte)0, mtTermTraceRsp);
    }

    //----------------------------------------------------------------------------
    //终端 log上传
    //-----------------
    private void procMtTermLogUploadReq(ChannelHandlerContext ctx, Message message) {
        MtTermLogUploadReq mtTermLogUploadReq = (MtTermLogUploadReq)message.getBody();
        int s_second = mtTermLogUploadReq.getS_second();
        int e_second = mtTermLogUploadReq.getE_second();
        LEVEL level = mtTermLogUploadReq.getLevel();
        String logpath = mtTermLogUploadReq.getLogPath();

        int size = 20480;
        Message rsp = buildMtTermLogUploadRsp(message.getSeqNo(), ServerCode.ERR_OK.value(), size);
        CmdUtils.get(ctx).sendRsp(rsp);

//        MockMt mockMt = MockEnv.getMockMt(ctx);
//        byte[] data = mockMt.getLogData(s_second, e_second, level);
//        boolean ret = Cloud88StorageUtils.doHttpPut(logpath, "ruimd.log");

        //send ret ind
        //mockMt.logUploadRet(logpath, ret ? 0 : 1, null);
    }

    private Message buildMtTermLogUploadRsp(short seq_no, int code, int size) {
        MtCodes result = new MtCodes(code);
        MtTermLogUploadRsp mtTermLogUploadRsp;
        if (code == 0) {
            mtTermLogUploadRsp = new MtTermLogUploadRsp(result, size);
        } else {
            mtTermLogUploadRsp = new MtTermLogUploadRsp(result);
        }

        return new Message(Message.MtTag, (short)CMDID.TERMLOGUPLOAD_VALUE, seq_no, (byte)0, mtTermLogUploadRsp);
    }

    //===========================  Response  =============================

    //----------------------------------------------------------------------------
    //终端 登录
    //-----------------
    private void procMtLoginRsp(ChannelHandlerContext ctx, Message message) {
        MtLoginRsp mtLoginRsp = (MtLoginRsp)message.getBody();
        int code = mtLoginRsp.getResult().getCode();

        ServerCode serverCode = ServerCode.valueOf((short)code);
        switch(serverCode) {
            case ERR_OK:
                MockEnv.getMockMt(ctx).clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_LOGINED);
                break;

            default:
                break;
        }
    }

    //----------------------------------------------------------------------------
    //终端 退录
    //---------------------
    private void procMtLogoutRsp(ChannelHandlerContext ctx, Message message) {
        MtLogoutRsp mtLogoutRsp = (MtLogoutRsp)message.getBody();
        MtCodes result = mtLogoutRsp.getResult();
        int code = result.getCode();

        ServerCode serverCode = ServerCode.valueOf((short)code);
        switch(serverCode) {
            case ERR_OK:
                MockEnv.getMockMt(ctx).clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_CONNECTED);
                break;
        }
    }

    //----------------------------------------------------------------------------
    //终端 分卡
    //---------------------
    private void procMtReqUimRsp(ChannelHandlerContext ctx, Message message) {
        MtReqUimRsp mtReqUimRsp = (MtReqUimRsp)message.getBody();
        MtCodes result = mtReqUimRsp.getResult();

        switch(ServerCode.valueOf((short)result.getCode())) {
            case ERR_OK:
                {
                    MtUim uim = mtReqUimRsp.getUim();
                    byte[] sessionId = mtReqUimRsp.getSessionId();

                    MockEnv.getMockMt(ctx).setImsi(uim.getImsi());
                    MockEnv.getMockMt(ctx).setSessionId(sessionId);

                    if (!MockEnv.getMockMt(ctx).isSimImgCached(uim.getImg_md5())) {
                        MockEnv.getMockMt(ctx).cacheImgMd5(uim.getImg_md5());
                        MockEnv.getMockMt(ctx).clientSyncToState(MockClientState.CLIENT_STATE_ASSIGNED);
                    } else {
                        MockEnv.getMockMt(ctx).clientSyncToState(MockClientState.CLIENT_STATE_ASSIGNED);
                    }
                }
                break;

            case ERR_USER_NOT_LOGIN:
                MockEnv.getMockMt(ctx).clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_CONNECTED);
                break;
        }
    }

    //----------------------------------------------------------------------------
    //终端 还卡
    //---------------------
    private void procMtRevUimRsp(ChannelHandlerContext ctx, Message message) {
        MockMt mockMt = MockEnv.getMockMt(ctx);
        MtRevUimRsp mtRevUimRsp = (MtRevUimRsp)message.getBody();
        MtCodes result = mtRevUimRsp.getResult();

        switch(ServerCode.valueOf((short)result.getCode())) {
            case ERR_OK:
                mockMt.clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_LOGINED);
                break;

            case ERR_USER_NOT_LOGIN:
                mockMt.clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_CONNECTED);
                break;
        }
    }

    //----------------------------------------------------------------------------
    //终端 使用session ID登录
    //---------------------
    private void procMtLoginByIdRsp(ChannelHandlerContext ctx, Message message) {
        MtLoginByIdRsp mtLoginByIdRsp = (MtLoginByIdRsp)message.getBody();
        MtCodes result = mtLoginByIdRsp.getResult();

        MockMt mockMt = MockEnv.getMockMt(ctx);
        if (result.getCode() == 0) {
            mockMt.clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_ASSIGNED);
        } else {
            //清除session
            mockMt.clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_CONNECTED);
        }
    }

    //----------------------------------------------------------------------------
    //终端 SIM卡镜像
    //---------------------
    private void procMtSimImgRsp(ChannelHandlerContext ctx, Message message) {
        MtSimImgRsp mtSimImgRsp = (MtSimImgRsp)message.getBody();
        MtCodes result = mtSimImgRsp.getResult();
        if (mtSimImgRsp.hasImg_data()) {
            MockMt mockMt = MockEnv.getMockMt(ctx);
            mockMt.cacheSimImg(mtSimImgRsp.getImg_data());
            mockMt.clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_ASSIGNED);
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 发送命令给simpool
    //-----------------
    private void procMtSimPoolRsp(ChannelHandlerContext ctx, Message message) {
        MtSimPoolRsp mtSimPoolRsp = (MtSimPoolRsp) message.getBody();
        MtCodes result = mtSimPoolRsp.getResult();
        byte[] response = mtSimPoolRsp.getResponse();
    }

    //----------------------------------------------------------------------------
    //处理终端 心跳Ping包
    //-----------------
    private void procMtHeartBeatRsp(ChannelHandlerContext ctx, Message message) {
        MtHeartBeatRsp mtHeartBeatRsp = (MtHeartBeatRsp)message.getBody();
        MtCodes result = mtHeartBeatRsp.getResult();
    }

    //----------------------------------------------------------------------------
    //终端 上传日志
    //---------------------
    private void procMtLogUploadRetRsp(ChannelHandlerContext ctx, Message message) {
        MtLogUploadRetRsp mtLogUploadRetRsp = (MtLogUploadRetRsp)message.getBody();
    }

    //----------------------------------------------------------------------------
    //终端 警告信息
    //---------------------
    private void procMtAlertRsp(ChannelHandlerContext ctx, Message message) {
        MtAlertRsp mtAlertRsp = (MtAlertRsp)message.getBody();
    }

    //----------------------------------------------------------------------------
    //处理终端 错误消息
    //-----------------
    private void procMtBadMsg(ChannelHandlerContext ctx, Message message) {
        logger.warn(ctx, "TestEngine bad msg found:" + message);
    }
}
