package com.zip132sy.mapweb;

import com.zip132sy.mapweb.command.MapWebCommand;
import com.zip132sy.mapweb.auth.AccountManager;
import com.zip132sy.mapweb.auth.SessionManager;
import com.zip132sy.mapweb.config.PluginConfig;
import com.zip132sy.mapweb.texture.TextureManager;
import com.zip132sy.mapweb.web.WebServer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MapWeb 主类。
 * 启动内置 Web 服务，提供服务器已加载区块的实时俯视地图网页。
 */
public class MapWebPlugin extends JavaPlugin implements Listener {

    private PluginConfig pluginConfig;
    private WebServer webServer;
    private AccountManager accountManager;
    private SessionManager sessionManager;
    private TextureManager textureManager;

    @Override
    public void onEnable() {
        // 1. 加载配置
        this.pluginConfig = new PluginConfig(this);

        // 2. 初始化账号与会话管理
        this.accountManager = new AccountManager(this);
        this.sessionManager = new SessionManager(pluginConfig.getSessionTimeout());

        // 3. 加载材质包
        this.textureManager = new TextureManager(this);
        // 材质包可能较大（含下载），放到异步线程，避免阻塞服务器启动
        // 注意：材质加载失败绝不能影响 Web 服务启动，这里做完整异常保护
        final java.util.List<String> downloadUrls = pluginConfig.getTextureDownloadUrls();
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    textureManager.load(downloadUrls);
                } catch (Throwable t) {
                    getLogger().warning("材质包加载异常（不影响网页地图）：" + t.getMessage());
                }
            }
        });

        // 4. 启动 Web 服务
        this.webServer = new WebServer(this, pluginConfig, accountManager, sessionManager, textureManager);
        boolean started = webServer.start();
        if (!started) {
            getLogger().warning("Web 服务未能启动，请检查端口配置。");
        }

        // 5. 注册命令
        MapWebCommand command = new MapWebCommand(this);
        if (getCommand("mapweb") != null) {
            getCommand("mapweb").setExecutor(command);
            getCommand("mapweb").setTabCompleter(command);
        }

        // 6. 注册事件监听（用于配置版本提示）
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("MapWeb 已启用。");
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        getLogger().info("MapWeb 已禁用。");
    }

    /**
     * 玩家进服时，若配置文件版本与代码期望不一致，提示 OP。
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }
        if (!pluginConfig.isConfigVersionMatched()) {
            player.sendMessage(ChatColor.RED + "[MapWeb] 检测到旧版配置文件，配置结构可能已变更。");
            player.sendMessage(ChatColor.YELLOW + "[MapWeb] 建议删除 plugins/MapWeb/config.yml 后重启，或手动更新 config-version。");
        }
    }

    /**
     * 重载配置（命令调用）。
     */
    public void reloadPluginConfig() {
        pluginConfig.load();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public TextureManager getTextureManager() {
        return textureManager;
    }
}
