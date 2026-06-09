package com.fushu.mmceguiext.client;

import com.fushu.mmceguiext.MMCEGuiExt;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = MMCEGuiExt.MODID, value = Side.CLIENT)
public final class ClientConfigSyncHandler {
    private ClientConfigSyncHandler() {
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MMCEGuiExt.MODID.equals(event.getModID())) {
            ConfigManager.sync(MMCEGuiExt.MODID, Config.Type.INSTANCE);
        }
    }
}
