package com.ctf.ass.server.netty.initialize;

import com.ctf.ass.server.netty.handler.HttpMsgHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * @author Charles
 * @create 2017/7/21 16:24
 */
@Component("httpServerInitialize")
public class HttpServerInitialize extends ChannelInitializer<SocketChannel> {
    @Autowired
    private HttpMsgHandler httpMsgHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                .addLast("decoder", new HttpRequestDecoder())
                .addLast("encoder", new HttpResponseEncoder())
                .addLast("deflator", new HttpContentCompressor())
                .addLast("handler", httpMsgHandler);
    }
}
