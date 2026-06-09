package com.fushu.mmceguiext.client.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.client.model.CustomAEBusBakedModel;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusGameRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusGameRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusGameRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID, value = Side.CLIENT)
public final class CustomAEBusClientRegistry {
    private static final ResourceLocation ITEM_INPUT_BLOCKSTATE_MODEL = new ResourceLocation("modularmachinery", "blockmeiteminputbus");
    private static final ResourceLocation ITEM_INPUT_BAKED_MODEL = new ResourceLocation("modularmachinery", "block/blockmeiteminputbus");
    private static final ResourceLocation MIXED_OUTPUT_BLOCKSTATE_MODEL = new ResourceLocation("modularmachinery", "blockmeitemoutputbus");
    private static final ResourceLocation MIXED_OUTPUT_BAKED_MODEL = new ResourceLocation("modularmachinery", "block/blockmeitemoutputbus");
    private static final Map<ModelResourceLocation, ResourceLocation> WRAP_TARGETS = new LinkedHashMap<ModelResourceLocation, ResourceLocation>();

    private CustomAEBusClientRegistry() {
    }

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        WRAP_TARGETS.clear();
        registerItemInputBuses();
        registerMixedInputBuses();
        registerMixedOutputBuses();
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for (Map.Entry<ModelResourceLocation, ResourceLocation> entry : WRAP_TARGETS.entrySet()) {
            wrapModel(event, entry.getKey(), entry.getValue());
        }
    }

    private static void registerItemInputBuses() {
        for (BlockCustomMEItemInputBus block : CustomAEItemInputBusGameRegistry.getRegisteredBlocks().values()) {
            CustomAEItemInputBusRegistry.Def def = block.getDefinition();
            String model = def == null || def.block == null ? null : def.block.model;
            if ((model == null || model.trim().isEmpty()) && def != null) {
                model = def.blockModel;
            }
            registerBusModel(block, model, ITEM_INPUT_BLOCKSTATE_MODEL, ITEM_INPUT_BAKED_MODEL);
        }
    }

    private static void registerMixedInputBuses() {
        for (BlockCustomAEMixedInputBus block : CustomAEMixedInputBusGameRegistry.getRegisteredBlocks().values()) {
            CustomAEMixedInputBusRegistry.Def def = block.getDefinition();
            registerBusModel(block, def == null ? null : def.blockModel, ITEM_INPUT_BLOCKSTATE_MODEL, ITEM_INPUT_BAKED_MODEL);
        }
    }

    private static void registerMixedOutputBuses() {
        for (BlockCustomAEMixedOutputBus block : CustomAEMixedOutputBusGameRegistry.getRegisteredBlocks().values()) {
            CustomAEMixedOutputBusRegistry.Def def = block.getDefinition();
            registerBusModel(block, def == null ? null : def.blockModel, MIXED_OUTPUT_BLOCKSTATE_MODEL, MIXED_OUTPUT_BAKED_MODEL);
        }
    }

    private static void registerBusModel(Block block, String model, ResourceLocation fallbackBlockstate, ResourceLocation bakedModel) {
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return;
        }
        final ModelResourceLocation location = CustomBlockModelResolver.resolve(model, fallbackBlockstate, "normal");
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return location;
            }
        });
        ModelLoader.setCustomModelResourceLocation(item, 0, location);
        WRAP_TARGETS.put(location, bakedModel);
        WRAP_TARGETS.put(new ModelResourceLocation(location.getNamespace() + ":" + location.getPath(), "inventory"), bakedModel);
    }

    private static void wrapModel(ModelBakeEvent event, ModelResourceLocation location, ResourceLocation bakedModel) {
        IBakedModel baked = event.getModelRegistry().getObject(location);
        if (baked != null && !(baked instanceof CustomAEBusBakedModel)) {
            event.getModelRegistry().putObject(location, new CustomAEBusBakedModel(baked, bakedModel));
        }
    }
}
