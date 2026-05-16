package com.fushu.mmceguiext.client.gui;

import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import net.minecraft.util.math.MathHelper;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemBusGuiLayout {
    private static final Map<ItemBusSize, List<Point>> DEFAULT_SLOT_LAYOUTS = createDefaultLayouts();

    private ItemBusGuiLayout() {
    }

    public static List<Point> getDefaultSlots(ItemBusSize size) {
        List<Point> layout = DEFAULT_SLOT_LAYOUTS.get(size);
        return layout == null ? Collections.<Point>emptyList() : layout;
    }

    public static List<Point> parseLayout(String raw, ItemBusSize size) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        String[] pairs = trimmed.split(";");
        List<Point> result = new ArrayList<Point>(pairs.length);
        for (String pair : pairs) {
            String token = pair == null ? "" : pair.trim();
            if (token.isEmpty()) {
                continue;
            }
            String[] xy = token.split(":");
            if (xy.length != 2) {
                return Collections.emptyList();
            }
            try {
                int x = Integer.parseInt(xy[0].trim());
                int y = Integer.parseInt(xy[1].trim());
                result.add(new Point(x, y));
            } catch (NumberFormatException ignored) {
                return Collections.emptyList();
            }
        }
        return result.size() == size.getSlotCount() ? result : Collections.<Point>emptyList();
    }

    public static List<Point> resolveConfiguredLayout(String[] configured, ItemBusSize size) {
        if (configured == null) {
            return getDefaultSlots(size);
        }
        String key = size.name().toLowerCase(Locale.ROOT);
        for (String entry : configured) {
            if (entry == null) {
                continue;
            }
            int split = entry.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String name = entry.substring(0, split).trim().toLowerCase(Locale.ROOT);
            if (!key.equals(name)) {
                continue;
            }
            List<Point> parsed = parseLayout(entry.substring(split + 1), size);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return getDefaultSlots(size);
    }

    public static String resolveConfiguredValue(String[] configured, String key, String fallback) {
        if (configured != null) {
            String normalizedKey = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            for (String entry : configured) {
                if (entry == null) {
                    continue;
                }
                int split = entry.indexOf('=');
                if (split <= 0) {
                    continue;
                }
                String name = entry.substring(0, split).trim().toLowerCase(Locale.ROOT);
                if (!normalizedKey.equals(name)) {
                    continue;
                }
                String value = entry.substring(split + 1).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return fallback;
    }

    public static List<Point> resolveConfiguredLayout(String configured, int slotCount, List<Point> fallback) {
        List<Point> parsed = parseLayout(configured, slotCount);
        return parsed.isEmpty() ? fallback : parsed;
    }

    public static List<Point> parseLayout(String raw, int slotCount) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        String[] pairs = trimmed.split(";");
        List<Point> result = new ArrayList<Point>(pairs.length);
        for (String pair : pairs) {
            String token = pair == null ? "" : pair.trim();
            if (token.isEmpty()) {
                continue;
            }
            String[] xy = token.split(":");
            if (xy.length != 2) {
                return Collections.emptyList();
            }
            try {
                int x = Integer.parseInt(xy[0].trim());
                int y = Integer.parseInt(xy[1].trim());
                result.add(new Point(x, y));
            } catch (NumberFormatException ignored) {
                return Collections.emptyList();
            }
        }
        return result.size() == slotCount ? result : Collections.<Point>emptyList();
    }

    public static int estimateTextureWidth(List<Point> slots, int fallback) {
        int max = fallback;
        for (Point point : slots) {
            max = Math.max(max, point.x + 18 + 7);
        }
        return MathHelper.clamp(max, 16, 4096);
    }

    public static int estimateTextureHeight(List<Point> slots, int fallback) {
        int max = fallback;
        for (Point point : slots) {
            max = Math.max(max, point.y + 18 + 84);
        }
        return MathHelper.clamp(max, 16, 4096);
    }

    private static Map<ItemBusSize, List<Point>> createDefaultLayouts() {
        Map<ItemBusSize, List<Point>> layouts = new EnumMap<ItemBusSize, List<Point>>(ItemBusSize.class);
        layouts.put(ItemBusSize.TINY, immutable(points(81, 30)));
        layouts.put(ItemBusSize.SMALL, immutable(points(70, 18, 88, 18, 70, 36, 88, 36)));
        layouts.put(ItemBusSize.NORMAL, immutable(grid(61, 18, 3, 2)));
        layouts.put(ItemBusSize.REINFORCED, immutable(grid(61, 13, 3, 3)));
        layouts.put(ItemBusSize.BIG, immutable(grid(52, 18, 4, 3)));
        layouts.put(ItemBusSize.HUGE, immutable(grid(53, 8, 4, 4)));
        layouts.put(ItemBusSize.LUDICROUS, immutable(grid(17, 8, 8, 4)));
        return layouts;
    }

    private static List<Point> grid(int startX, int startY, int columns, int rows) {
        List<Point> points = new ArrayList<Point>(columns * rows);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                points.add(new Point(startX + x * 18, startY + y * 18));
            }
        }
        return points;
    }

    private static List<Point> points(int... coords) {
        List<Point> points = new ArrayList<Point>(coords.length / 2);
        for (int i = 0; i + 1 < coords.length; i += 2) {
            points.add(new Point(coords[i], coords[i + 1]));
        }
        return points;
    }

    private static List<Point> immutable(List<Point> points) {
        return Collections.unmodifiableList(points);
    }
}
