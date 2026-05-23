package com.fushu.mmceguiext.common.block;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockCustomAEMixedInputBus extends BlockMEMachineComponent {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private final CustomAEMixedInputBusRegistry.Def definition;

    public BlockCustomAEMixedInputBus(CustomAEMixedInputBusRegistry.Def definition) {
        this.definition = definition;
        String path = normalizePath(definition == null ? null : definition.id);
        setRegistryName(new ResourceLocation("mmceguiext", path));
        setTranslationKey("mmceguiext." + path);
    }

    @Nullable
    public CustomAEMixedInputBusRegistry.Def getDefinition() {
        return this.definition;
    }

    public void ensureDefinitionId(TileCustomAEMixedInputBus tile) {
        if (tile == null) {
            return;
        }
        if (tile.getDefinitionId() == null || tile.getDefinitionId().trim().isEmpty()) {
            ResourceLocation registryName = getRegistryName();
            tile.setDefinitionId(registryName == null ? null : registryName.toString());
        }
    }

    @Override
    public boolean onBlockActivated(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity instanceof TileCustomAEMixedInputBus) {
                ensureDefinitionId((TileCustomAEMixedInputBus) tileEntity);
            }
            LOGGER.info("Custom AE mixed input bus activated at {} with tile {}", pos, tileEntity == null ? "null" : tileEntity.getClass().getName());
            playerIn.openGui(MMCEGuiExt.MODID, MMCEGuiExt.GUI_CUSTOM_AE_MIXED_INPUT, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCustomAEMixedInputBus();
    }

    @Override
    public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileCustomAEMixedInputBus) {
            ((TileCustomAEMixedInputBus) tileEntity).setDefinitionId(getRegistryName() == null ? null : getRegistryName().toString());
        }
    }

    private static String normalizePath(@Nullable String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value.isEmpty() ? "custom_ae_mixed_input_bus" : value;
    }
}
