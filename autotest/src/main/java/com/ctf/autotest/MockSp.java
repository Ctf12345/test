package com.ctf.autotest;

import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.sp_body.*;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.struct.SimPool;
import com.ctf.ass_public.utils.ConvUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MockSp extends MockClient {
    //日志
    private static LogWrapper logger = LogWrapper.getLogger(MockSp.class.getName());

    //===========================================================================================================
    //成员变量
    //==========
    //-------------------------------------------------------------------------------------
    //登录需要的基本信息
    private String mac_addr_str;
    private Integer max_sim_count;
    private String hw_ver;
    private String sw_ver;
    //在位的Sim卡
    private HashMap<Short, MockSim> mockSims;

    private byte[] macstr2bytes(String mac_addr_str) {
        return ConvUtils.hexStrToBytes(mac_addr_str.replace(":", ""));
    }

    //===========================================================================================================
    //构造函数
    //==========
    public MockSp(String macaddr_hexstr, int max_sim_count, String hw_ver, String sw_ver) {
        super(false);
        
        this.mac_addr_str = macaddr_hexstr;
        this.max_sim_count = max_sim_count;
        this.hw_ver = hw_ver;
        this.sw_ver = sw_ver;

        this.mockSims = new HashMap<Short, MockSim>();
    }

    public MockSp() {
        this(null, 0, null, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MockSp {");
        sb.append("mac_addr:" + mac_addr_str + ",");
        sb.append("max_sim_count:" + max_sim_count + ",");
        sb.append("hw_ver:" + hw_ver + ",");
        sb.append("sw_ver:" + sw_ver);
        sb.append("}");

        return sb.toString();
    }

    //===========================================================================================================
    //getter/setter函数
    //==================
    //MAC地址
    public String getMacAddrStr() {
        return mac_addr_str;
    }
    public void setMacAddrStr(String macaddr_hexstr) {
        this.mac_addr_str = macaddr_hexstr;
    }
    public void initByMacAddrStr(String macaddr_hexstr) {
        this.mac_addr_str = macaddr_hexstr.toLowerCase();
        SimPool simPool = MongoDbUtil.getDbSimPool(mac_addr_str);
        if (simPool != null) {
            this.max_sim_count = simPool.getCapacity();
            this.hw_ver = simPool.getHardwareVersion();
            this.sw_ver = simPool.getSoftwareVersion();
        }
    }

    //卡池容量
    public Integer getMaxSimCount() {
        return max_sim_count;
    }
    public void setMaxSimCount(Integer max_sim_count) {
        this.max_sim_count = max_sim_count;
    }

    //硬件版本
    public String getHwVer() {
        return hw_ver;
    }
    public void setHwVer(String hw_ver) {
        this.hw_ver = hw_ver;
    }

    //软件版本
    public String getSwVer() {
        return sw_ver;
    }
    public void setSwVer(String sw_ver) {
        this.sw_ver = sw_ver;
    }

    // ----------
    // SIM卡信息
    // ----------
    //获取所有sim卡信息
    public HashMap<Short, MockSim> getMockSims() {
        return this.mockSims;
    }
    //添加sim卡
    public void addMockSim(int locationInSimPool, MockSim mockSim) {
        this.mockSims.put((short)locationInSimPool, mockSim);
    }
    //移除sim卡
    public void delMockSim(int locationInSimPool) {
        this.mockSims.remove((short)locationInSimPool);
    }
    //获取sim卡信息(按位置编号)
    public MockSim getMockSim(int locationInSimPool) {
        return this.mockSims.get((short)locationInSimPool);
    }
    //获取sim卡信息(按IMSI号)
    public MockSim getMockSim(String imsi) {
        for (MockSim mockSim : this.mockSims.values()) {
            if (mockSim.getImsi().equals(imsi)) {
                return mockSim;
            }
        }

        return null;
    }

    //===========================================================================================================
    //业务函数
    //==========
    //连接服务器
    public void connsvr() throws Exception {
        if (connectToAss()) {
            logger.debug("MockSp start at " + getLocalAddress());
        } else {
            logger.fatal("MockSp init Failed");
        }
    }

    public void onClose() {
        logger.debug("onClose was called!");
        clearCtx();
        //clientSyncToState(MockEnv.MockClientState.CLIENT_STATE_UNKNOWN);
    }

    //登录
    public boolean login(String macHexStr, Integer maxSimCount, String hwVer, String swVer,
                         boolean bWaitRsp, String expectRet, String desc) throws TestException {
        //协议参数
        if (this.mac_addr_str != null) {
            if (macHexStr == null) {
                macHexStr = getMacAddrStr();
            }
        } else {
            if (macHexStr != null) {
                initByMacAddrStr(macHexStr);
            } else {
                //出错
                throw new TestException("login", "macHexStr", "Not null", macHexStr);
            }
        }
        if (maxSimCount == null) {
            maxSimCount = getMaxSimCount();
        }
        if (hwVer == null) {
            hwVer = getHwVer();
        }
        if (swVer == null) {
            swVer = getSwVer();
        }

        //断言参数
        AssertMessage assertMessage = new AssertMessage(SpBinaryMsg.CmdId.CMD_SP_LOGIN.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc).addSpExpectRet(expectRet);

        //----------
        Message req = new Message(Message.SpTag,
                SpBinaryMsg.CmdId.CMD_SP_LOGIN.value(),
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                new SpLoginReq(macstr2bytes(macHexStr),
                        maxSimCount.shortValue(),
                        hwVer,
                        swVer,
                        "3141592653580001")
        );
        return sendCmdAndAssertRsp(req, bWaitRsp,assertMessage);
    }

    private ArrayList<SpSimInfo> packSpSimInfo(HashMap<Short, MockSim> mockSims) {
        ArrayList<SpSimInfo> simInfos = new ArrayList<SpSimInfo>();
        for (Iterator<Map.Entry<Short, MockSim>> it = mockSims.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Short, MockSim> item = it.next();

            short locationInSimPool = item.getKey();
            MockSim mockSim = item.getValue();

            SpSimInfo simInfo = new SpSimInfo(
                    locationInSimPool,
                    mockSim.getIccid(),
                    mockSim.getImsi(),
                    (byte)(mockSim.getStatus() == MockSim.SimStatus.Normal ? 0 : 0x20),
                    mockSim.getImgMd5()
            );
            simInfos.add(simInfo);
        }
        return simInfos;
    }

    //SIM卡信息
    public boolean simInfo(Integer sim_count, HashMap<Short, MockSim> mockSims,
                           boolean bWaitRsp, String expectRet, String expectInvalidSimNos, String desc) throws TestException {
        //协议参数
        if (mockSims == null) {
            mockSims = this.mockSims;
        } else {
            this.mockSims = mockSims;
        }
        if (sim_count == null) {
            sim_count = this.mockSims.size();
        }

        //断言参数
        AssertMessage assertMessage = new AssertMessage(SpBinaryMsg.CmdId.CMD_SP_SIM_INFO.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc).addSpExpectRet(expectRet);
        if (expectInvalidSimNos != null) {
            assertMessage.addAssertBodyParam(new AssertBodyParam("expectInvalidSimNos",
                    expectInvalidSimNos,
                    "msgBody.getInvalidSimNos().toString()"));
        }

        Message req = new Message(Message.SpTag,
                SpBinaryMsg.CmdId.CMD_SP_SIM_INFO.value(),
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                new SpSimInfoReq(packSpSimInfo(mockSims)));
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //SIM卡信息更新
    public boolean simInfoUpdate(Integer sim_count, HashMap<Short, MockSim> mockSims,
                                 boolean bWaitRsp, String expectRet, String expectInvalidSimNos, String desc) throws TestException {
        //协议参数
        if (sim_count == null) {
            sim_count = mockSims.size();
        }
        for (Iterator<Map.Entry<Short, MockSim>> it = mockSims.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Short, MockSim> item = it.next();

            short pos = item.getKey();
            MockSim mockSim = item.getValue();

            if (mockSim.getStatus() == MockSim.SimStatus.NotInSp) {
                this.mockSims.remove(pos);
            } else {
                this.mockSims.put(pos, mockSim);
            }
        }

        //断言参数
        AssertMessage assertMessage = new AssertMessage(SpBinaryMsg.CmdId.CMD_SP_SIM_INFO_UPDATE.name(),
                CmdUtils.get(getCtx()).getCmdSeqNo(),
                desc).addSpExpectRet(expectRet);
        if (expectInvalidSimNos != null) {
            assertMessage.addAssertBodyParam(new AssertBodyParam("expectInvalidSimNos",
                    expectInvalidSimNos,
                    "msgBody.getInvalidSimNos().toString()"));
        }

        Message req = new Message(Message.SpTag,
                SpBinaryMsg.CmdId.CMD_SP_SIM_INFO_UPDATE.value(),
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                new SpSimInfoReq(packSpSimInfo(mockSims)));
        return sendCmdAndAssertRsp(req, bWaitRsp, assertMessage);
    }

    //心跳包
    public boolean heartbeat(boolean bWaitRsp, String desc) throws TestException {
        //协议参数

        //断言参数

        //-------------
        Message req = new Message(Message.SpTag,
                SpBinaryMsg.CmdId.CMD_SP_HEARTBEAT.value(),
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                new SpHeartBeatReq());
        return sendCmdAndAssertRsp(req, bWaitRsp, null);
    }

    //上传日志
    public boolean Loguploadres(boolean bWaitRsp, Byte result, String logpath, String desc) throws TestException {
        //协议参数

        //断言参数

        //-------------
        Message req = new Message(Message.SpTag,
                SpBinaryMsg.CmdId.CMD_SP_LOG_UPLOAD.value(),
                CmdUtils.get(getCtx()).getIncCmdSeqNo(),
                (byte)0,
                new SpLogUploadResReq(result,logpath));
        return sendCmdAndAssertRsp(req, bWaitRsp, null);
    }

    //=======================================================================

    public void term_sp_logupload_rsp(ErrCode.ServerCode retCode, String desc) throws TestException {

    }
}
