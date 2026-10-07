package com.zip132sy.mapweb.texture;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 材质包管理。
 * 从材质包 zip 中提取方块贴图，供 3D 视图使用。
 *
 * 材质来源优先级：
 * 1. 服主放置的 plugins/MapWeb/textures.zip
 * 2. 从配置的 URL 自动下载（下载后保存为 textures.zip）
 * 3. 插件 jar 内置的 textures.zip（兜底）
 *
 * 加载路径中包含 textures/blocks/ 或 textures/items/ 的 PNG，避免加载无关文件。
 * 不硬编码完整前缀，以兼容不同来源的材质包目录结构。
 */
public class TextureManager {

    /** 方块贴图路径特征（兼容 assets/minecraft/textures/blocks/ 与 textures/blocks/ 等结构） */
    private static final String BLOCK_MARKER = "textures/blocks/";
    /** 物品贴图路径特征 */
    private static final String ITEM_MARKER = "textures/items/";
    /** 单个贴图大小上限，防止恶意超大文件 */
    private static final int MAX_TEXTURE_SIZE = 512 * 1024;
    /** 贴图总数量上限 */
    private static final int MAX_TEXTURES = 2000;
    /** 材质包下载大小上限（100MB） */
    private static final long MAX_DOWNLOAD_SIZE = 100L * 1024 * 1024;
    /** 下载超时（毫秒） */
    private static final int DOWNLOAD_TIMEOUT = 30000;

    private final JavaPlugin plugin;
    /** 贴图名（不含 .png）→ PNG 字节 */
    private final Map<String, byte[]> textures = new HashMap<String, byte[]>();
    private boolean loaded = false;

    public TextureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载材质包。
     *
     * @param downloadUrls 材质包下载地址列表，依次尝试；为空则不下载
     */
    public void load(java.util.List<String> downloadUrls) {
        textures.clear();
        loaded = false;

        File external = new File(plugin.getDataFolder(), "textures.zip");

        // 1. 优先加载服主放置的材质包
        if (external.exists() && external.isFile()) {
            if (loadFromFile(external)) {
                plugin.getLogger().info("已加载外部材质包，共 " + textures.size() + " 个方块贴图。");
                return;
            }
        }

        // 2. 依次尝试从网络下载（多个源，第一个失败就试下一个）
        if (downloadUrls != null && !downloadUrls.isEmpty()) {
            plugin.getLogger().info("未找到材质包，开始尝试从网络下载（共 " + downloadUrls.size() + " 个源）。");
            for (int i = 0; i < downloadUrls.size(); i++) {
                String url = downloadUrls.get(i);
                if (url == null || url.trim().isEmpty()) {
                    continue;
                }
                plugin.getLogger().info("尝试第 " + (i + 1) + " 个源：" + url);
                if (download(url, external)) {
                    if (loadFromFile(external)) {
                        plugin.getLogger().info("材质包下载并加载成功，共 " + textures.size() + " 个方块贴图。");
                        return;
                    }
                }
                plugin.getLogger().warning("第 " + (i + 1) + " 个源失败，尝试下一个。");
            }
            plugin.getLogger().warning("所有材质源均下载失败，将使用纯色渲染。");
        }

        // 3. 兜底：从 jar 内置资源加载
        InputStream is = plugin.getResource("textures.zip");
        if (is != null) {
            try {
                loadFromStream(is);
                is.close();
                loaded = true;
                plugin.getLogger().info("已加载内置材质包，共 " + textures.size() + " 个方块贴图。");
            } catch (Exception e) {
                plugin.getLogger().warning("内置材质包加载失败：" + e.getMessage());
            }
        } else {
            plugin.getLogger().info("未找到材质包，3D 视图将使用纯色渲染。");
        }
    }

    /**
     * 从网络下载材质包到指定文件。
     */
    private boolean download(String urlStr, File target) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(DOWNLOAD_TIMEOUT);
            conn.setReadTimeout(DOWNLOAD_TIMEOUT);
            conn.setRequestProperty("User-Agent", "MapWeb/1.0");
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                plugin.getLogger().warning("下载材质包失败，HTTP 状态码：" + code);
                return false;
            }

            long contentLength = conn.getContentLengthLong();
            if (contentLength > MAX_DOWNLOAD_SIZE) {
                plugin.getLogger().warning("材质包过大，拒绝下载：" + contentLength + " 字节");
                return false;
            }

            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            in = conn.getInputStream();
            out = new FileOutputStream(target);
            byte[] buffer = new byte[8192];
            int len;
            long total = 0;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                if (total > MAX_DOWNLOAD_SIZE) {
                    plugin.getLogger().warning("材质包超过大小上限，中止下载。");
                    out.close();
                    target.delete();
                    return false;
                }
                out.write(buffer, 0, len);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("下载材质包异常：" + e.getMessage());
            if (target.exists()) {
                target.delete();
            }
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
        }
    }

    private boolean loadFromFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            loadFromStream(fis);
            loaded = true;
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("材质包解析失败：" + e.getMessage());
            return false;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 从 zip 输入流提取方块贴图。
     */
    private void loadFromStream(InputStream input) throws IOException {
        ZipInputStream zis = new ZipInputStream(input);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (entry.isDirectory()) {
                continue;
            }
            if (!name.endsWith(".png")) {
                continue;
            }
            // 路径中必须包含 textures/blocks/ 或 textures/items/
            int markerIndex = name.indexOf(BLOCK_MARKER);
            int markerLen = BLOCK_MARKER.length();
            if (markerIndex < 0) {
                markerIndex = name.indexOf(ITEM_MARKER);
                markerLen = ITEM_MARKER.length();
            }
            if (markerIndex < 0) {
                continue;
            }
            if (textures.size() >= MAX_TEXTURES) {
                break;
            }

            // 贴图名 = 标记之后、.png 之前的部分
            String textureName = name.substring(markerIndex + markerLen, name.length() - 4);
            // 跳过子目录中的贴图，只保留直接位于 blocks/ 或 items/ 下的
            if (textureName.indexOf('/') >= 0) {
                continue;
            }
            byte[] data = readEntry(zis);
            if (data != null && data.length > 0 && data.length <= MAX_TEXTURE_SIZE) {
                textures.put(textureName, data);
            }
        }
        zis.close();
    }

    private byte[] readEntry(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        int total = 0;
        while ((len = is.read(buffer)) != -1) {
            total += len;
            if (total > MAX_TEXTURE_SIZE) {
                return null;
            }
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    /**
     * 获取指定贴图的 PNG 字节。
     *
     * @param name 贴图名（不含 .png）
     * @return PNG 字节；不存在返回 null
     */
    public byte[] getTexture(String name) {
        if (name == null) {
            return null;
        }
        return textures.get(name);
    }

    /**
     * 是否存在指定贴图。
     */
    public boolean hasTexture(String name) {
        return name != null && textures.containsKey(name);
    }

    /**
     * 获取所有贴图名。
     */
    public Set<String> getTextureNames() {
        return textures.keySet();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int getTextureCount() {
        return textures.size();
    }
}
