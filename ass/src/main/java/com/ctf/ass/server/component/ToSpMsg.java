package com.ctf.ass.server.component;

import com.ctf.ass.server.utils.CmdUtils;
import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.sp_body.*;
import com.ctf.ass_codec.struct.Message;

import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.LogsState;
import com.ctf.oss.CCM;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;

@Component
public class ToSpMsg {
    @Autowired
    private MongoDbUtil mongoDbUtil;

    public boolean sendSpSimImgReq(ChannelHandlerContext ctx, short sim_no, String imsi) {
        Message req = new Message(Message.ToSpTag,
                SpBinaryMsg.CmdId.CMD_SP_SIM_IMG.value(),
                CmdUtils.get(ctx).getIncCmdSeqNo(),
                (byte) 0,
                new SpTermSimImgReq(sim_no, imsi));
        return CmdUtils.get(ctx).sendBgCmd(req);
    }

    public boolean sendSpSimApduReq(ChannelHandlerContext ctx, short seq_no, short sim_no, String imsi, byte[] apdu_cmd) {
        Message req_msg = new Message(Message.ToSpTag,
                SpBinaryMsg.CmdId.CMD_SP_SIM_APDU.value(),
                seq_no,
                (byte) 0,
                new SpTermSimApduReq(sim_no, imsi, apdu_cmd));
        return CmdUtils.get(ctx).sendMessage(req_msg);  //Note!!! Forward only, no resend
    }

    //来自web 请求simpool上传日志
    public ErrCode.ServerCode sendHttpSpLogUploadReq(ChannelHandlerContext httpCtx, String macAddress, String logPath, long day_second) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        //获取sp通道
        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
        if (StringUtils.isNotEmpty(ipAddr)){
            ChannelHandlerContext spCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);
            Message req_msg = new Message(Message.ToSpTag,
                    SpBinaryMsg.CmdId.CMD_SP_LOG_UPLOAD.value(),
                    CmdUtils.get(spCtx).getIncCmdSeqNo(),
                    (byte)0,
                    new SpTermLogUploadReq(day_second,logPath));
            if (CmdUtils.get(spCtx).httpSendCmd(httpCtx, req_msg)) {
                mongoDbUtil.dbSpUpdateLogState(logPath.split("\\.")[0], LogsState.SENDTO.value());
                errCode = ErrCode.ServerCode.ERR_OK;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return errCode;
    }


    //来自web 请求simpool软件升级
    public ErrCode.ServerCode sendHttpSpUpgradeReq(ChannelHandlerContext httpCtx, String macAddress, String url) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        //获取sp通道
        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
        if (StringUtils.isNotEmpty(ipAddr)){
            ChannelHandlerContext spCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);
            Message req_msg = new Message(Message.ToSpTag,
                    SpBinaryMsg.CmdId.CMD_SP_UPGRADE.value(),
                    CmdUtils.get(spCtx).getIncCmdSeqNo(),
                    (byte)0,
                    new SpTermUpgradeReq(url));
            if (CmdUtils.get(spCtx).httpSendCmd(httpCtx, req_msg)) {
                mongoDbUtil.doSaveUpgrade(macAddress,url,0);
                errCode = ErrCode.ServerCode.ERR_OK;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return errCode;
    }

    //来自web 控制SP tree
    public ErrCode.ServerCode sendHttpSpTermTraceReq(ChannelHandlerContext httpCtx, String macAddress, boolean on_off) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        //获取sp通道
        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
        if (StringUtils.isNotEmpty(ipAddr)){
            ChannelHandlerContext spCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);
            Message req_msg = new Message(Message.ToSpTag,
                    SpBinaryMsg.CmdId.CMD_SP_TRACE.value(),
                    CmdUtils.get(spCtx).getIncCmdSeqNo(),
                    (byte)0,
                    new SpTermTraceReq(on_off));
            if (CmdUtils.get(spCtx).httpSendCmd(httpCtx, req_msg)) {
                errCode = ErrCode.ServerCode.ERR_OK;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return errCode;
    }

    //来自web 控制SP重启
    public ErrCode.ServerCode sendHttpSpTermRebootReq(ChannelHandlerContext httpCtx, String macAddress, byte cause) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        //获取sp通道
        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
        if (StringUtils.isNotEmpty(ipAddr)){
            ChannelHandlerContext spCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);
            Message req_msg = new Message(Message.ToSpTag,
                    SpBinaryMsg.CmdId.CMD_SP_REBOOT.value(),
                    CmdUtils.get(spCtx).getIncCmdSeqNo(),
                    (byte)0,
                    new SpTermRebootReq(cause));
            if (CmdUtils.get(spCtx).httpSendCmd(httpCtx, req_msg)) {
                errCode = ErrCode.ServerCode.ERR_OK;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return errCode;
    }

    //来自web 控制SP清理日志
    public ErrCode.ServerCode sendHttpSpTermClearLogsReq(ChannelHandlerContext httpCtx, String macAddress, ArrayList<String> fileNameList) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        //获取sp通道
        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
        if (StringUtils.isNotEmpty(ipAddr)){
            ChannelHandlerContext spCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);
            Message req_msg = new Message(Message.ToSpTag,
                    SpBinaryMsg.CmdId.CMD_SP_CLEAR_LOGS.value(),
                    CmdUtils.get(spCtx).getIncCmdSeqNo(),
                    (byte)0,
                    new SpTermClearLogsReq(fileNameList));
            if (CmdUtils.get(spCtx).httpSendCmd(httpCtx, req_msg)) {
                errCode = ErrCode.ServerCode.ERR_OK;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return errCode;
    }

    //来自web 控制SP获取日志
    public ErrCode.ServerCode sendHttpSpTermGetLogsReq(ChannelHandlerContext httpCtx, String macAddress) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.SERR_SERVICE_DOWN;
        //获取sp通道
        String ipAddr = mongoDbUtil.getOnLineSimPoolIpAddr(macAddress);
        if (StringUtils.isNotEmpty(ipAddr)){
            ChannelHandlerContext spCtx = CCM.getCtx(CCM.ClientType.SP, ipAddr);
            Message req_msg = new Message(Message.ToSpTag,
                    SpBinaryMsg.CmdId.CMD_SP_CLEAR_LOGS.value(),
                    CmdUtils.get(spCtx).getIncCmdSeqNo(),
                    (byte)0,
                    new SpTermGetLogsReq());
            if (CmdUtils.get(spCtx).httpSendCmd(httpCtx, req_msg)) {
                errCode = ErrCode.ServerCode.ERR_OK;
            }
        }else{
            errCode = ErrCode.ServerCode.ERR_USER_NOT_LOGIN;
        }
        return errCode;
    }
}
