package com.fushu.mmceguiext;

import com.fushu.mmceguiext.common.energy.LongEnergyCapability;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomCapacityCardRegistry;
import com.fushu.mmceguiext.common.container.ContainerCustomMEItemInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.integration.crafttweaker.MMCEGEEvents;
import com.fushu.mmceguiext.common.integration.cfn.CFNEnergyIntegration;
import com.fushu.mmceguiext.common.network.PktControllerButtonAction;
import com.fushu.mmceguiext.common.network.PktControllerCustomDataSync;
import com.fushu.mmceguiext.common.network.PktCustomAEMixedSlotUpdate;
import com.fushu.mmceguiext.common.network.PktCustomHatchEnergySync;
import com.fushu.mmceguiext.common.network.PktCustomMEItemInputBusInvAction;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.network.PktControllerSmartInterfaceUpdate;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@Mod(
    modid = MMCEGuiExt.MODID,
    name = MMCEGuiExt.NAME,
    version = MMCEGuiExt.VERSION,
    dependencies = "required-after:modularmachinery;required-after:appliedenergistics2;required-after:mekanism;required-after:mekeng"
)
public class MMCEGuiExt {
    public static final String MODID = "mmceguiext";
    private static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final String NAME = "Modular Machinery: Community Edition Gui Edit";
    public static final String VERSION = "1.2.0";
    public static final int GUI_CUSTOM_HATCH = 1;
    public static final int GUI_CUSTOM_AE_MIXED_INPUT = 2;
    public static final int GUI_CUSTOM_AE_MIXED_OUTPUT = 3;
    public static final int GUI_CUSTOM_AE_ITEM_INPUT = 4;
    public static final SimpleNetworkWrapper NET_CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    private static int nextPacketId = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LongEnergyCapability.register();
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
        NET_CHANNEL.registerMessage(
            PktCustomHatchEnergySync.class,
            PktCustomHatchEnergySync.class,
            nextPacketId++,
            Side.CLIENT
        );
        NET_CHANNEL.registerMessage(
            PktControllerCustomDataSync.class,
            PktControllerCustomDataSync.class,
            nextPacketId++,
            Side.CLIENT
        );
        CustomHatchRegistry.loadAll();
        CustomAEItemInputBusRegistry.loadAll();
        CustomAEMixedInputBusRegistry.loadAll();
        CustomAEMixedOutputBusRegistry.loadAll();
        CustomCapacityCardRegistry.loadAll();
        if (event.getSide().isClient()) {
            preloadClientStyleCache();
            registerClientGuiEventHandler();
        }
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CFNEnergyIntegration.registerIfPresent();
        if (Mods.TOP.isPresent()) {
            registerTopProvider();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (Mods.CRAFTTWEAKER.isPresent()) {
            registerCraftTweakerEventBridge();
        }
    }

    public static Logger logger() {
        return LOGGER;
    }

