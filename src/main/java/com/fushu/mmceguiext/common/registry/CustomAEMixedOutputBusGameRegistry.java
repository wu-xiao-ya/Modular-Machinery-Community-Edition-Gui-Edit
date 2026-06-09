package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.client.model.CustomAEBusBakedModel;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
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
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class CustomAEMixedOutputBusGameRegistry {
    private static final Map<String, BlockCustomAEMixedOutputBus> BLOCKS = new LinkedHashMap<String, BlockCustomAEMixedOutputBus>();
    private static final ResourceLocation DEFAULT_BLOCKSTATE_MODEL = new ResourceLocation("modularmachinery", "blockmeitemoutputbus");
    private static final ResourceLocation DEFAULT_BAKED_MODEL = new ResourceLocation("modularmachinery", "block/blockmeitemoutputbus");

    private CustomAEMixedOutputBusGameRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        BLOCKS.clear();
        List<CustomAEMixedOutputBusRegistry.Def> defs = CustomAEMixedOutputBusRegistry.getCached();
        if (defs.isEmpty()) {
            defs = CustomAEMixedOutputBusRegistry.loadAll();
        }
        for (CustomAEMixedOutputBusRegistry.Def def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            String path = normalizePath(def.id);
            BlockCustomAEMixedOutputBus block = new BlockCustomAEMixedOutputBus(def);
            event.getRegistry().register(block);
            BLOCKS.put(path, block);
        }
        GameRegistry.registerTileEntity(TileCustomAEMixedOutputBus.class, MMCEGuiExt.MODID + ":custom_ae_mixed_output_bus");
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (BlockCustomAEMixedOutputBus block : BLOCKS.values()) {
            CustomAEMixedOutputBusRegistry.Def def = block.getDefinition();
            if (def != null) {
                event.getRegistry().register(new ItemBlockCustomAEMixedOutputBus(block, def));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        for (Map.Entry<String, BlockCustomAEMixedOutputBus> entry : BLOCKS.entrySet()) {
            BlockCustomAEMixedOutputBus block = entry.getValue();
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }
            CustomAEMixedOutputBusRegistry.Def def = block.getDefinition();
            String model = def == null ? null : def.blockModel;
            ModelResourceLocation location = CustomBlockModelResolver.resolve(model, DEFAULT_BLOCKSTATE_MODEL, "normal");
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return location;
                }
            });
            ModelLoader.setCustomModelResourceLocation(item, 0, location);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        wrapModel(event, new ModelResourceLocation(DEFAULT_BLOCKSTATE_MODEL, "normal"));
        wrapModel(event, new ModelResourceLocation(DEFAULT_BLOCKSTATE_MODEL, "inventory"));
    }

    @SideOnly(Side.CLIENT)
    private static void wrapModel(ModelBakeEvent event, ModelResourceLocation location) {
        IBakedModel baked = event.getModelRegistry().getObject(location);
        if (baked != null && !(baked instanceof CustomAEBusBakedModel)) {
            event.getModelRegistry().putObject(location, new CustomAEBusBakedModel(baked, DEFAULT_BAKED_MODEL));
        }
    }

    private static String normalizePath(String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value;
    }
}
