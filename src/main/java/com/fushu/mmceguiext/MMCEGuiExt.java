package com.fushu.mmceguiext;

import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.container.ContainerCustomMEItemInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.network.PktControllerButtonAction;
import com.fushu.mmceguiext.common.network.PktCustomAEMixedSlotUpdate;
import com.fushu.mmceguiext.common.network.PktCustomMEItemInputBusInvAction;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.network.PktControllerSmartInterfaceUpdate;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

@Mod(
    modid = MMCEGuiExt.MODID,
    name = MMCEGuiExt.NAME,
    version = MMCEGuiExt.VERSION,
    dependencies = "required-after:modularmachinery;required-after:appliedenergistics2;required-after:mekanism;required-after:mekeng"
)
public class MMCEGuiExt {
    public static final String MODID = "mmceguiext";
    public static final String NAME = "Modular Machinery: Community Edition Gui Edit";
    public static final String VERSION = "1.1.0-beta";
    public static final int GUI_CUSTOM_HATCH = 1;
    public static final int GUI_CUSTOM_AE_MIXED_INPUT = 2;
    public static final int GUI_CUSTOM_AE_MIXED_OUTPUT = 3;
    public static final int GUI_CUSTOM_AE_ITEM_INPUT = 4;
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
        NET_CHANNEL.registerMessage(
            PktCustomMEItemInputBusInvAction.class,
            PktCustomMEItemInputBusInvAction.class,
            nextPacketId++,
            Side.SERVER
        );
        CustomHatchRegistry.loadAll();
        CustomAEItemInputBusRegistry.loadAll();
        CustomAEMixedInputBusRegistry.loadAll();
        CustomAEMixedOutputBusRegistry.loadAll();
        if (event.getSide().isClient()) {
            preloadClientStyleCache();
            registerClientGuiEventHandler();
        }
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Mods.TOP.isPresent()) {
            registerTopProvider();
        }
    }

    private static class GuiHandler implements IGuiHandler {
        @Override
        public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            if (id == GUI_CUSTOM_AE_ITEM_INPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomMEItemInputBus)) {
                    return null;
                }
                TileCustomMEItemInputBus tile = (TileCustomMEItemInputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                resolveCustomItemInputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return new ContainerCustomMEItemInputBus(tile, player);
            }
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
            if (id == GUI_CUSTOM_AE_ITEM_INPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomMEItemInputBus)) {
                    return null;
                }
                TileCustomMEItemInputBus tile = (TileCustomMEItemInputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                resolveCustomItemInputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return createClientGui(
                    "com.fushu.mmceguiext.client.gui.GuiMEItemInputBusCustom",
                    new Class<?>[]{TileCustomMEItemInputBus.class, EntityPlayer.class},
                    tile,
                    player
                );
            }
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomAEMixedInputBus)) {
                    return null;
                }
                TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return def == null ? null : createClientGui(
                    "com.fushu.mmceguiext.client.gui.GuiCustomAEMixedInputBus",
                    new Class<?>[]{TileCustomAEMixedInputBus.class, EntityPlayer.class, com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def.class},
                    tile,
                    player,
                    def
                );
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomAEMixedOutputBus)) {
                    return null;
                }
                TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, new net.minecraft.util.math.BlockPos(x, y, z), tile);
                return def == null ? null : createClientGui(
                    "com.fushu.mmceguiext.client.gui.GuiCustomAEMixedOutputBus",
                    new Class<?>[]{TileCustomAEMixedOutputBus.class, EntityPlayer.class, com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def.class},
                    tile,
                    player,
                    def
                );
            }
            if (id != GUI_CUSTOM_HATCH) {
                return null;
            }
            if (!(world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z)) instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) world.getTileEntity(new net.minecraft.util.math.BlockPos(x, y, z));
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = tile.getDefinition();
            return def == null ? null : createClientGui(
                "com.fushu.mmceguiext.client.gui.GuiFluidProcessorHatchCustom",
                new Class<?>[]{TileCustomHatch.class, EntityPlayer.class, com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef.class},
                tile,
                player,
                def
            );
        }

        @Nullable
        private Object createClientGui(String className, Class<?>[] signature, Object... args) {
            try {
                return Class.forName(className).getConstructor(signature).newInstance(args);
            } catch (Exception e) {
                return null;
            }
        }

        @Nullable
        private com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry.Def resolveCustomItemInputBusDef(World world, net.minecraft.util.math.BlockPos pos, TileCustomMEItemInputBus tile) {
            com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry.Def def = tile.getDefinition();
            if (def != null) {
                return def;
            }
            if (world.getBlockState(pos).getBlock() instanceof BlockCustomMEItemInputBus) {
                BlockCustomMEItemInputBus block = (BlockCustomMEItemInputBus) world.getBlockState(pos).getBlock();
                tile.setDefinitionId(block.getRegistryName() == null ? null : block.getRegistryName().toString());
                return block.getDefinition();
            }
            return null;
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

    private static void preloadClientStyleCache() {
        if (!MMCEGuiExtConfig.novaEngCoreCompatibilityMode) {
            return;
        }
        try {
            Class.forName("com.fushu.mmceguiext.client.config.MachineGuiStyleManager")
                .getMethod("preloadAndPinCache")
                .invoke(null);
        } catch (Exception ignored) {
        }
    }

    private static void registerClientGuiEventHandler() {
        try {
            MinecraftForge.EVENT_BUS.register(Class.forName("com.fushu.mmceguiext.client.ClientGuiEventHandler").newInstance());
        } catch (Exception ignored) {
        }
    }

    private static void registerTopProvider() {
        try {
            Object top = Class.forName("mcjty.theoneprobe.TheOneProbe").getField("theOneProbeImp").get(null);
            top.getClass()
                .getMethod("registerProvider", Class.forName("mcjty.theoneprobe.api.IProbeInfoProvider"))
                .invoke(top, Class.forName("com.fushu.mmceguiext.common.integration.theoneprobe.CustomHatchInfoProvider").newInstance());
        } catch (Exception | LinkageError ignored) {
        }
    }
}

