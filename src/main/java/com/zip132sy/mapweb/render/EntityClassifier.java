package com.zip132sy.mapweb.render;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 生物分类工具。
 * 把生物分为友好、中立、敌对三类，用于地图上的圆点着色。
 *
 * 分类依据：
 * - 敌对：实现了 Monster 接口的生物
 * - 友好：实现了 Animals 接口的生物
 * - 中立：Bukkit 没有对应接口，使用手动维护的类型清单判断
 */
public final class EntityClassifier {

    /** 中立生物类型清单（1.12.x） */
    private static final Set<EntityType> NEUTRAL_TYPES = new HashSet<EntityType>(Arrays.asList(
            EntityType.ENDERMAN,
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.PIG_ZOMBIE,
            EntityType.WOLF,
            EntityType.POLAR_BEAR,
            EntityType.LLAMA,
            EntityType.IRON_GOLEM,
            EntityType.SNOWMAN
    ));

    /** 分类枚举 */
    public enum Category {
        FRIENDLY,
        NEUTRAL,
        HOSTILE,
        OTHER
    }

    private EntityClassifier() {
    }

    /**
     * 判断生物所属分类。
     *
     * @param entity 目标实体
     * @return 分类结果
     */
    public static Category classify(Entity entity) {
        if (entity == null) {
            return Category.OTHER;
        }

        EntityType type = entity.getType();

        // 中立优先判断：部分中立生物同时实现了 Animals 接口（如狼、羊驼），
        // 需要先按中立清单归类，避免被误判为友好。
        if (NEUTRAL_TYPES.contains(type)) {
            return Category.NEUTRAL;
        }

        if (entity instanceof Monster) {
            return Category.HOSTILE;
        }

        if (entity instanceof Animals) {
            return Category.FRIENDLY;
        }

        return Category.OTHER;
    }

    /**
     * 判断该分类是否被允许显示。
     */
    public static boolean isAllowed(Category category, boolean showFriendly,
                                    boolean showNeutral, boolean showHostile) {
        if (category == Category.FRIENDLY) {
            return showFriendly;
        }
        if (category == Category.NEUTRAL) {
            return showNeutral;
        }
        if (category == Category.HOSTILE) {
            return showHostile;
        }
        return false;
    }
}
