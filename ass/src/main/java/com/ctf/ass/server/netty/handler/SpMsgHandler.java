package com.ctf.ass.server.netty.handler;


import com.ctf.ass.server.component.MongoDbUtil;
import com.ctf.ass.server.component.RocketMQMessageSender;
import com.ctf.ass.server.component.ToMtMsg;
import com.ctf.ass.server.component.ToSpMsg;
import com.ctf.ass.server.utils.CmdUtils;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.mt_body.MtTermLogUploadReq;
import com.ctf.ass_public.globals.LogsState;
import com.ctf.ass_public.struct.OpHistory;
import com.ctf.ass_codec.binary.SpBinaryMsg.CmdId;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_codec.sp_body.*;
import com.ctf.ass_codec.struct.*;
import com.ctf.ass_public.utils.ConvUtils;;
import com.ctf.oss.CCM;
import com.ctf.ass_public.struct.OnLineUser;
import com.ctf.oss.SmcService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.channel.ChannelHandler.Sharable;

import java.util.ArrayList;

@Component("spMsgHandler")
@Scope("prototype")
@Sharable
public class SpMsgHandler extends ChannelInboundHandlerAdapter {
    private static LogWrapper logger = LogWrapper.getLogger(SpMsgHandler.class);
    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;
    @Autowired
    private MongoDbUtil mongoDbUtil;
    @Autowired
    private SmcService smcService;
    @Autowired
    private ToMtMsg toMtMsg;
    @Autowired
    private ToSpMsg toSpMsg;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug(ctx, "Got SimPool connection");
        if (mongoDbUtil.isBlackUser(ctx.channel().remoteAddress().toString())) {
            logger.error(ctx, "[server]:A BlackUser,will close connection.please contact server administrator!");
            ctx.close();
        } else {
            CmdUtils.init(ctx);
            CCM.addCtx(CCM.ClientType.SP, ctx);
            rocketMQMessageSender.sendOpHistory(new OpHistory(ctx.channel().remoteAddress().toString(), OpHistory.OpType.SpToSvr_Ind, "connect", 0));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
        if (object instanceof Message) {
            Message message = (Message) object;
            CmdUtils cmdUtils = CmdUtils.get(ctx);

            if (cmdUtils.getCurrCmdMessage() != null) {  //当前命令正在等待回复
                if (cmdUtils.isCmdRsp(message)) {   //如果是对当前命令的回复消息
                    //取消命令重发
                    cmdUtils.cancelSendingCmd(true);
                    //处理回复消息
                    procMsg(ctx, message);
                    //如果已等待关闭，则关闭ctx
                    if (cmdUtils.getWaitClose()) {
                        ctx.close();
                        return;
                    }
                    //下次发命令前，优先处理等待的req
                    Message req = cmdUtils.getWaitingRequest();
                    if (req != null) {
                        if (cmdUtils.procRepeatedRequest(req)) {  //重复的请求消息直接返回上次回复，保证消息的幂等性
                            logger.debug(ctx, "[Sp] Repeated req:" + req);
                        } else {
                            //保存接收到的seq_no，以便回复时使用同样的seq_no
                            CmdUtils.get(ctx).setMsgSeqNo(req.getSeqNo());
                            procMsg(ctx, req);
                        }
                    }
                    //发送命令队列中下一命令(如果有的话)
                    cmdUtils.sendNextCmd();
                } else if (cmdUtils.getWaitClose()) {
                    logger.warn(ctx, "[Sp] Receive message while waiting close:" + message);
                    return;
                } else if (cmdUtils.isReqMessage(message)) {    //收到请求消息
                    cmdUtils.setWaitingRequest(message);    //仅缓存但不立即处理。注意：为避免攻击，仅缓存1个req。正常终端不应连续发Req，发送后应等待回复
                } else if (cmdUtils.isIndMessage(message)) {    //收到通知消息可立即处理，因通知不需要回复
                    if (cmdUtils.isRepeatedInd(message)) {  //重复的通知消息直接忽略，保证消息的幂等性
                        logger.debug(ctx, "[Sp] Repeated ind:" + message);
                    } else {
                        procMsg(ctx, message);
                    }
                } else {
                    logger.warn(ctx, "[Sp] Unexpected message while waiting response:" + message);
                }
            } else {
                if (cmdUtils.getWaitClose()) {
                    logger.warn(ctx, "[Sp] Receive message while waiting close:" + message);
                    return;
                } else if (cmdUtils.isReqMessage(message)) {   //收到请求消息
                    if (cmdUtils.procRepeatedRequest(message)) {  //重复的请求消息直接返回上次回复，保证消息的幂等性
                        logger.debug(ctx, "[Sp] Repeated req:" + message);
                    } else {
                        //保存接收到的seq_no，以便回复时使用同样的seq_no
                        CmdUtils.get(ctx).setMsgSeqNo(message.getSeqNo());
                        procMsg(ctx, message);
                    }
                } else if (cmdUtils.isIndMessage(message)) {    //收到通知消息
                    if (cmdUtils.isRepeatedInd(message)) {  //重复的通知消息直接忽略，保证消息的幂等性
                        logger.debug(ctx, "[Sp] Repeated ind:" + message);
                    } else {
                        procMsg(ctx, message);
                    }
                } else {
                    logger.warn(ctx, "[Sp] Unexpected message while idle:" + message);
                }
            }
        } else {
            logger.warn(ctx, "[Sp] Not a valid Message! fireChannelRead!!!");
            ctx.fireChannelRead(object);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            /*
            IdleStateEvent event = (IdleStateEvent)evt;
            if (event.state() == IdleState.ALL_IDLE) {
                logger.debug(ctx, GlobalConfig.MT_IDLE_TIMEOUT_SECONDS + " seconds no message. Ready to close channel.");
                ctx.close();
            }
            */
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug(ctx, "ChannelInactive!");
        ctx.fireChannelInactive();

        smcService.SpCloseConn(ctx);

        rocketMQMessageSender.sendOpHistory(new OpHistory(ctx.channel().remoteAddress().toString(), OpHistory.OpType.SpToSvr_Ind, "disconn", 0));
        CCM.delCtx(CCM.ClientType.SP, ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(ctx, "ExceptionCaught!", cause.fillInStackTrace());

        ctx.close();
        //ctx.fireExceptionCaught(cause);
    }

        /*
    private static Map<String, Boolean> nodeCheck = new ConcurrentHashMap<String, Boolean>();
    private static String[] whiteList = { "127.0.0.1", "192.168.37.241", "192.168.37.156" };

    String nodeIndex = ctx.channel().remoteAddress().toString();
    Message login_ack = null;

    short msg_id = message.getHead().getMsgSeqNo();
    // 重复登陆，拒绝
    if (nodeCheck.containsKey(nodeIndex)) {
        login_ack = buildSpLoginRsp(msg_id, (byte) 1);
    } else {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = address.getAddress().getHostAddress();
        boolean isOK = false;
        for (String WIP : whiteList) {
            if (WIP.equals(ip)) {
                isOK = true;
                break;
            }
        }

        SpLoginReq spLoginInd = (SpLoginReq)message.getBody();

        login_ack = buildSpLoginRsp(msg_id, isOK ? (byte) 0 : (byte) 1);
        if (isOK) {
            nodeCheck.put(nodeIndex, true);
        }
    }

    ctx.writeAndFlush(login_ack);
    */

    //消息处理
    public void procMsg(ChannelHandlerContext ctx, Message message) {
        //打印log
        StringBuffer sb = new StringBuffer();
        sb.append("Sp=>Server ");
        sb.append(message);
        logger.debug(ctx, sb.toString());

        //保存操作历史
        rocketMQMessageSender.sendOpHistory(ctx, message);

        CmdId cmd_id = CmdId.valueOf(message.getCmdId());
        switch (cmd_id) {
            //=========  Request  ==========
            case CMD_SP_LOGIN:          //SimPool 登录Ind
                procSpLoginReq(ctx, message);
                break;

            case CMD_SP_HEARTBEAT:     //SimPool 心跳ping包
                procSpHeartBeatReq(ctx, message);
                break;

            case CMD_SP_SIM_INFO:       //SimPool SIM卡信息 上报
                procSpSimInfoReq(ctx, message, true);
                break;

            case CMD_SP_SIM_INFO_UPDATE:    //SimPool SIM卡信息 更新
                procSpSimInfoReq(ctx, message, false);
                break;

            case CMD_SP_LOG_UPLOAD_RES: //SimPool 主动上传日志结果
                procSpLogUploadResReq(ctx, message);
                break;

            case CMD_SP_UPGRADE_RES: //SimPool 主动上传软件升级结果
                procSpUpgradeResReq(ctx, message);
                break;
            //=========  Response  ==========
            //=========  Response  ==========
            case CMD_SP_SIM_IMG:        //SimPool SIM卡镜像Ack
                procSpTermSimImgRsp(ctx, message);
                break;

            case CMD_SP_SIM_APDU:       //SimPool SIM卡APDU Ack
                procSpTermSimApduRsp(ctx, message);
                break;

            case CMD_SP_LOG_UPLOAD:     //SimPool 上传日志响应
                procSpTermLogUploadRsp(ctx, message);
                break;

            case CMD_SP_REBOOT:          //SimPool 重启响应
                procSpTermRebootRsp(ctx, message);
                break;

            case CMD_SP_UPGRADE:       //SimPool 软件升级响应
                procSpTermUpgradeRsp(ctx, message);
                break;

            case CMD_SP_TRACE:         //SimPool tree响应
                procSpTermTraceRsp(ctx, message);
                break;

            case CMD_SP_CLEAR_LOGS:   //SimPool 清理日志响应
                procSpTermClearLogsRsp(ctx, message);
                break;

            case CMD_SP_GET_LOGS:     //SimPool 获取日志响应
                procSpTermGetLogsRsp(ctx, message);
                break;

            default:
                procSpBadMsg(ctx, message);
                break;
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 登录Ind
    //------------------
    private void procSpLoginReq(ChannelHandlerContext ctx, Message message) {
        byte result = 0;
        SpLoginReq spLoginReq = (SpLoginReq) message.getBody();
        String macAddress = ConvUtils.mac2str(spLoginReq.getMacAddr());
        ServerCode ret = mongoDbUtil.dbSpLogin(macAddress,
                spLoginReq.getMaxSimCount(),
                spLoginReq.getHwVer(),
                spLoginReq.getSwVer(),
                ctx.channel().remoteAddress().toString());
        switch (ret) {
            case ERR_OK:
                result = 0;
                CCM.setId(CCM.ClientType.SP, ctx.channel().remoteAddress().toString(), macAddress);
                break;

            default:
                result = 1;
                break;
        }
        CmdUtils.get(ctx).sendRsp(buildSpLoginRsp(message.getSeqNo(), result));
    }

    //生成SimPool 登录回复
    private Message buildSpLoginRsp(short seq_no, byte result) {
        return new Message(Message.ToSpTag,
                CmdId.CMD_SP_LOGIN.value(),
                seq_no,
                (byte) 0,
                new SpLoginRsp(result));
    }

    //----------------------------------------------------------------------------
    //处理SimPool SIM卡信息Ind
    //------------------
    private void procSpSimInfoReq(ChannelHandlerContext ctx, Message message, boolean isUpload) {
        if (mongoDbUtil.getOnLineSimPoolId(ctx.channel().remoteAddress().toString()) == null) {
            //未登录
            Message ack_msg = buildSpSimInfoRsp(message.getSeqNo(), (byte) 1, isUpload);
            CmdUtils.get(ctx).sendRsp(ack_msg);
            return;
        } else {
            SpSimInfoReq body = (SpSimInfoReq) message.getBody();

            SimInfosResult result = smcService.SpUploadSimInfos(ctx, body.getSimInfos(), isUpload);

            byte ret = 0;
            if (ServerCode.ERR_OK == result.getCode()) {
                ArrayList<Short> invalid_sim_nos = new ArrayList<Short>();
                for (SpSimInfo spSimInfo : result.getInvalidSims()) {
                    invalid_sim_nos.add(spSimInfo.getLocationInSimPool());
                }
                Message ack_msg = buildSpSimInfoRsp(message.getSeqNo(), ret, invalid_sim_nos, isUpload);
                CmdUtils.get(ctx).sendRsp(ack_msg);
            } else {
                ret = 2;
                Message ack_msg = buildSpSimInfoRsp(message.getSeqNo(), ret, isUpload);
                CmdUtils.get(ctx).sendRsp(ack_msg);
            }

            //==============================
            //request for sim images
            for (SpSimInfo simInfo : result.getReqImgSims()) {
                toSpMsg.sendSpSimImgReq(ctx, simInfo.getLocationInSimPool(), simInfo.getImsi());
            }
        }
    }

    private Message buildSpSimInfoRsp(short seq_no, byte result, boolean isUpload) {
        return new Message(Message.ToSpTag,
                isUpload ? CmdId.CMD_SP_SIM_INFO.value() : CmdId.CMD_SP_SIM_INFO_UPDATE.value(),
                seq_no,
                (byte) 0,
                new SpSimInfoRsp(result));
    }

    private Message buildSpSimInfoRsp(short seq_no, byte result, ArrayList<Short> invalid_sim_nos, boolean isUpload) {
        return new Message(Message.ToSpTag,
                isUpload ? CmdId.CMD_SP_SIM_INFO.value() : CmdId.CMD_SP_SIM_INFO_UPDATE.value(),
                seq_no,
                (byte) 0,
                new SpSimInfoRsp(result, invalid_sim_nos));
    }

    //----------------------------------------------------------------------------
    //处理SimPool SIM卡镜像Rsp
    //------------------
    private void procSpTermSimImgRsp(ChannelHandlerContext ctx, Message message) {
        SpTermSimImgRsp body = (SpTermSimImgRsp) message.getBody();

        ServerCode ret = smcService.SpUpdateImage(body.getImsi(), body.getSimImage());
    }

    //----------------------------------------------------------------------------
    //处理SimPool SIM卡APDU Rsp
    //------------------
    private void procSpTermSimApduRsp(ChannelHandlerContext ctx, Message message) {
        SpTermSimApduRsp spTermSimApduRsp = (SpTermSimApduRsp) message.getBody();

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByImsi(spTermSimApduRsp.getImsi());

        if (onLineUser != null) {
            String ipAddr = onLineUser.getIpAddr();
            ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, ipAddr);
            if (userCtx != null) {
                toMtMsg.sendMtSimPoolRsp(userCtx, message.getSeqNo(), ServerCode.ERR_OK, spTermSimApduRsp.getApduRsp());
                return;
            }
        }

        // Mt not there any more
        logger.warn(ctx, "****************************************************");
        logger.warn(ctx, "*  Mt not exist anymore when Simpool apdu received *");
        logger.warn(ctx, "****************************************************");
    }

    //----------------------------------------------------------------------------
    //处理SimPool 上传日志 Rsp
    //------------------
    private void procSpTermLogUploadRsp(ChannelHandlerContext ctx, Message message) {
        SpTermLogUploadRsp spTermLogUploadRsp = (SpTermLogUploadRsp)message.getBody();
        SpTermLogUploadReq req = (SpTermLogUploadReq) CmdUtils.get(ctx).getCurrCmdMessage().getBody();

        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;
        if(spTermLogUploadRsp.getResult()==0){
            mongoDbUtil.dbSpUpdateLogState(req.getLogPath().split("\\.")[0], LogsState.RECEIVED.value());
            retCode = ServerCode.ERR_OK;
        }
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, spTermLogUploadRsp.getResult(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 软件升级 Rsp
    //------------------
    private void procSpTermUpgradeRsp(ChannelHandlerContext ctx, Message message) {
        SpTermUpgradeRsp spTermUpgradeRsp = (SpTermUpgradeRsp)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;

        String macAddress = CCM.getId(CCM.ClientType.SP,ctx.channel().remoteAddress().toString());
        if(spTermUpgradeRsp.getResult()==0){
            mongoDbUtil.doSaveUpgrade(macAddress,null,1);
            retCode = ServerCode.ERR_OK;
        }
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, spTermUpgradeRsp.getResult(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 主动上传日志 Req
    //------------------
    private ServerCode procSpLogUploadResReq(ChannelHandlerContext ctx, Message message) {
        SpLogUploadResReq spLogUploadResReq = (SpLogUploadResReq)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;
        if(spLogUploadResReq.getResult()==0){
            retCode = ServerCode.ERR_OK;
            mongoDbUtil.dbSpUpdateLogState(spLogUploadResReq.getLogpath().split("\\.")[0], LogsState.SUCCESS.value());
        }
        //回复simpool  收到了
        Message message_back = new Message(Message.ToSpTag,
                CmdId.CMD_SP_LOG_UPLOAD_RES.value(),
                message.getSeqNo(),
                (byte)0,
                new SpLogUploadResRsp( (byte)0));
        CmdUtils.get(ctx).sendRsp(message_back);
        //todo 告诉web 已经上传成功
        return retCode;
    }

    //----------------------------------------------------------------------------
    //处理SimPool 主动软件升级结果 Req
    //------------------
    private void procSpUpgradeResReq(ChannelHandlerContext ctx, Message message) {
        SpUpgradeResReq spUpgradeResReq = (SpUpgradeResReq)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;
        String macAddress = CCM.getId(CCM.ClientType.SP,ctx.channel().remoteAddress().toString());
        if(spUpgradeResReq.getResult()==0){
            mongoDbUtil.doSaveUpgrade(macAddress,null,2);
            retCode = ServerCode.ERR_OK;
        }
        //回复simpool  收到了
        Message message_back = new Message(Message.ToSpTag,
                CmdId.CMD_SP_UPGRADE_RES.value(),
                message.getSeqNo(),
                (byte)0,
                new SpUpgradeResRsp( (byte)0));
        CmdUtils.get(ctx).sendRsp(message_back);
        //回复http日志上传结果
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, spUpgradeResReq.getResult(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool tree控制 Rsp
    //------------------
    private void procSpTermTraceRsp(ChannelHandlerContext ctx, Message message) {
        SpTermTraceRsp spTermTraceRsp = (SpTermTraceRsp)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;

        if(spTermTraceRsp.getResult()==0){
            retCode = ServerCode.ERR_OK;
        }
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, spTermTraceRsp.getResult(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 重启响应 Rsp
    //------------------
    private void procSpTermRebootRsp(ChannelHandlerContext ctx, Message message) {
        SpTermRebootRsp spTermRebootRsp = (SpTermRebootRsp)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;

        if(spTermRebootRsp.getResult()==0){
            retCode = ServerCode.ERR_OK;
        }
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, spTermRebootRsp.getResult(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 清理日志响应 Rsp
    //------------------
    private void procSpTermClearLogsRsp(ChannelHandlerContext ctx, Message message) {
        SpTermClearLogsRsp spTermClearLogsRsp = (SpTermClearLogsRsp)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;

        if(spTermClearLogsRsp.getResult()==0){
            retCode = ServerCode.ERR_OK;
        }
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, spTermClearLogsRsp.getResult(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 获取日志响应 Rsp
    //------------------
    private void procSpTermGetLogsRsp(ChannelHandlerContext ctx, Message message) {
        SpTermGetLogsRsp spTermGetLogsRsp = (SpTermGetLogsRsp)message.getBody();
        ServerCode retCode = ServerCode.SERR_SERVICE_DOWN;

        if(spTermGetLogsRsp.getFileNameList().size()>0){
            retCode = ServerCode.ERR_OK;
        }
        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            HttpMsgHandler.sendHttpRsp(httpCtx, retCode.value(), description);
        }
    }

    //----------------------------------------------------------------------------
    //处理SimPool 心跳Ping包
    //------------------
    private void procSpHeartBeatReq(ChannelHandlerContext ctx, Message message) {
        Message heartBeat = buildSpHeatBeatRsp(message.getSeqNo());
        CmdUtils.get(ctx).sendRsp(heartBeat);
    }

    //生成SimPool 心跳Pong包
    private Message buildSpHeatBeatRsp(short seq_no) {
        return new Message(Message.ToSpTag,
                CmdId.CMD_SP_HEARTBEAT.value(),
                seq_no,
                (byte) 0,
                new SpHeartBeatRsp());
    }

    //----------------------------------------------------------------------------
    //处理SimPool 错误消息
    //-----------------
    private void procSpBadMsg(ChannelHandlerContext ctx, Message message) {
        logger.error(ctx, "SimPool bad msg found:" + message);
    }

}
