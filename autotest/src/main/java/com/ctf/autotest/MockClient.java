package com.ctf.autotest;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ctf.ass_codec.binary.SpBinaryMsg;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.ErrCode.ServerCode;
import com.ctf.ass_public.utils.ConvUtils;
import com.ctf.autotest.MockEnv.MockClientState;

public abstract class MockClient {
    public static final int MT_CLIENT_HEARTBEAT_TIMEOUT_SECONDS = 5 * 60;
    public static final int SP_CLIENT_HEARTBEAT_TIMEOUT_SECONDS = 5 * 60;

    private static LogWrapper logger = LogWrapper.getLogger(MockClient.class.getName());

    private ChannelHandlerContext ctx = null;
    private EventLoopGroup group = null;
    private ChannelFuture future = null;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private SocketAddress localAddress = null;

    //上次错误码(用于连接断开时判断是重连还是释放资源)
    private ServerCode gLastError;
    //上次断言的结果
    private boolean isMt = false;

    private boolean bAutoReq = true;
    private boolean bAutoRsp = true;
    private boolean bWaitRsp = true;
    private boolean bAutoHeartBeat = true;

    public boolean getIsAutoReq() {
        return bAutoReq;
    }
    public boolean getIsAutoRsp() {
        return bAutoRsp;
    }
    public boolean getIsWaitRsp() {
        return bWaitRsp;
    }
    public boolean getIsAutoHeartBeat() {
        return bAutoHeartBeat;
    }

    private MockClientState mockClientState = MockClientState.CLIENT_STATE_OFFLINE;

    //等待状态

    public MockClient(boolean isMt) {
        this.isMt = isMt;
        this.ctx = null;
    }

    //抽象函数接口，需要子类去实现
    public void config(HashMap<String, Boolean> fields, String desc) {
        //更改设置
        for (Iterator<Map.Entry<String, Boolean>> it = fields.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Boolean> item = it.next();
            String name = item.getKey();
            Boolean value = item.getValue();
            switch(name) {
                case "bAutoReq":
                    bAutoReq = value;
                    break;

                case "bAutoRsp":
                    bAutoRsp = value;
                    break;

                case "bWaitRsp":
                    bWaitRsp = value;
                    break;

                case "bAutoHeatBeat":
                    bAutoHeartBeat = value;
                    break;

                default:
                    logger.fatal("unknown config field! name:[" + name + "], value:[" + value + "], desc:" + desc);
                    break;
            }
        }
    }

    public void assertReq(String cmdId, Integer seqNo, HashMap<String, String> assertExprs, String desc) throws TestException {
        AssertMessage assertMessage = new AssertMessage(cmdId, seqNo, null, desc);
        for (String expr : assertExprs.keySet()) {
            String expect = assertExprs.get(expr);
            AssertBodyParam assertBodyParam = new AssertBodyParam(expr, expect, expr);
            assertMessage.addAssertBodyParam(assertBodyParam);
        }
        CmdUtils.get(ctx).waitAndAssertReq(assertMessage);
    }

    public void assertRsp(Message message, HashMap<String, String> assertExprs, String desc) throws TestException {
        AssertMessage assertMessage = new AssertMessage(CmdUtils.getMsgCmdIdName(message), Integer.valueOf(message.getSeqNo()), null, desc);
        for (String expr : assertExprs.keySet()) {
            String expect = assertExprs.get(expr);
            AssertBodyParam assertBodyParam = new AssertBodyParam(expr, expect, expr);
            assertMessage.addAssertBodyParam(assertBodyParam);
        }
        CmdUtils.get(ctx).waitAndAssertRsp(assertMessage);
    }

    public ChannelHandlerContext getCtx() {
        return this.ctx;
    }
    public void clearCtx() {
        this.ctx = null;
    }
    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    //最后错误信息
    public ServerCode getLastError() {
        return gLastError;
    }
    public void setLastError(ServerCode code) {
        this.gLastError = code;
    }

    public MockClientState getMockClientState() {
        return mockClientState;
    }
    public void setMockClientState(MockClientState new_state) {
        mockClientState = new_state;
    }
    public boolean isClientLogined() {
        return (mockClientState == MockClientState.CLIENT_STATE_LOGINED ||
                mockClientState == MockClientState.CLIENT_STATE_ASSIGNED);
    }

    //========================================================================

    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
/*
            synchronized (objSleep) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sleep(ms);
                        synchronized (objSleep) {
                            objSleep.notify();
                        }
                    }
                }).run();
                objSleep.wait();
            }
            */
        } catch (Exception e) {

        }
    }

    /**
     * 监听
     */
    private final ChannelFutureListener remover = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            logger.debug(ctx,"group shutdownGracefully");
            group.shutdownGracefully();
            group = null;
            // 所有资源释放完成之后，清空资源，再次发起重连操作
            if (gLastError == ServerCode.ULR_USER_INACTIVE ||
                    gLastError == ServerCode.SERR_SERVICE_DOWN) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                            try {
                                connectToAss();// 发起重连操作
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    };

    //终端 连接服务器
    public boolean connectToAss() throws Exception {
        boolean bRet = false;
        try {
            if (future != null) {
                future.channel().closeFuture().removeListener(remover);
            }

            Bootstrap b = new Bootstrap();
            group = new NioEventLoopGroup();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            if (isMt) {
                                ch.pipeline().addLast("heartbeat_timeout", new IdleStateHandler(0, 0, MT_CLIENT_HEARTBEAT_TIMEOUT_SECONDS));
                                ch.pipeline().addLast(new MessageDecoder(Message.ToMtTag));
                                ch.pipeline().addLast(new MessageEncoder(Message.MtTag));
                                ch.pipeline().addLast(new MtMsgHandler());
                            } else {
                                ch.pipeline().addLast("heartbeat_timeout", new IdleStateHandler(0, 0, SP_CLIENT_HEARTBEAT_TIMEOUT_SECONDS));
                                ch.pipeline().addLast(new MessageDecoder(Message.ToSpTag));
                                ch.pipeline().addLast(new MessageEncoder(Message.SpTag));
                                ch.pipeline().addLast(new SpMsgHandler());
                            }
                        }
                    });
            // 发起异步连接操作
            int client_port;
            if (isMt) {
                client_port = MockEnv.getAssMtPort();
            } else {
                client_port = MockEnv.getAssSpPort();
            }
            future = b.connect(new InetSocketAddress(MockEnv.getAssHost(), client_port)).sync();
            this.ctx = MockEnv.popCtx(future.channel().localAddress());
            localAddress = future.channel().localAddress();
            future.channel().closeFuture().addListener(remover);
            mockClientState = MockClientState.CLIENT_STATE_CONNECTED;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            group.shutdownGracefully();
            group = null;
            mockClientState = MockClientState.CLIENT_STATE_OFFLINE;
        }

        return false;
    }

    //终端 断开连接
    public void disconn() {
        if (ctx != null) {
            ctx.close();
            while(ctx != null) {
                logger.debug("waitfor ctx == null");
                sleep(100);
            }
        }
    }

    //执行Groovy脚本
    public void execGroovy(String cmd) {

    }

    public boolean sendCmdAndAssertRsp(Message message, boolean bWaitRsp, AssertMessage assertMessage) throws TestException {
        CmdUtils.get(getCtx()).sendCmdAndAssertRsp(message, bWaitRsp, assertMessage);
        return true;
    }
}
