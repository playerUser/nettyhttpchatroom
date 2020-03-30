package test.message;

import lombok.Data;

import java.util.Date;

/**
 * 聊天室消息的封装类
 */
@Data
public class RoomMessage {
    final int protocol = 1;
    String userName;
    int portraitId;
    Date timeSend;
    String message;
}