    private static class GuiHandler implements IGuiHandler {
        @Override
        public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!canOpenTileGui(player, world, pos)) {
                return null;
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (id == GUI_CUSTOM_AE_ITEM_INPUT) {
                if (!(tileEntity instanceof TileCustomMEItemInputBus)) {
                    return null;
                }
                TileCustomMEItemInputBus tile = (TileCustomMEItemInputBus) tileEntity;
                resolveCustomItemInputBusDef(world, pos, tile);
                return new ContainerCustomMEItemInputBus(tile, player);
            }
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                if (!(tileEntity instanceof TileCustomAEMixedInputBus)) {
                    return null;
                }
                TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) tileEntity;
                com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, pos, tile);
                if (def == null) {
                    LOGGER.warn("Falling back to a safe mixed input GUI layout at {} because the definition could not be resolved.", pos);
                    def = buildFallbackMixedInputBusDef(tile);
                }
                return new ContainerCustomAEMixedInputBus(tile, player);
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                if (!(tileEntity instanceof TileCustomAEMixedOutputBus)) {
                    return null;
                }
                TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) tileEntity;
                com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, pos, tile);
                return def == null ? null : new ContainerCustomAEMixedOutputBus(tile, player);
            }
            if (id != GUI_CUSTOM_HATCH) {
                return null;
            }
            if (!(tileEntity instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) tileEntity;
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = resolveCustomHatchDef(world, pos, tile);
            if (def == null) {
                LOGGER.warn("Cannot open custom hatch GUI at {}: missing definition, tile id={}.", pos, tile.getDefinitionId());
                return null;
            }
            return new ContainerFluidProcessorHatchCustom(tile, player, def);
        }

        @Override
        public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!canOpenTileGui(player, world, pos)) {
                return null;
            }
            TileEntity tileEntity = world.getTileEntity(pos);
            if (id == GUI_CUSTOM_AE_ITEM_INPUT) {
                if (!(tileEntity instanceof TileCustomMEItemInputBus)) {
                    return null;
                }
                TileCustomMEItemInputBus tile = (TileCustomMEItemInputBus) tileEntity;
                resolveCustomItemInputBusDef(world, pos, tile);
                return createClientGui(
                    "com.fushu.mmceguiext.client.gui.GuiMEItemInputBusCustom",
                    new Class<?>[]{TileCustomMEItemInputBus.class, EntityPlayer.class},
                    tile,
                    player
                );
            }
            if (id == GUI_CUSTOM_AE_MIXED_INPUT) {
                if (!(tileEntity instanceof TileCustomAEMixedInputBus)) {
                    return null;
                }
                TileCustomAEMixedInputBus tile = (TileCustomAEMixedInputBus) tileEntity;
                com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = resolveMixedInputBusDef(world, pos, tile);
                if (def == null) {
                    LOGGER.warn("Falling back to a safe mixed input client GUI at {} because the definition could not be resolved.", pos);
                    def = buildFallbackMixedInputBusDef(tile);
                }
                return createClientGui(
                    "com.fushu.mmceguiext.client.gui.GuiCustomAEMixedInputBus",
                    new Class<?>[]{TileCustomAEMixedInputBus.class, EntityPlayer.class, com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def.class},
                    tile,
                    player,
                    def
                );
            }
            if (id == GUI_CUSTOM_AE_MIXED_OUTPUT) {
                if (!(tileEntity instanceof TileCustomAEMixedOutputBus)) {
                    return null;
                }
                TileCustomAEMixedOutputBus tile = (TileCustomAEMixedOutputBus) tileEntity;
                com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry.Def def = resolveMixedOutputBusDef(world, pos, tile);
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
            if (!(tileEntity instanceof TileCustomHatch)) {
                return null;
            }
            TileCustomHatch tile = (TileCustomHatch) tileEntity;
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = resolveCustomHatchDef(world, pos, tile);
            if (def == null) {
                LOGGER.warn("Cannot create custom hatch client GUI at {}: missing definition, tile id={}.", pos, tile.getDefinitionId());
                return null;
            }
            return createClientGui(
                "com.fushu.mmceguiext.client.gui.GuiFluidProcessorHatchCustom",
                new Class<?>[]{TileEntity.class, EntityPlayer.class, com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef.class},
                tile,
                player,
                def
            );
        }

        private boolean canOpenTileGui(EntityPlayer player, World world, BlockPos pos) {
            return player != null
                && world != null
                && pos != null
                && world.isBlockLoaded(pos)
                && player.world == world
                && player.getDistanceSqToCenter(pos) <= 64D;
        }

        @Nullable
        private Object createClientGui(String className, Class<?>[] signature, Object... args) {
            try {
                return Class.forName(className).getConstructor(signature).newInstance(args);
            } catch (Exception e) {
                LOGGER.warn("Failed to create GUI {}: {}", className, e.toString());
                return null;
            }
        }

        @Nullable
        private com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef resolveCustomHatchDef(World world, net.minecraft.util.math.BlockPos pos, TileCustomHatch tile) {
            com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef def = tile.getDefinition();
            if (def != null) {
                return def;
            }
            if (world.getBlockState(pos).getBlock() instanceof BlockCustomHatch) {
                BlockCustomHatch block = (BlockCustomHatch) world.getBlockState(pos).getBlock();
                tile.setDefinitionId(block.getRegistryName() == null ? null : block.getRegistryName().toString());
                return block.getDefinition();
            }
            return null;
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

        private com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def buildFallbackMixedInputBusDef(TileCustomAEMixedInputBus tile) {
            com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def def = new com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.Def();
            def.id = tile.getDefinitionId();
            def.playerInventoryX = 8;
            def.playerInventoryY = 141;
            def.guiWidth = 176;
            def.guiHeight = 235;
            def.backgroundTextureWidth = 176;
            def.backgroundTextureHeight = 235;
            def.configSlots = buildFallbackSlotPoints(tile.getConfigInventory().getSlots(), 8, 17, 18);
            def.storageSlots = buildFallbackSlotPoints(tile.getInternalInventory().getSlots(), 8, 53, 18);
            def.capacityCardSlots = buildFallbackSlotPoints(tile.getCapacityCardInventory().getSlots(), 8, 89, 18);
            def.fluidConfigTanks = java.util.Collections.emptyList();
            def.gasConfigTanks = java.util.Collections.emptyList();
            def.fluidStorageTanks = java.util.Collections.emptyList();
            def.gasStorageTanks = java.util.Collections.emptyList();
            def.gui = new com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.GuiDef();
            def.gui.width = def.guiWidth;
            def.gui.height = def.guiHeight;
            def.gui.components = java.util.Collections.emptyList();
            return def;
        }

        private java.util.List<com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.SlotPoint> buildFallbackSlotPoints(int slotCount, int startX, int startY, int spacingX) {
            java.util.List<com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.SlotPoint> points =
                new java.util.ArrayList<com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.SlotPoint>(Math.max(0, slotCount));
            for (int i = 0; i < slotCount; i++) {
                com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.SlotPoint point =
                    new com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry.SlotPoint();
                point.x = startX + i * spacingX;
                point.y = startY;
                points.add(point);
            }
            return points;
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

    private static void registerCraftTweakerEventBridge() {
        try {
            MinecraftForge.EVENT_BUS.register(MMCEGEEvents.instance());
        } catch (Exception | LinkageError ignored) {
        }
    }
}

