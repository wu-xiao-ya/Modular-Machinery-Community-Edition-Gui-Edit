package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID)
public final class CustomAEItemInputBusGameRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Map<String, BlockCustomMEItemInputBus> BLOCKS = new LinkedHashMap<String, BlockCustomMEItemInputBus>();

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
            String path = CustomIdValidator.normalizePath(def.id, "");
            if (!CustomIdValidator.isValidPath(path)) {
                LOGGER.warn("Skipping custom AE item input bus with invalid id '{}'.", def.id);
                continue;
            }
            if (BLOCKS.containsKey(path)) {
                LOGGER.warn("Skipping duplicate custom AE item input bus id '{}'.", def.id);
                continue;
            }
            if (!CustomBlockIdRegistry.claim(path, "custom AE item input bus", def.id)) {
                continue;
            }
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

    public static Map<String, BlockCustomMEItemInputBus> getRegisteredBlocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }
}
