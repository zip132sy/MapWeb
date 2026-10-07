package com.zip132sy.mapweb.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zip132sy.mapweb.auth.AccountManager;
import com.zip132sy.mapweb.auth.SessionManager;
import com.zip132sy.mapweb.config.PluginConfig;
import com.zip132sy.mapweb.render.EntityClassifier;
import com.zip132sy.mapweb.render.MapRenderer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * HTTP 请求处理器。
 * 负责路由分发：网页、地图图片、配置接口、世界列表接口。
 */
public class WebHandler implements HttpHandler {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final PluginConfig config;
    private final String indexHtml;
    private final AccountManager accountManager;
    private final SessionManager sessionManager;
    private final org.bukkit.plugin.Plugin plugin;
    private final com.zip132sy.mapweb.texture.TextureManager textureManager;

    public WebHandler(PluginConfig config, String indexHtml,
                      AccountManager accountManager, SessionManager sessionManager,
                      org.bukkit.plugin.Plugin plugin,
                      com.zip132sy.mapweb.texture.TextureManager textureManager) {
        this.config = config;
        this.indexHtml = indexHtml;
        this.accountManager = accountManager;
        this.sessionManager = sessionManager;
        this.plugin = plugin;
        this.textureManager = textureManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            if ("/".equals(path) || "/index.html".equals(path)) {
                handleIndex(exchange);
            } else if ("/map.png".equals(path)) {
                handleMap(exchange);
            } else if ("/api/worlds".equals(path)) {
                handleWorlds(exchange);
            } else if ("/api/config".equals(path)) {
                handleConfig(exchange);
            } else if ("/api/login".equals(path)) {
                handleLogin(exchange);
            } else if ("/api/logout".equals(path)) {
                handleLogout(exchange);
            } else if ("/api/console".equals(path)) {
                handleConsole(exchange);
            } else if ("/api/players".equals(path)) {
                handlePlayers(exchange);
            } else if ("/api/entities".equals(path)) {
                handleEntities(exchange);
            } else if ("/api/inventory".equals(path)) {
                handleInventory(exchange);
            } else if ("/api/voxels".equals(path)) {
                handleVoxels(exchange);
            } else if ("/api/topdown".equals(path)) {
                handleTopDown(exchange);
            } else if ("/api/textures/manifest".equals(path)) {
                handleTextureManifest(exchange);
            } else if (path.startsWith("/api/textures/")) {
                handleTexture(exchange, path);
            } else {
                sendText(exchange, 404, "404 Not Found");
            }
        } catch (Exception e) {
            try {
                sendText(exchange, 500, "500 Internal Error: " + e.getMessage());
            } catch (IOException ignored) {
                // 忽略二次异常
            }
        } finally {
            exchange.close();
        }
    }

    /**
     * 返回地图网页。
     */
    private void handleIndex(HttpExchange exchange) throws IOException {
        byte[] body = indexHtml.getBytes(UTF8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.flush();
    }

    /**
     * 返回指定世界的地图 PNG。
     * 参数：world=世界名，mode=top(默认俯视) / iso(等距)
     */
    private void handleMap(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String worldName = query.get("world");
        if (worldName == null || worldName.isEmpty()) {
            worldName = config.getDefaultWorld();
        }
        String mode = query.get("mode");
        boolean isometric = "iso".equalsIgnoreCase(mode) || "3d".equalsIgnoreCase(mode);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sendText(exchange, 404, "World not found: " + worldName);
            return;
        }

        MapRenderer.RenderResult result = isometric
                ? MapRenderer.renderWorldIso(plugin, world, config.getMapResolution(), config.isShowChunkBorder())
                : MapRenderer.renderWorld(plugin, world, config.getMapResolution(), config.isShowChunkBorder());
        if (result == null || result.png == null) {
            sendText(exchange, 204, "");
            return;
        }

        byte[] png = result.png;
        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, png.length);
        OutputStream os = exchange.getResponseBody();
        os.write(png);
        os.flush();
    }

    /**
     * 返回世界列表 JSON。
     */
    private void handleWorlds(HttpExchange exchange) throws IOException {
        List<World> worlds = Bukkit.getWorlds();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"worlds\":[");
        boolean first = true;
        for (World world : worlds) {
            if (!config.isShowAllWorlds() && !world.getName().equals(config.getDefaultWorld())) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(world.getName())).append("\"");
        }
        sb.append("],\"default\":\"").append(escapeJson(config.getDefaultWorld())).append("\"}");
        sendJson(exchange, sb.toString());
    }

    /**
     * 返回在线玩家列表 JSON。
     * 包含玩家名、UUID，以及按配置决定是否包含的世界、坐标、血量。
     */
    private void handlePlayers(HttpExchange exchange) throws IOException {
        if (!config.isPlayerListEnabled()) {
            sendJson(exchange, "{\"enabled\":false,\"count\":0,\"players\":[]}");
            return;
        }

        // 玩家数据读取必须在主线程完成
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                return buildPlayersJson();
            }
        });

        String json;
        try {
            if (Bukkit.isPrimaryThread()) {
                json = buildPlayersJson();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
                json = task.get();
            }
        } catch (Exception e) {
            json = "{\"enabled\":true,\"count\":0,\"players\":[]}";
        }

        sendJson(exchange, json);
    }

    /**
     * 构建玩家列表 JSON（必须在主线程执行）。
     */
    private String buildPlayersJson() {
        // 过滤假玩家（NPC 通常没有网络连接）
        List<Player> players = new java.util.ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (config.isExcludeFakePlayers() && player.getAddress() == null) {
                continue;
            }
            players.add(player);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"enabled\":true,\"count\":").append(players.size()).append(",\"players\":[");

        boolean first = true;
        for (Player player : players) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("{");
            sb.append("\"name\":\"").append(escapeJson(player.getName())).append("\"");
            sb.append(",\"uuid\":\"").append(player.getUniqueId().toString()).append("\"");

            if (config.isShowPlayerWorld()) {
                sb.append(",\"world\":\"").append(escapeJson(player.getWorld().getName())).append("\"");
            }
            if (config.isShowPlayerCoords()) {
                sb.append(",\"x\":").append(player.getLocation().getBlockX());
                sb.append(",\"y\":").append(player.getLocation().getBlockY());
                sb.append(",\"z\":").append(player.getLocation().getBlockZ());
            }
            if (config.isShowPlayerHealth()) {
                sb.append(",\"health\":").append(Math.round(player.getHealth()));
                sb.append(",\"maxHealth\":").append(Math.round(player.getMaxHealth()));
            }
            if (config.isShowPlayerFood()) {
                sb.append(",\"food\":").append(player.getFoodLevel());
                sb.append(",\"maxFood\":20");
            }

            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 返回指定世界的生物位置 JSON。
     * 参数：world=世界名
     * 只返回已加载区块内的生物，并按配置过滤分类。
     */
    private void handleEntities(HttpExchange exchange) throws IOException {
        if (!config.isEntityDisplayEnabled()) {
            sendJson(exchange, "{\"enabled\":false,\"entities\":[]}");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String worldName = query.get("world");
        if (worldName == null || worldName.isEmpty()) {
            worldName = config.getDefaultWorld();
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sendJson(exchange, "{\"enabled\":true,\"entities\":[]}");
            return;
        }

        // 世界数据读取必须在主线程完成
        final World targetWorld = world;
        final boolean showFriendly = config.isShowFriendly();
        final boolean showNeutral = config.isShowNeutral();
        final boolean showHostile = config.isShowHostile();

        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                return buildEntitiesJson(targetWorld, showFriendly, showNeutral, showHostile);
            }
        });

        String json;
        try {
            if (Bukkit.isPrimaryThread()) {
                json = buildEntitiesJson(targetWorld, showFriendly, showNeutral, showHostile);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
                json = task.get();
            }
        } catch (Exception e) {
            json = "{\"enabled\":true,\"entities\":[]}";
        }

        sendJson(exchange, json);
    }

    /**
     * 构建生物列表 JSON（必须在主线程执行）。
     */
    private String buildEntitiesJson(World world, boolean showFriendly,
                                     boolean showNeutral, boolean showHostile) {
        // 计算地图边界，与 MapRenderer 保持一致，供前端换算像素位置
        org.bukkit.Chunk[] chunks = world.getLoadedChunks();
        if (chunks == null || chunks.length == 0) {
            return "{\"enabled\":true,\"entities\":[]}";
        }
        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (org.bukkit.Chunk chunk : chunks) {
            if (chunk.getX() < minChunkX) {
                minChunkX = chunk.getX();
            }
            if (chunk.getX() > maxChunkX) {
                maxChunkX = chunk.getX();
            }
            if (chunk.getZ() < minChunkZ) {
                minChunkZ = chunk.getZ();
            }
            if (chunk.getZ() > maxChunkZ) {
                maxChunkZ = chunk.getZ();
            }
        }
        int mapWidth = maxChunkX - minChunkX + 1;
        int mapHeight = maxChunkZ - minChunkZ + 1;

        StringBuilder sb = new StringBuilder();
        sb.append("{\"enabled\":true");
        sb.append(",\"minChunkX\":").append(minChunkX);
        sb.append(",\"minChunkZ\":").append(minChunkZ);
        sb.append(",\"mapWidth\":").append(mapWidth);
        sb.append(",\"mapHeight\":").append(mapHeight);
        sb.append(",\"entities\":[");

        boolean first = true;
        // 遍历世界实体，只保留已加载区块内的活体生物
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            // 玩家单独由玩家列表处理，这里跳过
            if (entity instanceof Player) {
                continue;
            }

            int blockX = entity.getLocation().getBlockX();
            int blockZ = entity.getLocation().getBlockZ();

            // 只处理已加载区块内的生物
            if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                continue;
            }

            EntityClassifier.Category category = EntityClassifier.classify(entity);
            if (!EntityClassifier.isAllowed(category, showFriendly, showNeutral, showHostile)) {
                continue;
            }

            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("{");
            sb.append("\"type\":\"").append(escapeJson(entity.getType().name())).append("\"");
            sb.append(",\"category\":\"").append(category.name().toLowerCase()).append("\"");
            sb.append(",\"x\":").append(blockX);
            sb.append(",\"y\":").append(entity.getLocation().getBlockY());
            sb.append(",\"z\":").append(blockZ);
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 查看玩家背包接口（仅登录管理员可访问）。
     * POST 参数：token、player（玩家名）
     * 只读，不提供任何修改能力。
     */
    private void handleInventory(HttpExchange exchange) throws IOException {
        if (!config.isConsoleEnabled()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"该功能未启用\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, "{\"success\":false,\"message\":\"请使用 POST 请求\"}");
            return;
        }

        Map<String, String> params = parseQuery(readBody(exchange));
        String token = params.get("token");
        String playerName = params.get("player");

        String username = sessionManager.validate(token);
        if (username == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"未登录或会话已过期\"}");
            return;
        }
        if (playerName == null || playerName.trim().isEmpty()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"缺少玩家名\"}");
            return;
        }

        final String targetName = playerName.trim();
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                return buildInventoryJson(targetName);
            }
        });

        String json;
        try {
            if (Bukkit.isPrimaryThread()) {
                json = buildInventoryJson(targetName);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
                json = task.get();
            }
        } catch (Exception e) {
            json = "{\"success\":false,\"message\":\"读取背包失败\"}";
        }

        sendJson(exchange, json);
    }

    /**
     * 构建玩家背包 JSON（必须在主线程执行）。
     * 支持真实玩家与假玩家（NPC）。
     */
    private String buildInventoryJson(String playerName) {
        Player target = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(playerName)) {
                target = online;
                break;
            }
        }
        if (target == null) {
            return "{\"success\":false,\"message\":\"玩家不在线\"}";
        }

        org.bukkit.inventory.PlayerInventory inv = target.getInventory();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true");
        sb.append(",\"player\":\"").append(escapeJson(target.getName())).append("\"");
        sb.append(",\"items\":[");

        boolean first = true;
        org.bukkit.inventory.ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{");
            sb.append("\"slot\":").append(i);
            sb.append(",\"type\":\"").append(escapeJson(item.getType().name())).append("\"");
            sb.append(",\"amount\":").append(item.getAmount());
            sb.append(",\"durability\":").append(item.getDurability());
            // 材质名：类型名转小写，前端优先匹配 items/，再匹配 blocks/
            String texName = item.getType().name().toLowerCase();
            sb.append(",\"tex\":\"").append(escapeJson(texName)).append("\"");
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 3D 体素数据接口。
     * 参数：world=世界名，cx=中心区块X，cz=中心区块Z，radius=半径（区块数）
     * 返回表面方块列表 + 实体位置，供前端 Three.js 渲染。
     */
    private void handleVoxels(HttpExchange exchange) throws IOException {
        if (!config.isThreeDEnabled()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"3D 功能未启用\"}");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String worldName = query.get("world");
        if (worldName == null || worldName.isEmpty()) {
            worldName = config.getDefaultWorld();
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"世界不存在\"}");
            return;
        }

        int centerChunkX;
        int centerChunkZ;
        try {
            centerChunkX = Integer.parseInt(query.get("cx"));
            centerChunkZ = Integer.parseInt(query.get("cz"));
        } catch (Exception e) {
            sendJson(exchange, "{\"success\":false,\"message\":\"缺少有效的中心点坐标\"}");
            return;
        }

        int radius = config.getThreeDRadius();
        try {
            String r = query.get("radius");
            if (r != null && !r.isEmpty()) {
                radius = Integer.parseInt(r);
            }
        } catch (NumberFormatException ignored) {
            // 使用配置默认值
        }
        if (radius < 1) {
            radius = 1;
        }
        if (radius > 16) {
            radius = 16;
        }

        final World targetWorld = world;
        final int fCenterX = centerChunkX;
        final int fCenterZ = centerChunkZ;
        final int fRadius = radius;
        final int minY = config.getThreeDMinY();
        final int maxY = config.getThreeDMaxY();
        final int maxVoxels = config.getThreeDMaxVoxels();

        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                return buildVoxelsJson(targetWorld, fCenterX, fCenterZ, fRadius, minY, maxY, maxVoxels);
            }
        });

        String json;
        try {
            if (Bukkit.isPrimaryThread()) {
                json = buildVoxelsJson(targetWorld, fCenterX, fCenterZ, fRadius, minY, maxY, maxVoxels);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
                json = task.get();
            }
        } catch (Exception e) {
            json = "{\"success\":false,\"message\":\"提取体素失败\"}";
        }

        sendJson(exchange, json);
    }

    /**
     * 构建体素 JSON（必须在主线程生成快照，提取在快照上进行）。
     */
    private String buildVoxelsJson(World world, int centerChunkX, int centerChunkZ,
                                   int radius, int minY, int maxY, int maxVoxels) {
        // 主线程生成快照
        java.util.List<org.bukkit.ChunkSnapshot> snapshots =
                new java.util.ArrayList<org.bukkit.ChunkSnapshot>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                try {
                    snapshots.add(world.getChunkAt(cx, cz).getChunkSnapshot());
                } catch (Exception ignored) {
                    // 单个区块快照失败忽略
                }
            }
        }

        if (snapshots.isEmpty()) {
            return "{\"success\":false,\"message\":\"中心点周围没有已加载的区块\"}";
        }

        // 提取表面方块（在快照上进行，不占用主线程）
        java.util.List<com.zip132sy.mapweb.render.VoxelExtractor.Voxel> voxels =
                com.zip132sy.mapweb.render.VoxelExtractor.extract(snapshots, minY, maxY, maxVoxels);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true");
        sb.append(",\"centerX\":").append(centerChunkX << 4);
        sb.append(",\"centerZ\":").append(centerChunkZ << 4);
        sb.append(",\"voxels\":[");

        boolean first = true;
        for (com.zip132sy.mapweb.render.VoxelExtractor.Voxel voxel : voxels) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{\"x\":").append(voxel.x);
            sb.append(",\"y\":").append(voxel.y);
            sb.append(",\"z\":").append(voxel.z);
            sb.append(",\"c\":").append(voxel.color);
            if (voxel.topTexture != null) {
                sb.append(",\"t\":\"").append(escapeJson(voxel.topTexture)).append("\"");
            }
            if (voxel.sideTexture != null) {
                sb.append(",\"s\":\"").append(escapeJson(voxel.sideTexture)).append("\"");
            }
            if (voxel.bottomTexture != null) {
                sb.append(",\"b\":\"").append(escapeJson(voxel.bottomTexture)).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");

        // 实体位置（玩家 + 生物），带高度
        sb.append(",\"entities\":[");
        boolean firstEntity = true;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (!(entity instanceof org.bukkit.entity.LivingEntity)) {
                continue;
            }
            org.bukkit.Location loc = entity.getLocation();
            int ex = loc.getBlockX();
            int ez = loc.getBlockZ();
            // 只包含范围内的实体
            if (Math.abs((ex >> 4) - centerChunkX) > radius
                    || Math.abs((ez >> 4) - centerChunkZ) > radius) {
                continue;
            }
            if (!firstEntity) {
                sb.append(",");
            }
            firstEntity = false;

            String category;
            String label;
            if (entity instanceof Player) {
                category = "player";
                label = entity.getName();
            } else {
                com.zip132sy.mapweb.render.EntityClassifier.Category cat =
                        com.zip132sy.mapweb.render.EntityClassifier.classify(entity);
                category = cat.name().toLowerCase();
                label = entity.getType().name();
            }

            sb.append("{");
            sb.append("\"x\":").append(ex);
            sb.append(",\"y\":").append(loc.getBlockY());
            sb.append(",\"z\":").append(ez);
            sb.append(",\"category\":\"").append(category).append("\"");
            sb.append(",\"label\":\"").append(escapeJson(label)).append("\"");
            sb.append("}");
        }
        sb.append("]}");

        return sb.toString();
    }

    /**
     * 俯视方块数据接口（供前端 WebGL 渲染 2D 地图）。
     * 参数：world=世界名
     * 返回每个区块中心列最高方块的材质名、颜色、高度。
     */
    private void handleTopDown(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String worldName = query.get("world");
        if (worldName == null || worldName.isEmpty()) {
            worldName = config.getDefaultWorld();
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"世界不存在\"}");
            return;
        }

        final World targetWorld = world;
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                return buildTopDownJson(targetWorld);
            }
        });

        String json;
        try {
            if (Bukkit.isPrimaryThread()) {
                json = buildTopDownJson(targetWorld);
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
                json = task.get();
            }
        } catch (Exception e) {
            json = "{\"success\":false,\"message\":\"提取俯视数据失败\"}";
        }

        sendJson(exchange, json);
    }

    /**
     * 构建俯视方块 JSON（必须在主线程生成快照）。
     */
    private String buildTopDownJson(World world) {
        org.bukkit.Chunk[] chunks = world.getLoadedChunks();
        if (chunks == null || chunks.length == 0) {
            return "{\"success\":false,\"message\":\"没有已加载的区块\"}";
        }

        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (org.bukkit.Chunk chunk : chunks) {
            if (chunk.getX() < minChunkX) {
                minChunkX = chunk.getX();
            }
            if (chunk.getX() > maxChunkX) {
                maxChunkX = chunk.getX();
            }
            if (chunk.getZ() < minChunkZ) {
                minChunkZ = chunk.getZ();
            }
            if (chunk.getZ() > maxChunkZ) {
                maxChunkZ = chunk.getZ();
            }
        }
        int mapWidth = maxChunkX - minChunkX + 1;
        int mapHeight = maxChunkZ - minChunkZ + 1;

        StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":true");
        sb.append(",\"minChunkX\":").append(minChunkX);
        sb.append(",\"minChunkZ\":").append(minChunkZ);
        sb.append(",\"mapWidth\":").append(mapWidth);
        sb.append(",\"mapHeight\":").append(mapHeight);
        sb.append(",\"tiles\":[");

        boolean first = true;
        for (org.bukkit.Chunk chunk : chunks) {
            int cx = chunk.getX();
            int cz = chunk.getZ();
            int centerX = (cx << 4) + 8;
            int centerZ = (cz << 4) + 8;

            int highestY = world.getHighestBlockYAt(centerX, centerZ);
            org.bukkit.block.Block block = world.getBlockAt(centerX, highestY, centerZ);
            int typeId = block.getTypeId();
            int data = block.getData();
            int color = com.zip132sy.mapweb.render.BlockColor.getColorById(typeId, data);

            String topTex = null;
            com.zip132sy.mapweb.texture.BlockTextureMap.Faces faces =
                    com.zip132sy.mapweb.texture.BlockTextureMap.getFacesById(typeId, data);
            if (faces != null) {
                topTex = faces.top;
            }

            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("{\"x\":").append(cx - minChunkX);
            sb.append(",\"z\":").append(cz - minChunkZ);
            sb.append(",\"y\":").append(highestY);
            sb.append(",\"c\":").append(color);
            if (topTex != null) {
                sb.append(",\"t\":\"").append(escapeJson(topTex)).append("\"");
            }
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 材质清单接口。
     * 返回服务端材质包中所有可用的贴图名，供前端判断哪些方块有贴图。
     */
    private void handleTextureManifest(HttpExchange exchange) throws IOException {
        if (textureManager == null || !textureManager.isLoaded()) {
            sendJson(exchange, "{\"loaded\":false,\"textures\":[]}");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"loaded\":true,\"count\":").append(textureManager.getTextureCount());
        sb.append(",\"textures\":[");
        boolean first = true;
        for (String name : textureManager.getTextureNames()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(name)).append("\"");
        }
        sb.append("]}");
        sendJson(exchange, sb.toString());
    }

    /**
     * 单个贴图接口。
     * 路径：/api/textures/<贴图名>
     */
    private void handleTexture(HttpExchange exchange, String path) throws IOException {
        String name = path.substring("/api/textures/".length());
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        byte[] png = textureManager == null ? null : textureManager.getTexture(name);
        if (png == null) {
            sendText(exchange, 404, "Texture not found: " + name);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        exchange.sendResponseHeaders(200, png.length);
        OutputStream os = exchange.getResponseBody();
        os.write(png);
        os.flush();
    }

    /**
     * 配置接口。
     * GET  → 返回当前刷新间隔等配置
     * POST → 修改刷新间隔（参数 interval=秒）
     */
    private void handleConfig(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, String> params = parseQuery(body);
            String intervalStr = params.get("interval");
            if (intervalStr != null) {
                try {
                    int interval = Integer.parseInt(intervalStr.trim());
                    config.setRefreshInterval(interval);
                } catch (NumberFormatException ignored) {
                    // 非法数字忽略
                }
            }
        }

        String json = "{\"refreshInterval\":" + config.getRefreshInterval()
                + ",\"min\":" + config.getMinRefreshInterval()
                + ",\"max\":" + config.getMaxRefreshInterval() + "}";
        sendJson(exchange, json);
    }

    /**
     * 登录接口。
     * POST 参数：username、password
     * 成功返回 token，失败返回错误信息。
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!config.isConsoleEnabled()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"网页命令行功能未启用\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, "{\"success\":false,\"message\":\"请使用 POST 请求\"}");
            return;
        }

        Map<String, String> params = parseQuery(readBody(exchange));
        String username = params.get("username");
        String password = params.get("password");

        if (accountManager.verify(username, password)) {
            String token = sessionManager.createSession(username);
            sendJson(exchange, "{\"success\":true,\"token\":\"" + token + "\"}");
        } else {
            sendJson(exchange, "{\"success\":false,\"message\":\"用户名或密码错误\"}");
        }
    }

    /**
     * 登出接口。
     * POST 参数：token
     */
    private void handleLogout(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(readBody(exchange));
        sessionManager.invalidate(params.get("token"));
        sendJson(exchange, "{\"success\":true}");
    }

    /**
     * 命令执行接口。
     * POST 参数：token、command
     * 需登录且命令在白名单内。
     */
    private void handleConsole(HttpExchange exchange) throws IOException {
        if (!config.isConsoleEnabled()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"网页命令行功能未启用\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, "{\"success\":false,\"message\":\"请使用 POST 请求\"}");
            return;
        }

        Map<String, String> params = parseQuery(readBody(exchange));
        String token = params.get("token");
        String command = params.get("command");

        String username = sessionManager.validate(token);
        if (username == null) {
            sendJson(exchange, "{\"success\":false,\"message\":\"未登录或会话已过期\"}");
            return;
        }

        if (command == null || command.trim().isEmpty()) {
            sendJson(exchange, "{\"success\":false,\"message\":\"命令不能为空\"}");
            return;
        }

        if (!config.isCommandAllowed(command)) {
            sendJson(exchange, "{\"success\":false,\"message\":\"该命令不在白名单内，禁止执行\"}");
            return;
        }

        String finalCommand = command.trim();
        if (finalCommand.startsWith("/")) {
            finalCommand = finalCommand.substring(1);
        }

        // 命令必须在主线程执行
        final String execCommand = finalCommand;
        final String execUser = username;
        CommandResult result = dispatchOnMainThread(execCommand, execUser);
        if (result.success) {
            sendJson(exchange, "{\"success\":true,\"message\":\"命令已执行\",\"output\":\""
                    + escapeJson(result.output) + "\"}");
        } else {
            sendJson(exchange, "{\"success\":false,\"message\":\"命令执行失败\",\"output\":\""
                    + escapeJson(result.output) + "\"}");
        }
    }

    /**
     * 命令执行结果。
     */
    private static class CommandResult {
        boolean success;
        String output;

        CommandResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }

    /**
     * 在主线程执行命令，并捕获命令产生的控制台输出。
     * 使用 FutureTask 投递到主线程调度器，并等待结果。
     */
    private CommandResult dispatchOnMainThread(final String command, final String username) {
        final com.zip132sy.mapweb.util.ConsoleCapture capture =
                new com.zip132sy.mapweb.util.ConsoleCapture();
        final boolean captureStarted = capture.start();

        try {
            FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    ConsoleCommandSender console = Bukkit.getConsoleSender();
                    Bukkit.getLogger().info("[MapWeb] 网页管理员 " + username + " 执行命令: /" + command);
                    return Bukkit.dispatchCommand(console, command);
                }
            });
            Bukkit.getScheduler().runTask(plugin, task);
            boolean dispatched = task.get();

            // 等待日志刷入（命令输出可能异步写入）
            Thread.sleep(80L);

            String output = captureStarted ? capture.getText() : "";
            return new CommandResult(dispatched, output);
        } catch (Exception e) {
            return new CommandResult(false, captureStarted ? capture.getText() : "");
        } finally {
            if (captureStarted) {
                capture.stop();
            }
        }
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(UTF8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.flush();
    }

    private void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] body = text.getBytes(UTF8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(code, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.flush();
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return new String(out.toByteArray(), UTF8);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<String, String>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(key, value);
                } else if (idx < 0) {
                    map.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (Exception ignored) {
                // 忽略解析失败的参数
            }
        }
        return map;
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
