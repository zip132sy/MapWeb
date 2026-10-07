package com.zip132sy.mapweb.web;

import com.sun.net.httpserver.HttpServer;
import com.zip132sy.mapweb.auth.AccountManager;
import com.zip132sy.mapweb.auth.SessionManager;
import com.zip132sy.mapweb.config.PluginConfig;
import com.zip132sy.mapweb.texture.TextureManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 内置 Web 服务管理。
 * 使用 JDK 自带的 HttpServer，支持端口自动检测。
 */
public class WebServer {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int MAX_PORT_TRIES = 20;

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final AccountManager accountManager;
    private final SessionManager sessionManager;
    private final TextureManager textureManager;

    private HttpServer server;
    private ExecutorService executor;
    private int actualPort = -1;

    public WebServer(JavaPlugin plugin, PluginConfig config,
                     AccountManager accountManager, SessionManager sessionManager,
                     TextureManager textureManager) {
        this.plugin = plugin;
        this.config = config;
        this.accountManager = accountManager;
        this.sessionManager = sessionManager;
        this.textureManager = textureManager;
    }

    /**
     * 启动 Web 服务。
     *
     * @return 是否启动成功
     */
    public boolean start() {
        String indexHtml = loadResource("web/index.html");
        if (indexHtml == null) {
            plugin.getLogger().severe("无法加载网页资源 web/index.html，Web 服务启动失败。");
            return false;
        }

        int startPort = config.getPort();
        int boundPort = -1;

        if (config.isAutoPort()) {
            boundPort = findAvailablePort(startPort);
        } else {
            if (isPortAvailable(startPort)) {
                boundPort = startPort;
            }
        }

        if (boundPort < 0) {
            plugin.getLogger().severe("端口 " + startPort + " 及后续端口均不可用，Web 服务启动失败。");
            return false;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(boundPort), 0);
            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);
            server.createContext("/", new WebHandler(config, indexHtml, accountManager, sessionManager, plugin, textureManager));
            server.start();
            this.actualPort = boundPort;
            plugin.getLogger().info("地图网页已启动：http://<服务器IP>:" + boundPort + "/");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Web 服务启动异常：" + e.getMessage());
            return false;
        }
    }

    /**
     * 停止 Web 服务。
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        actualPort = -1;
    }

    /**
     * 从起始端口开始，向后寻找第一个可用端口。
     */
    private int findAvailablePort(int startPort) {
        for (int i = 0; i < MAX_PORT_TRIES; i++) {
            int candidate = startPort + i;
            if (candidate > 65535) {
                break;
            }
            if (isPortAvailable(candidate)) {
                return candidate;
            }
        }
        return -1;
    }

    /**
     * 检测端口是否可用（尝试绑定后立即释放）。
     */
    private boolean isPortAvailable(int port) {
        java.net.ServerSocket socket = null;
        try {
            socket = new java.net.ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 从 jar 内读取文本资源。
     */
    private String loadResource(String path) {
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = plugin.getResource(path);
            if (is == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(is, UTF8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
        }
    }

    public int getActualPort() {
        return actualPort;
    }

    public boolean isRunning() {
        return server != null;
    }
}
