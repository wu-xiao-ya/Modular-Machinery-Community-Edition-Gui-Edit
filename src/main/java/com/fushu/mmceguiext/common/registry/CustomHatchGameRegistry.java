package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.client.model.CustomHatchBakedModel;
import com.fushu.mmceguiext.client.model.CustomHatchModelRegistry;
import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.item.ItemBlockCustomHatch;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class CustomHatchGameRegistry {
    private static final Map<String, BlockCustomHatch> BLOCKS = new LinkedHashMap<String, BlockCustomHatch>();
    private static final Map<String, ModelBinding> MODEL_BINDINGS = new LinkedHashMap<String, ModelBinding>();

    private CustomHatchGameRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        BLOCKS.clear();
        MODEL_BINDINGS.clear();
        List<CustomHatchRegistry.CustomHatchDef> defs = CustomHatchRegistry.getRegistered();
        if (defs.isEmpty()) {
            defs = CustomHatchRegistry.getCached();
        }
        for (CustomHatchRegistry.CustomHatchDef def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            String path = normalizePath(def.id);
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

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        CustomHatchModelRegistry.rebuild();
        for (Map.Entry<String, BlockCustomHatch> entry : BLOCKS.entrySet()) {
            BlockCustomHatch block = entry.getValue();
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }
            ModelBinding binding = MODEL_BINDINGS.get(entry.getKey());
            if (binding == null) {
                binding = new ModelBinding(new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch"), "normal");
            }
            final ModelBinding modelBinding = binding;
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return resolveStateModelLocation(modelBinding, state);
                }
            });
            ModelLoader.setCustomModelResourceLocation(item, 0, resolveInventoryModelLocation(modelBinding));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for (Map.Entry<String, ModelBinding> entry : MODEL_BINDINGS.entrySet()) {
            ModelBinding binding = entry.getValue();
            if (binding == null) {
                continue;
            }
            ModelResourceLocation location = binding.toModelResourceLocation();
            IBakedModel baked = event.getModelRegistry().getObject(location);
            if (baked != null) {
                ResourceLocation model = binding.location;
                if (MMCEGuiExt.MODID.equals(model.getNamespace()) && "custom_hatch".equals(model.getPath())) {
                    event.getModelRegistry().putObject(location, new CustomHatchBakedModel(baked));
                }
            }
        }
    }

    public static BlockCustomHatch getBlock(@SuppressWarnings("SameParameterValue") String id) {
        return BLOCKS.get(normalizePath(id));
    }

    private static String normalizePath(String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value;
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
            location = new ModelBinding(new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch"), "normal");
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

    private static ModelResourceLocation resolveStateModelLocation(ModelBinding binding, IBlockState state) {
        if (binding == null) {
            return new ModelResourceLocation(new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch"), "normal");
        }
        if (usesFacingVariants(binding.location)) {
            if (state != null && state.getPropertyKeys().contains(BlockCustomHatch.FACING)) {
                EnumFacing facing = state.getValue(BlockCustomHatch.FACING);
                return new ModelResourceLocation(binding.location, "facing=" + facing.getName());
            }
            return new ModelResourceLocation(binding.location, "facing=north");
        }
        String variant = binding.variant;
        if (state != null && state.getPropertyKeys().contains(BlockCustomHatch.FACING)) {
            EnumFacing facing = state.getValue(BlockCustomHatch.FACING);
            if (variant != null && variant.contains("facing=")) {
                variant = applyFacingVariant(variant, facing);
            }
        }
        return new ModelResourceLocation(binding.location, variant);
    }

    private static ModelResourceLocation resolveInventoryModelLocation(ModelBinding binding) {
        if (binding == null) {
            return new ModelResourceLocation(new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch"), "inventory");
        }
        return new ModelResourceLocation(binding.location, "inventory");
    }

    private static boolean usesFacingVariants(ResourceLocation location) {
        if (location == null || !MMCEGuiExt.MODID.equals(location.getNamespace())) {
            return false;
        }
        String path = location.getPath();
        return "custom_hatch".equals(path)
            || "custom_gas_input_hatch".equals(path)
            || "custom_gas_output_hatch".equals(path);
    }

    private static String applyFacingVariant(String variant, EnumFacing facing) {
        if (variant == null || variant.trim().isEmpty()) {
            return "facing=" + facing.getName();
        }
        String[] parts = variant.split(",");
        StringBuilder builder = new StringBuilder();
        boolean replaced = false;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("facing=")) {
                trimmed = "facing=" + facing.getName();
                replaced = true;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(trimmed);
        }
        if (!replaced) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append("facing=").append(facing.getName());
        }
        return builder.toString();
    }

    private static final class ModelBinding {
        private final ResourceLocation location;
        private final String variant;

        private ModelBinding(ResourceLocation location, String variant) {
            this.location = location;
            this.variant = variant;
        }

        private ModelResourceLocation toModelResourceLocation() {
            return new ModelResourceLocation(this.location, this.variant);
        }
    }
}
