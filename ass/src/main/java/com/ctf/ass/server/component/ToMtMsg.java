package com.ctf.ass.server.component;

import com.ctf.ass.server.utils.CmdUtils;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.mt_body.MtCell;
import com.ctf.ass_codec.mt_body.MtCodes;
import com.ctf.ass_codec.mt_body.MtHeartBeatRsp;
import com.ctf.ass_codec.mt_body.MtSimPoolRsp;
import com.ctf.ass_codec.mt_body.MtTermAssignUimReq;
import com.ctf.ass_codec.mt_body.MtTermInfoReq;
import com.ctf.ass_codec.mt_body.MtTermLogUploadReq;
import com.ctf.ass_codec.mt_body.MtTermLogoutReq;
import com.ctf.ass_codec.mt_body.MtTermRevUimReq;
import com.ctf.ass_codec.mt_body.MtTermTraceReq;
import com.ctf.ass_codec.mt_body.MtUim;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.ClientState;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.LogsState;
import com.ctf.ass_public.struct.MtUser;
import com.ctf.ass_public.struct.OnLineUser;
import com.ctf.ass_public.struct.SimCard;
import com.ctf.oss.CCM;
import com.ctf.oss.CRMService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Charles
 * @create 2017/7/21 15:15
 */
@Component
public class ToMtMsg {
    @Autowired
    private MongoDbUtil mongoDbUtil;
    @Autowired
    private CRMService crmService;
    private static LogWrapper logger = LogWrapper.getLogger(ToMtMsg.class);
    //发送 SimPool回复
    public void sendMtSimPoolRsp(ChannelHandlerContext ctx, short seq_no, ErrCode.ServerCode code) {
        MtCodes result = new MtCodes(code.value(),code.enErrMsg());
        MtSimPoolRsp mtSimPoolRsp = new MtSimPoolRsp(result);

        Message simApduAck =  new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.SIMPOOL_VALUE,
                seq_no,
                (byte)0,
                mtSimPoolRsp);
        CmdUtils.get(ctx).sendRsp(simApduAck);
    }

    //发送 SimPool回复
    public boolean sendMtSimPoolRsp(ChannelHandlerContext ctx, short seq_no, ErrCode.ServerCode code, byte[] response) {
        MtCodes result = new MtCodes(code.value(),code.enErrMsg());
        MtSimPoolRsp mtSimPoolRsp = new MtSimPoolRsp(result, response);

        Message apdu_rsp = new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.SIMPOOL_VALUE,
                seq_no,
                (byte)0,
                mtSimPoolRsp);
        return CmdUtils.get(ctx).sendMessage(apdu_rsp);    //Note!!! Forward only, no resend
    }

    //发送 未登录错误 回复
    public void sendMtNotLoginRsp(ChannelHandlerContext ctx, Message message) {
        MtCodes result = new MtCodes(ErrCode.ServerCode.ERR_USER_NOT_LOGIN.value(), ErrCode.ServerCode.ERR_USER_NOT_LOGIN.enErrMsg());
        MtHeartBeatRsp body = new MtHeartBeatRsp(result);
        Message rsp = new Message(Message.ToMtTag,
                (short)message.getCmdId(),
                message.getSeqNo(),
                (byte)0,
                body);
        CmdUtils.get(ctx).sendRsp(rsp);
    }

    //-------------------------------------------------------------------------------------------------------------
    //终端信息 请求
    public boolean sendMtTermInfoReq(ChannelHandlerContext ctx) {
        MtTermInfoReq mtTermInfoReq = new MtTermInfoReq();
        Message req = new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.TERMINFO_VALUE,
                CmdUtils.get(ctx).getIncCmdSeqNo(),
                (byte)0,
                mtTermInfoReq);
        return CmdUtils.get(ctx).sendCmd(req);
    }

    //来自web的 终端信息 请求
    public ErrCode.ServerCode sendHttpMtTermInfoReq(ChannelHandlerContext httpCtx, String userId) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.INVALID_PARAMETER;
        if (userId != null && !userId.equals("")) {
            if (mongoDbUtil.getDbUser(userId) != null) {
                OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                if (onLineUser != null) {
                    ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr());
                    MtTermInfoReq mtTermInfoReq = new MtTermInfoReq();
                    Message req = new Message(Message.ToMtTag,
                            (short) MtProtoMsg.CMDID.TERMINFO_VALUE,
                            CmdUtils.get(userCtx).getIncCmdSeqNo(),
                            (byte)0,
                            mtTermInfoReq);
                    if (CmdUtils.get(userCtx).httpSendCmd(httpCtx, req)) {
                        errCode = ErrCode.ServerCode.ERR_OK;
                    } else {
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                    }
                } else {
                    errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }
        return errCode;
    }

    //请求还卡
    public boolean sendMtTermRevUimReq(ChannelHandlerContext ctx, int code, int retry_wait_sec) {
        MtCodes cause = new MtCodes(code, -1, retry_wait_sec,  ErrCode.ServerCode.valueOf((short) code).enErrMsg());
        MtTermRevUimReq mtTermRevUimReq = new MtTermRevUimReq(cause);

        Message req = new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.TERMREVUIM_VALUE,
                CmdUtils.get(ctx).getIncCmdSeqNo(),
                (byte)0,
                mtTermRevUimReq);
        return CmdUtils.get(ctx).sendCmd(req);
    }

    //来自web的请求还卡
    public ErrCode.ServerCode sendHttpMtTermRevUimReq(ChannelHandlerContext httpCtx, String userId, int code) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.INVALID_PARAMETER;
        if (StringUtils.isNotEmpty(userId)) {
            MtUser mtUser = mongoDbUtil.getDbUser(userId);
            if (mtUser != null) {
                switch(crmService.userGetState(userId)) {
                    case CLIENT_STATE_ASSIGNED:
                    {
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;

                        MtCodes cause = new MtCodes(code, -1, 0, ErrCode.ServerCode.valueOf((short) code).enErrMsg());
                        MtTermRevUimReq mtTermRevUimReq = new MtTermRevUimReq(cause);

                        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                        if (onLineUser != null) {
                            String ipAddr = onLineUser.getIpAddr();
                            ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, ipAddr);
                            if (userCtx != null) {
                                Message req = new Message(Message.ToMtTag,
                                        (short) MtProtoMsg.CMDID.TERMREVUIM_VALUE,
                                        CmdUtils.get(userCtx).getIncCmdSeqNo(),
                                        (byte) 0,
                                        mtTermRevUimReq);
                                if (CmdUtils.get(userCtx).httpSendCmd(httpCtx, req)) {
                                    errCode = ErrCode.ServerCode.ERR_OK;
                                }
                            }
                        }
                    }
                    break;

                    case CLIENT_STATE_WAITING_ASSIGN:
                    case CLIENT_STATE_LOGINED:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_REQUIM;
                        break;

                    case CLIENT_STATE_UNKNOWN:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        break;
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }
        return errCode;
    }

    //请求退录
    public boolean sendMtTermLogoutReq(ChannelHandlerContext ctx, int code, int retry_wait_sec) {
        MtCodes cause = new MtCodes(code, -1, retry_wait_sec, ErrCode.ServerCode.valueOf((short) code).enErrMsg());
        MtTermLogoutReq mtTermLogoutReq = new MtTermLogoutReq(cause);

        Message req = new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.TERMLOGOUT_VALUE,
                CmdUtils.get(ctx).getIncCmdSeqNo(),
                (byte)0,
                mtTermLogoutReq);
        return CmdUtils.get(ctx).sendCmd(req);
    }

    //来自web的退录请求
    public ErrCode.ServerCode sendHttpMtTermLogoutReq(ChannelHandlerContext httpCtx, String userId, int code) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.INVALID_PARAMETER;
        if (StringUtils.isNotEmpty(userId)) {
            MtUser mtUser = mongoDbUtil.getDbUser(userId);
            if (mtUser != null) {
                switch(crmService.userGetState(userId)) {
                    case CLIENT_STATE_ASSIGNED:
                    case CLIENT_STATE_WAITING_ASSIGN:
                        crmService.userSyncToState(mongoDbUtil.getOnLineUserByUserId(userId), ClientState.CLIENT_STATE_LOGINED);
                    case CLIENT_STATE_LOGINED:
                        int retry_wait_sec = 0;
                        switch(code) {
                            case 0:
                                retry_wait_sec = 0;
                                break;
                        }
                        String ipAddr = mongoDbUtil.getOnLineUserByUserId(userId).getIpAddr();
                        ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, ipAddr);
                        MtCodes cause = new MtCodes(code, -1, retry_wait_sec, ErrCode.ServerCode.valueOf((short) code).enErrMsg());
                        MtTermLogoutReq mtTermLogoutReq = new MtTermLogoutReq(cause);

                        Message req = new Message(Message.ToMtTag,
                                (short) MtProtoMsg.CMDID.TERMLOGOUT_VALUE,
                                CmdUtils.get(userCtx).getIncCmdSeqNo(),
                                (byte)0,
                                mtTermLogoutReq);
                        CmdUtils.get(userCtx).httpSendCmd(httpCtx, req);
                        errCode = ErrCode.ServerCode.ERR_OK;
                        break;

                    case CLIENT_STATE_UNKNOWN:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        break;
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }
        return errCode;
    }

    //主动分卡
    public boolean sendMtTermAssignUimReq(ChannelHandlerContext ctx, String imsi, byte[] img_md5, ArrayList<MtCell> mtCells, byte[] sessionId) {
        MtCodes cause = new MtCodes(ErrCode.ServerCode.ERR_OK.value(), ErrCode.ServerCode.ERR_OK.enErrMsg());
        MtUim uim = new MtUim(imsi, img_md5, mtCells);
        MtTermAssignUimReq mtTermAssignUimReq = new MtTermAssignUimReq(cause, uim, sessionId);

        Message req = new Message(Message.ToMtTag,
                (short) MtProtoMsg.CMDID.TERMASSIGNUIM_VALUE,
                CmdUtils.get(ctx).getIncCmdSeqNo(),
                (byte)0,
                mtTermAssignUimReq);
        return CmdUtils.get(ctx).sendCmd(req);
    }

    //来自web的主动分卡请求
    public ErrCode.ServerCode sendHttpMtTermAssignUimReq(ChannelHandlerContext httpCtx, String userId, String imsi) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.INVALID_PARAMETER;
        if (StringUtils.isNotEmpty(userId) && StringUtils.isNotEmpty(imsi)) {
            MtUser mtUser = mongoDbUtil.getDbUser(userId);
            if (mtUser != null) {
                switch(crmService.userGetState(userId)) {
                    case CLIENT_STATE_ASSIGNED:
                        crmService.userSyncToState(mongoDbUtil.getOnLineUserByUserId(userId), ClientState.CLIENT_STATE_WAITING_ASSIGN);
                    case CLIENT_STATE_WAITING_ASSIGN:
                    case CLIENT_STATE_LOGINED:
                        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                        if (onLineUser == null){
                            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                            break;
                        }
                        ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr());
                        SimCard simCard = mongoDbUtil.getDbSimCard(imsi);
                        if (simCard == null){
                            errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
                            break;
                        }
                        if (!simCard.getIsAssignable(userId)){
                            errCode = ErrCode.ServerCode.RUR_SIM_NOT_AVAILABLE;
                            break;
                        }
                        ArrayList<MtCell> cell_list = new ArrayList<>();
                        cell_list.add(new MtCell(460, 0, 0));
                        MtUim uim = new MtUim(imsi, simCard.getImgMd5(), cell_list);
                        MtTermAssignUimReq mtTermAssignUimReq = new MtTermAssignUimReq(new MtCodes(ErrCode.ServerCode.ERR_OK.value(), ErrCode.ServerCode.ERR_OK.enErrMsg()), uim, onLineUser.getSessionId());

                        Message req = new Message(Message.ToMtTag,
                                (short) MtProtoMsg.CMDID.TERMASSIGNUIM_VALUE,
                                CmdUtils.get(userCtx).getIncCmdSeqNo(),
                                (byte)0,
                                mtTermAssignUimReq);
                        if (CmdUtils.get(userCtx).httpSendCmd(httpCtx, req)) {
                            errCode = ErrCode.ServerCode.ERR_OK;
                        } else {
                            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        }
                        break;

                    case CLIENT_STATE_UNKNOWN:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        break;
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }
        return errCode;
    }

    //来自web的实时调试请求
    public ErrCode.ServerCode sendHttpMtTermTraceReq(ChannelHandlerContext httpCtx, String userId, MtProtoMsg.LEVEL level) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.INVALID_PARAMETER;
        if (StringUtils.isNotEmpty(userId)) {
            MtUser mtUser = mongoDbUtil.getDbUser(userId);
            if (mtUser != null) {
                switch(crmService.userGetState(userId)) {
                    case CLIENT_STATE_ASSIGNED:
                    case CLIENT_STATE_WAITING_ASSIGN:
                    case CLIENT_STATE_LOGINED:
                        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                        ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr());
                        MtTermTraceReq mtTermTraceReq = new MtTermTraceReq(level != null ? 1 : 0, level);

                        Message req = new Message(Message.ToMtTag,
                                (short) MtProtoMsg.CMDID.TERMTRACE_VALUE,
                                CmdUtils.get(userCtx).getIncCmdSeqNo(),
                                (byte)0,
                                mtTermTraceReq);
                        if (CmdUtils.get(userCtx).httpSendCmd(httpCtx, req)) {
                            errCode = ErrCode.ServerCode.ERR_OK;
                        } else {
                            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        }
                        break;

                    case CLIENT_STATE_UNKNOWN:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        break;
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }
        return errCode;
    }

    //来自web的日志上传请求
    public ErrCode.ServerCode sendHttpMtTermLogUploadReq(ChannelHandlerContext httpCtx, String userId, long s_second, long e_second, MtProtoMsg.LEVEL level, String logpath, long log_size_limit) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.INVALID_PARAMETER;
        if (StringUtils.isNotEmpty(userId)) {
            MtUser mtUser = mongoDbUtil.getDbUser(userId);
            if (mtUser != null) {
                switch(crmService.userGetState(userId)) {
                    case CLIENT_STATE_ASSIGNED:
                    case CLIENT_STATE_WAITING_ASSIGN:
                    case CLIENT_STATE_LOGINED:
                        OnLineUser onLineUser = mongoDbUtil.getOnLineUserByUserId(userId);
                        ChannelHandlerContext userCtx = CCM.getCtx(CCM.ClientType.MT, onLineUser.getIpAddr());
                        MtTermLogUploadReq mtTermLogUploadReq = new MtTermLogUploadReq((int)s_second, (int)e_second, level, logpath, (int)log_size_limit);
                        Message req = new Message(Message.ToMtTag,
                                (short) MtProtoMsg.CMDID.TERMLOGUPLOAD_VALUE,
                                CmdUtils.get(userCtx).getIncCmdSeqNo(),
                                (byte) 0,
                                mtTermLogUploadReq);
                        if (CmdUtils.get(userCtx).httpSendCmd(httpCtx, req)) {
                            mongoDbUtil.dbMtUserUploadLogStatus(logpath.split("\\.")[0], LogsState.SENDTO.value());
                            errCode = ErrCode.ServerCode.ERR_OK;
                        } else {
                            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        }
                        break;

                    case CLIENT_STATE_UNKNOWN:
                        errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
                        break;
                }
            } else {
                errCode = ErrCode.ServerCode.ERR_USER_NOT_EXIST;
            }
        }
        return errCode;
    }
}
