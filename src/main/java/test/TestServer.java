package test;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;


import io.netty.util.concurrent.ImmediateEventExecutor;
import test.handler.HttpRequestHandler;
import test.handler.WebSocketHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 服务器实现类
 */
public class TestServer {


    public void start() throws Exception {

        //为了简化，未区分服务器连接和处理的线程，只用了一个线程池，并采用了默认参数
        EventLoopGroup group = new NioEventLoopGroup();

        //为了简化，聊天室房间固定为6个，为每个房间创建一个channelgroup，记录用户channel
        int roomNum = 6;
        List<ChannelGroup> groups = new ArrayList<>(roomNum);
        for (int i = 0; i < roomNum; i++) {
            groups.add(new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE));
        }



        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            ch.pipeline().addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new ChunkedWriteHandler())
                                    //规定所有websocket请求，以/ws开头
                                    //在 WebSocketServerProtocolHandler 添加这条规则，自动处理websocket的连接和断开请求
                                    .addLast(new WebSocketServerProtocolHandler("/ws",true))
                                    //处理websocket的TextWebSocketFrame信息
                                    .addLast(new WebSocketHandler(groups))
                                    //处理静态文件请求，以及json请求
                                    .addLast(new HttpRequestHandler(groups));

                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ;

            ChannelFuture f = b.bind(80).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }


    public static void main(String[] args) throws Exception {
        new TestServer().start();

    }


}



