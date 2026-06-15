package com.fushu.mmceguiext.common.registry;

import appeng.api.AEApi;
import com.fushu.mmceguiext.MMCEGuiExt;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public final class CustomCapacityCardRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Path CONFIG_DIR = resolveConfigDir();
    private static final long MAX_CONFIG_BYTES = 1024L * 1024L;
    private static final double DEFAULT_MULTIPLIER = 1.0D;
    private static final List<Entry> JSON_ENTRIES = new ArrayList<Entry>();
    private static final List<Entry> SCRIPT_ENTRIES = new CopyOnWriteArrayList<Entry>();

    private CustomCapacityCardRegistry() {
    }

    public static List<Entry> loadAll() {
        JSON_ENTRIES.clear();
        if (!Files.isDirectory(CONFIG_DIR)) {
            return getEntries();
        }
        try (Stream<Path> stream = Files.list(CONFIG_DIR)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(CustomCapacityCardRegistry::load);
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan custom capacity card dir {}: {}", CONFIG_DIR, ex.getMessage());
        }
        return getEntries();
    }

    public static void clearScriptEntries() {
        SCRIPT_ENTRIES.clear();
    }

    public static boolean registerScriptEntry(String id,
                                              String itemId,
                                              int meta,
                                              @Nullable String nbt,
                                              double multiplier,
                                              long flatFluid,
                                              long flatGas,
                                              boolean matchNbt) {
        Entry entry = buildEntry(id, itemId, meta, nbt, multiplier, flatFluid, flatGas, matchNbt, "CraftTweaker");
        if (entry == null) {
            return false;
        }
        SCRIPT_ENTRIES.add(entry);
        return true;
    }

    public static List<Entry> getEntries() {
        List<Entry> out = new ArrayList<Entry>(JSON_ENTRIES.size() + SCRIPT_ENTRIES.size());
        out.addAll(JSON_ENTRIES);
        out.addAll(SCRIPT_ENTRIES);
        return out;
    }

    public static CapacityModifier resolveModifier(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return CapacityModifier.EMPTY;
        }
        CapacityModifier aeModifier = resolveOfficialCapacityCard(stack);
        if (!aeModifier.isEmpty()) {
            return aeModifier;
        }
        for (Entry entry : getEntries()) {
            if (entry.matches(stack)) {
                return entry.modifier;
            }
        }
        return CapacityModifier.EMPTY;
    }

    private static void load(Path path) {
        try {
            if (Files.size(path) > MAX_CONFIG_BYTES) {
                LOGGER.warn("Skipping custom capacity card config {} because it is larger than {} bytes.", path, MAX_CONFIG_BYTES);
                return;
            }
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonElement root = new JsonParser().parse(text);
            if (root == null || root.isJsonNull()) {
                return;
            }
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    if (!array.get(i).isJsonObject()) {
                        continue;
                    }
                    Entry entry = parseEntry(array.get(i).getAsJsonObject(), path.toString() + "#" + i);
                    if (entry != null) {
                        JSON_ENTRIES.add(entry);
                    }
                }
                return;
            }
            Entry entry = parseEntry(root.getAsJsonObject(), path.toString());
            if (entry != null) {
                JSON_ENTRIES.add(entry);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse custom capacity card config {}", path, ex);
        }
    }

    @Nullable
    private static Entry parseEntry(JsonObject obj, String source) {
        String itemId = getString(obj, "item");
        if (itemId == null || itemId.trim().isEmpty()) {
            LOGGER.warn("Skipping custom capacity card entry in {} because `item` is missing.", source);
            return null;
        }
        String id = getString(obj, "id");
        int meta = Math.max(0, getInt(obj, "meta", getInt(obj, "damage", 0)));
        String nbt = getString(obj, "nbt");
        boolean matchNbt = getBoolean(obj, "matchNbt", nbt != null && !nbt.trim().isEmpty());
        double multiplier = sanitizeMultiplier(getDouble(obj, "multiplier", DEFAULT_MULTIPLIER), source);
        long flatFluid = Math.max(0L, getLong(obj, "flatFluid", getLong(obj, "flatFluidCapacity", 0L)));
        long flatGas = Math.max(0L, getLong(obj, "flatGas", getLong(obj, "flatGasCapacity", flatFluid)));
        if (multiplier == DEFAULT_MULTIPLIER && flatFluid <= 0L && flatGas <= 0L) {
            LOGGER.warn("Skipping custom capacity card entry {} because it has no effective modifier.", source);
            return null;
        }
        return buildEntry(id, itemId, meta, nbt, multiplier, flatFluid, flatGas, matchNbt, source);
    }

    @Nullable
    private static Entry buildEntry(@Nullable String id,
                                    String itemId,
                                    int meta,
                                    @Nullable String nbt,
                                    double multiplier,
                                    long flatFluid,
                                    long flatGas,
                                    boolean matchNbt,
                                    String source) {
        ResourceLocation itemKey;
        try {
            itemKey = new ResourceLocation(itemId.trim());
        } catch (RuntimeException ex) {
            LOGGER.warn("Skipping custom capacity card entry from {} because item id `{}` is invalid.", source, itemId);
            return null;
        }
        Item item = Item.REGISTRY.getObject(itemKey);
        if (item == null) {
            LOGGER.warn("Skipping custom capacity card entry from {} because item `{}` was not found.", source, itemId);
            return null;
        }
        NBTTagCompound tag = parseNbt(nbt, source);
        if (nbt != null && tag == null) {
            return null;
        }
        Entry entry = new Entry();
        entry.id = id == null || id.trim().isEmpty() ? itemKey.toString() : id.trim();
        entry.itemId = itemKey;
        entry.item = item;
        entry.meta = meta;
        entry.matchNbt = matchNbt && tag != null;
        entry.nbt = tag;
        entry.modifier = new CapacityModifier(multiplier, flatFluid, flatGas);
        return entry;
    }

    @Nullable
    private static NBTTagCompound parseNbt(@Nullable String raw, String source) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return JsonToNBT.getTagFromJson(raw.trim());
        } catch (NBTException ex) {
            LOGGER.warn("Skipping custom capacity card entry from {} because NBT is invalid: {}", source, ex.getMessage());
            return null;
        }
    }

    private static CapacityModifier resolveOfficialCapacityCard(ItemStack stack) {
        try {
            ItemStack official = AEApi.instance().definitions().materials().cardCapacity().maybeStack(1).orElse(ItemStack.EMPTY);
            if (!official.isEmpty() && isSameItem(official, stack)) {
                return new CapacityModifier(2.0D, 0L, 0L);
            }
        } catch (Throwable ignored) {
        }
        return CapacityModifier.EMPTY;
    }

    private static boolean isSameItem(ItemStack expected, ItemStack actual) {
        return !expected.isEmpty()
            && !actual.isEmpty()
            && expected.getItem() == actual.getItem()
            && (!expected.getHasSubtypes() || expected.getMetadata() == actual.getMetadata());
    }

    private static double sanitizeMultiplier(double raw, String source) {
        if (!Double.isFinite(raw) || raw <= 0.0D) {
            LOGGER.warn("Invalid capacity multiplier {} in {}; using {}.", raw, source, DEFAULT_MULTIPLIER);
            return DEFAULT_MULTIPLIER;
        }
        return raw;
    }

    @Nullable
    private static String getString(@Nullable JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean getBoolean(@Nullable JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static int getInt(@Nullable JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static long getLong(@Nullable JsonObject obj, String key, long fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsLong();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static double getDouble(@Nullable JsonObject obj, String key, double fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsDouble();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static Path resolveConfigDir() {
        Path dir = Loader.instance().getConfigDir().toPath().resolve("mmceguiext").resolve("capacity_cards");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    public static final class Entry {
        public String id;
        public ResourceLocation itemId;
        public Item item;
        public int meta;
        public boolean matchNbt;
        @Nullable
        public NBTTagCompound nbt;
        public CapacityModifier modifier = CapacityModifier.EMPTY;

        public boolean matches(@Nullable ItemStack stack) {
            if (stack == null || stack.isEmpty() || this.item == null || stack.getItem() != this.item) {
                return false;
            }
            if (stack.getHasSubtypes() && stack.getMetadata() != this.meta) {
                return false;
            }
            if (!this.matchNbt) {
                return true;
            }
            return this.nbt != null && this.nbt.equals(stack.getTagCompound());
        }
    }

    public static final class CapacityModifier {
        public static final CapacityModifier EMPTY = new CapacityModifier(DEFAULT_MULTIPLIER, 0L, 0L);
        public final double multiplier;
        public final long flatFluid;
        public final long flatGas;

        public CapacityModifier(double multiplier, long flatFluid, long flatGas) {
            this.multiplier = multiplier > 0.0D ? multiplier : DEFAULT_MULTIPLIER;
            this.flatFluid = Math.max(0L, flatFluid);
            this.flatGas = Math.max(0L, flatGas);
        }

        public boolean isEmpty() {
            return this.multiplier == DEFAULT_MULTIPLIER && this.flatFluid <= 0L && this.flatGas <= 0L;
        }
    }
}
