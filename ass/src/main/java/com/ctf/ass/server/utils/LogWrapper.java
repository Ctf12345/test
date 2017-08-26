package com.ctf.ass.server.utils;

import com.ctf.oss.CCM;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Charles
 * @create 2017/7/21 14:14
 */
public class LogWrapper {
    private Logger logger = null;

    private LogWrapper(String className) {
        this.logger = LogManager.getLogger(className);
    }

    private LogWrapper(Class<?> clazz) {
        this.logger = LogManager.getLogger(clazz);
    }

    public static LogWrapper getLogger(String className) {
        return new LogWrapper(className);
    }

    public static LogWrapper getLogger(Class<?> clazz) {
        return new LogWrapper(clazz);
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

        if (ctx == null || ctx.channel() == null) {
            return clientInfo;
        }

        String clientIp = ctx.channel().remoteAddress().toString();
        String clientId = CCM.getId(CCM.ClientType.MT, clientIp);
        if (clientId != null) {
            clientInfo = "Mt:" + clientId;
        } else {
            clientId = CCM.getId(CCM.ClientType.SP, clientIp);
            if (clientId != null) {
                clientInfo = "Sp:" + clientId;
            }
        }

        return "[" + clientInfo + "@" + clientIp + "]";
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

    public void warn(ChannelHandlerContext ctx, String s, Throwable throwable) {
        logger.debug(getClientInfo(ctx), s, throwable);
    }

    public void warn(ChannelHandlerContext ctx, String s) {
        logger.warn(getClientInfo(ctx) + s);
    }

    public void error(ChannelHandlerContext ctx, String s) {
        logger.error(getClientInfo(ctx) + s);
    }

    public void fatal(ChannelHandlerContext ctx, String s) {
        logger.fatal(getClientInfo(ctx) + s);
    }
}
