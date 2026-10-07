package com.zip132sy.mapweb.render;

import com.zip132sy.mapweb.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 地图渲染器。
 * 把某个世界当前已加载的区块渲染成 PNG 图片，支持俯视（2D）与等距（2.5D）两种模式。
 *
 * 性能设计：
 * - 主线程只负责生成 ChunkSnapshot（轻量操作）
 * - 采样与绘制在调用线程（HTTP 线程）完成，不阻塞服务器
 * - 每个区块按 resolution × resolution 采样，分辨率越高越清晰，但耗时越长
 */
public final class MapRenderer {

    /** 单次渲染的区块数量上限，防止内存溢出 */
    private static final long MAX_CHUNKS = 4_000_000L;

    private MapRenderer() {
    }

    /**
     * 渲染结果，包含图片数据与地图边界信息。
     */
    public static class RenderResult {
        public final byte[] png;
        public final int minChunkX;
        public final int minChunkZ;
        public final int mapWidth;
        public final int mapHeight;

        RenderResult(byte[] png, int minChunkX, int minChunkZ, int mapWidth, int mapHeight) {
            this.png = png;
            this.minChunkX = minChunkX;
            this.minChunkZ = minChunkZ;
            this.mapWidth = mapWidth;
            this.mapHeight = mapHeight;
        }
    }

    /**
     * 区块快照集合。
     */
    private static class SnapshotSet {
        List<ChunkSnapshot> snapshots = new ArrayList<ChunkSnapshot>();
        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        boolean valid = false;
    }

    /**
     * 渲染指定世界的地图（俯视 2D）。
     */
    public static RenderResult renderWorld(Plugin plugin, World world, int resolution, boolean showBorder) {
        return render(plugin, world, resolution, showBorder, false);
    }

    /**
     * 渲染指定世界的地图（等距 2.5D）。
     */
    public static RenderResult renderWorldIso(Plugin plugin, World world, int resolution, boolean showBorder) {
        return render(plugin, world, resolution, showBorder, true);
    }

