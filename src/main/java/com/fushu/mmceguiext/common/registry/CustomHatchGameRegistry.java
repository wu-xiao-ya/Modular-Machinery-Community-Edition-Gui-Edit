package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.item.ItemBlockCustomHatch;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class CustomHatchGameRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Map<String, BlockCustomHatch> BLOCKS = new LinkedHashMap<String, BlockCustomHatch>();
    private static final Map<String, ModelBinding> MODEL_BINDINGS = new LinkedHashMap<String, ModelBinding>();

    private CustomHatchGameRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        BLOCKS.clear();
        MODEL_BINDINGS.clear();
        List<CustomHatchRegistry.CustomHatchDef> defs = CustomHatchRegistry.getCached();
        if (defs.isEmpty()) {
            defs = CustomHatchRegistry.loadAll();
        }
        for (CustomHatchRegistry.CustomHatchDef def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            String path = normalizePath(def.id);
            if (!isValidRegistryPath(path)) {
                LOGGER.warn("Skipping custom hatch with invalid id '{}'.", def.id);
                continue;
            }
            if (BLOCKS.containsKey(path)) {
                LOGGER.warn("Skipping duplicate custom hatch id '{}'.", def.id);
                continue;
            }
            BlockCustomHatch block = new BlockCustomHatch(def);
            event.getRegistry().register(block);
            BLOCKS.put(path, block);
            MODEL_BINDINGS.put(path, resolveModelBinding(def));
        }
        GameRegistry.registerTileEntity(TileCustomHatch.class, MMCEGuiExt.MODID + ":custom_hatch_tile");
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (Map.Entry<String, BlockCustomHatch> entry : BLOCKS.entrySet()) {
            BlockCustomHatch block = entry.getValue();
            CustomHatchRegistry.CustomHatchDef def = block.getDefinition();
            if (def == null) {
                continue;
            }
            event.getRegistry().register(new ItemBlockCustomHatch(block, def));
        }
    }

    public static BlockCustomHatch getBlock(@SuppressWarnings("SameParameterValue") String id) {
        return BLOCKS.get(normalizePath(id));
    }

    public static Map<String, BlockCustomHatch> getRegisteredBlocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }

    public static ModelBinding getModelBinding(String id) {
        return MODEL_BINDINGS.get(normalizePath(id));
    }

    private static String normalizePath(String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value;
    }

    private static boolean isValidRegistryPath(String path) {
        return path != null
            && !path.trim().isEmpty()
            && !path.contains("..")
            && path.matches("[a-z0-9_./-]+");
    }

    private static ModelBinding resolveModelBinding(CustomHatchRegistry.CustomHatchDef def) {
        String model = null;
        if (def != null && def.block != null && def.block.model != null && !def.block.model.trim().isEmpty()) {
            model = def.block.model.trim();
        } else if (def != null && def.blockModel != null && !def.blockModel.trim().isEmpty()) {
            model = def.blockModel.trim();
        }
        ModelBinding location = parseModelBinding(model);
        if (location == null) {
            location = new ModelBinding(new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch"), "facing=north");
        }
        return location;
    }

    @Nullable
    private static ModelBinding parseModelBinding(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        if (value.isEmpty()) {
            return null;
        }
        String variant = "normal";
        if (value.contains("#")) {
            int split = value.indexOf('#');
            variant = value.substring(split + 1).trim();
            value = value.substring(0, split);
        } else if (value.contains("[")) {
            int split = value.indexOf('[');
            int end = value.lastIndexOf(']');
            if (end > split) {
                variant = value.substring(split + 1, end).trim();
                value = value.substring(0, split);
            }
        }
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - 5);
        }
        if (value.startsWith("assets/")) {
            String rest = value.substring("assets/".length());
            int slash = rest.indexOf('/');
            if (slash > 0) {
                String namespace = rest.substring(0, slash);
                String path = normalizeModelPath(rest.substring(slash + 1));
                return new ModelBinding(new ResourceLocation(namespace, path), normalizeVariant(variant));
            }
        }
        if (value.contains(":")) {
            String[] split = value.split(":", 2);
            String namespace = split[0];
            String path = normalizeModelPath(split[1]);
            return new ModelBinding(new ResourceLocation(namespace, path), normalizeVariant(variant));
        }
        return new ModelBinding(new ResourceLocation(MMCEGuiExt.MODID, normalizeModelPath(value)), normalizeVariant(variant));
    }

    private static String normalizeVariant(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "normal";
        }
        return raw.trim();
    }

    private static String normalizeModelPath(String raw) {
        String path = raw == null ? "" : raw.trim();
        while (path.startsWith("blockstates/")) {
            path = path.substring("blockstates/".length());
        }
        while (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
        return path;
    }

    public static boolean usesFacingVariants(ResourceLocation location) {
        if (location == null || !MMCEGuiExt.MODID.equals(location.getNamespace())) {
            return false;
        }
        String path = location.getPath();
        return "custom_hatch".equals(path)
            || "custom_gas_input_hatch".equals(path)
            || "custom_gas_output_hatch".equals(path);
    }

    public static final class ModelBinding {
        public final ResourceLocation location;
        public final String variant;

        public ModelBinding(ResourceLocation location, String variant) {
            this.location = location;
            this.variant = variant;
        }
    }
}
