package com.fushu.mmceguiext;

import com.fushu.mmceguiext.client.ClientGuiEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(
    modid = MMCEGuiExt.MODID,
    name = MMCEGuiExt.NAME,
    version = MMCEGuiExt.VERSION,
    dependencies = "required-after:modularmachinery"
)
public class MMCEGuiExt {
    public static final String MODID = "mmceguiext";
    public static final String NAME = "Modular Machinery: Community Edition Gui Edit";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClientGuiEventHandler());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ConfigSyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (MODID.equals(event.getModID())) {
                ConfigManager.sync(MODID, Config.Type.INSTANCE);
            }
        }
    }
}
