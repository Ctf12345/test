package com.ctf.ass.server.netty.handler;

import com.ctf.ass.server.component.MongoDbUtil;
import com.ctf.ass.server.component.RocketMQMessageSender;
import com.ctf.ass.server.component.ToMtMsg;
import com.ctf.ass.server.component.ToSpMsg;
import com.ctf.ass.server.utils.CmdUtils;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.mt_body.MtAlertRsp;
import com.ctf.ass_codec.mt_body.MtCell;
import com.ctf.ass_codec.mt_body.MtCodes;
import com.ctf.ass_codec.mt_body.MtHeartBeatReq;
import com.ctf.ass_codec.mt_body.MtHeartBeatRsp;
import com.ctf.ass_codec.mt_body.MtLogUploadRetReq;
import com.ctf.ass_codec.mt_body.MtLogUploadRetRsp;
import com.ctf.ass_codec.mt_body.MtLoginByIdReq;
import com.ctf.ass_codec.mt_body.MtLoginByIdRsp;
import com.ctf.ass_codec.mt_body.MtLoginReq;
import com.ctf.ass_codec.mt_body.MtLoginRsp;
import com.ctf.ass_codec.mt_body.MtLogoutReq;
import com.ctf.ass_codec.mt_body.MtLogoutRsp;
import com.ctf.ass_codec.mt_body.MtModem;
import com.ctf.ass_codec.mt_body.MtRTraceInd;
import com.ctf.ass_codec.mt_body.MtReqUimReq;
import com.ctf.ass_codec.mt_body.MtReqUimRsp;
import com.ctf.ass_codec.mt_body.MtRevUimReq;
import com.ctf.ass_codec.mt_body.MtRevUimRsp;
import com.ctf.ass_codec.mt_body.MtSimImgRsp;
import com.ctf.ass_codec.mt_body.MtSimPoolReq;
import com.ctf.ass_codec.mt_body.MtTerm;
import com.ctf.ass_codec.mt_body.MtTermAssignUimRsp;
import com.ctf.ass_codec.mt_body.MtTermInfoRsp;
import com.ctf.ass_codec.mt_body.MtTermLogUploadReq;
import com.ctf.ass_codec.mt_body.MtTermLogUploadRsp;
import com.ctf.ass_codec.mt_body.MtTermLogoutRsp;
import com.ctf.ass_codec.mt_body.MtTermRevUimRsp;
import com.ctf.ass_codec.mt_body.MtTermTraceRsp;
import com.ctf.ass_codec.mt_body.MtUim;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_codec.struct.MessageBody;
import com.ctf.ass_codec.utils.Cloud88StorageUtils;
import com.ctf.ass_public.globals.ClientState;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.GlobalConfig;
import com.ctf.ass_public.globals.LogsState;
import com.ctf.ass_public.struct.MtLog;
import com.ctf.ass_public.struct.OnLineUser;
import com.ctf.ass_public.struct.OpHistory;
import com.ctf.ass_public.struct.SimCard;
import com.ctf.ass_public.struct.UserReqUimHistory;
import com.ctf.oss.CCM;
import com.ctf.oss.CRMService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author Charles
 * @create 2017/7/21 15:56
 */
@Component("mtMsgHandler")
@Scope("prototype")
@ChannelHandler.Sharable
public class MtMsgHandler extends ChannelInboundHandlerAdapter {
    private static LogWrapper logger = LogWrapper.getLogger(MtMsgHandler.class.getName());
    @Autowired
    private MongoDbUtil mongoDbUtil;
    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;
    @Autowired
    private ToMtMsg toMtMsg;
    @Autowired
    private ToSpMsg toSpMsg;

