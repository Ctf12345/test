package com.ctf.autotest;

import com.ctf.ass_codec.mt_body.*;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg.LEVEL;
import com.ctf.ass_codec.struct.*;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_public.utils.CheckUtils;
import com.ctf.ass_public.utils.ConvUtils;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;

public class MockMt extends MockClient {
    private static LogWrapper logger = LogWrapper.getLogger(MockMt.class.getName());

    //mt login account
    private String user_id;
    private String password;
    //mt info
    private String serialno;
    private String model;
    private String sw_ver;
    private String buildno;
    private String ini_ver;
    private String dev_id;

    private ArrayList<MtCell> mtCells = null;
    private String imsi = null;
    private HashMap<byte[], byte[]> md5_imgs = new HashMap<byte[], byte[]>();
    private HashMap<String, byte[]> sim_md5 = new HashMap<String, byte[]>();
    private byte[] sessionId = null;

    private boolean isTraceOn = false;

    private void init() {

    }

    public MockMt(String serialno, String model, String sw_ver, String buildno, String ini_ver, String dev_id) {
        super(true);

        this.serialno = serialno;
        this.model = model;
        this.sw_ver = sw_ver;
        this.buildno = buildno;
        this.ini_ver = ini_ver;

        this.dev_id = dev_id;

        init();
    }

    public MockMt() {
        this("DFDFIEW90D",     //serialno
                "msm8960",      //model
                "6.0",          //sw_ver
                "BDFDIIASD release-keys",   //build_no
                "0.1",          //ini_ver
                null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MockMt {");
        sb.append("serialno:" + serialno + ",");
        sb.append("model:" + model + ",");
        sb.append("sw_ver:" + sw_ver + ",");
        sb.append("buildno:" + buildno + ",");
        sb.append("ini_ver:" + ini_ver + ",");
        sb.append("dev_id:" + dev_id);
        sb.append("}");

        return sb.toString();
    }

    //========================================================
    public String getUserId() {
        return user_id;
    }
    public String getPassword() {
        return password;
    }

    public String getSerialNo() {
        return serialno;
    }

    public String getModel() {
        return model;
    }

    public String getSwVer() {
        return sw_ver;
    }

    public String getBuildNo() {
        return buildno;
    }

    public String getIniVer() {
        return ini_ver;
    }

    public String getDevId() {
        return this.dev_id;
    }
    public void setDevId(String devId) {
        this.dev_id = devId;
    }

    //session id
    public String getImsi() {
        return imsi;
    }
    public void setImsi(String imsi) {
        this.imsi = imsi;
    }
    public String getSessionIdStr() {
        return ConvUtils.bytesToHexStr(sessionId);
    }
    public byte[] getSessionId() {
        return sessionId;
    }
    public void setSessionId(byte[] sessionId) {
        this.sessionId = new byte[sessionId.length];
        System.arraycopy(sessionId, 0, this.sessionId, 0, sessionId.length);
    }
    //simcard imgs
    public boolean cacheSimImg(byte[] img_data) {
        if (imsi != null) {
            byte[] cached_md5 = sim_md5.get(imsi);
            if (cached_md5 != null) {
                md5_imgs.put(cached_md5, img_data);
                return true;
            }
        }

        return false;
    }

    public boolean cacheImgMd5(byte[] img_md5) {
        if (imsi != null) {
            sim_md5.put(imsi, img_md5);
            return true;
        }

        return false;
    }

    public boolean isSimImgCached(byte[] img_md5) {
        byte[] cached_md5 = sim_md5.get(imsi);
        if (cached_md5 != null) {
            if (cached_md5.equals(img_md5) && md5_imgs.get(img_md5) != null) {   //image MockEnv matched
                return true;
            } else {    //image out-updated
                md5_imgs.remove(cached_md5);
                sim_md5.remove(imsi);
            }
        }

        return false;
    }

    //state
    public void clientSyncToState(MockEnv.MockClientState new_state) {
        setMockClientState(new_state);
    }

    //trace
    public boolean getIsTraceOn() {
        return this.isTraceOn;
    }
    public void setIsTraceOn(boolean isTraceOn) {
        this.isTraceOn = isTraceOn;
    }
    //log
    public int getLogSize(int s_seconds, int e_seconds, LEVEL level) {
        return 20*1024;
    }

    public byte[] getLogData(int s_seconds, int e_seconds, LEVEL level) {
        return ConvUtils.readResFileBytes("ruimd.log");
    }

    //=================================================================================================================
    //终端 连接服务器
    public boolean connsvr() throws Exception {
        if (connectToAss()) {
            logger.debug("MockMt start at " + getLocalAddress());
            //切换到已连接状态
            clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_CONNECTED);
        } else {
            logger.fatal("MockMt init Failed");
        }

        return true;
    }

    public void onClose() {
        logger.debug("onClose was called!");
        clearCtx();
        clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_OFFLINE);
    }

