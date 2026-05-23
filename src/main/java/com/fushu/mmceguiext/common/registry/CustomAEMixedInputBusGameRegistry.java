package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
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
public final class CustomAEMixedInputBusGameRegistry {
    private static final Map<String, BlockCustomAEMixedInputBus> BLOCKS = new LinkedHashMap<String, BlockCustomAEMixedInputBus>();

    private CustomAEMixedInputBusGameRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        BLOCKS.clear();
        List<CustomAEMixedInputBusRegistry.Def> defs = CustomAEMixedInputBusRegistry.getCached();
        if (defs.isEmpty()) {
            defs = CustomAEMixedInputBusRegistry.loadAll();
        }
        for (CustomAEMixedInputBusRegistry.Def def : defs) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            String path = normalizePath(def.id);
            BlockCustomAEMixedInputBus block = new BlockCustomAEMixedInputBus(def);
            event.getRegistry().register(block);
            BLOCKS.put(path, block);
        }
        GameRegistry.registerTileEntity(TileCustomAEMixedInputBus.class, MMCEGuiExt.MODID + ":custom_ae_mixed_input_bus");
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (BlockCustomAEMixedInputBus block : BLOCKS.values()) {
            CustomAEMixedInputBusRegistry.Def def = block.getDefinition();
            if (def != null) {
                event.getRegistry().register(new ItemBlockCustomAEMixedInputBus(block, def));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        for (Map.Entry<String, BlockCustomAEMixedInputBus> entry : BLOCKS.entrySet()) {
            BlockCustomAEMixedInputBus block = entry.getValue();
            Item item = Item.getItemFromBlock(block);
            if (item == null) {
                continue;
            }
            ModelResourceLocation location = new ModelResourceLocation(new ResourceLocation("modularmachinery", "blockmeiteminputbus"), "normal");
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return location;
                }
            });
            ModelLoader.setCustomModelResourceLocation(item, 0, location);
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