    @Autowired
    private CRMService crmService;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug(ctx, "Got Mt connection");
        if (mongoDbUtil.isBlackUser(ctx.channel().remoteAddress().toString())) {
            logger.error(ctx, "[server]:A BlackUser,will close connection.please contact server administrator!");
            ctx.close();
        } else {
            CmdUtils.init(ctx);
            CCM.addCtx(CCM.ClientType.MT, ctx);
            rocketMQMessageSender.sendOpHistory(new OpHistory(ctx.channel().remoteAddress().toString(), OpHistory.OpType.MtToSvr_Ind, "connect", 0));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
        if (object instanceof Message) {
            Message message = (Message) object;
            CmdUtils cmdUtils = CmdUtils.get(ctx);

            if (cmdUtils.getCurrCmdMessage() != null)  { //当前命令正在等待回复
                if (cmdUtils.isCmdRsp(message)) {   //对当前命令的回复消息
                    //取消命令重发
                    cmdUtils.cancelSendingCmd(true);
                    //处理消息
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
                            logger.debug(ctx, "[Mt] Repeated req:" + req);
                        } else {
                            //保存接收到的seq_no，以便回复时使用同样的seq_no
                            CmdUtils.get(ctx).setMsgSeqNo(req.getSeqNo());
                            procMsg(ctx, req);
                        }
                    }
                    //发送命令队列中下一命令(如果有的话)
                    cmdUtils.sendNextCmd();
                } else if (cmdUtils.getWaitClose()) {   //等待关闭时，仅处理强制退录消息的回复，不再处理req和ind
                    logger.warn(ctx, "[Mt] Receive message while waiting close:" + message);
                    return;
                } else if (cmdUtils.isReqMessage(message)) {    //收到请求消息
                    cmdUtils.setWaitingRequest(message);    //仅缓存但不立即处理。注意：为避免攻击，仅缓存1个req。正常终端不应连续发Req，发送后应等待回复
                } else if (cmdUtils.isIndMessage(message)) {    //收到通知消息可立即处理，因通知不需要回复
                    if (cmdUtils.isRepeatedInd(message)) {  //重复的通知消息直接忽略，保证消息的幂等性
                        logger.debug(ctx, "[Mt] Repeated ind:" + message);
                    } else {
                        procMsg(ctx, message);
                    }
                } else {
                    logger.warn(ctx, "[Mt] Unexpected message while waiting response:" + message);
                }
            } else {
                if (cmdUtils.getWaitClose()) {  //等待关闭时，仅处理强制退录消息的回复，不再处理req和ind
                    logger.warn(ctx, "[Mt] Receive message while waiting close:" + message);
                    return;
                } else if (cmdUtils.isReqMessage(message)) {   //收到请求消息
                    if (cmdUtils.procRepeatedRequest(message)) {  //重复的请求消息直接返回上次回复，保证消息的幂等性
                        logger.debug(ctx, "[Mt] Repeated req:" + message);
                    } else {
                        //保存接收到的seq_no，以便回复时使用同样的seq_no
                        CmdUtils.get(ctx).setMsgSeqNo(message.getSeqNo());
                        procMsg(ctx, message);
                    }
                } else if (cmdUtils.isIndMessage(message)) {    //收到通知消息
                    if (cmdUtils.isRepeatedInd(message)) {  //重复的通知消息直接忽略，保证消息的幂等性
                        logger.debug(ctx, "[Mt] Repeated ind:" + message);
                    } else {
                        procMsg(ctx, message);
                    }
                } else {
                    logger.warn(ctx, "[Mt] Unexpected message while idle:" + message);
                }
            }
        } else {
            logger.fatal(ctx, "[Mt] Not a valid Message! fireChannelRead!!!");
            ctx.fireChannelRead(object);
        }
    }

    //用户事件处理(目前仅处理空闲超时：如果长时间无任何通信，则依次请求还卡-踢掉用户-断开连接)
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                String ipAddr = crmService.getIpAddr(ctx);
                OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(ipAddr);
                if (onLineUser != null) {
                    switch (onLineUser.getClientState()) {
                        case CLIENT_STATE_ASSIGNED:  //已分卡，请求用户还卡，可拒绝
                            logger.warn(ctx, GlobalConfig.MT_IDLE_TIMEOUT_SECONDS + " seconds no message. Request to revuim.");
                            toMtMsg.sendMtTermRevUimReq(ctx, ErrCode.ServerCode.ULR_USER_INACTIVE.value(), 1);
                            break;

                        case CLIENT_STATE_LOGINED:  //已登录，请求用户退录，可拒绝
                            logger.warn(ctx, GlobalConfig.MT_IDLE_TIMEOUT_SECONDS + " seconds no message. Request to logout.");
                            toMtMsg.sendMtTermLogoutReq(ctx, ErrCode.ServerCode.RUR_USER_INACTIVE.value(), 1);
                            break;
                    }
                } else {    //未登录，断开连接
                    logger.warn(ctx, GlobalConfig.MT_IDLE_TIMEOUT_SECONDS + " seconds no message. Ready to close channel.");
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();

        logger.debug("channelInactive!");
        if (CmdUtils.get(ctx) != null) {
            //如果是web请求，终端断开需要通知web
            if (CmdUtils.get(ctx).getCurrCmdHttpCtx() != null) {
                ErrCode.ServerCode errCode = ErrCode.ServerCode.PACK_ACK_TIMEOUT;
                HttpMsgHandler.sendHttpRsp(CmdUtils.get(ctx).getCurrCmdHttpCtx(), errCode.value(), errCode.cnErrMsg());
            }
            if (CmdUtils.get(ctx).getWaitClose()) {
                logger.debug("user wait close!!!");
                crmService.userLogout(crmService.getIpAddr(ctx), ErrCode.ServerCode.ULR_USER_KICKED);
            } else {
                logger.debug("will close connection!!!");
                crmService.userCloseConn(ctx);
            }
            rocketMQMessageSender.sendOpHistory(new OpHistory(ctx.channel().remoteAddress().toString(), OpHistory.OpType.MtToSvr_Ind, "disconn", 0));
            CCM.delCtx(CCM.ClientType.MT, ctx);
        }

        logger.warn(ctx, "ChannelInactive!");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(ctx, "ExceptionCaught!", cause.fillInStackTrace());

        ctx.close();
        //ctx.fireExceptionCaught(cause);
    }

    private void procMsg(ChannelHandlerContext ctx, Message message) {
        //打印log
        logger.debug(ctx, "Mt=>Server " + message);

        //保存操作历史
        rocketMQMessageSender.sendOpHistory(ctx, message);

        //获取消息的CMDID
        MtProtoMsg.CMDID cmd_id = MtProtoMsg.CMDID.forNumber(message.getCmdId());

        if (cmd_id == null) {
            procMtBadMsg(ctx, message);
            return;
        }

        // 处理MT相关消息
        switch (cmd_id) {
            //=========  Request  ==========
            case LOGIN:                    //终端 登录
                procMtLoginReq(ctx, message);
                break;

            case LOGINBYID:                 //终端 用session ID登录
                procMtLoginByIdReq(ctx, message);
                break;

            case LOGOUT:                    //终端 退出登录
                procMtLogoutReq(ctx, message);
                break;

            case REQUIM:                   //终端 分卡
                procMtReqUimReq(ctx, message);
                break;

            case REVUIM:                    //终端 还卡
                procMtRevUimReq(ctx, message);
                break;

            case SIMIMG:                    //终端 下载SIM卡镜像
                procMtSimImgReq(ctx, message);
                break;

            case SIMPOOL:                  //终端 发命令给SimPool
                procMtSimPoolReq(ctx, message);
                break;

            case HEARTBEAT:                 //终端 心跳包
                procMtHeartBeatReq(ctx, message);
                break;

            case LOGUPLOADRET:             //终端 log上传
                procMtLogUploadRetReq(ctx, message);
                break;

            case ALERTS:                    //终端 警告信息
                procMtAlertReq(ctx, message);
                break;

            //=========  Response  ==========
            case TERMINFO:                  //终端 信息
                procMtTermInfoRsp(ctx, message);
                break;

            case TERMREVUIM:               //终端 强制还卡
                procMtTermRevUimRsp(ctx, message);
                break;

            case TERMLOGOUT:               //终端 强制退录
                procMtTermLogoutRsp(ctx, message);
                break;

            case TERMASSIGNUIM:           //终端 主动分卡
                procMtTermAssignUimRsp(ctx, message);
                break;

            case TERMTRACE:               //终端 实时trace控制
                procMtTermTraceRsp(ctx, message);
                break;

            case TERMLOGUPLOAD:         //终端 log上传
                procMtTermLogUploadRsp(ctx, message);
                break;

            //=========  Indication  ==========
            case RTRACE:
                procRTraceInd(ctx, message);  //终端 实时trace log通知
                break;

            //=========  Bad Message  ==========
            default:
                procMtBadMsg(ctx, message);   //终端 错误消息
                break;
        }
    }

    //===========================  Request  =============================
    //----------------------------------------------------------------------------
    //终端 请求用session ID登录
    //-----------------
    private void procMtLoginByIdReq(ChannelHandlerContext ctx, Message message) {
        MtLoginByIdReq mtLoginByIdReq = (MtLoginByIdReq) message.getBody();
        ErrCode.ServerCode errCode = crmService.userLoginById(ctx, mtLoginByIdReq.getSessionId());

        Message mtLoginByIdRsp = buildMtLoginByIdRsp(message.getSeqNo(), errCode);
        CmdUtils.get(ctx).sendRsp(mtLoginByIdRsp);
    }

    private Message buildMtLoginByIdRsp(short seq_no, ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(),errCode.enErrMsg());
        MtLoginByIdRsp mtLoginByIdRsp = new MtLoginByIdRsp(result);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.LOGINBYID_VALUE,
                seq_no,
                (byte) 0,
                mtLoginByIdRsp);
    }

    //----------------------------------------------------------------------------
    //终端 请求登录
    //-----------------
    private void procMtLoginReq(ChannelHandlerContext ctx, Message message) {
        ErrCode.ServerCode errCode;
        MtLoginReq mtLoginReq = (MtLoginReq) message.getBody();
        if (mtLoginReq.getProtoVer() != MtProtoMsg.VERSION.PROTO_VALUE) {
            errCode = ErrCode.ServerCode.ERR_PROTO_NOT_COMPATIABLE;
        } else {
            if (!mtLoginReq.getCodesVer().equals(ErrCode.VERSION)){
                logger.warn("MT ErrCode Version is "+mtLoginReq.getCodesVer()+" and Server ErrCode Version is "+ErrCode.VERSION);
            }
            errCode = crmService.userLogin(ctx, mtLoginReq.getUserid(), mtLoginReq.getPsdmd5(), mtLoginReq.getDevid());
        }

        Message mtLoginRsp = buildMtLoginRsp(message.getSeqNo(), errCode);
        CmdUtils.get(ctx).sendRsp(mtLoginRsp);
        /*
        //如果登录成功，发消息查询终端信息
        if (errCode == ServerCode.ERR_OK) {
            sendMtTermInfoReq(ctx);
        }
        */
    }

    private Message buildMtLoginRsp(short seq_no, ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(),errCode.enErrMsg());

        MtLoginRsp mtLoginRsp;

        if (errCode == ErrCode.ServerCode.ERR_PROTO_NOT_COMPATIABLE) {
            mtLoginRsp = new MtLoginRsp(result, MtProtoMsg.VERSION.PROTO_VALUE, ErrCode.VERSION);
        } else {
            mtLoginRsp = new MtLoginRsp(result);
        }

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.LOGIN_VALUE,
                seq_no,
                (byte) 0,
                mtLoginRsp);
    }

    //----------------------------------------------------------------------------
    //终端 退出登录
    //-----------------
    private void procMtLogoutReq(ChannelHandlerContext ctx, Message message) {
        switch (crmService.userGetState(ctx)) {
            case CLIENT_STATE_ASSIGNED:
            case CLIENT_STATE_WAITING_ASSIGN:
            case CLIENT_STATE_LOGINED: {
                MtLogoutReq mtLogoutReq = (MtLogoutReq) message.getBody();
                int code = mtLogoutReq.getCause().getCode();    //logout cause
                ErrCode.ServerCode errCode = crmService.userLogout(ctx, ErrCode.ServerCode.valueOf((short) code));

                Message mtLogoutRsp = buildMtLogoutRsp(message.getSeqNo(), errCode);
                CmdUtils.get(ctx).sendRsp(mtLogoutRsp);
            }
            break;

            case CLIENT_STATE_UNKNOWN: {
                //直接回复OK
                Message mtLogoutRsp = buildMtLogoutRsp(message.getSeqNo(), ErrCode.ServerCode.ERR_OK);
                CmdUtils.get(ctx).sendRsp(mtLogoutRsp);
            }
            break;
        }
    }

    private Message buildMtLogoutRsp(short seq_no, ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());

        MtLogoutRsp mtLogoutRsp = new MtLogoutRsp(result);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.LOGOUT_VALUE,
                seq_no,
                (byte) 0,
                mtLogoutRsp);
    }

    //----------------------------------------------------------------------------
    //终端 请求分卡
    //-----------------
    private void procMtReqUimReq(ChannelHandlerContext ctx, Message message) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_OK;
        String mt_imsi = null;
        ArrayList<MtCell> mtCells = null;
        ArrayList<String> plmn_list = null;

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(ctx.channel().remoteAddress().toString());
        if (onLineUser != null) {
            //检查参数
            MtReqUimReq body = (MtReqUimReq) message.getBody();
            if (body.getModem().hasCells()) {
                mtCells = body.getModem().getCells();
                if (mtCells.size() == 0) {
                    errCode = ErrCode.ServerCode.ERR_ILLEGAL_PARAM;
                } else {
                    plmn_list = new ArrayList<String>();
                    int mcc;
                    int mnc;
                    String plmn;
                    for (MtCell cell : mtCells) {
                        mcc = cell.getMcc();
                        mnc = cell.getMnc();
                        if (mnc >= 0xF00) {
                            mnc -= 0xF00;
                            plmn = String.format("%d%02d", mcc, mnc);
                        } else {
                            plmn = String.format("%d%03d", mcc, mnc);
                        }
                        plmn_list.add(plmn);
                    }
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_ILLEGAL_PARAM;
            }

            if (errCode != ErrCode.ServerCode.ERR_ILLEGAL_PARAM) {
                if (body.getModem().hasImsi()) {
                    mt_imsi = body.getModem().getImsi();
                    if (mt_imsi.length() == 0) {
                        mt_imsi = null;
                    }
                }

                switch (onLineUser.getClientState()) {
                    case CLIENT_STATE_ASSIGNED:
                        if (mt_imsi != null && mt_imsi.equals(onLineUser.getImsi())) {  //尝试换卡
                            errCode = crmService.userReqSim(ctx, plmn_list, mt_imsi);

                            if (errCode == ErrCode.ServerCode.ERR_PLMN_NO_SIM ||
                                    errCode == ErrCode.ServerCode.ERR_BIND_NO_SIM) {
                                mongoDbUtil.dbMtWaitAssignSim(onLineUser.getUserId(), plmn_list);
                                crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_WAITING_ASSIGN);
                            }
                        } else {
                            //暂不考虑终端上传的小区信息与服务器的分卡信息不匹配的情况
                            // 直接返回成功，回复已分配的卡
                        }
                        break;

                    case CLIENT_STATE_WAITING_ASSIGN:
                        crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_LOGINED);
                    case CLIENT_STATE_LOGINED:
                        //尝试分其它卡
                        errCode = crmService.userReqSim(ctx, plmn_list, mt_imsi);

                        //分卡失败，则同步到等待分卡
                        if (errCode != ErrCode.ServerCode.ERR_OK) {
                            mongoDbUtil.dbMtWaitAssignSim(onLineUser.getUserId(), plmn_list);
                            crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_WAITING_ASSIGN);
                        }
                        break;
                    case CLIENT_STATE_UNKNOWN:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        break;
                }
            }
        } else {
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }

        //根据操作结果进行回复
        Message mtLoginAssignRsp;
        if (errCode == ErrCode.ServerCode.ERR_OK) {
            //重新获取在线用户信息
            onLineUser = mongoDbUtil.getOnLineUserByUserId(onLineUser.getUserId());
            //生成回复
            String imsi = onLineUser.getImsi();
            SimCard simCard = mongoDbUtil.getDbSimCard(imsi);
            byte[] imgMd5 = simCard.getImgMd5();
            byte[] sessionId = onLineUser.getSessionId();

            mtLoginAssignRsp = buildMtReqUimRsp(message.getSeqNo(), errCode, imsi, imgMd5, sessionId);   //回复使用相同的msg id
            //记录分卡成功历史
            rocketMQMessageSender.sendUserReqUimHistory(new UserReqUimHistory(onLineUser.getUserId(), imsi));
        } else {
            mtLoginAssignRsp = buildMtReqUimRsp(message.getSeqNo(), errCode);   //回复使用相同的msg id
        }
        //回复
        CmdUtils.get(ctx).sendRsp(mtLoginAssignRsp);
    }

    //生成终端 分卡回复
    private Message buildMtReqUimRsp(short seq_no, ErrCode.ServerCode errCode, String imsi, byte[] img_md5, byte[] sessionId) {
        MtReqUimRsp mtReqUimRsp;

        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());
        ArrayList<MtCell> mtCells = new ArrayList<MtCell>();
        //TODO: 回复数据库里该卡的实际Cell信息
        mtCells.add(new MtCell(460, 0xF00 + 01, 0));
        MtUim uim = new MtUim(imsi, img_md5, mtCells);
        mtReqUimRsp = new MtReqUimRsp(result, uim, sessionId);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.REQUIM_VALUE,
                seq_no,
                (byte) 0,
                mtReqUimRsp);
    }

    //生成终端 分卡回复
    private Message buildMtReqUimRsp(short seq_no, ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());
        MtReqUimRsp mtReqUimRsp = new MtReqUimRsp(result);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.REQUIM_VALUE,
                seq_no,
                (byte) 0,
                mtReqUimRsp);
    }

    //----------------------------------------------------------------------------
    //终端 请求还卡
    //-----------------
    private void procMtRevUimReq(ChannelHandlerContext ctx, Message message) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_OK;

        switch (crmService.userGetState(ctx)) {
            case CLIENT_STATE_ASSIGNED:
                MtRevUimReq mtRevUimReq = (MtRevUimReq) message.getBody();
                int code = mtRevUimReq.getCause().getCode();

                errCode = crmService.userRevSim(ctx, code);
                break;
            case CLIENT_STATE_WAITING_ASSIGN:
                crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_LOGINED);
            case CLIENT_STATE_LOGINED:
                //未分卡时直接回复成功
                errCode = ErrCode.ServerCode.ERR_OK;
                break;
            case CLIENT_STATE_UNKNOWN:
                errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                break;
        }

        Message mtRevUimRsp = buildMtRevUimRsp(message.getSeqNo(), errCode);
        CmdUtils.get(ctx).sendRsp(mtRevUimRsp);
    }

    private Message buildMtRevUimRsp(short seq_no, ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());
        MtRevUimRsp mtRevUimRsp = new MtRevUimRsp(result);
        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.REVUIM_VALUE,
                seq_no,
                (byte) 0,
                mtRevUimRsp);
    }

    //----------------------------------------------------------------------------
    //终端 请求下载SIM卡镜像
    //-----------------
    private void procMtSimImgReq(ChannelHandlerContext ctx, Message message) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_OK;
        byte[] simImage = null;

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(crmService.getIpAddr(ctx));
        if (onLineUser != null){
            switch (onLineUser.getClientState()) {
                case CLIENT_STATE_ASSIGNED:
                    try {
                        String imsi = onLineUser.getImsi();
                        SimCard simCard = mongoDbUtil.getDbSimCard(imsi);
                        simImage = simCard.getSimcardImage();
                        errCode = ErrCode.ServerCode.ERR_OK;
                    } catch (Exception e) {
                        errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                    }
                    break;
                case CLIENT_STATE_WAITING_ASSIGN:
                    crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_LOGINED);
                case CLIENT_STATE_LOGINED:
                    //请求分卡
                    errCode = ErrCode.ServerCode.ERR_USER_NOT_REQUIM;
                    break;
                case CLIENT_STATE_UNKNOWN:
                    errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                    break;
            }
        }else {
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }

        Message mtSimImgRsp;
        if (errCode == ErrCode.ServerCode.ERR_OK) {
            mtSimImgRsp = buildMtSimImgRsp(message.getSeqNo(), errCode, simImage);
        } else {
            mtSimImgRsp = buildMtSimImgRsp(message.getSeqNo(), errCode);
        }
        CmdUtils.get(ctx).sendRsp(mtSimImgRsp);
    }

    private Message buildMtSimImgRsp(short seq_no, ErrCode.ServerCode errCode, byte[] simImage) {
        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());
        MtSimImgRsp mtSimImgRsp = new MtSimImgRsp(result, simImage);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.SIMIMG_VALUE,
                seq_no,
                (byte) 0,
                mtSimImgRsp);
    }

    private Message buildMtSimImgRsp(short seq_no, ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());
        MtSimImgRsp mtSimImgRsp = new MtSimImgRsp(result);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.SIMIMG_VALUE,
                seq_no,
                (byte) 0,
                mtSimImgRsp);
    }

    //----------------------------------------------------------------------------
    //处理终端 发送命令到SimPool
    //-----------------
    private void procMtSimPoolReq(ChannelHandlerContext ctx, Message message) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_OK;

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(crmService.getIpAddr(ctx));
        if (onLineUser != null){
            switch (onLineUser.getClientState()) {
                case CLIENT_STATE_ASSIGNED:
                    try {
                        //Forward apdu cmd to SimPool
                        String imsi = onLineUser.getImsi();
                        SimCard simCard = mongoDbUtil.getDbSimCard(imsi);
                        String macAddress = simCard.getSimPoolMacAddr();
                        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
                        ChannelHandlerContext simPoolCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);

                        if (simPoolCtx != null) {
                            int simNo = simCard.getLocationInSimPool();
                            MtSimPoolReq body = (MtSimPoolReq) message.getBody();

                            toSpMsg.sendSpSimApduReq(simPoolCtx, message.getSeqNo(), (short) simNo, imsi, body.getRequest());
                            //set result
                            errCode = ErrCode.ServerCode.ERR_OK;
                        } else {
                            errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
                    }
                    break;
                case CLIENT_STATE_WAITING_ASSIGN:
                    crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_LOGINED);
                case CLIENT_STATE_LOGINED:
                    errCode = ErrCode.ServerCode.ERR_USER_NOT_REQUIM;
                    break;
                case CLIENT_STATE_UNKNOWN:
                    errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                    break;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        if (ErrCode.ServerCode.ERR_OK != errCode) {
            toMtMsg.sendMtSimPoolRsp(ctx, message.getSeqNo(), errCode);
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 心跳Ping包
    //-----------------
    private void procMtHeartBeatReq(ChannelHandlerContext ctx, Message message) {
        if (crmService.userGetState(ctx) == ClientState.CLIENT_STATE_UNKNOWN) {
            toMtMsg.sendMtNotLoginRsp(ctx, message);
        } else {
            MtHeartBeatReq mtHeartBeatReq = (MtHeartBeatReq) message.getBody();

            if (mtHeartBeatReq != null && mtHeartBeatReq.hasModem()) {
                MtModem modem = mtHeartBeatReq.getModem();
                Message heartBeat = buildMtHeatBeatRsp(message.getSeqNo());
                CmdUtils.get(ctx).sendRsp(heartBeat);
            } else {
                Message heartBeat = buildMtHeatBeatRsp(message.getSeqNo());
                CmdUtils.get(ctx).sendRsp(heartBeat);
            }
        }
    }

    //生成终端 心跳pong包
    private Message buildMtHeatBeatRsp(short seq_no) {
        MtCodes result = new MtCodes(ErrCode.ServerCode.ERR_OK.value(), ErrCode.ServerCode.ERR_OK.enErrMsg());
        MessageBody heartBeatPong = new MtHeartBeatRsp(result);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.HEARTBEAT_VALUE,
                seq_no,
                (byte) 0,
                heartBeatPong);
    }

    //----------------------------------------------------------------------------
    //处理终端 log上传结果
    //-----------------
    private void procMtLogUploadRetReq(ChannelHandlerContext ctx, Message message) {
        if (crmService.userGetState(ctx) == ClientState.CLIENT_STATE_UNKNOWN) {
            toMtMsg.sendMtNotLoginRsp(ctx, message);
        } else {
            //先回复，如果提取不到日志，可重发提取日志命令。
            Message rsp = buildMtLogUploadRetRsp(message.getSeqNo(),ErrCode.ServerCode.ERR_OK);
            CmdUtils.get(ctx).sendRsp(rsp);

            MtLogUploadRetReq mtLogUploadRetReq = (MtLogUploadRetReq) message.getBody();
            String _id = mtLogUploadRetReq.getLogPath().split("\\.")[0];
            if (mtLogUploadRetReq.getResult().getCode() == ErrCode.ServerCode.ERR_OK.value()) {
                mongoDbUtil.dbMtUserUploadLogStatus(_id, LogsState.SUCCESS.value());
                //TODO 通知运维
            } else {
                mongoDbUtil.dbMtUserUploadLogStatus(_id, LogsState.FAIL.value());
            }
        }
    }

    private Message buildMtLogUploadRetRsp(short seq_no,ErrCode.ServerCode errCode) {
        MtCodes result = new MtCodes(errCode.value(), errCode.enErrMsg());
        MtLogUploadRetRsp mtLogUploadRetRsp = new MtLogUploadRetRsp(result);
        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.LOGUPLOADRET_VALUE,
                seq_no,
                (byte) 0,
                mtLogUploadRetRsp);
    }

    //----------------------------------------------------------------------------
    //处理终端 警告信息
    //-----------------
    private void procMtAlertReq(ChannelHandlerContext ctx, Message message) {
        if (crmService.userGetState(ctx) == ClientState.CLIENT_STATE_UNKNOWN) {
            toMtMsg.sendMtNotLoginRsp(ctx, message);
        } else {
            int code = 0;
            Message rsp = buildMtAlertRsp(message.getSeqNo(), code);
            CmdUtils.get(ctx).sendRsp(rsp);
        }
    }

    private Message buildMtAlertRsp(short seq_no, int code) {
        String description = null;
        ErrCode.ServerCode scode = ErrCode.ServerCode.valueOf((short)code);
        if ( scode!= null){
            description = scode.enErrMsg();
        }
        MtCodes result = new MtCodes(code,description);
        MtAlertRsp mtAlertRsp = new MtAlertRsp(result);

        return new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.ALERTS_VALUE,
                seq_no,
                (byte) 0,
                mtAlertRsp);
    }


    //===========================  Response  =============================

    //----------------------------------------------------------------------------
    //处理终端 消息
    //-----------------
    private void procMtTermInfoRsp(ChannelHandlerContext ctx, Message message) {
        MtTermInfoRsp mtTermInfoRsp = (MtTermInfoRsp) message.getBody();

        MtTerm term = mtTermInfoRsp.getTerm();
        String serialno = null;
        String model = null;
        String sw_ver = null;
        String buildno = null;
        if (term.hasSerialno()) {
            serialno = term.getSerialno();
        }
        if (term.hasModel()) {
            model = term.getModel();
        }
        if (term.hasSw_ver()) {
            sw_ver = term.getSw_ver();
        }
        if (term.hasBuildno()) {
            buildno = term.getBuildno();
        }

        mongoDbUtil.dbMtUserUpdateInfo(serialno, model, sw_ver, buildno);
    }

    //----------------------------------------------------------------------------
    //处理终端 强制还卡
    //-----------------
    private void procMtTermRevUimRsp(ChannelHandlerContext ctx, Message message) {
        MtTermRevUimRsp mtTermRevUimRsp = (MtTermRevUimRsp) message.getBody();
        MtCodes result = mtTermRevUimRsp.getResult();

        int code = result.getCode();
        logger.debug(ctx, "MtTermRevUim result:" + code);

        ErrCode.ServerCode retCode = ErrCode.ServerCode.valueOf((short) code);
        switch (retCode) {
            case ERR_USER_NOT_LOGIN:
                crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_UNKNOWN);
            case USER_OPERATING:
                break;

            case ERR_OK:
                crmService.userRevSim(ctx, code);
                break;
        }

        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            if (result.hasDescription()) {
                description = result.getDescription();
            }
            HttpMsgHandler.sendHttpRsp(httpCtx, code, description);
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 强制退录
    //-----------------
    private void procMtTermLogoutRsp(ChannelHandlerContext ctx, Message message) {
        MtTermLogoutRsp mtTermLogoutRsp = (MtTermLogoutRsp) message.getBody();
        MtCodes result = mtTermLogoutRsp.getResult();

        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(ctx.channel().remoteAddress().toString());
        int code = result.getCode();
        logger.debug(ctx, "MtTermLogout result:" + code);
        ErrCode.ServerCode retCode = ErrCode.ServerCode.valueOf((short) code);
        switch (retCode) {
            case USER_OPERATING:
                break;

            case ERR_OK:
                if (onLineUser != null && onLineUser.getClientState() == ClientState.CLIENT_STATE_LOGINED) {
                    crmService.userLogout(ctx, ErrCode.ServerCode.ULR_USER_KICKED);
                }
                else {
                    logger.debug("===========no close==========");
                    logger.debug("===========no close==========");
                    //ctx.close();    //强制退录后，将断开连接
                }
                break;
        }

        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            if (result.hasDescription()) {
                description = result.getDescription();
            }
            HttpMsgHandler.sendHttpRsp(httpCtx, code, description);
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 主动分卡
    //-----------------
    private void procMtTermAssignUimRsp(ChannelHandlerContext ctx, Message message) {
        MtTermAssignUimRsp mtTermAssignUimRsp = (MtTermAssignUimRsp) message.getBody();
        MtCodes result = mtTermAssignUimRsp.getResult();

        int code = result.getCode();
        logger.debug(ctx, "MtTermAssignUim result:" + code);

        ErrCode.ServerCode retCode = ErrCode.ServerCode.valueOf((short) code);
        switch (retCode) {
            case ERR_USER_NOT_LOGIN:
                crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_UNKNOWN);
            case USER_OPERATING:
                //TODO：取消已分的卡
                break;

            case ERR_OK:
                break;
        }

        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            if (result.hasDescription()) {
                description = result.getDescription();
            }
            HttpMsgHandler.sendHttpRsp(httpCtx, code, description);
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 实时trace控制
    //-----------------
    private void procMtTermTraceRsp(ChannelHandlerContext ctx, Message message) {
        MtTermTraceRsp mtTermTraceRsp = (MtTermTraceRsp) message.getBody();
        MtCodes result = mtTermTraceRsp.getResult();

        int code = result.getCode();
        logger.debug(ctx, "MtTermTraceRsp result:" + code);
        ErrCode.ServerCode retCode = ErrCode.ServerCode.valueOf((short) code);
        switch (retCode) {
            case ERR_USER_NOT_LOGIN:
                crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_UNKNOWN);
                break;

            case ERR_OK:
                //TODO：写入trace开关？
                //TODO:TERMINFO 可查询trace开关状态
                break;
        }

        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();

        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            if (result.hasDescription()) {
                description = result.getDescription();
            }

            ChannelHandlerContext webCtx = CCM.getCtx(CCM.ClientType.WEB, httpCtx.channel().remoteAddress().toString());
            if (webCtx != null) {
                WebSocketMsgHandler.sendWebSocketText(webCtx, description);
            } else {
                HttpMsgHandler.sendHttpRsp(httpCtx, code, description);
            }
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 log上传回复
    //-----------------
    private void procMtTermLogUploadRsp(ChannelHandlerContext ctx, Message message) {
        MtTermLogUploadRsp mtTermLogUploadRsp = (MtTermLogUploadRsp) message.getBody();
        logger.debug(ctx, "MtTermLogUpload result:" + mtTermLogUploadRsp.getResult().getCode());

        MtTermLogUploadReq req = (MtTermLogUploadReq) CmdUtils.get(ctx).getCurrCmdMessage().getBody();
        MtCodes result = mtTermLogUploadRsp.getResult();
        int code = result.getCode();
        ErrCode.ServerCode retCode = ErrCode.ServerCode.valueOf((short) code);
        switch (retCode) {
            case ERR_USER_NOT_LOGIN:
                crmService.userSyncToState(ctx, ClientState.CLIENT_STATE_UNKNOWN);
                break;

            case USER_OPERATING:
                if (mtTermLogUploadRsp.hasSize()) {
                    int size = mtTermLogUploadRsp.getSize();
                    if (size == 0) {
                        //日志内容为空
                    } else {
                        //日志内容过大
                    }
                }
                mongoDbUtil.dbMtUserUploadLogStatus(req.getLogPath().split("\\.")[0], LogsState.FAIL.value());
                break;

            case ERR_OK:
                mongoDbUtil.dbMtUserUploadLogStatus(req.getLogPath().split("\\.")[0], LogsState.RECEIVED.value());
                break;
        }

        ChannelHandlerContext httpCtx = CmdUtils.get(ctx).getCurrCmdHttpCtx();
        if (httpCtx != null) {
            String description = retCode.cnErrMsg();
            if (result.hasDescription()) {
                description = result.getDescription();
            }
            if (mtTermLogUploadRsp.hasSize()) {
                description += "\nsize:" + mtTermLogUploadRsp.getSize();
            }
            HttpMsgHandler.sendHttpRsp(httpCtx, code, description);
        }
    }

    //===========================  Indication  =============================

    //----------------------------------------------------------------------------
    //处理终端 实时trace log通知
    //-----------------
    private void procRTraceInd(ChannelHandlerContext ctx, Message message) {
        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByIpAddr(crmService.getIpAddr(ctx));
        if (onLineUser == null || onLineUser.getClientState() == ClientState.CLIENT_STATE_UNKNOWN) {
            logger.debug(ctx, "Receive RTraceInd before login.");
        } else {
            String userId = onLineUser.getUserId();
            ArrayList<ChannelHandlerContext> webCtxs = CCM.getWebCtxs(userId);
            for (ChannelHandlerContext webctx : webCtxs) {
                MtRTraceInd mtRTraceInd = (MtRTraceInd) message.getBody();
                String line = mtRTraceInd.getLine();
                WebSocketMsgHandler.sendWebSocketText(webctx, line);
                logger.debug(ctx, String.format("RTraceInd: %s", line));
            }
        }
    }

    //----------------------------------------------------------------------------
    //处理终端 错误消息
    //-----------------
    private void procMtBadMsg(ChannelHandlerContext ctx, Message message) {
        logger.error(ctx, "Mt bad msg found:" + message);
    }
}