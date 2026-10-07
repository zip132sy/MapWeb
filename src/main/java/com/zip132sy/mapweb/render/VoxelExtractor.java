package com.zip132sy.mapweb.render;

import org.bukkit.ChunkSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 体素（方块）提取器。
 * 从区块快照中提取「表面方块」，用于前端 Three.js 渲染真 3D 地图。
 *
 * 隐藏面剔除：如果一个方块六个面都被不透明方块包围，则不提取，大幅减少数据量。
 */
public final class VoxelExtractor {

    /** 单个体素数据 */
    public static class Voxel {
        public final int x;
        public final int y;
        public final int z;
        public final int color;
        /** 顶面材质名（不含 .png），无贴图时为 null */
        public final String topTexture;
        /** 侧面材质名 */
        public final String sideTexture;
        /** 底面材质名 */
        public final String bottomTexture;

        public Voxel(int x, int y, int z, int color,
                     String topTexture, String sideTexture, String bottomTexture) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.topTexture = topTexture;
            this.sideTexture = sideTexture;
            this.bottomTexture = bottomTexture;
        }
    }

    private VoxelExtractor() {
    }

    /**
     * 从一组区块快照中提取表面方块。
     *
     * @param snapshots 区块快照列表
     * @param minY      最低扫描高度
     * @param maxY      最高扫描高度
     * @param maxVoxels 最大体素数量上限（防止数据量爆炸）
     * @return 体素列表
     */
    public static List<Voxel> extract(List<ChunkSnapshot> snapshots, int minY, int maxY, int maxVoxels) {
        List<Voxel> result = new ArrayList<Voxel>();
        if (snapshots == null || snapshots.isEmpty()) {
            return result;
        }

        // 建立区块坐标索引，方便跨区块判断邻居
        java.util.Map<Long, ChunkSnapshot> chunkMap = new java.util.HashMap<Long, ChunkSnapshot>();
        for (ChunkSnapshot snapshot : snapshots) {
            chunkMap.put(chunkKey(snapshot.getX(), snapshot.getZ()), snapshot);
        }

        for (ChunkSnapshot snapshot : snapshots) {
            int baseX = snapshot.getX() << 4;
            int baseZ = snapshot.getZ() << 4;

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int y = minY; y <= maxY; y++) {
                        int typeId = snapshot.getBlockTypeId(lx, y, lz);
                        if (typeId == 0) {
                            continue;
                        }

                        int worldX = baseX + lx;
                        int worldZ = baseZ + lz;

                        // 隐藏面剔除：六面都被不透明方块包围则跳过
                        if (isFullySurrounded(chunkMap, worldX, y, worldZ)) {
                            continue;
                        }

                        int data = snapshot.getBlockData(lx, y, lz);
                        int color = BlockColor.getColorById(typeId, data);

                        // 查询材质名
                        String topTex = null;
                        String sideTex = null;
                        String bottomTex = null;
                        com.zip132sy.mapweb.texture.BlockTextureMap.Faces faces =
                                com.zip132sy.mapweb.texture.BlockTextureMap.getFacesById(typeId, data);
                        if (faces != null) {
                            topTex = faces.top;
                            sideTex = faces.side;
                            bottomTex = faces.bottom;
                        }

                        result.add(new Voxel(worldX, y, worldZ, color, topTex, sideTex, bottomTex));

                        if (result.size() >= maxVoxels) {
                            return result;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * 判断某方块是否六面都被不透明方块包围。
     */
    private static boolean isFullySurrounded(java.util.Map<Long, ChunkSnapshot> chunkMap,
                                             int x, int y, int z) {
        return isOpaque(chunkMap, x + 1, y, z)
                && isOpaque(chunkMap, x - 1, y, z)
                && isOpaque(chunkMap, x, y + 1, z)
                && isOpaque(chunkMap, x, y - 1, z)
                && isOpaque(chunkMap, x, y, z + 1)
                && isOpaque(chunkMap, x, y, z - 1);
    }

    /**
     * 判断指定世界坐标的方块是否不透明（用于隐藏面剔除）。
     * 若该位置所在区块不在快照集合内，视为不透明（避免边界处多传方块）。
     */
    private static boolean isOpaque(java.util.Map<Long, ChunkSnapshot> chunkMap, int x, int y, int z) {
        if (y < 0 || y > 255) {
            return true;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ChunkSnapshot snapshot = chunkMap.get(chunkKey(chunkX, chunkZ));
        if (snapshot == null) {
            return true;
        }
        int lx = x & 0x0F;
        int lz = z & 0x0F;
        int typeId = snapshot.getBlockTypeId(lx, y, lz);
        return typeId != 0;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
