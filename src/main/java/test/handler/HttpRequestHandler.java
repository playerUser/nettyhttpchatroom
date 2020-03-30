package test.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 处理http请求的handler
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    //静态文件的地址
    private static final String STATIC_LOCATION;

    //404页面
    private static final File NOT_FOUND;

    //代表聊天室的组
    private final List<ChannelGroup> GROUPS;

     //初始化静态文件和404页面的位置
    static {
        STATIC_LOCATION = "/E:/ex/IdeaProjects/nettyhttpchatroom/target/classes/static";
        NOT_FOUND = new File(STATIC_LOCATION + "/404.html");
    }

    //注入代表聊天室的组
    public HttpRequestHandler(List<ChannelGroup> groups) {
        GROUPS = groups;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

         //获取请求地址
        String uri = request.uri();


        if ("/".equals(uri)) {
            uri = "/login.html";
        }
        //System.out.println(uri);


        //对请求地址分类处理
        if (uri.startsWith("/json")) {
            handleJsonQuery(ctx, request, uri);
        } else
            handleStaticFile(ctx, request, uri);


    }

    //json请求的处理
    private void handleJsonQuery(ChannelHandlerContext ctx, FullHttpRequest request, String uri) {
        //uri形式： /json/XXX 截掉“/json”
        uri = uri.substring(5);
//        System.out.println(uri);


        if (uri.equals("/queryPeopleNumber")) {
            //目前只定义了一个json请求，即查询各房间人数。
            //依次查询各房间人数，封装成json返回
            int[] groupNumber = new int[6];
            for (int i = 0; i < 6; i++) {
                groupNumber[i] = GROUPS.get(i).size();
            }
            //将结果返回
            ResponseJson(ctx, request, JSON.toJSONString(groupNumber));
        }

    }

     //json响应函数
    private void ResponseJson(ChannelHandlerContext ctx, FullHttpRequest req, String jsonStr) {

        boolean keepAlive = HttpUtil.isKeepAlive(req);
        byte[] jsonByteByte = jsonStr.getBytes();
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(jsonByteByte));
        response.headers().set(CONTENT_TYPE, "text/json");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }

    //简化异常处理，打印堆栈并关闭连接
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    //静态文件请求处理函数
    private void handleStaticFile(ChannelHandlerContext ctx, FullHttpRequest request, String uri) throws IOException {
        //截掉请求中的参数
        //从问号截断请求
        int lastIndexOf;
        if ((lastIndexOf = uri.lastIndexOf("?")) != -1) {
            uri = uri.substring(0, lastIndexOf);
        }
//        System.out.println(uri);

        //找到请求的静态文件
        String path = STATIC_LOCATION + uri;
//        System.out.println(path);
        File staticFile = new File(path);

        //处理100continue请求
        if (HttpUtil.is100ContinueExpected(request)) {
            DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE);
            ctx.writeAndFlush(response);
        }

        //如果没找到文件，返回404页面
        if (!staticFile.exists())
            staticFile = NOT_FOUND;

        //打开文件
        RandomAccessFile file = new RandomAccessFile(staticFile, "r");

        //采用netty自带的http响应类
        DefaultHttpResponse response = new DefaultHttpResponse(request.protocolVersion(), OK);

        //设置响应类型，并返回对应文件
        if (staticFile == NOT_FOUND)
            response.setStatus(HttpResponseStatus.NOT_FOUND);

        if (path.endsWith(".html")) {
            response.headers().set(CONTENT_TYPE, "text/html;charset=UTF-8");
        } else if (path.endsWith(".js")) {
            response.headers().set(CONTENT_TYPE, "application/x-javascript");
        } else if (path.endsWith(".css")) {
            response.headers().set(CONTENT_TYPE, "text/css; charset=UTF-8");
        } else if (path.endsWith(".png")) {
            response.headers().set(CONTENT_TYPE, "image/png");
        } else if (path.endsWith(".gif")) {
            response.headers().set(CONTENT_TYPE, "image/gif");
        } else if (path.endsWith(".jpg")) {
            response.headers().set(CONTENT_TYPE, "image/jpg");
        }

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(CONTENT_LENGTH, file.length());
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        ctx.write(response);

        if (ctx.pipeline().get(SslHandler.class) == null) {
            ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
        } else
            ctx.write(new ChunkedNioFile(file.getChannel()));

        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        future.addListener(ChannelFutureListener.CLOSE);
        file.close();
    }


}
