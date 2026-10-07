package com.zip132sy.mapweb.auth;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 管理员账号管理。
 * 账号数据持久化到 accounts.yml，密码使用 SHA-256 + 随机盐哈希存储，绝不保存明文。
 */
public class AccountManager {

    private static final String FILE_NAME = "accounts.yml";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public AccountManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    /**
     * 从磁盘加载账号数据。
     */
    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建账号文件：" + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 保存账号数据到磁盘。
     */
    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存账号文件：" + e.getMessage());
        }
    }

    /**
     * 创建或更新一个管理员账号。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 是否创建成功
     */
    public boolean createAccount(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.length() < 4) {
            return false;
        }

        String key = "accounts." + username.toLowerCase();
        String salt = generateSalt();
        String hash = hash(password, salt);

        data.set(key + ".username", username);
        data.set(key + ".salt", salt);
        data.set(key + ".hash", hash);
        save();
        return true;
    }

    /**
     * 删除一个管理员账号。
     */
    public boolean removeAccount(String username) {
        if (username == null) {
            return false;
        }
        String key = "accounts." + username.toLowerCase();
        if (!data.contains(key)) {
            return false;
        }
        data.set(key, null);
        save();
        return true;
    }

    /**
     * 校验账号密码是否正确。
     */
    public boolean verify(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        String key = "accounts." + username.toLowerCase();
        if (!data.contains(key)) {
            return false;
        }
        String salt = data.getString(key + ".salt");
        String expected = data.getString(key + ".hash");
        if (salt == null || expected == null) {
            return false;
        }
        String actual = hash(password, salt);
        return constantTimeEquals(expected, actual);
    }

    /**
     * 列出所有管理员用户名。
     */
    public List<String> listAccounts() {
        List<String> result = new ArrayList<String>();
        if (data.isConfigurationSection("accounts")) {
            Set<String> keys = data.getConfigurationSection("accounts").getKeys(false);
            for (String key : keys) {
                String name = data.getString("accounts." + key + ".username");
                if (name != null) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    /**
     * 是否存在任何账号。
     */
    public boolean hasAnyAccount() {
        return !listAccounts().isEmpty();
    }

    private String generateSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    private String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes("UTF-8"));
            byte[] hashed = digest.digest(password.getBytes("UTF-8"));
            // 再迭代若干次，增加暴力破解成本
            for (int i = 0; i < 1000; i++) {
                digest.reset();
                hashed = digest.digest(hashed);
            }
            return toHex(hashed);
        } catch (Exception e) {
            return "";
        }
    }

    private String toHex(byte[] bytes) {
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

    /**
     * 恒定时间比较，避免时序攻击。
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
