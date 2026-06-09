package com.fushu.mmceguiext.client.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.client.model.CustomHatchBakedModel;
import com.fushu.mmceguiext.client.model.CustomHatchModelRegistry;
import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.registry.CustomHatchGameRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID, value = Side.CLIENT)
public final class CustomHatchClientRegistry {
    private static final ResourceLocation DEFAULT_MODEL = new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch");

    private CustomHatchClientRegistry() {
    }

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        CustomHatchModelRegistry.rebuild();
        for (Map.Entry<String, BlockCustomHatch> entry : CustomHatchGameRegistry.getRegisteredBlocks().entrySet()) {
            BlockCustomHatch block = entry.getValue();
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }
            CustomHatchGameRegistry.ModelBinding binding = CustomHatchGameRegistry.getModelBinding(entry.getKey());
            if (binding == null) {
                binding = new CustomHatchGameRegistry.ModelBinding(DEFAULT_MODEL, "facing=north");
            }
            final CustomHatchGameRegistry.ModelBinding modelBinding = binding;
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return resolveStateModelLocation(modelBinding, state);
                }
            });
            ModelLoader.setCustomModelResourceLocation(item, 0, resolveInventoryModelLocation(modelBinding));
        }
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        wrapFacingModel(event, new ResourceLocation(MMCEGuiExt.MODID, "custom_hatch"));
        wrapFacingModel(event, new ResourceLocation(MMCEGuiExt.MODID, "custom_gas_input_hatch"));
        wrapFacingModel(event, new ResourceLocation(MMCEGuiExt.MODID, "custom_gas_output_hatch"));
        for (CustomHatchGameRegistry.ModelBinding binding : CustomHatchGameRegistry.getRegisteredBlocks().keySet()
            .stream()
            .map(CustomHatchGameRegistry::getModelBinding)
            .toArray(CustomHatchGameRegistry.ModelBinding[]::new)) {
            if (binding != null && !CustomHatchGameRegistry.usesFacingVariants(binding.location)) {
                wrapCustomHatchModel(event, new ModelResourceLocation(binding.location, binding.variant));
                wrapCustomHatchModel(event, new ModelResourceLocation(binding.location, "inventory"));
            }
        }
    }

    private static void wrapFacingModel(ModelBakeEvent event, ResourceLocation model) {
        wrapCustomHatchModel(event, new ModelResourceLocation(model, "facing=north"));
        wrapCustomHatchModel(event, new ModelResourceLocation(model, "facing=south"));
        wrapCustomHatchModel(event, new ModelResourceLocation(model, "facing=west"));
        wrapCustomHatchModel(event, new ModelResourceLocation(model, "facing=east"));
        wrapCustomHatchModel(event, new ModelResourceLocation(model, "inventory"));
    }

    private static void wrapCustomHatchModel(ModelBakeEvent event, ModelResourceLocation location) {
        IBakedModel baked = event.getModelRegistry().getObject(location);
        if (baked != null && !(baked instanceof CustomHatchBakedModel)) {
            event.getModelRegistry().putObject(location, new CustomHatchBakedModel(baked, resolveBakedModelLocation(location)));
        }
    }

    private static ResourceLocation resolveBakedModelLocation(ModelResourceLocation location) {
        if (location == null) {
            return new ResourceLocation(MMCEGuiExt.MODID, "block/custom_hatch");
        }
        String path = location.getPath();
        return new ResourceLocation(location.getNamespace(), path.startsWith("block/") ? path : "block/" + path);
    }

    private static ModelResourceLocation resolveStateModelLocation(CustomHatchGameRegistry.ModelBinding binding, IBlockState state) {
        if (binding == null) {
            return new ModelResourceLocation(DEFAULT_MODEL, "facing=north");
        }
        if (CustomHatchGameRegistry.usesFacingVariants(binding.location)) {
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

    private static ModelResourceLocation resolveInventoryModelLocation(CustomHatchGameRegistry.ModelBinding binding) {
        if (binding == null) {
            return new ModelResourceLocation(DEFAULT_MODEL, "inventory");
        }
        return new ModelResourceLocation(binding.location, "inventory");
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
}
