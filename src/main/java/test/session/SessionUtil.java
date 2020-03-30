package test.session;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * session工具类
 */
public class SessionUtil {

    //从uri中获取session信息，并封装
    public static Session parseSessionFromUri(String uri) {
        Session session = new Session();

        //截取问号后的请求参数
        String[] parameters = uri.substring(uri.lastIndexOf("?") + 1).split("&");

        //遍历这些参数
        for (String parameter : parameters) {
            //从字符串中截取请求参数的key和value
            String key = parameter.substring(0, parameter.lastIndexOf("="));
            String value = parameter.substring(parameter.lastIndexOf("=") + 1);

            switch (key) {
                case "userName":
                    //这里用utf-8解码，否则会出现中文乱码
                    try {
                        value = java.net.URLDecoder.decode(value, "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    session.getUser().setUserName(value);
                    break;
                case "userPortrait":
                    session.getUser().setPortraitId(Integer.parseInt(value));
                    break;
                case "room":
                    session.setRoomId(Integer.parseInt(value));
                    break;
            }
        }

        return session;
    }

    //将用户session与连接channel绑定
    //实现：在channel上添加叫“session”的attribute，并将用户session设置为其值
    public static void bindChannel(Session session, Channel channel, List<ChannelGroup> channelGroups) {
        channel.attr(SessionAttribute.SESSION_ATTRIBUTE_KEY).set(session);
        channelGroups.get(session.getRoomId() - 1).add(channel);

    }

    //用户session与channel解绑
    //实现：channel的“session” attribute设置为null。
    //另外：使用了channelgroup记录聊天室用户列表，所以要从列表中删除该用户
    public static void unBindChannel(Channel channel, List<ChannelGroup> channelGroups) {
        Session session = channel.attr(SessionAttribute.SESSION_ATTRIBUTE_KEY).get();
        if (session != null) {

            channel.attr(SessionAttribute.SESSION_ATTRIBUTE_KEY).set(null);
            channelGroups.get(session.getRoomId() - 1).remove(channel);
        }
    }

    //从channel上获取session信息
    //实现：获取“session”的attribute值
    public static Session getSessionFromChannel(Channel channel) {
        return channel.attr(SessionAttribute.SESSION_ATTRIBUTE_KEY).get();
    }
}
