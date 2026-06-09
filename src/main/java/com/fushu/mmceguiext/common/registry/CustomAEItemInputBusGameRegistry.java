package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.client.model.CustomAEBusBakedModel;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
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
public final class CustomAEItemInputBusGameRegistry {
    private static final Map<String, BlockCustomMEItemInputBus> BLOCKS = new LinkedHashMap<String, BlockCustomMEItemInputBus>();
    private static final ResourceLocation DEFAULT_BLOCKSTATE_MODEL = new ResourceLocation("modularmachinery", "blockmeiteminputbus");
    private static final ResourceLocation DEFAULT_BAKED_MODEL = new ResourceLocation("modularmachinery", "block/blockmeiteminputbus");

    private CustomAEItemInputBusGameRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        BLOCKS.clear();
        List<CustomAEItemInputBusRegistry.Def> defs = CustomAEItemInputBusRegistry.getCached();
        if (defs.isEmpty()) {
            defs = CustomAEItemInputBusRegistry.loadAll();
        }
        for (CustomAEItemInputBusRegistry.Def def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            String path = normalizePath(def.id);
            BlockCustomMEItemInputBus block = new BlockCustomMEItemInputBus(def);
            event.getRegistry().register(block);
            BLOCKS.put(path, block);
        }
        GameRegistry.registerTileEntity(TileCustomMEItemInputBus.class, MMCEGuiExt.MODID + ":custom_me_item_input_bus");
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (BlockCustomMEItemInputBus block : BLOCKS.values()) {
            CustomAEItemInputBusRegistry.Def def = block.getDefinition();
            if (def != null) {
                event.getRegistry().register(new ItemBlockCustomMEItemInputBus(block, def));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        for (Map.Entry<String, BlockCustomMEItemInputBus> entry : BLOCKS.entrySet()) {
            BlockCustomMEItemInputBus block = entry.getValue();
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }
            CustomAEItemInputBusRegistry.Def def = block.getDefinition();
            String model = def == null || def.block == null ? null : def.block.model;
            if ((model == null || model.trim().isEmpty()) && def != null) {
                model = def.blockModel;
            }
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
