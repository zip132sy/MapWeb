package com.zip132sy.mapweb.auth;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话管理。
 * 登录成功后生成随机 token，保存在内存中并带过期时间。
 */
public class SessionManager {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
    private final long timeoutMillis;

    public SessionManager(int timeoutMinutes) {
        this.timeoutMillis = timeoutMinutes * 60L * 1000L;
    }

    /**
     * 创建一个新会话。
     *
     * @param username 用户名
     * @return 会话 token
     */
    public String createSession(String username) {
        cleanup();
        String token = generateToken();
        sessions.put(token, new Session(username, System.currentTimeMillis() + timeoutMillis));
        return token;
    }

    /**
     * 校验 token 是否有效，有效则刷新过期时间。
     *
     * @param token 会话 token
     * @return 对应的用户名；无效返回 null
     */
    public String validate(String token) {
        if (token == null) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (System.currentTimeMillis() > session.expireAt) {
            sessions.remove(token);
            return null;
        }
        // 刷新过期时间（滑动过期）
        session.expireAt = System.currentTimeMillis() + timeoutMillis;
        return session.username;
    }

    /**
     * 注销会话。
     */
    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /**
     * 清理过期会话。
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Session>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Session> entry = it.next();
            if (now > entry.getValue().expireAt) {
                it.remove();
            }
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private static class Session {
        private final String username;
        private volatile long expireAt;

        Session(String username, long expireAt) {
            this.username = username;
            this.expireAt = expireAt;
        }
    }
}
