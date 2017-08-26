package com.ctf.ass.server.netty.initialize;

import com.ctf.ass.server.netty.handler.MtMsgHandler;
import com.ctf.ass.server.netty.proto.MessageDecoder;
import com.ctf.ass.server.netty.proto.MessageEncoder;
import com.ctf.ass_codec.struct.Message;
import com.ctf.ass_public.globals.GlobalConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author Charles
 * @create 2017/7/21 15:56
 */
@Component("mtServerInitialize")
public class MtServerInitialize extends ChannelInitializer<SocketChannel> {
    @Autowired
    private MtMsgHandler mtMsgHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                .addLast("idle_timeout", new IdleStateHandler(0, 0, GlobalConfig.MT_IDLE_TIMEOUT_SECONDS))
                .addLast(new MessageDecoder(Message.MtTag))
                .addLast(new MessageEncoder(Message.ToMtTag))
                .addLast(mtMsgHandler);
    }
}
