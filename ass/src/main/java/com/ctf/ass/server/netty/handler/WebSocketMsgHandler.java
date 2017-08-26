package com.ctf.ass.server.netty.handler;

import com.ctf.ass.server.component.ToMtMsg;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.protobuf.MtProtoMsg;
import com.ctf.ass_public.globals.ErrCode;
import com.ctf.oss.CCM;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Component("webSocketMsgHandler")
@Scope("prototype")
@ChannelHandler.Sharable
public class WebSocketMsgHandler extends SimpleChannelInboundHandler<Object> {
    private static final LogWrapper logger = LogWrapper.getLogger(WebSocketMsgHandler.class.getName());
    private WebSocketServerHandshaker webSocketServerHandshaker;

    private static Pattern websocket_user_trace_level_pattern = Pattern.compile("^userId=([^,]+),level=(.+)$");     //trace用户及等级设置

    @Autowired
    private ToMtMsg toMtMsg;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        CCM.addCtx(CCM.ClientType.WEB, ctx);
        logger.debug("客户端 [" + channel.remoteAddress() + "] 与服务器段开启");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        CCM.delCtx(CCM.ClientType.WEB, ctx);
        CCM.delWebUserId(ctx.channel().remoteAddress().toString());
        logger.debug("客户端 [" + channel.remoteAddress() + "] 与服务器段关闭");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
        // 传统的HTTP接入
        if (message instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) message);
        }
        // WebSocket接入
        else if (message instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) message);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

        // 如果HTTP解码失败，返回HHTP异常
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // 构造握手响应返回，本机测试
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://192.168.199.188:18080/websocket", null, false);
        webSocketServerHandshaker = wsFactory.newHandshaker(req);
        if (webSocketServerHandshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            webSocketServerHandshaker.handshake(ctx.channel(), req);
        }
    }

    /**
     * WebSocket处理
     *
     * @param ctx
     * @param frame
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        // 判断是否是关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            webSocketServerHandshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //二进制数据处理
        if (frame instanceof BinaryWebSocketFrame) {
            logger.info("|==================二进制数据接收==================");
            ByteBuf byteBuf = frame.content();
            for (int i = 0, length = byteBuf.capacity(); i < length; i++) {
                byte b = byteBuf.getByte(i);
                logger.info("\t" + b);
            }
            logger.info("|==================二进制数据接收 End===============");
            return;
        }
        // 仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }

        // 打印收到的文本消息
        String text = ((TextWebSocketFrame) frame).text();
        logger.info(String.format("WebSocket [%s] received [%s] ", ctx.channel(), text));

        procWebCmd(ctx, text);
    }

    public static void sendWebSocketText(ChannelHandlerContext webCtx, String text) {
        TextWebSocketFrame frame = new TextWebSocketFrame(text);
        ChannelFuture f = webCtx.writeAndFlush(frame);
        f.addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);    //if any error occur in writeAndFlush, will fire exception can caught by 'exceptionCaught'
    }

    private void procWebCmd(ChannelHandlerContext webCtx, String cmd) {
        ErrCode.ServerCode errCode = ErrCode.ServerCode.ERR_ILLEGAL_PARAM;
        Matcher m = websocket_user_trace_level_pattern.matcher(cmd);

        logger.debug(webCtx, cmd);
        sendWebSocketText(webCtx, cmd);   //ECHO

        if (m.find()) {
            String userId = m.group(1);
            String level = m.group(2);
            if (StringUtils.isNotEmpty(userId)) {
                CCM.setWebUserId(webCtx.channel().remoteAddress().toString(),userId);
            }else{
                userId = CCM.getWebUserId(webCtx.channel().remoteAddress().toString());
            }
            //发送Http命令给终端
            errCode = toMtMsg.sendHttpMtTermTraceReq(webCtx, userId, MtProtoMsg.LEVEL.valueOf(level));
        }
        if (errCode != ErrCode.ServerCode.ERR_OK) {
            sendWebSocketText(webCtx, errCode.cnErrMsg());
        }
    }


    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest, FullHttpResponse fullHttpResponse) {
        // 返回应答给客户端
        if (fullHttpResponse.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(fullHttpResponse.status().toString(), CharsetUtil.UTF_8);
            fullHttpResponse.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(fullHttpResponse, fullHttpResponse.content().readableBytes());
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(fullHttpResponse);
        if (!HttpUtil.isKeepAlive(fullHttpRequest) || fullHttpResponse.status().code() != 200) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) throws Exception {
        logger.warn(ctx, "ExceptionCaught!", throwable.fillInStackTrace());

        Channel incoming = ctx.channel();
        logger.info("Client:" + incoming.remoteAddress() + "异常");

        // 当出现异常就关闭连接
        ctx.close();
    }
}
