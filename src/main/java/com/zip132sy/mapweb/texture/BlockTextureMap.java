package com.zip132sy.mapweb.texture;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块 → 材质文件名映射表。
 * 用于 3D 视图给方块贴材质。
 *
 * 每个方块映射三个面：顶面（top）、侧面（side）、底面（bottom）。
 * 材质文件名不含 .png 后缀，对应材质包内 assets/minecraft/textures/blocks/ 下的文件。
 *
 * 说明：1.12 的方块名与材质文件名不完全对应，本表覆盖常见方块，
 * 未收录的方块在前端用纯色兜底。
 */
public final class BlockTextureMap {

    /** 方块的三面材质 */
    public static class Faces {
        public final String top;
        public final String side;
        public final String bottom;

        public Faces(String top, String side, String bottom) {
            this.top = top;
            this.side = side;
            this.bottom = bottom;
        }

        public Faces(String all) {
            this.top = all;
            this.side = all;
            this.bottom = all;
        }
    }

    private static final Map<String, Faces> MAP = new HashMap<String, Faces>();

    static {
        // 草方块：顶面草、侧面草土、底面土
        put(Material.GRASS, new Faces("grass_top", "grass_side", "dirt"));
        put(Material.DIRT, new Faces("dirt"));
        put(Material.SAND, new Faces("sand"));
        put(Material.SANDSTONE, new Faces("sandstone_top", "sandstone_normal", "sandstone_bottom"));
        put(Material.GRAVEL, new Faces("gravel"));
        put(Material.CLAY, new Faces("clay"));
        put(Material.SOUL_SAND, new Faces("soul_sand"));

        // 石头类
        put(Material.STONE, new Faces("stone"));
        put(Material.COBBLESTONE, new Faces("cobblestone"));
        put(Material.MOSSY_COBBLESTONE, new Faces("cobblestone_mossy"));
        put(Material.OBSIDIAN, new Faces("obsidian"));
        put(Material.NETHERRACK, new Faces("netherrack"));
        put(Material.ENDER_STONE, new Faces("end_stone"));
        put(Material.BEDROCK, new Faces("bedrock"));
        put(Material.MONSTER_EGGS, new Faces("stone"));

        // 木头
        put(Material.LOG, new Faces("log_oak_top", "log_oak", "log_oak_top"));
        put(Material.LOG_2, new Faces("log_acacia_top", "log_acacia", "log_acacia_top"));
        put(Material.WOOD, new Faces("planks_oak"));
        put(Material.LEAVES, new Faces("leaves_oak"));
        put(Material.LEAVES_2, new Faces("leaves_acacia"));

        // 水 / 冰 / 雪
        put(Material.WATER, new Faces("water_still"));
        put(Material.STATIONARY_WATER, new Faces("water_still"));
        put(Material.ICE, new Faces("ice"));
        put(Material.PACKED_ICE, new Faces("ice_packed"));
        put(Material.SNOW, new Faces("snow"));
        put(Material.SNOW_BLOCK, new Faces("snow"));

        // 岩浆 / 火
        put(Material.LAVA, new Faces("lava_still"));
        put(Material.STATIONARY_LAVA, new Faces("lava_still"));
        put(Material.FIRE, new Faces("fire_0"));

        // 矿石
        put(Material.COAL_ORE, new Faces("coal_ore"));
        put(Material.IRON_ORE, new Faces("iron_ore"));
        put(Material.GOLD_ORE, new Faces("gold_ore"));
        put(Material.DIAMOND_ORE, new Faces("diamond_ore"));
        put(Material.REDSTONE_ORE, new Faces("redstone_ore"));
        put(Material.LAPIS_ORE, new Faces("lapis_ore"));
        put(Material.EMERALD_ORE, new Faces("emerald_ore"));
        put(Material.QUARTZ_ORE, new Faces("quartz_ore"));

        // 金属块
        put(Material.IRON_BLOCK, new Faces("iron_block"));
        put(Material.GOLD_BLOCK, new Faces("gold_block"));
        put(Material.DIAMOND_BLOCK, new Faces("diamond_block"));
        put(Material.EMERALD_BLOCK, new Faces("emerald_block"));
        put(Material.LAPIS_BLOCK, new Faces("lapis_block"));
        put(Material.REDSTONE_BLOCK, new Faces("redstone_block"));
        put(Material.COAL_BLOCK, new Faces("coal_block"));
        put(Material.QUARTZ_BLOCK, new Faces("quartz_block_top", "quartz_block_side", "quartz_block_bottom"));

        // 玻璃
        put(Material.GLASS, new Faces("glass"));
        put(Material.THIN_GLASS, new Faces("glass"));

        // 其他
        put(Material.BRICK, new Faces("brick"));
        put(Material.BRICK_STAIRS, new Faces("brick"));
        put(Material.NETHER_BRICK, new Faces("nether_brick"));
        put(Material.PRISMARINE, new Faces("prismarine_rough"));
        put(Material.SEA_LANTERN, new Faces("sea_lantern"));
        put(Material.GLOWSTONE, new Faces("glowstone"));
        put(Material.MYCEL, new Faces("mycelium_top", "mycelium_side", "dirt"));
        put(Material.HAY_BLOCK, new Faces("hay_block_top", "hay_block_side", "hay_block_top"));
        put(Material.SPONGE, new Faces("sponge"));
        put(Material.WEB, new Faces("web"));
        put(Material.BOOKSHELF, new Faces("bookshelf"));
        put(Material.CRAFTING_TABLE, new Faces("crafting_table_top", "crafting_table_side", "planks_oak"));
        put(Material.FURNACE, new Faces("furnace_top", "furnace_side", "furnace_top"));
        put(Material.CHEST, new Faces("chest_top", "chest_side", "chest_top"));
        put(Material.TNT, new Faces("tnt_top", "tnt_side", "tnt_bottom"));
        put(Material.PUMPKIN, new Faces("pumpkin_top", "pumpkin_side", "pumpkin_top"));
        put(Material.MELON_BLOCK, new Faces("melon_top", "melon_side", "melon_top"));
        put(Material.NETHER_WART_BLOCK, new Faces("nether_wart_block"));
        put(Material.BONE_BLOCK, new Faces("bone_block_top", "bone_block_side", "bone_block_top"));
        put(Material.END_BRICKS, new Faces("end_bricks"));
        put(Material.PURPUR_BLOCK, new Faces("purpur_block"));
        put(Material.PURPUR_PILLAR, new Faces("purpur_pillar_top", "purpur_pillar", "purpur_pillar_top"));
        put(Material.CHORUS_PLANT, new Faces("chorus_plant"));
        put(Material.CHORUS_FLOWER, new Faces("chorus_flower"));
        put(Material.MAGMA, new Faces("magma"));
        put(Material.NETHER_WART, new Faces("nether_wart_stage_0"));
        put(Material.SLIME_BLOCK, new Faces("slime"));
        put(Material.BARRIER, new Faces("barrier"));
        put(Material.DRAGON_EGG, new Faces("dragon_egg"));
        put(Material.END_PORTAL_FRAME, new Faces("end_portal_frame_top", "end_portal_frame_side", "end_portal_frame_top"));

        // 羊毛（按颜色 data 值，前端处理）
        put(Material.WOOL, new Faces("wool_colored_white"));

        // 混凝土 / 陶瓦（1.12 有）
        put(Material.STAINED_CLAY, new Faces("hardened_clay_stained_white"));
        put(Material.STAINED_GLASS, new Faces("glass_white"));
        put(Material.STAINED_GLASS_PANE, new Faces("glass_white"));
    }

    private BlockTextureMap() {
    }

    private static void put(Material material, Faces faces) {
        if (material != null) {
            MAP.put(material.name(), faces);
        }
    }

    /**
     * 获取方块的三面材质。
     *
     * @param material 方块类型
     * @return 材质信息；未收录返回 null
     */
    public static Faces getFaces(Material material) {
        if (material == null) {
            return null;
        }
        return MAP.get(material.name());
    }

    /**
     * 获取方块的三面材质（按类型 ID）。
     */
    public static Faces getFacesById(int typeId) {
        Material material = Material.getMaterial(typeId);
        return getFaces(material);
    }
}
