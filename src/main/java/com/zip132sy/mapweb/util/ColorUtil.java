package com.zip132sy.mapweb.util;

/**
 * 颜色处理工具类。
 * 负责把基础方块颜色根据高度做明暗处理，模拟俯视阴影效果。
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    /**
     * 根据基础颜色和高度，计算带阴影的最终颜色。
     * 高度越高越亮，越低越暗，形成立体感。
     *
     * @param baseRgb 基础 RGB（0xRRGGBB）
     * @param height  方块所在高度（Y 坐标）
     * @param minY    当前渲染区域的最低高度
     * @param maxY    当前渲染区域的最高高度
     * @return 处理后的 RGB（0xRRGGBB）
     */
    public static int applyHeightShade(int baseRgb, int height, int minY, int maxY) {
        int r = (baseRgb >> 16) & 0xFF;
        int g = (baseRgb >> 8) & 0xFF;
        int b = baseRgb & 0xFF;

        // 计算高度比例，范围 [0, 1]
        double ratio;
        if (maxY <= minY) {
            ratio = 0.5D;
        } else {
            ratio = (double) (height - minY) / (double) (maxY - minY);
        }

        // 阴影系数：0.65 ~ 1.15，避免过暗或过曝
        double factor = 0.65D + ratio * 0.5D;

        r = clamp((int) (r * factor));
        g = clamp((int) (g * factor));
        b = clamp((int) (b * factor));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * 把 RGB 拆成 ARGB 整数，用于 BufferedImage.setRGB。
     */
    public static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }
}
