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
        putName("GRASS", new Faces("grass_top", "grass_side", "dirt"));
        putName("DIRT", new Faces("dirt"));
        putName("SAND", new Faces("sand"));
        putName("SANDSTONE", new Faces("sandstone_top", "sandstone_normal", "sandstone_bottom"));
        putName("GRAVEL", new Faces("gravel"));
        putName("CLAY", new Faces("clay"));
        putName("SOUL_SAND", new Faces("soul_sand"));

        // 石头类
        putName("STONE", new Faces("stone"));
        putName("COBBLESTONE", new Faces("cobblestone"));
        putName("MOSSY_COBBLESTONE", new Faces("cobblestone_mossy"));
        putName("OBSIDIAN", new Faces("obsidian"));
        putName("NETHERRACK", new Faces("netherrack"));
        putName("ENDER_STONE", new Faces("end_stone"));
        putName("BEDROCK", new Faces("bedrock"));
        putName("MONSTER_EGGS", new Faces("stone"));

        // 木头
        putName("LOG", new Faces("log_oak_top", "log_oak", "log_oak_top"));
        putName("LOG_2", new Faces("log_acacia_top", "log_acacia", "log_acacia_top"));
        putName("WOOD", new Faces("planks_oak"));
        putName("LEAVES", new Faces("leaves_oak"));
        putName("LEAVES_2", new Faces("leaves_acacia"));

        // 水 / 冰 / 雪
        putName("WATER", new Faces("water_still"));
        putName("STATIONARY_WATER", new Faces("water_still"));
        putName("ICE", new Faces("ice"));
        putName("PACKED_ICE", new Faces("ice_packed"));
        putName("SNOW", new Faces("snow"));
        putName("SNOW_BLOCK", new Faces("snow"));

        // 岩浆 / 火
        putName("LAVA", new Faces("lava_still"));
        putName("STATIONARY_LAVA", new Faces("lava_still"));
        putName("FIRE", new Faces("fire_0"));

        // 矿石
        putName("COAL_ORE", new Faces("coal_ore"));
        putName("IRON_ORE", new Faces("iron_ore"));
        putName("GOLD_ORE", new Faces("gold_ore"));
        putName("DIAMOND_ORE", new Faces("diamond_ore"));
        putName("REDSTONE_ORE", new Faces("redstone_ore"));
        putName("LAPIS_ORE", new Faces("lapis_ore"));
        putName("EMERALD_ORE", new Faces("emerald_ore"));
        putName("QUARTZ_ORE", new Faces("quartz_ore"));

        // 金属块
        putName("IRON_BLOCK", new Faces("iron_block"));
        putName("GOLD_BLOCK", new Faces("gold_block"));
        putName("DIAMOND_BLOCK", new Faces("diamond_block"));
        putName("EMERALD_BLOCK", new Faces("emerald_block"));
        putName("LAPIS_BLOCK", new Faces("lapis_block"));
        putName("REDSTONE_BLOCK", new Faces("redstone_block"));
        putName("COAL_BLOCK", new Faces("coal_block"));
        putName("QUARTZ_BLOCK", new Faces("quartz_block_top", "quartz_block_side", "quartz_block_bottom"));

        // 玻璃
        putName("GLASS", new Faces("glass"));
        putName("THIN_GLASS", new Faces("glass"));

        // 其他
        putName("BRICK", new Faces("brick"));
        putName("BRICK_STAIRS", new Faces("brick"));
        putName("NETHER_BRICK", new Faces("nether_brick"));
        putName("PRISMARINE", new Faces("prismarine_rough"));
        putName("SEA_LANTERN", new Faces("sea_lantern"));
        putName("GLOWSTONE", new Faces("glowstone"));
        putName("MYCEL", new Faces("mycelium_top", "mycelium_side", "dirt"));
        putName("HAY_BLOCK", new Faces("hay_block_top", "hay_block_side", "hay_block_top"));
        putName("SPONGE", new Faces("sponge"));
        putName("WEB", new Faces("web"));
        putName("BOOKSHELF", new Faces("bookshelf"));
        putName("WORKBENCH", new Faces("crafting_table_top", "crafting_table_side", "planks_oak"));
        putName("FURNACE", new Faces("furnace_top", "furnace_side", "furnace_top"));
        putName("CHEST", new Faces("chest_top", "chest_side", "chest_top"));
        putName("TNT", new Faces("tnt_top", "tnt_side", "tnt_bottom"));
        putName("PUMPKIN", new Faces("pumpkin_top", "pumpkin_side", "pumpkin_top"));
        putName("MELON_BLOCK", new Faces("melon_top", "melon_side", "melon_top"));
        putName("NETHER_WART_BLOCK", new Faces("nether_wart_block"));
        putName("BONE_BLOCK", new Faces("bone_block_top", "bone_block_side", "bone_block_top"));
        putName("END_BRICKS", new Faces("end_bricks"));
        putName("PURPUR_BLOCK", new Faces("purpur_block"));
        putName("PURPUR_PILLAR", new Faces("purpur_pillar_top", "purpur_pillar", "purpur_pillar_top"));
        putName("CHORUS_PLANT", new Faces("chorus_plant"));
        putName("CHORUS_FLOWER", new Faces("chorus_flower"));
        putName("MAGMA", new Faces("magma"));
        putName("NETHER_WARTS", new Faces("nether_wart_stage_0"));
        putName("SLIME_BLOCK", new Faces("slime"));
        putName("BARRIER", new Faces("barrier"));
        putName("DRAGON_EGG", new Faces("dragon_egg"));
        putName("ENDER_PORTAL_FRAME", new Faces("end_portal_frame_top", "end_portal_frame_side", "end_portal_frame_top"));

        // 羊毛（按颜色 data 值，前端处理）
        putName("WOOL", new Faces("wool_colored_white"));

        // 混凝土 / 陶瓦（1.12 有）
        putName("STAINED_CLAY", new Faces("hardened_clay_stained_white"));
        putName("STAINED_GLASS", new Faces("glass_white"));
        putName("STAINED_GLASS_PANE", new Faces("glass_white"));

        // ============ 下界方块 ============
        putName("NETHER_BRICK_STAIRS", new Faces("nether_brick"));
        putName("NETHER_BRICK_FENCE", new Faces("nether_brick"));
        putName("NETHER_FENCE", new Faces("nether_brick"));
        putName("NETHER_STALK", new Faces("nether_wart_stage_0"));
        putName("NETHER_WART_BLOCK", new Faces("nether_wart_block"));
        putName("RED_NETHER_BRICK", new Faces("red_nether_brick"));
        putName("MAGMA", new Faces("magma"));
        putName("QUARTZ_STAIRS", new Faces("quartz_block_side"));
        putName("QUARTZ_ORE", new Faces("quartz_ore"));
        putName("GLOWSTONE", new Faces("glowstone"));
        putName("NETHERRACK", new Faces("netherrack"));
        putName("SOUL_SAND", new Faces("soul_sand"));
        putName("LAVA", new Faces("lava_still"));
        putName("STATIONARY_LAVA", new Faces("lava_still"));
        putName("OBSIDIAN", new Faces("obsidian"));
        putName("PORTAL", new Faces("portal"));

        // ============ 末地方块 ============
        putName("ENDER_STONE", new Faces("end_stone"));
        putName("END_BRICKS", new Faces("end_bricks"));
        putName("END_STONE", new Faces("end_stone"));
        putName("PURPUR_BLOCK", new Faces("purpur_block"));
        putName("PURPUR_PILLAR", new Faces("purpur_pillar_top", "purpur_pillar", "purpur_pillar_top"));
        putName("PURPUR_STAIRS", new Faces("purpur_block"));
        putName("PURPUR_DOUBLE_SLAB", new Faces("purpur_block"));
        putName("PURPUR_SLAB", new Faces("purpur_block"));
        putName("CHORUS_PLANT", new Faces("chorus_plant"));
        putName("CHORUS_FLOWER", new Faces("chorus_flower"));
        putName("DRAGON_EGG", new Faces("dragon_egg"));
        putName("ENDER_PORTAL_FRAME", new Faces("end_portal_frame_top", "end_portal_frame_side", "end_portal_frame_top"));
        putName("END_PORTAL_FRAME", new Faces("end_portal_frame_top", "end_portal_frame_side", "end_portal_frame_top"));
        putName("END_GATEWAY", new Faces("end_gateway"));
        putName("END_ROD", new Faces("end_rod"));
        putName("END_CRYSTAL", new Faces("end_crystal"));
        putName("ENDER_CHEST", new Faces("ender_chest_top", "ender_chest_side", "ender_chest_top"));
        putName("ENDER_OBSIDIAN", new Faces("obsidian"));

        // ============ 常见建筑方块（补充） ============
        putName("STONE_STAIRS", new Faces("stone"));
        putName("COBBLESTONE_STAIRS", new Faces("cobblestone"));
        putName("SANDSTONE_STAIRS", new Faces("sandstone_normal"));
        putName("SMOOTH_STAIRS", new Faces("stone_slab_top", "stone_slab_side", "stone_slab_top"));
        putName("SPRUCE_STAIRS", new Faces("planks_spruce"));
        putName("BIRCH_STAIRS", new Faces("planks_birch"));
        putName("JUNGLE_STAIRS", new Faces("planks_jungle"));
        putName("ACACIA_STAIRS", new Faces("planks_acacia"));
        putName("DARK_OAK_STAIRS", new Faces("planks_big_oak"));
        putName("WOOD_STAIRS", new Faces("planks_oak"));
        putName("WOOD_DOOR", new Faces("door_wood_upper"));
        putName("IRON_DOOR_BLOCK", new Faces("door_iron_upper"));
        putName("FENCE", new Faces("planks_oak"));
        putName("SPRUCE_FENCE", new Faces("planks_spruce"));
        putName("BIRCH_FENCE", new Faces("planks_birch"));
        putName("JUNGLE_FENCE", new Faces("planks_jungle"));
        putName("ACACIA_FENCE", new Faces("planks_acacia"));
        putName("DARK_OAK_FENCE", new Faces("planks_big_oak"));
        putName("IRON_FENCE", new Faces("iron_bars"));
        putName("IRON_TRAPDOOR", new Faces("iron_trapdoor"));
        putName("TRAP_DOOR", new Faces("trapdoor"));
        putName("LADDER", new Faces("ladder"));
        putName("SIGN_POST", new Faces("planks_oak"));
        putName("WALL_SIGN", new Faces("planks_oak"));
        putName("TORCH", new Faces("torch_on"));
        putName("REDSTONE_TORCH_ON", new Faces("redstone_torch_on"));
        putName("REDSTONE_TORCH_OFF", new Faces("redstone_torch_off"));
        putName("REDSTONE_LAMP_ON", new Faces("redstone_lamp_on"));
        putName("REDSTONE_LAMP_OFF", new Faces("redstone_lamp_off"));
        putName("RAILS", new Faces("rail_normal"));
        putName("POWERED_RAIL", new Faces("rail_golden"));
        putName("DETECTOR_RAIL", new Faces("rail_detector"));
        putName("ACTIVATOR_RAIL", new Faces("rail_activator"));
        putName("IRON_PLATE", new Faces("iron_trapdoor"));
        putName("STONE_PLATE", new Faces("stone"));
        putName("WOOD_PLATE", new Faces("planks_oak"));
        putName("GOLD_PLATE", new Faces("gold_block"));
        putName("IRON_DOOR", new Faces("door_iron_upper"));
        putName("WOODEN_DOOR", new Faces("door_wood_upper"));
        putName("DISPENSER", new Faces("dispenser_top", "furnace_side", "dispenser_top"));
        putName("DROPPER", new Faces("dropper_top", "furnace_side", "dropper_top"));
        putName("HOPPER", new Faces("hopper_top", "hopper_outside", "hopper_top"));
        putName("PISTON_BASE", new Faces("piston_top_normal", "piston_side", "piston_bottom"));
        putName("PISTON_STICKY_BASE", new Faces("piston_top_sticky", "piston_side", "piston_bottom"));
        putName("PISTON_EXTENSION", new Faces("piston_top_normal", "piston_side", "piston_bottom"));
        putName("OBSERVER", new Faces("observer_top", "observer_side", "observer_top"));
        putName("BEACON", new Faces("beacon"));
        putName("ANVIL", new Faces("anvil_top", "anvil_base", "anvil_base"));
        putName("CAULDRON", new Faces("cauldron_top", "cauldron_side", "cauldron_bottom"));
        putName("BREWING_STAND", new Faces("brewing_stand"));
        putName("ENCHANTMENT_TABLE", new Faces("enchanting_table_top", "enchanting_table_side", "enchanting_table_bottom"));
        putName("JUKEBOX", new Faces("jukebox_top", "jukebox_side", "jukebox_side"));
        putName("NOTE_BLOCK", new Faces("noteblock"));
        putName("DAYLIGHT_DETECTOR", new Faces("daylight_detector_top", "daylight_detector_side", "daylight_detector_top"));
        putName("DAYLIGHT_DETECTOR_INVERTED", new Faces("daylight_detector_inverted_top", "daylight_detector_side", "daylight_detector_inverted_top"));
        putName("TRAPPED_CHEST", new Faces("trapped_chest_top", "trapped_chest_side", "trapped_chest_top"));
        putName("ENDER_CHEST", new Faces("ender_chest_top", "ender_chest_side", "ender_chest_top"));
        putName("IRON_BARS", new Faces("iron_bars"));
        putName("COBWEB", new Faces("web"));
        putName("VINE", new Faces("vine"));
        putName("WATER_LILY", new Faces("waterlily"));
        putName("CACTUS", new Faces("cactus_top", "cactus_side", "cactus_bottom"));
        putName("SUGAR_CANE_BLOCK", new Faces("reeds"));
        putName("PUMPKIN_STEM", new Faces("pumpkin_stem_disconnected"));
        putName("MELON_STEM", new Faces("melon_stem_disconnected"));
        putName("CARROT", new Faces("carrots_stage_3"));
        putName("POTATO", new Faces("potatoes_stage_3"));
        putName("CROPS", new Faces("wheat_stage_7"));
        putName("BEETROOT_BLOCK", new Faces("beetroots_stage_3"));
        putName("NETHER_WARTS", new Faces("nether_wart_stage_2"));
        putName("FLOWER_POT", new Faces("flower_pot"));
        putName("SKULL", new Faces("skull_skeleton"));
        putName("BED_BLOCK", new Faces("bed_feet_top", "bed_feet_side", "bed_feet_top"));
        putName("IRON_TRAPDOOR", new Faces("iron_trapdoor"));
        putName("SLIME_BLOCK", new Faces("slime"));
        putName("HAY_BLOCK", new Faces("hay_block_top", "hay_block_side", "hay_block_top"));
        putName("BONE_BLOCK", new Faces("bone_block_top", "bone_block_side", "bone_block_top"));
        putName("CHORUS_PLANT", new Faces("chorus_plant"));
        putName("STRUCTURE_BLOCK", new Faces("structure_block"));
        putName("COMMAND", new Faces("command_block"));
        putName("COMMAND_CHAIN", new Faces("chain_command_block"));
        putName("COMMAND_REPEATING", new Faces("repeating_command_block"));
        putName("MOB_SPAWNER", new Faces("mob_spawner"));
        putName("SPONGE", new Faces("sponge"));
        putName("WET_SPONGE", new Faces("sponge_wet"));
        putName("SEA_LANTERN", new Faces("sea_lantern"));
        putName("PRISMARINE", new Faces("prismarine_rough"));
        putName("PRISMARINE_CRYSTALS", new Faces("prismarine_crystals"));
        putName("PRISMARINE_SHARD", new Faces("prismarine_shard"));
        putName("END_BRICKS", new Faces("end_bricks"));
        putName("RED_SANDSTONE", new Faces("red_sandstone_top", "red_sandstone_normal", "red_sandstone_bottom"));
        putName("RED_SANDSTONE_STAIRS", new Faces("red_sandstone_normal"));
        putName("HARD_CLAY", new Faces("hardened_clay"));
        putName("STAINED_HARDENED_CLAY", new Faces("hardened_clay_stained_white"));
        putName("PACKED_ICE", new Faces("ice_packed"));
        putName("FROSTED_ICE", new Faces("frosted_ice_0"));
        putName("GRASS_PATH", new Faces("grass_path_top", "grass_path_side", "dirt"));
        putName("DIRT_PATH", new Faces("grass_path_top", "grass_path_side", "dirt"));
        putName("CHORUS_FLOWER", new Faces("chorus_flower"));
        putName("END_ROD", new Faces("end_rod"));
        putName("PURPUR_SLAB", new Faces("purpur_block"));
        putName("MAGMA", new Faces("magma"));
        putName("NETHER_WART_BLOCK", new Faces("nether_wart_block"));
        putName("RED_NETHER_BRICK", new Faces("red_nether_brick"));
        putName("BONE_BLOCK", new Faces("bone_block_top", "bone_block_side", "bone_block_top"));
        putName("OBSERVER", new Faces("observer_top", "observer_side", "observer_top"));
        putName("WHITE_SHULKER_BOX", new Faces("shulker_top_white", "shulker_side_white", "shulker_top_white"));
        putName("ORANGE_SHULKER_BOX", new Faces("shulker_top_orange", "shulker_side_orange", "shulker_top_orange"));
        putName("MAGENTA_SHULKER_BOX", new Faces("shulker_top_magenta", "shulker_side_magenta", "shulker_top_magenta"));
        putName("LIGHT_BLUE_SHULKER_BOX", new Faces("shulker_top_light_blue", "shulker_side_light_blue", "shulker_top_light_blue"));
        putName("YELLOW_SHULKER_BOX", new Faces("shulker_top_yellow", "shulker_side_yellow", "shulker_top_yellow"));
        putName("LIME_SHULKER_BOX", new Faces("shulker_top_lime", "shulker_side_lime", "shulker_top_lime"));
        putName("PINK_SHULKER_BOX", new Faces("shulker_top_pink", "shulker_side_pink", "shulker_top_pink"));
        putName("GRAY_SHULKER_BOX", new Faces("shulker_top_gray", "shulker_side_gray", "shulker_top_gray"));
        putName("SILVER_SHULKER_BOX", new Faces("shulker_top_silver", "shulker_side_silver", "shulker_top_silver"));
        putName("CYAN_SHULKER_BOX", new Faces("shulker_top_cyan", "shulker_side_cyan", "shulker_top_cyan"));
        putName("PURPLE_SHULKER_BOX", new Faces("shulker_top_purple", "shulker_side_purple", "shulker_top_purple"));
        putName("BLUE_SHULKER_BOX", new Faces("shulker_top_blue", "shulker_side_blue", "shulker_top_blue"));
        putName("BROWN_SHULKER_BOX", new Faces("shulker_top_brown", "shulker_side_brown", "shulker_top_brown"));
        putName("GREEN_SHULKER_BOX", new Faces("shulker_top_green", "shulker_side_green", "shulker_top_green"));
        putName("RED_SHULKER_BOX", new Faces("shulker_top_red", "shulker_side_red", "shulker_top_red"));
        putName("BLACK_SHULKER_BOX", new Faces("shulker_top_black", "shulker_side_black", "shulker_top_black"));
        putName("SHULKER_BOX", new Faces("shulker_top_white", "shulker_side_white", "shulker_top_white"));
        putName("GLOWING_OBSIDIAN", new Faces("obsidian"));
        putName("NETHER_PORTAL", new Faces("portal"));
        putName("END_PORTAL", new Faces("end_portal"));
        putName("END_GATEWAY", new Faces("end_gateway"));
    }

    /**
     * 注册自定义映射（供服主配置模组方块）。
     * 格式：方块名 → 材质名（顶面/侧面/底面相同）
     */
    public static void registerCustom(String materialName, String textureName) {
        if (materialName == null || textureName == null) {
            return;
        }
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) {
            MAP.put(material.name(), new Faces(textureName));
        }
    }

    /**
     * 注册自定义映射（三面不同）。
     */
    public static void registerCustom(String materialName, String top, String side, String bottom) {
        if (materialName == null) {
            return;
        }
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) {
            MAP.put(material.name(), new Faces(top, side, bottom));
        }
    }

    private BlockTextureMap() {
    }

    private static void put(Material material, Faces faces) {
        if (material != null) {
            MAP.put(material.name(), faces);
        }
    }

    /**
     * 按方块名注册材质。
     * 使用字符串查找而非直接引用枚举常量，避免不同服务端版本枚举名差异导致的编译错误。
     */
    private static void putName(String materialName, Faces faces) {
        Material material = Material.getMaterial(materialName);
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

    /**
     * 获取方块的三面材质（按类型 ID + 数据值）。
     * 处理变种方块（树叶、木头、羊毛等），根据 data 值选择正确的贴图。
     */
    public static Faces getFacesById(int typeId, int data) {
        Material material = Material.getMaterial(typeId);
        if (material == null) {
            return null;
        }
        String name = material.name();
        int d = data & 0x0F;

        // 树叶变种
        if ("LEAVES".equals(name)) {
            String[] leaves = {"leaves_oak", "leaves_spruce", "leaves_birch", "leaves_jungle"};
            return new Faces(leaves[Math.min(d, 3)]);
        }
        if ("LEAVES_2".equals(name)) {
            String[] leaves2 = {"leaves_acacia", "leaves_big_oak"};
            return new Faces(leaves2[Math.min(d, 1)]);
        }

        // 木头变种
        if ("LOG".equals(name)) {
            String[] logs = {"log_oak", "log_spruce", "log_birch", "log_jungle"};
            String log = logs[Math.min(d, 3)];
            return new Faces(log + "_top", log, log + "_top");
        }
        if ("LOG_2".equals(name)) {
            String[] logs2 = {"log_acacia", "log_big_oak"};
            String log = logs2[Math.min(d, 1)];
            return new Faces(log + "_top", log, log + "_top");
        }

        // 木板变种
        if ("WOOD".equals(name)) {
            String[] planks = {"planks_oak", "planks_spruce", "planks_birch", "planks_jungle",
                    "planks_acacia", "planks_big_oak"};
            return new Faces(planks[Math.min(d, 5)]);
        }

        // 羊毛变种
        if ("WOOL".equals(name)) {
            String[] wool = {"wool_colored_white", "wool_colored_orange", "wool_colored_magenta",
                    "wool_colored_light_blue", "wool_colored_yellow", "wool_colored_lime",
                    "wool_colored_pink", "wool_colored_gray", "wool_colored_silver",
                    "wool_colored_cyan", "wool_colored_purple", "wool_colored_blue",
                    "wool_colored_brown", "wool_colored_green", "wool_colored_red",
                    "wool_colored_black"};
            return new Faces(wool[Math.min(d, 15)]);
        }

        // 其他方块按名字查找
        return getFaces(material);
    }
}
