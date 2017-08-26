package com.ctf.oss;

import java.util.ArrayList;
import java.util.HashMap;

import io.netty.channel.ChannelHandlerContext;

/**
 * Client Connection Manager
 */
public class CCM {
    public enum ClientType {
        MT,
        SP,
        WEB
    }

    private static ArrayList<ChannelHandlerContext> mtCtxs;
    private static ArrayList<ChannelHandlerContext> spCtxs;
    private static ArrayList<ChannelHandlerContext> webCtxs;
    private static HashMap<String, String> webUserIds;
    private static HashMap<String, String> mtIds;
    private static HashMap<String, String> spIds;

    static {
        mtCtxs = new ArrayList<>();
        spCtxs = new ArrayList<>();
        webCtxs = new ArrayList<>();

        webUserIds = new HashMap<>();
        mtIds = new HashMap<>();
        spIds = new HashMap<>();
    }

    private static ArrayList<ChannelHandlerContext> getCtxs(ClientType clientType) {
        ArrayList<ChannelHandlerContext> ctxs = null;
        switch (clientType) {
            case MT:
                ctxs = mtCtxs;
                break;

            case SP:
                ctxs = spCtxs;
                break;

            case WEB:
                ctxs = webCtxs;
                break;
        }

        return ctxs;
    }

    //添加ctx到Ctxs
    public static void addCtx(ClientType clientType, ChannelHandlerContext ctx) {
        ArrayList<ChannelHandlerContext> ctxs = getCtxs(clientType);
        if (ctxs != null) {
            ctxs.add(ctx);
        }
    }

    //按ipAddr从Ctxs中获取ctx
    public static ChannelHandlerContext getCtx(ClientType clientType, String ipAddr) {
        ArrayList<ChannelHandlerContext> ctxs = getCtxs(clientType);
        if (ctxs != null) {
            if (ipAddr != null) {
                for (ChannelHandlerContext ctx : ctxs) {
                    if(ctx != null){
                        if (ctx.channel().remoteAddress().toString().equals(ipAddr)) {
                            return ctx;
                        }
                    }
                }
            }
        }

        return null;
    }

    //当ctx断开时，从Ctxs中移除该ctx
    public static void delCtx(ClientType clientType, ChannelHandlerContext ctx) {
        ArrayList<ChannelHandlerContext> ctxs = getCtxs(clientType);
        if (ctxs != null) {
            if (ctx != null) {
                for (ChannelHandlerContext vCtx : ctxs) {
                    if (vCtx.channel().equals(ctx.channel())) {
                        ctxs.remove(vCtx);
                        delId(clientType, vCtx.channel().remoteAddress().toString());
                        return;
                    }
                }
            }
        }
    }

    private static void delId(ClientType clientType, String ipAddr) {
        switch (clientType) {
            case MT:
                mtIds.remove(ipAddr);
                break;

            case SP:
                spIds.remove(ipAddr);
                break;
        }
    }

    public static void setId(ClientType clientType, String ipAddr, String id) {
        switch (clientType) {
            case MT:
                mtIds.put(ipAddr, id);
                break;

            case SP:
                spIds.put(ipAddr, id);
                break;
        }
    }

    public static String getId(ClientType clientType, String ipAddr) {
        String id = null;
        switch (clientType) {
            case MT:
                id = mtIds.get(ipAddr);
                break;

            case SP:
                id = spIds.get(ipAddr);
                break;
        }

        return id;
    }

    public static void delWebUserId(String ipAddr) {
        webUserIds.remove(ipAddr);
    }

    public static void setWebUserId(String ipAddr, String userId) {
        webUserIds.put(ipAddr, userId);
    }

    public static String getWebUserId(String ipAddr) {
        return webUserIds.get(ipAddr);
    }

    public static ArrayList<ChannelHandlerContext> getWebCtxs(String userId) {
        ArrayList<ChannelHandlerContext> webCtxs = new ArrayList<ChannelHandlerContext>();
        for (String key : webUserIds.keySet()) {
            String value = webUserIds.get(key);
            if (value.equals(userId)) {
                ChannelHandlerContext webctx = getCtx(ClientType.WEB, key);
                if (webctx != null) {
                    webCtxs.add(webctx);
                }
            }
        }
        return webCtxs;
    }
}
