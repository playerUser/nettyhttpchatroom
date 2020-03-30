package test.session;

import lombok.Data;

/**
 * 用户连接信息封装类
 */

@Data
public class Session {

    User user = new User();
    private int roomId;

}
