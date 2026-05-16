package com.fushu.mmceguiext;

import com.fushu.mmceguiext.client.ClientGuiEventHandler;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.client.gui.GuiFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.integration.theoneprobe.CustomHatchInfoProvider;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.network.PktControllerSmartInterfaceUpdate;
import hellfirepvp.modularmachinery.common.base.Mods;
import mcjty.theoneprobe.TheOneProbe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
    modid = MMCEGuiExt.MODID,
    name = MMCEGuiExt.NAME,
    version = MMCEGuiExt.VERSION,
    dependencies = "required-after:modularmachinery"
)
public class MMCEGuiExt {
    public static final String MODID = "mmceguiext";
    public static final String NAME = "Modular Machinery: Community Edition Gui Edit";
    public static final String VERSION = "1.0.1-beta";
    public static final int GUI_CUSTOM_HATCH = 1;
    public static final SimpleNetworkWrapper NET_CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    private static int nextPacketId = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NET_CHANNEL.registerMessage(
            PktControllerSmartInterfaceUpdate.class,
            PktControllerSmartInterfaceUpdate.class,
            nextPacketId++,
            Side.SERVER
        );
        if (event.getSide().isClient() && MMCEGuiExtConfig.novaEngCoreCompatibilityMode) {
            MachineGuiStyleManager.preloadAndPinCache();
        }
        CustomHatchRegistry.loadAll();
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClientGuiEventHandler());
        }
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Mods.TOP.isPresent()) {
            TheOneProbe.theOneProbeImp.registerProvider(new CustomHatchInfoProvider());
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

    private static class GuiHandler implements IGuiHandler {
        @Override
        public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            if (id != GUI_CUSTOM_HATCH) {
                return null;
            }
            if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = tile.getDefinition();
            return def == null ? null : new ContainerFluidProcessorHatchCustom(tile, player, def);
        }

        @Override
        public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            if (id != GUI_CUSTOM_HATCH) {
                return null;
            }
            if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = tile.getDefinition();
            return def == null ? null : new GuiFluidProcessorHatchCustom(tile, player, def);
        }
    }
}

