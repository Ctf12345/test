package com.ctf.autotest;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogWrapper {
    private Logger logger = null;

    private LogWrapper(String className) {
        this.logger = LogManager.getLogger(className);
    }

    public static LogWrapper getLogger(String className) {
        return new LogWrapper(className);
    }

    // ==============  Server ==========================

    public void trace(String s) {
        logger.trace(s);
    }

    public void info(String s) {
        logger.info(s);
    }

    public void debug(String s) {
        logger.debug(s);
    }

    public void warn(String s) {
        logger.warn(s);
    }

    public void error(String s) {
        logger.error(s);
    }

    public void fatal(String s) {
        logger.fatal(s);
    }

    // ===============  client =========================

    private String getClientInfo(ChannelHandlerContext ctx) {
        String clientInfo = "???";
        if (ctx == null) {
            return clientInfo;
        }

        MockMt mockMt = MockEnv.getMockMt(ctx);
        if (mockMt != null) {
            clientInfo = "Mt:" + mockMt.getUserId();
        } else {
            MockSp mockSp = MockEnv.getMockSp(ctx);
            if (mockSp != null) {
                clientInfo = "Sp:" + mockSp.getMacAddrStr();
            }
        }

        return "[" + clientInfo + "@" + ctx.channel().localAddress().toString() + "]";
    }

    public void trace(ChannelHandlerContext ctx, String s) {
        logger.trace(getClientInfo(ctx) + s);
    }

    public void info(ChannelHandlerContext ctx, String s) {
        logger.info(getClientInfo(ctx) + s);
    }

    public void debug(ChannelHandlerContext ctx, String s) {
        logger.debug(getClientInfo(ctx) + s);
    }

    public void warn(ChannelHandlerContext ctx, String s) {
        logger.warn(getClientInfo(ctx) + s);
    }

    public void warn(ChannelHandlerContext ctx, String s, Throwable throwable) {
        logger.debug(getClientInfo(ctx), s, throwable);
    }

    public void error(ChannelHandlerContext ctx, String s) {
        logger.error(getClientInfo(ctx) + s);
    }

    public void fatal(ChannelHandlerContext ctx, String s) {
        logger.fatal(getClientInfo(ctx) + s);
    }
}
