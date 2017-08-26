package com.ctf.ass.server.netty;

import com.ctf.ass.server.component.RocketMQMessageSender;
import com.ctf.ass.server.configuration.ServerConfig;
import com.ctf.ass.server.netty.initialize.MtServerInitialize;
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
 * MT服务端
 *
 * @author Charles
 * @create 2017/7/21 15:53
 */
@Component("mtServer")
public class MtServer extends NettyServer {
    private static final LogWrapper logger = LogWrapper.getLogger(MtServer.class);
    @Autowired
    private MtServerInitialize mtServerInitialize;
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
                .childHandler(mtServerInitialize);

        // 绑定端口，同步等待成功
        ChannelFuture future = b.bind(ServerConfig.mt_port).sync();
//        future.channel().closeFuture().addListener(remover);

        //发送操作历史
        rocketMQMessageSender.sendOpHistory(new OpHistory(ServerConfig.localhost + ":" + ServerConfig.mt_port, OpHistory.OpType.Server, "bind_mt_port", 0));

        logger.info("Mt server start ok at : " + ServerConfig.mt_port);
        //存储EventLoopGroup 用于关闭释放资源
        addEventLoopGroup(bossGroup, workerGroup);
    }
}
