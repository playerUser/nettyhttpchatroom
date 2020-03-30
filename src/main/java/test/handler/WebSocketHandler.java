package test.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import test.message.RoomMessage;
import test.message.RoomUserListMessage;
import test.session.Session;
import test.session.SessionAttribute;
import test.session.SessionUtil;
import test.session.User;

import java.util.LinkedList;
import java.util.List;

/**
 * 处理websocket的handler
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final List<ChannelGroup> channelGroups;

    public WebSocketHandler(List<ChannelGroup> channelGroups) {
        this.channelGroups = channelGroups;
    }


    //websocket连接成功后，将session与channel绑定
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {

            Channel channel = ctx.channel();
            WebSocketServerProtocolHandler.HandshakeComplete req =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;

            String uri = req.requestUri();

            Session session = SessionUtil.parseSessionFromUri(uri);
            SessionUtil.bindChannel(session, channel, channelGroups);


        }
    }

    //连接断开后，将用户session与channel解绑
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SessionUtil.unBindChannel(ctx.channel(), channelGroups);
        super.channelInactive(ctx);
    }

    //根据请求类型，做出相对响应
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        //连接后，获取绑定的session信息
        Channel channel = ctx.channel();
        Session session = SessionUtil.getSessionFromChannel(channel);


        if (session != null) {
            //websocket传递的信息，均使用json格式传递
            String jsonString = msg.text();
            JSONObject jsonObj = JSONObject.parseObject(jsonString);

            //获取请求类型
            Integer protocol = jsonObj.getInteger("protocol");

            switch (protocol) {
                //向聊天室发送信息
                //实现：对信息简单加工后，向聊天室channelgroup广播
                case 1:
                    RoomMessage message = new RoomMessage();
                    message.setUserName(session.getUser().getUserName());
                    message.setPortraitId(session.getUser().getPortraitId());
                    message.setTimeSend(jsonObj.getDate("timeNow"));

                    message.setMessage(jsonObj.getString("message"));
                    jsonString = JSON.toJSONString(message);

                    channelGroups.get(session.getRoomId() - 1).writeAndFlush(new TextWebSocketFrame(jsonString));
                    break;

                    //获取聊天室用户列表
                //实现：从聊天室对应的channelgroup遍历所有channel的session，返回列表
                case 2:
                    ChannelGroup channels = channelGroups.get(session.getRoomId() - 1);
                    List<User> userList = new LinkedList<>();
                    for (Channel userChannel : channels) {
                        userList.add(SessionUtil.getSessionFromChannel(userChannel).getUser());
                    }
                    RoomUserListMessage roomUserListMessage = new RoomUserListMessage();
                    roomUserListMessage.setUserNum(channels.size());
                    roomUserListMessage.setUserList(userList);
                    jsonString = JSON.toJSONString(roomUserListMessage);

                    ctx.writeAndFlush(new TextWebSocketFrame(jsonString));
                    break;
//            case 3:
//
//                break;
                default:

            }


        }
    }
}
