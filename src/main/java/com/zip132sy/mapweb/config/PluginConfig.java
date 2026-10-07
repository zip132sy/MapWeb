package com.zip132sy.mapweb.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件配置管理。
 * 负责读取 config.yml、校验配置版本，并提供运行时可变配置（如刷新间隔）。
 */
public class PluginConfig {

    /** 当前代码期望的配置版本，配置结构变更时需同步提升此值 */
    public static final int CURRENT_CONFIG_VERSION = 9;

    private final JavaPlugin plugin;

    private int port;
    private boolean autoPort;
    private int refreshInterval;
    private int minRefreshInterval;
    private int maxRefreshInterval;
    private boolean showAllWorlds;
    private String defaultWorld;
    private boolean consoleEnabled;
    private int sessionTimeout;
    private java.util.List<String> commandWhitelist;
    private boolean playerListEnabled;
    private boolean showPlayerWorld;
    private boolean showPlayerCoords;
    private boolean showPlayerHealth;
    private boolean showPlayerFood;
    private boolean excludeFakePlayers;
    private boolean entityDisplayEnabled;
    private boolean showFriendly;
    private boolean showNeutral;
    private boolean showHostile;
    private int mapResolution;
    private boolean showChunkBorder;
    private boolean threeDEnabled;
    private int threeDRadius;
    private int threeDMinY;
    private int threeDMaxY;
    private int threeDMaxVoxels;
    private java.util.List<String> textureDownloadUrls;
    private java.util.Map<String, String> customTextureMap;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * 从 config.yml 加载配置。
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.port = config.getInt("web.port", 8123);
        this.autoPort = config.getBoolean("web.auto-port", true);
        this.refreshInterval = config.getInt("web.refresh-interval", 5);
        this.minRefreshInterval = config.getInt("web.min-refresh-interval", 1);
        this.maxRefreshInterval = config.getInt("web.max-refresh-interval", 300);
        this.showAllWorlds = config.getBoolean("map.show-all-worlds", true);
        this.defaultWorld = config.getString("map.default-world", "world");
        this.mapResolution = config.getInt("map.resolution", 8);
        if (this.mapResolution < 1) {
            this.mapResolution = 1;
        }
        this.showChunkBorder = config.getBoolean("map.show-chunk-border", true);
        this.threeDEnabled = config.getBoolean("three-d.enabled", true);
        this.threeDRadius = config.getInt("three-d.radius", 3);
        if (this.threeDRadius < 1) {
            this.threeDRadius = 1;
        }
        if (this.threeDRadius > 16) {
            this.threeDRadius = 16;
        }
        this.threeDMinY = config.getInt("three-d.min-y", 0);
        this.threeDMaxY = config.getInt("three-d.max-y", 128);
        this.threeDMaxVoxels = config.getInt("three-d.max-voxels", 120000);
        this.textureDownloadUrls = config.getStringList("three-d.texture-download-urls");
        if (this.textureDownloadUrls == null || this.textureDownloadUrls.isEmpty()) {
            this.textureDownloadUrls = new java.util.ArrayList<String>();
            this.textureDownloadUrls.add("https://github.com/InventivetalentDev/minecraft-assets/archive/refs/heads/1.12.2.zip");
        }
        // 自定义方块材质映射（供模组方块使用）
        this.customTextureMap = new java.util.HashMap<String, String>();
        org.bukkit.configuration.ConfigurationSection customSection =
                config.getConfigurationSection("custom-texture-map");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                String value = customSection.getString(key);
                if (value != null && !value.trim().isEmpty()) {
                    this.customTextureMap.put(key, value.trim());
                }
            }
        }
        this.consoleEnabled = config.getBoolean("web-console.enabled", true);
        this.sessionTimeout = config.getInt("web-console.session-timeout", 30);
        this.commandWhitelist = config.getStringList("web-console.command-whitelist");
        if (this.commandWhitelist == null || this.commandWhitelist.isEmpty()) {
            this.commandWhitelist = new java.util.ArrayList<String>();
            this.commandWhitelist.add("say");
            this.commandWhitelist.add("list");
            this.commandWhitelist.add("tps");
            this.commandWhitelist.add("time");
            this.commandWhitelist.add("weather");
        }
        this.playerListEnabled = config.getBoolean("player-list.enabled", true);
        this.showPlayerWorld = config.getBoolean("player-list.show-world", true);
        this.showPlayerCoords = config.getBoolean("player-list.show-coords", true);
        this.showPlayerHealth = config.getBoolean("player-list.show-health", true);
        this.showPlayerFood = config.getBoolean("player-list.show-food", true);
        this.excludeFakePlayers = config.getBoolean("player-list.exclude-fake-players", true);
        this.entityDisplayEnabled = config.getBoolean("entity-display.enabled", false);
        this.showFriendly = config.getBoolean("entity-display.show-friendly", false);
        this.showNeutral = config.getBoolean("entity-display.show-neutral", false);
        this.showHostile = config.getBoolean("entity-display.show-hostile", false);

        // 修正刷新间隔到合法范围
        this.refreshInterval = clampRefresh(this.refreshInterval);
    }

    /**
     * 校验配置文件版本。若与代码期望版本不一致，返回 false。
     */
    public boolean isConfigVersionMatched() {
        int fileVersion = plugin.getConfig().getInt("config-version", -1);
        return fileVersion == CURRENT_CONFIG_VERSION;
    }

    /**
     * 把刷新间隔限制在合法范围内。
     */
    public int clampRefresh(int value) {
        if (value < minRefreshInterval) {
            return minRefreshInterval;
        }
        if (value > maxRefreshInterval) {
            return maxRefreshInterval;
        }
        return value;
    }

    public int getPort() {
        return port;
    }

    public boolean isAutoPort() {
        return autoPort;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * 运行时修改刷新间隔（网页提交时调用），并持久化到 config.yml。
     */
    public void setRefreshInterval(int seconds) {
        this.refreshInterval = clampRefresh(seconds);
        plugin.getConfig().set("web.refresh-interval", this.refreshInterval);
        plugin.saveConfig();
    }

    public int getMinRefreshInterval() {
        return minRefreshInterval;
    }

    public int getMaxRefreshInterval() {
        return maxRefreshInterval;
    }

    public boolean isShowAllWorlds() {
        return showAllWorlds;
    }

    public String getDefaultWorld() {
        return defaultWorld;
    }

    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public java.util.List<String> getCommandWhitelist() {
        return commandWhitelist;
    }

    public boolean isPlayerListEnabled() {
        return playerListEnabled;
    }

    public boolean isShowPlayerWorld() {
        return showPlayerWorld;
    }

    public boolean isShowPlayerCoords() {
        return showPlayerCoords;
    }

    public boolean isShowPlayerHealth() {
        return showPlayerHealth;
    }

    public boolean isShowPlayerFood() {
        return showPlayerFood;
    }

    public boolean isExcludeFakePlayers() {
        return excludeFakePlayers;
    }

    public boolean isEntityDisplayEnabled() {
        return entityDisplayEnabled;
    }

    public boolean isShowFriendly() {
        return showFriendly;
    }

    public boolean isShowNeutral() {
        return showNeutral;
    }

    public boolean isShowHostile() {
        return showHostile;
    }

    public int getMapResolution() {
        return mapResolution;
    }

    public boolean isShowChunkBorder() {
        return showChunkBorder;
    }

    public boolean isThreeDEnabled() {
        return threeDEnabled;
    }

    public int getThreeDRadius() {
        return threeDRadius;
    }

    public int getThreeDMinY() {
        return threeDMinY;
    }

    public int getThreeDMaxY() {
        return threeDMaxY;
    }

    public int getThreeDMaxVoxels() {
        return threeDMaxVoxels;
    }

    public java.util.List<String> getTextureDownloadUrls() {
        return textureDownloadUrls;
    }

    public java.util.Map<String, String> getCustomTextureMap() {
        return customTextureMap;
    }

    /**
     * 判断某条命令是否在白名单内。
     * 命令可能带前导斜杠，这里统一去掉后比较命令名。
     */
    public boolean isCommandAllowed(String command) {
        if (command == null) {
            return false;
        }
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        String name = space > 0 ? trimmed.substring(0, space) : trimmed;
        for (String allowed : commandWhitelist) {
            if (allowed.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
