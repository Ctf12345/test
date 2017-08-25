package com.ctf.autotest;

import com.cootf.cloudsim.oms.service.api.service.AutoTestIgniteService;
import com.cootf.cloudsim.oms.service.api.service.AutoTestMongoDBService;
import com.ctf.ass_codec.mt_body.MtCodes;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.ass_public.globals.GlobalConfig;
import com.ctf.autotest.Xml.*;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_public.utils.ConvUtils;
import com.ctf.autotest.Xml.SimCard;
import com.ctf.autotest.Xml.SimPool;
import com.ctf.autotest.spring.SpringContextHandler;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import io.netty.channel.ChannelHandlerContext;

import junit.framework.Test;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xml.sax.Locator;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.validation.SchemaFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.*;

public class TestEngine {

    //Exec命令使用的成员变量
    private String retStr = "";

    private static final String VER_INFO = "1.0_20170606";
    private static LogWrapper logger = LogWrapper.getLogger(TestEngine.class.getName());
    private static String ass_host = null;         //默认为null，以测试本机
    private LinkedList<String> testPaths = null;
    private boolean gLastIsSp = false;
    private MockMt gLastMockMt = null;
    private MockSp gLastMockSp = null;
    private Object gLastAction = null;
    private static AutoTestIgniteService autoTestIgniteService;
    private static AutoTestMongoDBService autoTestMongoDBService;
    private static boolean bdebug = false;
    private static int startNo = 0;

    public static String getAssHost() {
        return ass_host;
    }

    private static void initDubboService() {
        autoTestIgniteService = SpringContextHandler.getBean(AutoTestIgniteService.class);
        autoTestMongoDBService = SpringContextHandler.getBean(AutoTestMongoDBService.class);
    }

