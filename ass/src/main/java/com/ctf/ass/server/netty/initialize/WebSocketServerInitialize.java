package com.ctf.ass.server.netty.initialize;

import com.ctf.ass.server.netty.handler.WebSocketMsgHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Charles
 * @create 2017/7/21 16:24
 */
@Component("webSocketServerInitialize")
public class WebSocketServerInitialize extends ChannelInitializer<SocketChannel> {
    @Autowired
    private WebSocketMsgHandler webSocketMsgHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                .addLast("http-codec", new HttpServerCodec())
                .addLast("aggregator", new HttpObjectAggregator(65536))
                .addLast("http-chunked", new ChunkedWriteHandler())
                .addLast("handler", webSocketMsgHandler);
    }
}