    private static RenderResult render(Plugin plugin, World world, int resolution,
                                       boolean showBorder, boolean isometric) {
        if (world == null) {
            return null;
        }
        if (resolution < 1) {
            resolution = 1;
        }

        SnapshotSet set = collectSnapshots(plugin, world);
        if (set == null || !set.valid) {
            return null;
        }

        int chunkCols = set.maxChunkX - set.minChunkX + 1;
        int chunkRows = set.maxChunkZ - set.minChunkZ + 1;
        if (chunkCols <= 0 || chunkRows <= 0 || (long) chunkCols * chunkRows > MAX_CHUNKS) {
            return null;
        }

        BufferedImage image = isometric
                ? drawIsometric(set, chunkCols, chunkRows, resolution, showBorder)
                : drawTopDown(set, chunkCols, chunkRows, resolution, showBorder);

        if (image == null) {
            return null;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new RenderResult(out.toByteArray(), set.minChunkX, set.minChunkZ, chunkCols, chunkRows);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 在主线程采集区块快照。
     */
    private static SnapshotSet collectSnapshots(final Plugin plugin, final World world) {
        FutureTask<SnapshotSet> task = new FutureTask<SnapshotSet>(new Callable<SnapshotSet>() {
            @Override
            public SnapshotSet call() {
                return collect(world);
            }
        });

        try {
            if (Bukkit.isPrimaryThread()) {
                return collect(world);
            }
            Bukkit.getScheduler().runTask(plugin, task);
            return task.get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 实际采集逻辑（必须在主线程执行）。
     * 只生成快照，不做采样，速度很快。
     */
    private static SnapshotSet collect(World world) {
        SnapshotSet set = new SnapshotSet();

        Chunk[] chunks = world.getLoadedChunks();
        if (chunks == null || chunks.length == 0) {
            return set;
        }

        for (Chunk chunk : chunks) {
            int cx = chunk.getX();
            int cz = chunk.getZ();
            if (cx < set.minChunkX) {
                set.minChunkX = cx;
            }
            if (cx > set.maxChunkX) {
                set.maxChunkX = cx;
            }
            if (cz < set.minChunkZ) {
                set.minChunkZ = cz;
            }
            if (cz > set.maxChunkZ) {
                set.maxChunkZ = cz;
            }
        }

        int chunkCols = set.maxChunkX - set.minChunkX + 1;
        int chunkRows = set.maxChunkZ - set.minChunkZ + 1;
        if (chunkCols <= 0 || chunkRows <= 0 || (long) chunkCols * chunkRows > MAX_CHUNKS) {
            return set;
        }

        // 生成快照（主线程操作，但很快）
        for (Chunk chunk : chunks) {
            try {
                set.snapshots.add(chunk.getChunkSnapshot());
            } catch (Exception ignored) {
                // 单个区块快照失败忽略
            }
        }

        set.valid = !set.snapshots.isEmpty();
        return set;
    }

    /**
     * 绘制俯视图（2D）。
     * 每个区块占 resolution × resolution 像素。
     */
    private static BufferedImage drawTopDown(SnapshotSet set, int chunkCols, int chunkRows,
                                             int resolution, boolean showBorder) {
        int imgW = chunkCols * resolution;
        int imgH = chunkRows * resolution;

        if (imgW <= 0 || imgH <= 0 || (long) imgW * imgH > 64_000_000L) {
            return null;
        }

        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);

        for (ChunkSnapshot snapshot : set.snapshots) {
            int cx = snapshot.getX();
            int cz = snapshot.getZ();
            int baseX = (cx - set.minChunkX) * resolution;
            int baseZ = (cz - set.minChunkZ) * resolution;

            // 采样：每个像素对应区块内一个位置
            for (int px = 0; px < resolution; px++) {
                for (int pz = 0; pz < resolution; pz++) {
                    // 映射到区块内坐标（0-15）
                    int localX = resolution == 1 ? 8 : (px * 16 / resolution);
                    int localZ = resolution == 1 ? 8 : (pz * 16 / resolution);

                    int highestY = snapshot.getHighestBlockYAt(localX, localZ);
                    int typeId = snapshot.getBlockTypeId(localX, highestY, localZ);
                    int data = snapshot.getBlockData(localX, highestY, localZ);

                    int color = BlockColor.getColorById(typeId, data);
                    int shaded = ColorUtil.applyHeightShade(color, highestY, 0, 255);

                    image.setRGB(baseX + px, baseZ + pz, ColorUtil.toArgb(shaded));
                }
            }

            // 区块边界线
            if (showBorder) {
                drawBorder(image, baseX, baseZ, resolution);
            }
        }

        return image;
    }

    /**
     * 绘制等距视图（2.5D）。
     * 每个区块画成一个菱形顶面 + 侧面。
     */
    private static BufferedImage drawIsometric(SnapshotSet set, int chunkCols, int chunkRows,
                                               int resolution, boolean showBorder) {
        int tileW = resolution;
        int tileH = Math.max(1, resolution / 2);
        int tileDepth = Math.max(1, resolution / 4);

        int imgW = (chunkCols + chunkRows) * tileW + tileW;
        int imgH = (chunkCols + chunkRows) * tileH + tileDepth + 2;

        if (imgW <= 0 || imgH <= 0 || (long) imgW * imgH > 64_000_000L) {
            return null;
        }

        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);

        // 从后往前绘制，保证遮挡关系
        for (int row = 0; row < chunkRows; row++) {
            for (int col = 0; col < chunkCols; col++) {
                ChunkSnapshot snapshot = findSnapshot(set, set.minChunkX + col, set.minChunkZ + row);
                if (snapshot == null) {
                    continue;
                }

                // 取区块中心点颜色作为该区块代表色
                int highestY = snapshot.getHighestBlockYAt(8, 8);
                int typeId = snapshot.getBlockTypeId(8, highestY, 8);
                int data = snapshot.getBlockData(8, highestY, 8);
                int color = BlockColor.getColorById(typeId, data);
                int shaded = ColorUtil.applyHeightShade(color, highestY, 0, 255);

                int screenX = (col - row) * tileW + (chunkRows * tileW);
                int screenY = (col + row) * tileH;

                drawIsoTile(image, screenX, screenY, tileW, tileH, tileDepth, shaded);
            }
        }

        return image;
    }

    private static ChunkSnapshot findSnapshot(SnapshotSet set, int chunkX, int chunkZ) {
        for (ChunkSnapshot snapshot : set.snapshots) {
            if (snapshot.getX() == chunkX && snapshot.getZ() == chunkZ) {
                return snapshot;
            }
        }
        return null;
    }

    /**
     * 绘制区块边界线（淡淡的深色线）。
     */
    private static void drawBorder(BufferedImage image, int baseX, int baseZ, int resolution) {
        int borderColor = ColorUtil.toArgb(0x000000);
        // 只画左边和上边，避免重复
        for (int i = 0; i < resolution; i++) {
            setPixelBlend(image, baseX, baseZ + i, borderColor);
            setPixelBlend(image, baseX + i, baseZ, borderColor);
        }
    }

    /**
     * 绘制单个等距方块（菱形顶面 + 左右侧面）。
     */
    private static void drawIsoTile(BufferedImage image, int cx, int cy,
                                    int tileW, int tileH, int tileDepth, int color) {
        int topColor = color;
        int leftColor = darken(color, 0.75D);
        int rightColor = darken(color, 0.6D);

        // 顶面菱形
        for (int dy = 0; dy <= tileH; dy++) {
            int halfWidth = tileW - (dy * tileW / Math.max(1, tileH));
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                setPixel(image, cx + dx, cy + dy, topColor);
            }
        }

        // 左侧面
        for (int dy = 0; dy < tileDepth; dy++) {
            for (int dx = -tileW; dx <= 0; dx++) {
                setPixel(image, cx + dx, cy + tileH + dy, leftColor);
            }
        }

        // 右侧面
        for (int dy = 0; dy < tileDepth; dy++) {
            for (int dx = 0; dx <= tileW; dx++) {
                setPixel(image, cx + dx, cy + tileH + dy, rightColor);
            }
        }
    }

    private static void setPixel(BufferedImage image, int x, int y, int rgb) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }
        image.setRGB(x, y, ColorUtil.toArgb(rgb));
    }

    /**
     * 半透明混合像素（用于边界线）。
     */
    private static void setPixelBlend(BufferedImage image, int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return;
        }
        int old = image.getRGB(x, y);
        int oldR = (old >> 16) & 0xFF;
        int oldG = (old >> 8) & 0xFF;
        int oldB = old & 0xFF;
        // 与黑色按 25% 混合
        int newR = (int) (oldR * 0.75D);
        int newG = (int) (oldG * 0.75D);
        int newB = (int) (oldB * 0.75D);
        image.setRGB(x, y, ColorUtil.toArgb((newR << 16) | (newG << 8) | newB));
    }

    private static int darken(int rgb, double factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }
}
