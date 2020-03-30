package test.message;

import lombok.Data;
import test.session.User;

import java.util.List;

/**
 * 聊天室用户列表封装类
 */
@Data
public class RoomUserListMessage {
    final int protocol = 2;
    int userNum;
    List<User> userList;

}
