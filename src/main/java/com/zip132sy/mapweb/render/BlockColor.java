package com.zip132sy.mapweb.render;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块颜色映射表。
 * 为常见方块提供俯视图代表色，未收录的方块按材质名生成稳定的伪随机色。
 * 兼容 1.12.x：使用 Material + data 值区分带颜色的方块。
 */
public final class BlockColor {

    private static final Map<String, Integer> COLOR_MAP = new HashMap<String, Integer>();
    private static final int DEFAULT_COLOR = 0x8B8B8B;

    static {
        // 草方块 / 泥土 / 沙
        put(Material.GRASS, 0x7CB342);
        put(Material.DIRT, 0x8B6B4A);
        put(Material.SAND, 0xE8DCA0);
        put(Material.SANDSTONE, 0xD8CBA0);
        put(Material.GRAVEL, 0x9A9A9A);
        put(Material.CLAY, 0xA4A8B8);
        put(Material.SOUL_SAND, 0x5A4A3A);

        // 石头类
        put(Material.STONE, 0x8A8A8A);
        put(Material.COBBLESTONE, 0x7A7A7A);
        put(Material.MOSSY_COBBLESTONE, 0x6A7A5A);
        put(Material.OBSIDIAN, 0x1A1024);
        put(Material.NETHERRACK, 0x6E2B2B);
        put(Material.ENDER_STONE, 0xDBDEA0);
        put(Material.BEDROCK, 0x333333);

        // 木头
        put(Material.LOG, 0x6B4F2A);
        put(Material.LOG_2, 0x5A3A1A);
        put(Material.WOOD, 0xB08A50);
        put(Material.LEAVES, 0x3E7A2E);
        put(Material.LEAVES_2, 0x2E6A1E);

        // 水 / 冰 / 雪
        put(Material.WATER, 0x3A6EA5);
        put(Material.STATIONARY_WATER, 0x3A6EA5);
        put(Material.ICE, 0xA8D8F0);
        put(Material.PACKED_ICE, 0x90C8E8);
        put(Material.SNOW, 0xF0F0F0);
        put(Material.SNOW_BLOCK, 0xF5F5F5);

        // 岩浆 / 火
        put(Material.LAVA, 0xE85A10);
        put(Material.STATIONARY_LAVA, 0xE85A10);
        put(Material.FIRE, 0xFFAA00);

        // 矿石
        put(Material.COAL_ORE, 0x6A6A6A);
        put(Material.IRON_ORE, 0xB8A088);
        put(Material.GOLD_ORE, 0xE8C860);
        put(Material.DIAMOND_ORE, 0x60E8E0);
        put(Material.REDSTONE_ORE, 0xC03030);
        put(Material.LAPIS_ORE, 0x3050A0);
        put(Material.EMERALD_ORE, 0x30C060);

        // 金属块
        put(Material.IRON_BLOCK, 0xD8D8D8);
        put(Material.GOLD_BLOCK, 0xF0D040);
        put(Material.DIAMOND_BLOCK, 0x60E8E0);
        put(Material.EMERALD_BLOCK, 0x30C060);
        put(Material.LAPIS_BLOCK, 0x3050A0);
        put(Material.REDSTONE_BLOCK, 0xC03030);
        put(Material.COAL_BLOCK, 0x2A2A2A);

        // 羊毛（1.12 用 WOOL + data）
        putWool();

        // 玻璃
        put(Material.GLASS, 0xC8E8F0);
        put(Material.THIN_GLASS, 0xC8E8F0);

        // 其他
        put(Material.BRICK, 0x9A5A40);
        put(Material.BRICK_STAIRS, 0x9A5A40);
        put(Material.NETHER_BRICK, 0x3A2020);
        put(Material.QUARTZ_BLOCK, 0xE8E4DC);
        put(Material.PRISMARINE, 0x60A090);
        put(Material.SEA_LANTERN, 0x80D8C8);
        put(Material.GLOWSTONE, 0xF0E0A0);
        put(Material.MYCEL, 0x8A7A8A);
        put(Material.HAY_BLOCK, 0xD8C040);
        put(Material.SPONGE, 0xC8C840);
        put(Material.WEB, 0xE8E8E8);
    }

    private BlockColor() {
    }

    private static void put(Material material, int rgb) {
        if (material != null) {
            COLOR_MAP.put(material.name(), rgb);
        }
    }

    private static void putWool() {
        // 1.12 羊毛颜色数据值 0-15
        int[] woolColors = {
                0xF0F0F0, // 0 白
                0xE8A040, // 1 橙
                0xC050C0, // 2 品红
                0x60A0E0, // 3 淡蓝
                0xE8E040, // 4 黄
                0x60C040, // 5 黄绿
                0xE880A0, // 6 粉
                0x505050, // 7 灰
                0xA0A0A0, // 8 淡灰
                0x40A0A0, // 9 青
                0x8040C0, // 10 紫
                0x3050C0, // 11 蓝
                0x604020, // 12 棕
                0x305030, // 13 绿
                0xA03030, // 14 红
                0x202020  // 15 黑
        };
        for (int i = 0; i < woolColors.length; i++) {
            COLOR_MAP.put("WOOL:" + i, woolColors[i]);
        }
    }

    /**
     * 获取方块的代表色。
     *
     * @param block 目标方块
     * @return RGB（0xRRGGBB）
     */
    public static int getColor(Block block) {
        Material material = block.getType();
        if (material == null || material == Material.AIR) {
            return DEFAULT_COLOR;
        }

        String name = material.name();

        // 羊毛按 data 值取色
        if ("WOOL".equals(name)) {
            byte data = block.getData();
            Integer wool = COLOR_MAP.get("WOOL:" + (data & 0x0F));
            if (wool != null) {
                return wool;
            }
        }

        // 1.12 灰化土是 DIRT + data 2，单独处理
        if ("DIRT".equals(name) && (block.getData() & 0x0F) == 2) {
            return 0x6A4A2A;
        }

        Integer color = COLOR_MAP.get(name);
        if (color != null) {
            return color;
        }

        // 未收录：按材质名生成稳定的伪随机色，避免全灰一片
        return pseudoColor(name);
    }

    /**
     * 按方块类型 ID 与数据值获取代表色。
     * 供 ChunkSnapshot 使用（1.12 的 ChunkSnapshot 只提供 typeId 与 data）。
     *
     * @param typeId 方块类型 ID
     * @param data   数据值
     * @return RGB（0xRRGGBB）
     */
    public static int getColorById(int typeId, int data) {
        if (typeId == 0) {
            return DEFAULT_COLOR;
        }
        Material material = Material.getMaterial(typeId);
        if (material == null) {
            return DEFAULT_COLOR;
        }

        String name = material.name();

        // 羊毛按 data 值取色
        if ("WOOL".equals(name)) {
            Integer wool = COLOR_MAP.get("WOOL:" + (data & 0x0F));
            if (wool != null) {
                return wool;
            }
        }

        // 1.12 灰化土是 DIRT + data 2
        if ("DIRT".equals(name) && (data & 0x0F) == 2) {
            return 0x6A4A2A;
        }

        Integer color = COLOR_MAP.get(name);
        if (color != null) {
            return color;
        }

        return pseudoColor(name);
    }

    /**
     * 根据材质名生成稳定的伪随机颜色。
     */
    private static int pseudoColor(String name) {
        int hash = name.hashCode();
        int r = 96 + Math.abs((hash >> 16) & 0x3F);
        int g = 96 + Math.abs((hash >> 8) & 0x3F);
        int b = 96 + Math.abs(hash & 0x3F);
        return (r << 16) | (g << 8) | b;
    }
}
