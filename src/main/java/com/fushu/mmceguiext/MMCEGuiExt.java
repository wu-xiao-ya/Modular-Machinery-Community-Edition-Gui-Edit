package com.fushu.mmceguiext;

import com.fushu.mmceguiext.client.ClientGuiEventHandler;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.client.gui.GuiFluidProcessorHatchCustom;
import com.fushu.mmceguiext.client.gui.GuiCustomAEMixedInputBus;
import com.fushu.mmceguiext.client.gui.GuiCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.integration.theoneprobe.CustomHatchInfoProvider;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.network.PktControllerButtonAction;
import com.fushu.mmceguiext.common.network.PktCustomAEMixedSlotUpdate;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
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

import javax.annotation.Nullable;

@Mod(
    modid = MMCEGuiExt.MODID,
    name = MMCEGuiExt.NAME,
    version = MMCEGuiExt.VERSION,
    dependencies = "required-after:modularmachinery"
)
public class MMCEGuiExt {
    public static final String MODID = "mmceguiext";
    public static final String NAME = "Modular Machinery: Community Edition Gui Edit";
    public static final String VERSION = "1.1.0-beta";
    public static final int GUI_CUSTOM_HATCH = 1;
    public static final int GUI_CUSTOM_AE_MIXED_INPUT = 2;
    public static final int GUI_CUSTOM_AE_MIXED_OUTPUT = 3;
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
        NET_CHANNEL.registerMessage(
            PktControllerButtonAction.class,
            PktControllerButtonAction.class,
            nextPacketId++,
            Side.SERVER
        );
        NET_CHANNEL.registerMessage(
            PktCustomAEMixedSlotUpdate.class,
            PktCustomAEMixedSlotUpdate.class,
            nextPacketId++,
            Side.SERVER
        );
        if (event.getSide().isClient() && MMCEGuiExtConfig.novaEngCoreCompatibilityMode) {
            MachineGuiStyleManager.preloadAndPinCache();
        }
        CustomHatchRegistry.loadAll();
        CustomAEItemInputBusRegistry.loadAll();
        CustomAEMixedInputBusRegistry.loadAll();
        CustomAEMixedOutputBusRegistry.loadAll();
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
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomAEMixedInputBus)) {
                    return null;
                }
                TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return def == null ? null : new ContainerCustomAEMixedInputBus(tile, player);
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomAEMixedOutputBus)) {
                    return null;
                }
                TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return def == null ? null : new ContainerCustomAEMixedOutputBus(tile, player);
            }
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
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomAEMixedInputBus)) {
                    return null;
                }
                TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return def == null ? null : new GuiCustomAEMixedInputBus(tile, player, def);
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomAEMixedOutputBus)) {
                    return null;
                }
                TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return def == null ? null : new GuiCustomAEMixedOutputBus(tile, player, def);
            }
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

        @Nullable
        private com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def resolveMixedInputBusDef(World world, net.minecraft.util.math.BlockPos pos, TileCustomAEMixedInputBus tile) {
            com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = tile.getDefinition();
            if (def != null) {
                return def;
            }
            if (world.getBlockState(pos).getBlock() instanceof BlockCustomAEMixedInputBus) {
                BlockCustomAEMixedInputBus block = (BlockCustomAEMixedInputBus) world.getBlockState(pos).getBlock();
                block.ensureDefinitionId(tile);
                return block.getDefinition();
            }
            return null;
        }

        @Nullable
        private com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def resolveMixedOutputBusDef(World world, net.minecraft.util.math.BlockPos pos, TileCustomAEMixedOutputBus tile) {
            com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def def = tile.getDefinition();
            if (def != null) {
                return def;
            }
            if (world.getBlockState(pos).getBlock() instanceof BlockCustomAEMixedOutputBus) {
                BlockCustomAEMixedOutputBus block = (BlockCustomAEMixedOutputBus) world.getBlockState(pos).getBlock();
                block.ensureDefinitionId(tile);
                return block.getDefinition();
            }
            return null;
        }
    }
}

