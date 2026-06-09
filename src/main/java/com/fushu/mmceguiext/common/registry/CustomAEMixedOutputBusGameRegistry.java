package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class CustomAEMixedOutputBusGameRegistry {
    private static final Map<String, BlockCustomAEMixedOutputBus> BLOCKS = new LinkedHashMap<String, BlockCustomAEMixedOutputBus>();

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

    private static String normalizePath(String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value;
    }

    public static Map<String, BlockCustomAEMixedOutputBus> getRegisteredBlocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }
}
