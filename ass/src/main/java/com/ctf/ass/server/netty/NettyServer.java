package com.ctf.ass.server.netty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.netty.channel.EventLoopGroup;

/**
 * @author Charles
 * @create 2017/7/21 16:15
 */
public abstract class NettyServer {
    private List<EventLoopGroup> eventLoopGroupList = new ArrayList<>();

    /**
     * 启动NettyServer
     */
    @PostConstruct
    public abstract void start() throws InterruptedException;

    /**
     * 存储EventLoopGroup
     */
    protected void addEventLoopGroup(EventLoopGroup... eventLoopGroups) {
        eventLoopGroupList.addAll(Arrays.asList(eventLoopGroups));
    }

    /**
     * 释放EventLoopGroup
     */
    @PreDestroy
    public void destroy() {
        if (eventLoopGroupList.size() > 0) {
            eventLoopGroupList.forEach(eventLoopGroup -> {
                eventLoopGroup.shutdownGracefully();
            });
        }
    }
}
