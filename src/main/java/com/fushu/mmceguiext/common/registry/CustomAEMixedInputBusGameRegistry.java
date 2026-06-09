package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
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
public final class CustomAEMixedInputBusGameRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
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
            String path = CustomIdValidator.normalizePath(def.id, "");
            if (!CustomIdValidator.isValidPath(path)) {
                LOGGER.warn("Skipping custom AE mixed input bus with invalid id '{}'.", def.id);
                continue;
            }
            if (BLOCKS.containsKey(path)) {
                LOGGER.warn("Skipping duplicate custom AE mixed input bus id '{}'.", def.id);
                continue;
            }
            if (!CustomBlockIdRegistry.claim(path, "custom AE mixed input bus", def.id)) {
                continue;
            }
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

    public static Map<String, BlockCustomAEMixedInputBus> getRegisteredBlocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }
}
