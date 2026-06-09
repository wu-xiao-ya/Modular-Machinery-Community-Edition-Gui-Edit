package com.fushu.mmceguiext.common.block;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent;
import net.minecraft.block.Block;
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

public class BlockCustomAEMixedOutputBus extends BlockMEMachineComponent {
    private final CustomAEMixedOutputBusRegistry.Def definition;

    public BlockCustomAEMixedOutputBus(CustomAEMixedOutputBusRegistry.Def definition) {
        this.definition = definition;
        String path = CustomIdValidator.normalizePath(definition == null ? null : definition.id, "custom_ae_mixed_output_bus");
        setRegistryNameSafe(this, new ResourceLocation("mmceguiext", path));
        setTranslationKeySafe(this, "mmceguiext." + path);
    }

    @Nullable
    public CustomAEMixedOutputBusRegistry.Def getDefinition() {
        return this.definition;
    }

    public void ensureDefinitionId(TileCustomAEMixedOutputBus tile) {
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
            if (tileEntity instanceof TileCustomAEMixedOutputBus) {
                ensureDefinitionId((TileCustomAEMixedOutputBus) tileEntity);
            }
            playerIn.openGui(MMCEGuiExt.MODID, MMCEGuiExt.GUI_CUSTOM_AE_MIXED_OUTPUT, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCustomAEMixedOutputBus();
    }

    @Override
    public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileCustomAEMixedOutputBus) {
            ((TileCustomAEMixedOutputBus) tileEntity).setDefinitionId(getRegistryName() == null ? null : getRegistryName().toString());
        }
    }

    private static void setRegistryNameSafe(final Block block, final ResourceLocation name) {
        block.setRegistryName(name);
    }

    private static void setTranslationKeySafe(final Block block, final String key) {
        block.setTranslationKey(key);
    }
}
