package com.ctf.ass.server.netty;

import com.ctf.ass.server.component.RocketMQMessageSender;
import com.ctf.ass.server.configuration.ServerConfig;
import com.ctf.ass.server.netty.initialize.SimPoolServerInitialize;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_public.struct.OpHistory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * SimPool服务端
 */
@Component("simPoolServer")
public class SimPoolServer extends NettyServer {
    private static final LogWrapper logger = LogWrapper.getLogger(SimPoolServer.class);
    @Autowired
    private SimPoolServerInitialize simPoolServerInitialize;
    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;

    @Override
    public void start() throws InterruptedException {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(simPoolServerInitialize);

        // 绑定端口，同步等待成功
        ChannelFuture future = b.bind(ServerConfig.sp_port).sync();
//        future.channel().closeFuture().addListener(remover);
        //发送操作历史
        rocketMQMessageSender.sendOpHistory(new OpHistory(ServerConfig.localhost + ":" + ServerConfig.sp_port, OpHistory.OpType.Server, "bind_sp_port", 0));
        logger.info("SimPool server start ok at : " + ServerConfig.sp_port);

        //存储EventLoopGroup 用于关闭释放资源
        addEventLoopGroup(bossGroup, workerGroup);
    }
}
