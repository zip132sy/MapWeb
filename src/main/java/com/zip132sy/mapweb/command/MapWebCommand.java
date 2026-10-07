package com.zip132sy.mapweb.command;

import com.zip132sy.mapweb.MapWebPlugin;
import com.zip132sy.mapweb.auth.AccountManager;
import com.zip132sy.mapweb.config.PluginConfig;
import com.zip132sy.mapweb.web.WebServer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /mapweb 命令处理器，含 Tab 补全。
 * 子命令：reload、port、status、admin。
 * admin 子命令仅允许控制台执行：add / remove / list。
 */
public class MapWebCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("reload", "port", "status", "admin", "downloadtextures");
    private static final List<String> ADMIN_SUB_COMMANDS = Arrays.asList("add", "remove", "list");

    private final MapWebPlugin plugin;

    public MapWebCommand(MapWebPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mapweb.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用该命令。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        if ("reload".equals(sub)) {
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "[MapWeb] 配置已重载。");
            return true;
        }

        if ("port".equals(sub)) {
            WebServer server = plugin.getWebServer();
            if (server != null && server.isRunning()) {
                sender.sendMessage(ChatColor.GREEN + "[MapWeb] 当前网页端口：" + server.getActualPort());
            } else {
                sender.sendMessage(ChatColor.RED + "[MapWeb] Web 服务未运行。");
            }
            return true;
        }

        if ("status".equals(sub)) {
            sendStatus(sender);
            return true;
        }

        if ("admin".equals(sub)) {
            handleAdmin(sender, args);
            return true;
        }

        if ("downloadtextures".equals(sub)) {
            handleDownloadTextures(sender, args);
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    /**
     * 手动下载材质包。
     * 用法：/mapweb downloadtextures <url>
     */
    private void handleDownloadTextures(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "用法：/mapweb downloadtextures <材质包URL>");
            return;
        }
        final String url = args[1];
        sender.sendMessage(ChatColor.YELLOW + "[MapWeb] 开始下载材质包：" + url);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    java.util.List<String> urls = new java.util.ArrayList<String>();
                    urls.add(url);
                    plugin.getTextureManager().load(urls);
                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (plugin.getTextureManager().isLoaded()) {
                                plugin.getServer().broadcastMessage(ChatColor.GREEN
                                        + "[MapWeb] 材质包下载完成，共 "
                                        + plugin.getTextureManager().getTextureCount() + " 个贴图。");
                            } else {
                                plugin.getServer().broadcastMessage(ChatColor.RED
                                        + "[MapWeb] 材质包下载失败，请检查 URL 或手动放置 textures.zip。");
                            }
                        }
                    });
                } catch (Throwable t) {
                    plugin.getLogger().warning("手动下载材质包异常：" + t.getMessage());
                }
            }
        });
    }

    /**
     * 处理 admin 子命令。出于安全考虑，仅允许控制台执行。
     */
    private void handleAdmin(CommandSender sender, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "[MapWeb] 管理员账号操作仅允许在服务器控制台执行。");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "用法：/mapweb admin <add|remove|list> [用户名] [密码]");
            return;
        }

        String action = args[1].toLowerCase();
        AccountManager accountManager = plugin.getAccountManager();

        if ("add".equals(action)) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.YELLOW + "用法：/mapweb admin add <用户名> <密码>");
                return;
            }
            String username = args[2];
            String password = args[3];
            if (accountManager.createAccount(username, password)) {
                sender.sendMessage(ChatColor.GREEN + "[MapWeb] 管理员账号 " + username + " 已创建/更新。");
            } else {
                sender.sendMessage(ChatColor.RED + "[MapWeb] 创建失败：用户名不能为空，密码至少 4 位。");
            }
            return;
        }

        if ("remove".equals(action)) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "用法：/mapweb admin remove <用户名>");
                return;
            }
            if (accountManager.removeAccount(args[2])) {
                sender.sendMessage(ChatColor.GREEN + "[MapWeb] 管理员账号 " + args[2] + " 已删除。");
            } else {
                sender.sendMessage(ChatColor.RED + "[MapWeb] 账号不存在：" + args[2]);
            }
            return;
        }

        if ("list".equals(action)) {
            List<String> accounts = accountManager.listAccounts();
            if (accounts.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "[MapWeb] 当前没有任何管理员账号。");
            } else {
                sender.sendMessage(ChatColor.GOLD + "[MapWeb] 管理员账号列表：");
                for (String name : accounts) {
                    sender.sendMessage(ChatColor.WHITE + " - " + name);
                }
            }
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "用法：/mapweb admin <add|remove|list> [用户名] [密码]");
    }

    private void sendStatus(CommandSender sender) {
        WebServer server = plugin.getWebServer();
        PluginConfig config = plugin.getPluginConfig();
        sender.sendMessage(ChatColor.GOLD + "===== MapWeb 状态 =====");
        sender.sendMessage(ChatColor.YELLOW + "Web 服务：" + (server != null && server.isRunning() ? ChatColor.GREEN + "运行中" : ChatColor.RED + "已停止"));
        if (server != null && server.isRunning()) {
            sender.sendMessage(ChatColor.YELLOW + "端口：" + ChatColor.WHITE + server.getActualPort());
        }
        sender.sendMessage(ChatColor.YELLOW + "刷新间隔：" + ChatColor.WHITE + config.getRefreshInterval() + " 秒");
        sender.sendMessage(ChatColor.YELLOW + "显示所有世界：" + ChatColor.WHITE + config.isShowAllWorlds());
        sender.sendMessage(ChatColor.YELLOW + "网页命令行：" + ChatColor.WHITE + (config.isConsoleEnabled() ? "已启用" : "已禁用"));
        sender.sendMessage(ChatColor.YELLOW + "管理员账号数：" + ChatColor.WHITE + plugin.getAccountManager().listAccounts().size());
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "===== MapWeb 帮助 =====");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status " + ChatColor.GRAY + "- 查看运行状态");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " port " + ChatColor.GRAY + "- 查看网页端口");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload " + ChatColor.GRAY + "- 重载配置");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " admin <add|remove|list> " + ChatColor.GRAY + "- 管理网页管理员账号（仅控制台）");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " downloadtextures <url> " + ChatColor.GRAY + "- 手动下载材质包");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();
        if (!sender.hasPermission("mapweb.admin")) {
            return result;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }

        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase();
            for (String sub : ADMIN_SUB_COMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }

        return result;
    }
}
