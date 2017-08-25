package com.ctf.autotest;

import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.*;

public class MockEnv {
    private static LogWrapper logger = LogWrapper.getLogger(MockEnv.class.getName());

    private static String ass_host = null;
    private static int ass_sp_port = 18081;
    private static int ass_mt_port = 18080;
    private static int ass_http_port = 18088;

    private static ArrayList<ChannelHandlerContext> mockCtxs = new ArrayList<ChannelHandlerContext>();
    private static HashMap<String, MockSp> mockSPs = new HashMap<>();
    private static HashMap<String, MockMt> mockMTs = new HashMap<>();

    public static Collection<MockSp> getMockSps() {
        return mockSPs.values();
    }

    public static Collection<MockMt> getMockMts() {
        return mockMTs.values();
    }

    public static void initAss(String host, int sp_port, int mt_port, int http_port) {
        ass_host = host;
        ass_sp_port = sp_port;
        ass_mt_port = mt_port;
        ass_http_port = http_port;
    }

    public static String getAssHost() {
        return ass_host;
    }

    public static int getAssSpPort() {
        return ass_sp_port;
    }

    public static int getAssMtPort() {
        return ass_mt_port;
    }

    public static int getAssHttpPort() {
        return ass_http_port;
    }

    public static void pushCtx(ChannelHandlerContext ctx) {
        synchronized (mockCtxs) {
            mockCtxs.add(ctx);
            mockCtxs.notifyAll();
        }
    }

    public static ChannelHandlerContext popCtx(SocketAddress socketAddress) {
        ChannelHandlerContext ctx = null;
        synchronized (mockCtxs) {
            while (ctx == null) {
                for (ChannelHandlerContext mockCtx : mockCtxs) {
                    if (mockCtx.channel().localAddress().equals(socketAddress)) {
                        ctx = mockCtx;
                        mockCtxs.remove(ctx);
                        break;
                    }
                }

                if (ctx == null) {
                    logger.debug("popCtx is null. Waiting......");
                    try {
                        mockCtxs.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return ctx;
    }

    //================================================================

    public static void addMockSp(String id, MockSp mockSp) {
        mockSPs.put(id, mockSp);
    }

    public static boolean delMockSp(ChannelHandlerContext ctx) {
        if (ctx != null) {
            ChannelHandlerContext tempCtx;
            for (Iterator<Map.Entry<String, MockSp>> it = mockSPs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, MockSp> item = it.next();
                MockSp mockSp = item.getValue();

                tempCtx = mockSp.getCtx();
                if (tempCtx != null && tempCtx.channel().equals(ctx.channel())) {
                    mockSPs.remove(tempCtx);
                    return true;
                }
            }
        }

        return false;
    }

    public static MockSp getMockSp(ChannelHandlerContext ctx) {
        ChannelHandlerContext tempCtx;
        for (MockSp mockSp : mockSPs.values()) {
            tempCtx = mockSp.getCtx();
            if (tempCtx != null && tempCtx.channel().localAddress().equals(ctx.channel().localAddress())) {
                return mockSp;
            }
        }

        return null;
    }

    public static MockSp getMockSp(String id) {
        MockSp mockSp = mockSPs.get(id);
        if (mockSp == null) {
            mockSp = new MockSp();
            addMockSp(id, mockSp);
        }
        return mockSp;
    }

    //================================================================

    public static void addMockMt(String id, MockMt mockMt) {
        mockMTs.put(id, mockMt);
    }

    public static boolean delMockMt(ChannelHandlerContext ctx) {
        if (ctx != null) {
            ChannelHandlerContext tempCtx;

            for (Iterator<Map.Entry<String, MockMt>> it = mockMTs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, MockMt> item = it.next();
                MockMt mockMt = item.getValue();

                tempCtx = mockMt.getCtx();
                if (tempCtx != null && tempCtx.channel().localAddress().equals(ctx.channel().localAddress())) {
                    mockMTs.remove(tempCtx);
                    return true;
                }
            }
        }

        return false;
    }

    public static MockMt getMockMt(ChannelHandlerContext ctx) {
        ChannelHandlerContext tempCtx;

        for (MockMt mockMt : mockMTs.values()) {
            tempCtx = mockMt.getCtx();
            if (tempCtx != null && tempCtx.channel().localAddress().equals(ctx.channel().localAddress())) {
                return mockMt;
            }
        }

        return null;
    }

    public static MockMt getMockMt(String id) {
        MockMt mockMt = mockMTs.get(id);
        if (mockMt == null) {
            mockMt = new MockMt();
            addMockMt(id, mockMt);
        }
        return mockMt;
    }

    public static void resetEnv() {
        mockCtxs.clear();
        mockSPs.clear();
        mockMTs.clear();
    }

    public enum MockClientState {
        CLIENT_STATE_OFFLINE,           //离线
        CLIENT_STATE_CONNECTED,         //已连接，仅客户端有此状态
        CLIENT_STATE_LOGINED,           //已登录
        //-----  (SimPool最高到达上面的状态)  -----
        CLIENT_STATE_ASSIGNED           //已分卡
    }
}
