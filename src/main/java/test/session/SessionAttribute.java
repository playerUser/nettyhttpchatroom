package test.session;

import io.netty.util.AttributeKey;

/**
 * channel上attribute
 */
public interface SessionAttribute {

     AttributeKey<Session> SESSION_ATTRIBUTE_KEY = AttributeKey.newInstance("session");

}