    /**
     * 示例：执行进程并返回结果
     */
    private static String execCmd(String cmd) {
        char script_type = cmd.charAt(0);
        String script_cmd = "python " + getJarDirPath() + File.separator + cmd.substring(1);
        switch (script_type) {
            case '>': //执行外部程序
                // 执行程序
                try {
                    Process process = null;
                    process = Runtime.getRuntime().exec(script_cmd);
                    String rtStr = read(process.getInputStream(), System.out);
                    String reStrErr = read(process.getErrorStream(), System.err);
                    // 等待程序执行结束并输出状态
                    if (0 == process.waitFor()) {
                        int code = Integer.valueOf(rtStr);
                        logger.debug(reStrErr);
                        return ServerCode.valueOf((short)code).name();
                    } else {
                        logger.debug(reStrErr);
                        return "SCRIPT_EXEC_FAILED";
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                break;
            case '$':  //执行Groovy脚本，其中可用$MT00001来获取指定终端
                //imsi="$#MT00001.getSessionId()"
                //#MT00001 == MockEnv.getMockMt("00001")
                //#SP00001 == MockEnv.getMockSp("00001")
                try {
                    execGroovy(cmd.substring(1));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                break;
            default:
                break;
        }
        return "UNKONW_FAILED";
    }

    private static String   execGroovy(String script) {
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);
        //添加对类CmdUtils的引用
        binding.setProperty("ConvUtils", ConvUtils.class);
        binding.setProperty("MockEnv", MockEnv.class);
        //添加所有ServerCode的定义
        for (ErrCode.ServerCode code : ErrCode.ServerCode.values()) {
            binding.setVariable(code.name(), code.value());
        }
        String got_script = String.format("got = (%s)", script);
        shell.evaluate(got_script);
        String ret = (String) binding.getVariable("got");
        return ret;
    }

    private Locator getLastActionLoc() {
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);

        //添加action的定义
        binding.setVariable("action", gLastAction);
        shell.evaluate("locator = action.sourceLocation();");
        Locator locator = (Locator) binding.getVariable("locator");

        return locator;
    }

    /**
     * 记录测试记录到临时文件
     */
    private static void setTmpRecord(int count, int skiped, int failed){
        File f = new File("tmp.txt");
        FileWriter fw = null;
        try {
            if(!f.exists()){
                f.createNewFile();
            }
            fw = new FileWriter(f);
            fw.write(String.format("count=%d\r\n", count));
            fw.write(String.format("skiped=%d\r\n", skiped));
            fw.write(String.format("failed=%d\r\n", failed));
            fw.flush();
        }catch (Exception e){
                e.printStackTrace();
        } finally {
            if(fw != null){
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从临时文件获取当前执行的编号
     * @return
     */
    public static int getStartNo(){
        File f = new File("tmp.txt");
        if(f.isFile() && f.exists()){
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String a = br.readLine();
                System.out.println(a);
                String[] s = a.split("=");
                return Integer.parseInt(s[1]);
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
        return 0;
    }

    // 读取输入流
    private static String read(InputStream inputStream, PrintStream out) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GB2312"));
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    //生成若干个TAB
    private String nTabs(int n) {
        String ret = new String("");
        for (int i = 0; i < n; i++) {
            ret += "....";
        }
        return ret;
    }

    //获取当前测试项路径
    private String getCurrTestPath() {
        int count = testPaths.size();

        if (count > 0) {
            String currPath = nTabs(count - 1);
            currPath += testPaths.getLast();
            return currPath;
        }

        return null;
    }

    //获取当前测试项完整路径
    private String getCurrFullTestPath() {
        String currFullPath = "";
        for (int i = 0; i < testPaths.size(); i++) {
            currFullPath += nTabs(i) + testPaths.get(i) + '\n';
        }
        return currFullPath;
    }


    private HashMap<String, String> getExprsHashMap(List<AssertExpr> expr_list) {
        if (expr_list != null) {
            HashMap<String, String> exprs_hashmap = new HashMap<String, String>();
            for (AssertExpr expr : expr_list) {
                exprs_hashmap.put(expr.getExpr(), expr.getExpect());
            }
            return exprs_hashmap;
        }

        return null;
    }

    private HashMap<String, Boolean> getFieldsHashMap(List<Field> fields) {
        if (fields != null) {
            HashMap<String, Boolean> fieldList = new HashMap<>();
            for (Field field : fields) {
                fieldList.put(field.getName(), field.isValue());
            }
            return fieldList;
        }

        return null;
    }

    //TestPlan.InitClient.SimPool
    private void procSimPool(SimPool simPool) {
        testPaths.addLast(String.format("[SimPool desc=%s]", simPool.getDesc()));
        System.out.println(getCurrTestPath());

        //获取参数
        String macAddr = simPool.getMacAddr();
        String newMacAddr = simPool.getNewMacAddr();
        Integer capacity = simPool.getCapacity();
        String swVer = simPool.getSwVer();
        String hwVer = simPool.getHwVer();
        Boolean isEnabled = simPool.isIsEnabled();
        Boolean isOnLine = simPool.isIsOnLine();
        //更新数据库
        MongoDbUtil.dbSpUpdate(macAddr, newMacAddr, capacity, swVer, hwVer, isEnabled, isOnLine);

        testPaths.removeLast();
    }

    //TestPlan.ConfigDB.SimCard
    private void procSimCard(SimCard simCard) {
        testPaths.addLast(String.format("[SimCard desc=%s]", simCard.getDesc()));
        System.out.println(getCurrTestPath());

        //获取参数
        String imsi = simCard.getImsi();
        String newImsi = simCard.getNewImsi();
        String iccid = simCard.getIccid();
        String sim_img = simCard.getSimImg();
        String img_md5 = simCard.getImgMd5();
        Boolean isActivate = simCard.isIsActivate();
        Boolean isDisabled = simCard.isIsDisabled();
        Boolean isBroken = simCard.isIsBroken();
        Boolean isInSimPool = simCard.isIsInSimpool();
        Boolean isInUsed = simCard.isIsInUsed();
        String bindUserId = simCard.getBindUserId();
        String desc = simCard.getDesc();
        //更新数据库
        MongoDbUtil.dbSimUpdate(imsi, newImsi, iccid, sim_img, img_md5, isActivate, isDisabled, isBroken, isInSimPool, isInUsed, bindUserId);

        testPaths.removeLast();
    }

    //TestPlan.ConfigDB.User
    private void procUser(User user) {
        testPaths.addLast(String.format("[User desc=%s]", user.getDesc()));
        System.out.println(getCurrTestPath());

        String userId = user.getId();
        String newUserId = user.getNewId();
        String password = user.getPassword();
        Integer status = user.getStatus();
        //更新数据库
        MongoDbUtil.dbUserUpdate(userId, newUserId, password, status);

        testPaths.removeLast();
    }

    private void procActionId(Object action) throws TestException {
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);
        //添加action的定义
        binding.setVariable("action", action);

        String id = null;
        try {
            shell.evaluate("id = action.getId();");
            id = (String) binding.getVariable("id");
        } catch (Exception e) {
            //groovy.lang.MissingMethodException
        }
        if (id != null) {
            String type = id.substring(0, 2);
            String no = id.substring(2);
            switch (type) {
                case "MT":
                    gLastMockMt = MockEnv.getMockMt(no);
                    gLastIsSp = false;
                    break;

                case "SP":
                    gLastMockSp = MockEnv.getMockSp(no);
                    gLastIsSp = true;
                    break;

                default:
                    break;
            }
        }
    }

    //TestPlan.TestGroup.PreAction
    //TestPlan.TestGroup.TestCase
    private void procAction(Object action) throws TestException {
        gLastAction = action;
        //处理action中的id属性
        procActionId(action);

        String[] class_names = action.getClass().getName().split("\\.");
        String cls_name = class_names[class_names.length - 1];

        System.out.println(String.format("%s[%s]", nTabs(testPaths.size() - 1), cls_name));
        switch (cls_name) {
            case "Sleep":
                Sleep sleep = (Sleep) action;
                int time = sleep.getTime().intValue();
                if (gLastIsSp) {
                    gLastMockSp.sleep(time);
                } else {
                    gLastMockMt.sleep(time);
                }
                break;

            //----------------------------------------

            case "Connsvr":
                try {
                    if (gLastIsSp) {
                        gLastMockSp.connsvr();
                    } else {
                        gLastMockMt.connsvr();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case "Disconn":
                if (gLastIsSp) {
                    gLastMockSp.disconn();
                } else {
                    gLastMockMt.disconn();
                }
                break;

            case "Config": {
                Config config = (Config) action;
                HashMap<String, Boolean> fields = getFieldsHashMap(config.getField());
                String desc = config.getDesc();
                if (gLastIsSp) {
                    gLastMockSp.config(fields, desc);
                } else {
                    logger.debug("config " + gLastMockMt);
                    gLastMockMt.config(fields, desc);
                }
            }
            break;

            case "Exec": {
                Exec exec = (Exec) action;
                int waitMs = exec.getWaitMs();
                if(waitMs == 0){
                    retStr = execCmd(exec.getCmd());
                    String expected = exec.getExpectRetcode();
                    if (!expected.equals(retStr)) {
                        throw new TestException("Exec", exec.getCmd(), expected, retStr);
                    }
                }else{
                    new Thread(){
                        public void run(){
                            try {
                                sleep(waitMs);
                                retStr = execCmd(exec.getCmd());
                                String expected = exec.getExpectRetcode();
                                if (!expected.equals(retStr)) {
                                    throw new TestException("Exec", exec.getCmd(), expected, retStr);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (TestException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
            break;

            //----------------------------------------

            case "AssertRsp": {
                AssertRsp assertRsp = (AssertRsp) action;
                HashMap<String, String> exprs = getExprsHashMap(assertRsp.getAssertExpr());
                //TODO: check lastTermReq is null and throws TestException
                if (gLastIsSp) {
                    gLastMockSp.assertRsp(CmdUtils.get(gLastMockSp.getCtx()).getLastReq(), exprs, assertRsp.getDesc());
                } else {
                    gLastMockMt.assertRsp(CmdUtils.get(gLastMockMt.getCtx()).getLastReq(), exprs, assertRsp.getDesc());
                }
            }
            break;

            case "AssertReq": {
                AssertReq assertReq = (AssertReq) action;
                HashMap<String, String> exprs = getExprsHashMap(assertReq.getAssertExpr());
                if (gLastIsSp) {
                    gLastMockSp.assertReq(assertReq.getCmdId(), assertReq.getSeqNo(), exprs, assertReq.getDesc());
                } else {
                    logger.debug("assertReq " + gLastMockMt);
                    gLastMockMt.assertReq(assertReq.getCmdId(), assertReq.getSeqNo(), exprs, assertReq.getDesc());
                }
            }
            break;

            //----------------------------------------

            case "MtLogin": {
                MtLogin mtLogin = (MtLogin) action;

                //协议参数
                String userId = mtLogin.getUserId();
                if (userId == null) {
                    userId = gLastMockMt.getUserId();
                }
                String password = mtLogin.getPassword();
                if (password == null) {
                    password = gLastMockMt.getPassword();
                }
                String devId = mtLogin.getDevId();
                if (devId == null) {
                    devId = gLastMockMt.getDevId();
                }
                int protoVer = mtLogin.getProtoVer();
                String codesVer = mtLogin.getCodesVer();

                //断言参数
                boolean bWaitRsp = mtLogin.isWaitRsp();
                String expectRet = mtLogin.getExpectRet();
                String desc = mtLogin.getDesc();

                //--------
                gLastMockMt.login(userId, password, devId, protoVer, codesVer,
                        bWaitRsp, expectRet, desc);
            }
            break;

            case "MtLoginbyid": {
                MtLoginbyid mtLoginbyid = (MtLoginbyid) action;

                //协议参数
                String sessionId = mtLoginbyid.getSessionid();
                if(!StringUtils.isEmpty(sessionId)){
                    char script = sessionId.charAt(0);
                    if(script == '$'){
                        sessionId = execGroovy(sessionId.substring(1));
                    }
                }

                //断言参数
                boolean bWaitRsp = mtLoginbyid.isWaitRsp();
                String expectRet = mtLoginbyid.getExpectRet();
                String desc = mtLoginbyid.getDesc();

                //--------
                gLastMockMt.loginById(sessionId,
                        bWaitRsp, expectRet, desc);
            }
            break;

            case "MtRequim": {
                MtRequim mtRequim = (MtRequim) action;

                //协议参数
                String cells = mtRequim.getCells();
                String imsi = mtRequim.getImsi();

                //断言参数
                boolean bWaitRsp = mtRequim.isWaitRsp();
                String expectRet = mtRequim.getExpectRet();
                String expectImsi = mtRequim.getExpectImsi();
                String expectImgMd5 = mtRequim.getExpectImgMd5();
                String desc = mtRequim.getDesc();

                //--------
                gLastMockMt.requim(cells, imsi,
                        bWaitRsp, expectRet, expectImsi, expectImgMd5, desc);
            }
            break;

            case "MtSimimg": {
                MtSimimg mtSimimg = (MtSimimg) action;

                //协议参数

                //断言参数
                boolean bWaitRsp = mtSimimg.isWaitRsp();
                String expectRet = mtSimimg.getExpectRet();
                String expectImgData = mtSimimg.getExpectImgData();
                String desc = mtSimimg.getDesc();

                //--------
                gLastMockMt.simimg(bWaitRsp, expectRet, expectImgData, desc);
            }
            break;

            case "MtSimpool": {
                MtSimpool mtSimpool = (MtSimpool) action;

                //协议参数
                String apdu_hexstr = mtSimpool.getApduHexstr();

                //断言参数
                boolean bWaitRsp = mtSimpool.isWaitRsp();
                String expectRet = mtSimpool.getExpectRet();
                String expectRsp = mtSimpool.getExpectResponse();
                String desc = mtSimpool.getDesc();

                //--------
                gLastMockMt.simpool(apdu_hexstr,
                        bWaitRsp, expectRet, expectRsp, desc);
            }
            break;

            case "MtRevuim": {
                MtRevuim mtRevuim = (MtRevuim) action;

                //协议参数
                ServerCode cause = ServerCode.valueOf(mtRevuim.getCause());

                //断言参数
                boolean bWaitRsp = mtRevuim.isWaitRsp();
                String expectRet = mtRevuim.getExpectRet();
                String desc = mtRevuim.getDesc();

                //--------
                gLastMockMt.revuim(cause,
                        bWaitRsp, expectRet, desc);
            }
            break;

            case "MtLogout": {
                MtLogout mtLogout = (MtLogout) action;

                //协议参数
                ServerCode cause = ServerCode.valueOf(mtLogout.getCause());

                //断言参数
                boolean bWaitRsp = mtLogout.isWaitRsp();
                String expectRet = mtLogout.getExpectRet();
                String desc = mtLogout.getDesc();

                //--------
                gLastMockMt.logout(cause,
                        bWaitRsp, expectRet, desc);
            }
            break;

            case "MtAlert": {
                MtAlert mtAlert = (MtAlert) action;

                //协议参数
                ServerCode cause = ServerCode.valueOf(mtAlert.getCause());

                //断言参数
                boolean bWaitRsp = mtAlert.isWaitRsp();
                String expectRet = mtAlert.getExpectRet();
                String desc = mtAlert.getDesc();

                //--------
                gLastMockMt.alert(cause,
                        bWaitRsp, expectRet, desc);
            }
            break;

            case "MtTrace": {
                MtTrace mtTrace = (MtTrace) action;

                //协议参数
                String line = mtTrace.getLine();

                //断言参数
                String desc = mtTrace.getDesc();

                //--------
                gLastMockMt.traceInd(line,
                        desc);
            }
            break;

            case "MtHeartbeat": {
                MtHeartbeat mtHeartbeat = (MtHeartbeat) action;

                //协议参数

                //断言参数
                boolean bWaitRsp = mtHeartbeat.isWaitRsp();
                String expectRet = mtHeartbeat.getExpectRet();
                String desc = mtHeartbeat.getDesc();

                //--------
                gLastMockMt.heartbeat(bWaitRsp, expectRet, desc);
            }
            break;

            case "MtLoguploadret": {
                MtLoguploadret mtLoguploadret = (MtLoguploadret) action;

                //协议参数

                //断言参数
                boolean bWaitRsp = mtLoguploadret.isWaitRsp();
                String logpath = mtLoguploadret.getLogpath();
                int result = mtLoguploadret.getResult();
                MtCodes code = new MtCodes(result);
                String expectRet = mtLoguploadret.getExpectRet();
                String desc = mtLoguploadret.getDesc();


                //--------
                gLastMockMt.mtLoguploadret(logpath,code,bWaitRsp,expectRet,desc);
            }
            break;

            //----------------------------------------

            case "TermMTAssignUimRsp": {
                TermMtAssignUimRsp termMtAssignUimRsp = (TermMtAssignUimRsp) action;

                //协议参数
                ServerCode retCode = ServerCode.valueOf(termMtAssignUimRsp.getRetCode());

                //断言参数
                String desc = termMtAssignUimRsp.getDesc();

                //--------
                gLastMockMt.term_mt_assignuim_rsp(retCode,
                        desc);
            }
            break;

            case "TermMtRevUimRsp": {
                TermMtRevUimRsp termMtRevUimRsp = (TermMtRevUimRsp) action;

                //协议参数
                ServerCode retCode = ServerCode.valueOf(termMtRevUimRsp.getRetCode());

                //断言参数
                String desc = termMtRevUimRsp.getDesc();

                //--------
                gLastMockMt.term_mt_revuim_rsp(retCode,
                        desc);
            }
            break;

            case "TermMtLogoutRsp": {
                TermMtLogoutRsp termMtLogoutRsp = (TermMtLogoutRsp) action;

                //协议参数
                ServerCode retCode = ServerCode.valueOf(termMtLogoutRsp.getRetCode());

                //断言参数
                String desc = termMtLogoutRsp.getDesc();

                //--------
                gLastMockMt.term_mt_logout_rsp(retCode,
                        desc);
            }
            break;

            case "TermMtTraceRsp": {
                TermMtTraceRsp termMtTraceRsp = (TermMtTraceRsp) action;

                //协议参数
                ServerCode retCode = ServerCode.valueOf(termMtTraceRsp.getRetCode());

                //断言参数
                String desc = termMtTraceRsp.getDesc();

                //--------
                gLastMockMt.term_mt_trace_rsp(retCode,
                        desc);
            }
            break;

            case "TermMtLoguploadRsp": {
                TermMtLoguploadRsp termMtLoguploadRsp = (TermMtLoguploadRsp) action;

                //协议参数
                ServerCode retCode = ServerCode.valueOf(termMtLoguploadRsp.getRetCode());

                //断言参数
                String desc = termMtLoguploadRsp.getDesc();

                //--------
                gLastMockMt.term_mt_logupload_rsp(retCode,
                        desc);
            }
            break;

            case "TermSPLoguploadRsp": {
                TermSPLoguploadRsp termSPLoguploadRsp = (TermSPLoguploadRsp) action;

                //协议参数
                ServerCode retCode = ServerCode.valueOf(termSPLoguploadRsp.getRetCode());

                //断言参数
                String desc = termSPLoguploadRsp.getDesc();

                //--------
                gLastMockSp.term_sp_logupload_rsp(retCode,
                        desc);
            }
            break;

            //----------------------------------------

            case "SpLogin": {
                SpLogin spLogin = (SpLogin) action;

                //协议参数
                String macHexStr = spLogin.getMacHexstr();
                if (macHexStr == null) {
                    macHexStr = gLastMockSp.getMacAddrStr();
                }
                Integer maxSimCount = spLogin.getMaxSimCount();
                String hwVer = spLogin.getHwVer();
                String swVer = spLogin.getSwVer();

                //断言参数
                boolean bWaitRsp = spLogin.isWaitRsp();
                String expectRet = spLogin.getExpectRet();
                String desc = spLogin.getDesc();

                //--------
                gLastMockSp.login(macHexStr, maxSimCount, hwVer, swVer,
                        bWaitRsp, expectRet, desc);
            }
            break;

            case "SpSiminfo": {
                SpSiminfo spSiminfo = (SpSiminfo) action;

                //协议参数
                HashMap<Short, MockSim> mockSims = new HashMap<>();
                if (spSiminfo.getSimInfo() != null) {
                    for (SimInfo simInfo : spSiminfo.getSimInfo()) {
                        MockSim mockSim = new MockSim(simInfo.getImsi());
                        if (simInfo.getIccid() != null) {
                            mockSim.setIccid(simInfo.getIccid());
                        }
                        if (simInfo.getSimImg() != null) {
                            mockSim.setSimImage(ConvUtils.hexStrToBytes(simInfo.getSimImg()));
                        }
                        if (simInfo.getImgMd5() != null) {
                            mockSim.setImgMd5(ConvUtils.hexStrToBytes(simInfo.getImgMd5()));
                        }
                        if (simInfo.getStatus() != null) {
                            mockSim.setStatus(MockSim.SimStatus.valueOf(simInfo.getStatus()));
                        }
                        if (simInfo.getDesc() != null) {
                            mockSim.setDesc(simInfo.getDesc());
                        }

                        String missingField = mockSim.getMissingField();
                        if (missingField != null) {
                            throw new TestException("SpSimInfo", simInfo.getImsi(), missingField, "");
                        }

                        mockSims.put((short) simInfo.getPos(), mockSim);
                    }
                }

                Integer sim_count = spSiminfo.getSimCount();
                if (sim_count == null) {
                    sim_count = mockSims.size();
                }

                //断言参数
                boolean bWaitRsp = spSiminfo.isWaitRsp();
                String expectRet = spSiminfo.getExpectRet();
                String expectInvalidSimNos = spSiminfo.getExpectInvalidSimNos();
                String desc = spSiminfo.getDesc();

                //--------
                gLastMockSp.simInfo(sim_count, mockSims,
                        bWaitRsp, expectRet, expectInvalidSimNos, desc);
            }
            break;

            case "SpSiminfoUpdate": {
                SpSiminfoUpdate spSiminfoUpdate = (SpSiminfoUpdate) action;

                //协议参数
                Integer sim_count = spSiminfoUpdate.getSimCount();
                HashMap<Short, MockSim> mockSims = new HashMap<>();
                for (SimInfo simInfo : spSiminfoUpdate.getSimInfo()) {
                    MockSim mockSim = new MockSim(simInfo.getImsi());
                    if (simInfo.getIccid() != null) {
                        mockSim.setIccid(simInfo.getIccid());
                    }
                    if (simInfo.getSimImg() != null) {
                        mockSim.setSimImage(ConvUtils.hexStrToBytes(simInfo.getSimImg()));
                    }
                    if (simInfo.getImgMd5() != null) {
                        mockSim.setImgMd5(ConvUtils.hexStrToBytes(simInfo.getImgMd5()));
                    }
                    if (simInfo.getStatus() != null) {
                        mockSim.setStatus(MockSim.SimStatus.valueOf(simInfo.getStatus()));
                    }
                    if (simInfo.getDesc() != null) {
                        mockSim.setDesc(simInfo.getDesc());
                    }

                    String missingField = mockSim.getMissingField();
                    if (missingField != null) {
                        throw new TestException("SpSimInfo", simInfo.getImsi(), missingField, "");
                    }

                    mockSims.put((short) simInfo.getPos(), mockSim);
                }

                //断言参数
                boolean bWaitRsp = spSiminfoUpdate.isWaitRsp();
                String expectRet = spSiminfoUpdate.getExpectRet();
                String expectInvalidSimNos = spSiminfoUpdate.getExpectInvalidSimNos();
                String desc = spSiminfoUpdate.getDesc();

                //--------
                gLastMockSp.simInfoUpdate(sim_count, mockSims,
                        bWaitRsp, expectRet, expectInvalidSimNos, desc);
            }
            break;

            case "SpHeartbeat": {
                SpHeartbeat spHeartbeat = (SpHeartbeat) action;

                //协议参数

                //断言参数
                boolean bWaitRsp = spHeartbeat.isWaitRsp();
                String desc = spHeartbeat.getDesc();

                //--------
                gLastMockSp.heartbeat(bWaitRsp, desc);
            }
            break;

            case "SpLoguploadres": {
                SpLoguploadres spLoguploadres = (SpLoguploadres) action;

                //协议参数

                //断言参数
                boolean bWaitRsp = spLoguploadres.isWaitRsp();
                int result = spLoguploadres.getResult();
                Integer iO =new Integer(result);
                Byte b = iO.byteValue();
                String logpath = spLoguploadres.getLogpath();
                String desc = spLoguploadres.getDesc();

                //--------
                gLastMockSp.Loguploadres(bWaitRsp,b,logpath, desc);
            }
            break;

            default:
                System.out.println("Unknown Client action class:" + action.getClass().getName());
                break;
        }
    }

    //TestPlan.InitDB
    private void procInitDB(InitDB initDB) throws TestException {
        int userCount;
        int simPoolCount;
        int simCardCount;
        if (initDB == null) {
            InitDB tempInitDB = new InitDB();
            userCount = (int) tempInitDB.getUserCount();
            simPoolCount = (int) tempInitDB.getSimpoolCount();
            simCardCount = (int) tempInitDB.getSimcardCount();
        } else {
            userCount = (int) initDB.getUserCount();
            simPoolCount = (int) initDB.getSimpoolCount();
            simCardCount = (int) initDB.getSimcardCount();
        }
        MongoDbUtil.genTestClients(userCount);
        MongoDbUtil.genTestSimPools(simPoolCount);
        MongoDbUtil.genTestSimCards(simCardCount);
    }

    //TestPlan.ConfigDB
    private void procConfigDB(ConfigDB configDB) throws TestException {
        logger.debug("configDB [" + getCurrTestPath() + "] desc:" + configDB.getDesc());

        //TestPlan.ConfigDB.SimPool
        for (SimPool simPool : configDB.getSimPool()) {
            procSimPool(simPool);
        }
        //TestPlan.ConfigDB.SimCard
        for (SimCard simCard : configDB.getSimCard()) {
            procSimCard(simCard);
        }
        //TestPlan.ConfigDB.User
        for (User user : configDB.getUser()) {
            procUser(user);
        }
    }

    //TestPlan.TestSet.TestGroup.PreAction
    private void procPreAction(PreAction preAction) throws TestException {
        //TestPlan.TestSet.TestGroup.PreAction
        if (preAction != null) {
            logger.debug("preAction [" + getCurrTestPath() + "] desc:" + preAction.getDesc());

            for (Object obj : preAction.getSleepOrConnsvrOrDisconn()) {
                procAction(obj);
            }
        }
    }

    //清理测试环境
    private void deInitTestEnv() {
        logger.debug("deInitTestEnv");

        //断开所有模拟client的连接
        ChannelHandlerContext tempCtx;
        for (MockMt mockMt : MockEnv.getMockMts()) {
            tempCtx = mockMt.getCtx();
            if (tempCtx != null) {
                tempCtx.close();
            }
        }
        for (MockSp mockSp : MockEnv.getMockSps()) {
            tempCtx = mockSp.getCtx();
            if (tempCtx != null) {
                tempCtx.close();
            }
        }

        //等待所有ctx成功关闭
        CmdUtils.waitAllCtxsDeInit();

        //清除MockEnv
        MockEnv.resetEnv();

        gLastIsSp = false;
        gLastMockMt = null;
        gLastMockSp = null;

        //清理数据库中数据
        MongoDbUtil.resetDb();
    }

    //TestPlan
    private void procTestPlan(InputStream xml_is, TestPlan testPlan) {
        int count = 0;      //测试用例总数
        int skiped = 0;     //跳过用例总数
        int failed = 0;     //失败用例总数
        boolean testSetSkiped;
        boolean testGroupSkiped;
        boolean testCaseSkiped;

        testPaths.addLast("[TestPlan]");
        System.out.println(getCurrTestPath());

        //依次处理每个TestPlan.TestSet.TestGroup.TestCase
        outer:
        for (TestSet testSet : testPlan.getTestSet()) {
            //TestPlan.TestSet
            testPaths.addLast(String.format("[TestSet desc=%s]", testSet.getDesc()));

            testSetSkiped = testSet.isSkip();
            System.out.println(getCurrTestPath() + (testSetSkiped ? " Skipped" : ""));  //TestSet skiped
            for (TestGroup testGroup : testSet.getTestGroup()) {
                //TestPlan.TestGroup
                testPaths.addLast(String.format("[TestGroup#%s desc=%s]", testGroup.getNo(), testGroup.getDesc()));
                if (testSetSkiped) {
                    testGroupSkiped = true;
                } else {
                    testGroupSkiped = testGroup.isSkip();
                }
                System.out.println(getCurrTestPath() + (testGroupSkiped ? "Skipped" : ""));

                //TestPlan.TestGroup.TestCase
                for (TestCase testCase : testGroup.getTestCase()) {
                    testPaths.addLast(String.format("[TestCase#%s desc=%s]", testCase.getNo(), testCase.getDesc()));
                    if (testGroupSkiped) {
                        testCaseSkiped = true;
                    } else {
                        testCaseSkiped = testCase.isSkip();
                    }
                    System.out.println(getCurrTestPath() + (testCaseSkiped ? "Skipped" : ""));

                    count++;
                    if (testCaseSkiped) {
                        skiped++;
                    }else if(testCase.getNo() < startNo){
                        System.out.println(getCurrTestPath() + String.format("currentNo:%d is not in startNo:%d", testCase.getNo(), startNo));
                    }else {
                        try {
                            //===================
                            //建立测试用例环境
                            //===================
                            //TestCase的测试环境由以下3部分叠加而成：
                            //1.TestPlan.InitDB
                            procInitDB(testPlan.getInitDB());
                            //2.TestPlan.ConfigDB
                            procConfigDB(testPlan.getConfigDB());
                            //3.TestPlan.TestSet.TestGroup.PreAction
                            procPreAction(testGroup.getPreAction());

                            //===================
                            //执行测试用例
                            //===================
                            for (Object obj : testCase.getSleepOrConnsvrOrDisconn()) {
                                procAction(obj);
                            }
                        } catch (Exception e) {
                            failed++;

                            System.out.println(getCurrFullTestPath());
                            outputXmlErrLoc(xml_is);
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        } finally {
                            //===================
                            //清理测试用例环境
                            //===================
                            deInitTestEnv();
                        }

                        if (bdebug && failed > 0) {
                            break outer;
                        }
                    }
                    testPaths.removeLast();
                }
                testPaths.removeLast();
            }
            testPaths.removeLast();
        }
        testPaths.removeLast();

        System.out.println(String.format("##################################"));
        System.out.println(String.format("#              %s", (failed == 0 ? "PASS:)" : "FAIL!!!")));
        System.out.println(String.format("----------------------------------"));
        System.out.println(String.format("# count        : %d", count));
        System.out.println(String.format("# skiped       : %d", skiped));
        System.out.println(String.format("# failed       : %d", failed));
        System.out.println(String.format("# passed       : %d", count - skiped - failed));
        System.out.println(String.format("# pass_percent : %.2f%%", (count-skiped-failed) * 100.0/count));
        System.out.println(String.format("##################################"));
        if(bdebug == true){
            System.out.println("记录临时文件");
            setTmpRecord(count, skiped, failed);
        }

    }

    private void outputXmlErrLoc(InputStream xml_is) {
        Locator locator = getLastActionLoc();
        int errline = locator.getLineNumber();
        System.out.println(String.format("line: %d, column:%d", locator.getLineNumber(), locator.getColumnNumber()));
        InputStreamReader isr = new InputStreamReader(xml_is);
        LineNumberReader reader = new LineNumberReader(isr);
        try {
            for (int i=1; i<errline; i++) {
                reader.readLine();
            }
            for (int i=0; i<5; i++) {
                System.out.println(reader.readLine());
            }
        } catch (Exception e) {

        }
    }

    private void validityAndExecTestPlan(InputStream xml_is) throws Exception {
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance(TestPlan.class);
            Unmarshaller u = jc.createUnmarshaller();
            u.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(TestEngine.class.getClassLoader().getResource("TestPlan.xsd")));
            u.setEventHandler(validationEvent -> {
                ValidationEventLocator locator = validationEvent.getLocator();
                System.out.println(String.format("XML解析出错！%d行%d列：%s", locator.getLineNumber(), locator.getColumnNumber(), validationEvent.getMessage()));
                return false;
            });

            testPaths = new LinkedList<>();
            TestPlan testPlan = (TestPlan) u.unmarshal(xml_is);
            procTestPlan(xml_is, testPlan);
        }  catch(Exception e) {
            System.out.println("************************");
            System.out.println("* Execption occured!!! *");
            System.out.println("************************");
            System.out.println(getCurrFullTestPath());
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            //deInitDB();
        }
    }

    //获取jar包运行时目录
    public static String getJarDirPath()
    {
        String path = TestEngine.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        File file = new File(path);
        if (file == null)
            return null;
        return file.getParent();
    }

    public static void main(String[] args) throws Exception {

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("dubbo.xml");
        applicationContext.start();
        SpringContextHandler.setApplicationContext(applicationContext);

        int sp_port = GlobalConfig.ASS_SP_PORT;
        int mt_port = GlobalConfig.ASS_MT_PORT;
        int http_port = GlobalConfig.ASS_HTTP_PORT;
        String db_ip = null;    //测试时暂时使用本机IP     //GlobalConfig.MONGODB_IP;
        String db_id = null;
        String db_psd = null;
        InputStream is = null;

        Options options = new Options();
        options.addOption("h", "help", false, "print help usages.");
        options.addOption(Option.builder("v").longOpt("version").hasArg(false).desc("print version info").build());
        options.addOption("debug", "switch on debug mode.");
        options.addOption(Option.builder("f").hasArg(true).argName("testplan_file").desc("use given file for testplan input").build());
        options.addOption(Option.builder("D").hasArgs().numberOfArgs(2).argName("property=value").valueSeparator('=').desc("set value for given property(Will override settings in <config_file>)").build());

        CommandLine cmd;
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.fatal("Invalid command line parameters!!!" + e.getMessage());
            return;
        }

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java TestEngine.jar [options]", options);
            return;
        }

        if (cmd.hasOption("v")) {
            System.out.println(VER_INFO);
            return;
        }


        if (cmd.hasOption("debug")) {
            bdebug = true;
            startNo = getStartNo();
        }

        if (cmd.hasOption("f")) {
            //TODO: 添加对配置文件的支持
            String testplan_file = getJarDirPath() + File.separator + cmd.getOptionValue("f");
            System.out.println("Use test plan file:[" + testplan_file + "]");
            is = new FileInputStream(new File(testplan_file));
        }

        if (cmd.hasOption("D")) {
            Properties properties = cmd.getOptionProperties("D");
            String value = properties.getProperty("ass_host");
            if (value != null) {
                ass_host = value;
            }

            value = properties.getProperty("sp_port");
            if (value != null) {
                sp_port = Integer.parseInt(value);
            }

            value = properties.getProperty("mt_port");
            if (value != null) {
                mt_port = Integer.parseInt(value);
            }

            value = properties.getProperty("http_port");
            if (value != null) {
                http_port = Integer.parseInt(value);
            }

            value = properties.getProperty("db_ip");
            if (value != null) {
                db_ip = value;
            }

            value = properties.getProperty("db_id");
            if (value != null) {
                db_id = value;
            }

            value = properties.getProperty("db_psd");
            if (value != null) {
                db_psd = value;
            }
        }

        InetAddress addr = InetAddress.getLocalHost();
        String localhost = addr.getHostAddress().toString();  //本机IP
        if (ass_host == null) {
            ass_host = localhost;   //默认使用本机IP
        }
        if (db_ip == null) {
            db_ip = localhost;
        }
        //==============================================================================================================
        MongoDbUtil.initDb(db_ip, db_id, db_psd);
        MockEnv.initAss(ass_host, sp_port, mt_port, http_port);

        if (is == null) {
            is = TestEngine.class.getClassLoader().getResourceAsStream("TestPlan_new.xml");
        }
        new TestEngine().validityAndExecTestPlan(is);

        System.exit(0);

    }
}
