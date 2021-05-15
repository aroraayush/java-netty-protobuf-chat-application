package edu.usfca.cs.chat.net;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServerMessageRouter {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;
    private MessagePipeline pipeline;

    private Map<Integer, Channel> ports = new HashMap<>();

    public ServerMessageRouter(ChannelInboundHandlerAdapter inboundHandler) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(4);

        pipeline = new MessagePipeline(inboundHandler);

        bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(pipeline)
            .option(ChannelOption.SO_BACKLOG, 128)  // determining the number of connections queued
            .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * Begins listening for incoming messages on the specified port. When this
     * method returns, the server socket is open and ready to accept
     * connections.
     *
     * @param port The port to listen on
     */
    public void listen(int port) {
        // syncUninterruptibly()
        // Waits for this future until it is done, and rethrows the cause of the failure if this future failed.
        ChannelFuture cf = bootstrap.bind(port).syncUninterruptibly();
        // awaitUninterruptibly()
        // Waits for this future to be completed without interruption.
        cf.awaitUninterruptibly();
        if (cf.isSuccess() == true) {
            ports.put(port, cf.channel());
        } else {
            System.out.println("Failed to listen on port " + port);
        }
    }

    public void close(int port) {
        Channel c = ports.get(port);
        if (c == null) {
            return;
        }
        ports.remove(port);
        c.disconnect().syncUninterruptibly();
    }

    /**
     * Closes the server socket channel and stops processing incoming
     * messages.
     */
    public void shutdown() throws IOException {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        for (Channel c : ports.values()) {
            c.close().syncUninterruptibly();
        }
    }
}