    //----------------  终端 => 服务器  ----------------
    //终端 登录
    public boolean login(String user_id,
                         String password,
                         String dev_id,
                         int proto_ver,
                         String codes_ver,

                         boolean bWaitRsp,
                         String expect_ret,
                         String desc) throws TestException {
        //协议参数
        if (user_id == null) {
            user_id = this.user_id;
        } else {
            this.user_id = user_id;
        }
        if (password == null) {
            password = this.password;
        } else {
            this.password = password;
        }
        if (dev_id == null) {
            dev_id = this.dev_id;
        } else {
            this.dev_id = dev_id;
        }
        //断言参数
        AssertMessage assertMessage = new AssertMessage( MtProtoMsg.CMDID.LOGIN.name(),
                                                         CmdUtils.get(getCtx()).getCmdSeqNo(),
                                                         desc
        ).addMtExpectRet(expect_ret);
        //---------
        MtLoginReq mtLoginReq = new MtLoginReq(user_id,
                                                ConvUtils.bytesToHexStr(CheckUtils.MD5(password.getBytes())),
                                                dev_id,
                                                proto_ver,
                                                codes_ver);
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.LOGIN_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtLoginReq);

        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 使用session ID登录
    public boolean loginById(String sessionIdStr,

                             boolean bWaitRsp,
                             String expect_ret,
                             String desc) throws TestException {
        //协议参数
        byte[] sessionId;
        if (sessionIdStr == null) {
            sessionId = this.sessionId;
        } else {
            sessionId = ConvUtils.hexStrToBytes(sessionIdStr);
        }
        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.LOGINBYID.name(),
                                                        CmdUtils.get(getCtx()).getCmdSeqNo(),
                                                        desc
        ).addMtExpectRet(expect_ret);
        //---------
        MtLoginByIdReq mtLoginByIdReq = new MtLoginByIdReq(sessionId);
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.LOGINBYID_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtLoginByIdReq);

        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 分卡
    public boolean requim(String cells, String imsi,
                          boolean bWaitRsp, String expect_ret, String expect_imsi, String expect_imgmd5, String desc) throws TestException {
        //协议参数
        ArrayList<MtCell> mtCells;
        if (cells != null) {
            mtCells = new ArrayList<>();
            for (String plmn_rat : cells.split(";")) {
                String[] sArr = plmn_rat.split(",");
                String plmn = sArr[0];
                int mcc = Integer.parseInt(plmn.substring(0, 3));
                int mnc = Integer.parseInt(plmn.substring(3));
                if (mnc < 0xF00) {
                    mnc += 0xF00;
                }
                int rat = Integer.parseInt(sArr[1]);
                mtCells.add(new MtCell(mcc, mnc, rat));   //modify mcc, mnc here to get different SimCard
            }
            this.mtCells = mtCells;
        } else {
            mtCells = this.mtCells;
        }
        if (imsi == null) {
            imsi = this.imsi;
        }
        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.REQUIM.name(),
                                                        CmdUtils.get(getCtx()).getCmdSeqNo(),
                                                        desc
        ).addMtExpectRet(expect_ret);
        if (expect_imsi != null) {
            assertMessage.addAssertBodyParam(new AssertBodyParam("expect_imsi",
                    expect_imsi,
                    "msgBody.getUim().getImsi()"));
        }
        if (expect_imgmd5 != null) {
            assertMessage.addAssertBodyParam(new AssertBodyParam("expect_imgmd5",
                    expect_imgmd5,
                    "ConvUtils.bytesToHexStr(msgBody.getUim().getImgmd5())"));
        }
        //--------
        MtModem modem = new MtModem(null, imsi, mtCells, -1, -1);
        MtReqUimReq mtReqUimReq = new MtReqUimReq(modem);

        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.REQUIM_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtReqUimReq);
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 SIM卡镜像
    public boolean simimg(boolean bWaitRsp, String expect_ret, String expect_img_data, String desc) throws TestException {
        //协议参数

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.SIMIMG.name(),
                                                        CmdUtils.get(getCtx()).getCmdSeqNo(),
                                                        desc
        ).addMtExpectRet(expect_ret);
        if (expect_img_data != null) {
            assertMessage.addAssertBodyParam(new AssertBodyParam("expect_img_data",
                    expect_img_data,
                    "ConvUtils.bytesToHexStr(msgBody.getImgData())"));
        }

        //--------
        MtSimImgReq mtSimImgReq = new MtSimImgReq();
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.SIMIMG_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtSimImgReq);
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 发送命令给simpool
    public boolean simpool(String apdu_hexstr,
                           boolean bWaitRsp, String expect_ret, String expect_response, String desc) throws TestException {
        //协议参数
        byte[] apdu_data = ConvUtils.hexStrToBytes(apdu_hexstr);

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.SIMPOOL.name(),
                                                        CmdUtils.get(getCtx()).getCmdSeqNo(),
                                                        desc
        ).addMtExpectRet(expect_ret);
        if (expect_response != null) {
            assertMessage.addAssertBodyParam(new AssertBodyParam("expect_response",
                    expect_response,
                    "ConvUtils.bytesToHexStr(msgBody.getResponse())"));
        }

        //--------
        MtSimPoolReq mtSimPoolReq = new MtSimPoolReq(apdu_data);
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.SIMPOOL_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtSimPoolReq);

        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 还卡
    public boolean revuim(ServerCode code,
                          boolean bWaitRsp, String expect_ret, String desc) throws TestException {
        //协议参数
        MtCodes cause = new MtCodes(code.value());

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.REVUIM.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc
        ).addMtExpectRet(expect_ret);

        //--------
        MtRevUimReq mtRevUimReq = new MtRevUimReq(cause);
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.REVUIM_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtRevUimReq);

        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 退录
    public boolean logout(ServerCode code,
                          boolean bWaitRsp, String expect_ret, String desc) throws TestException {
        //协议参数
        MtCodes cause = new MtCodes(code.value());

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.LOGOUT.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc
        ).addMtExpectRet(expect_ret);

        //--------
        MtLogoutReq mtLogoutReq = new MtLogoutReq(cause);
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.LOGOUT_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtLogoutReq);
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 警告
    public boolean alert(ServerCode code,
                         boolean bWaitRsp, String expect_ret, String desc) throws TestException {
        //协议参数
        MtCodes cause = new MtCodes(code.value());

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.ALERTS.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc
        ).addMtExpectRet(expect_ret);

        //--------
        MtAlertReq mtAlertReq = new MtAlertReq(cause);
        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.ALERTS_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtAlertReq);
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 实时trace Ind
    public boolean traceInd(String line,
                            String desc) {
        //协议参数

        //断言参数

        //--------
        MessageBody mtRTraceInd = new MtRTraceInd(line);
        Message ind = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.RTRACE_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                mtRTraceInd);
        return CmdUtils.get(getCtx()).sendCmd(ind, false);
    }

    //终端 心跳包
    public boolean heartbeat(boolean bWaitRsp, String expect_ret, String desc) throws TestException {
        //协议参数

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.HEARTBEAT.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc
        ).addMtExpectRet(expect_ret);

        //--------
        MtModem modem = new MtModem();
        MtHeartBeatReq heartBeatReq = new MtHeartBeatReq(modem);

        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.HEARTBEAT_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                heartBeatReq);
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //终端 上传日志
    public boolean mtLoguploadret( String logpath,
                                   MtCodes result,
                                   boolean bWaitRsp, String expect_ret, String desc) throws TestException {
        //协议参数

        //断言参数
        AssertMessage assertMessage = new AssertMessage(MtProtoMsg.CMDID.LOGUPLOADRET.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc
        ).addMtExpectRet(expect_ret);

        //--------
        MtLogUploadRetReq loguploadretReq = new MtLogUploadRetReq(logpath,result);

        Message req = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.LOGUPLOADRET_VALUE,
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                loguploadretReq);
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }



    //=======================================================================
    public void term_mt_revuim_rsp(ServerCode retCode, String desc) throws TestException {
        MtCodes result = new MtCodes(retCode.value());

        MtTermRevUimRsp mtTermRevUimRsp = new MtTermRevUimRsp(result);
        ChannelHandlerContext ctx = getCtx();
        CmdUtils cmdUtils = CmdUtils.get(ctx);
        Message rsp = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.TERMREVUIM_VALUE,
                cmdUtils.getLastTermReq().getSeqNo(),
                (byte)0,
                mtTermRevUimRsp);
        cmdUtils.sendRsp(rsp);
    }

    public void term_mt_logout_rsp(ServerCode retCode, String desc) throws TestException {
        MtCodes result = new MtCodes(retCode.value());

        MtTermLogoutRsp mtTermLogoutRsp = new MtTermLogoutRsp(result);
        ChannelHandlerContext ctx = getCtx();
        CmdUtils cmdUtils = CmdUtils.get(ctx);
        Message rsp = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.TERMLOGOUT_VALUE,
                cmdUtils.getLastTermReq().getSeqNo(),
                (byte)0,
                mtTermLogoutRsp);
        cmdUtils.sendRsp(rsp);
    }

    public void term_mt_assignuim_rsp(ServerCode retCode, String desc) throws TestException {
        MtCodes result = new MtCodes(retCode.value());

        MtTermAssignUimRsp mtTermAssignUimRsp = new MtTermAssignUimRsp(result);
        ChannelHandlerContext ctx = getCtx();
        CmdUtils cmdUtils = CmdUtils.get(ctx);
        Message rsp = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.TERMASSIGNUIM_VALUE,
                cmdUtils.getLastTermReq().getSeqNo(),
                (byte)0,
                mtTermAssignUimRsp);
        cmdUtils.sendRsp(rsp);
    }

    public void term_mt_trace_rsp(ServerCode retCode, String desc) throws TestException {
        MtCodes result = new MtCodes(retCode.value());

        MtTermTraceRsp mtTermTraceRsp = new MtTermTraceRsp(result);
        ChannelHandlerContext ctx = getCtx();
        CmdUtils cmdUtils = CmdUtils.get(ctx);
        Message rsp = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.TERMTRACE_VALUE,
                cmdUtils.getLastTermReq().getSeqNo(),
                (byte)0,
                mtTermTraceRsp);
        cmdUtils.sendRsp(rsp);
    }


    public void term_mt_logupload_rsp(ServerCode retCode, String desc) throws TestException {
        MtCodes result = new MtCodes(retCode.value());

        MtLogUploadRetRsp mtLoguploadRsp = new MtLogUploadRetRsp(result);
        ChannelHandlerContext ctx = getCtx();
        CmdUtils cmdUtils = CmdUtils.get(ctx);
        Message rsp = new Message(Message.MtTag,
                (short) MtProtoMsg.CMDID.TERMLOGUPLOAD_VALUE,
                cmdUtils.getLastTermReq().getSeqNo(),
                (byte)0,
                mtLoguploadRsp);
        cmdUtils.sendRsp(rsp);
    }


}
