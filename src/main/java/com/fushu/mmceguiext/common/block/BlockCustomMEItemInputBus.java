package com.fushu.mmceguiext.common.block;

import appeng.api.implementations.items.IMemoryCard;
import com.fushu.mmceguiext.common.item.ItemBlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockCustomMEItemInputBus extends BlockMEItemInputBus {
    private final CustomAEItemInputBusRegistry.Def definition;

    public BlockCustomMEItemInputBus(CustomAEItemInputBusRegistry.Def definition) {
        this.definition = definition;
        String path = normalizePath(definition == null ? null : definition.id);
        setRegistryName(new ResourceLocation("mmceguiext", path));
        setTranslationKey("mmceguiext." + path);
    }

    @Nullable
    public CustomAEItemInputBusRegistry.Def getDefinition() {
        return this.definition;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        return new TileCustomMEItemInputBus();
    }

    @Override
    public boolean onBlockActivated(
        @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state,
        @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand,
        @Nonnull EnumFacing facing,
        float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileCustomMEItemInputBus) {
                TileCustomMEItemInputBus itemInputBus = (TileCustomMEItemInputBus) te;
                ItemStack heldItem = playerIn.getHeldItem(hand);
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof IMemoryCard) {
                    boolean handled = handleSettingsTransfer(itemInputBus, (IMemoryCard) heldItem.getItem(), playerIn, heldItem);
                    if (handled) {
                        return true;
                    }
                }
                playerIn.openGui(ModularMachinery.MODID, CommonProxy.GuiType.ME_ITEM_INPUT_BUS.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public void breakBlock(final World worldIn, @Nonnull final BlockPos pos, @Nonnull final IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        ItemStack dropped = ItemBlockCustomMEItemInputBus.createStack(this);

        if (!(te instanceof MEItemInputBus)) {
            spawnAsEntity(worldIn, pos, dropped);
            worldIn.removeTileEntity(pos);
            return;
        }
        MEItemInputBus bus = (MEItemInputBus) te;
        if (!bus.hasItem() && !bus.configInvHasItem()) {
            spawnAsEntity(worldIn, pos, dropped);
            worldIn.removeTileEntity(pos);
            return;
        }

        IOInventory inventory = bus.getInternalInventory();
        IOInventory cfgInventory = bus.getConfigInventory();

        NBTTagCompound tag = dropped.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            dropped.setTagCompound(tag);
        }
        tag.setTag("inventory", inventory.writeNBT());
        tag.setTag("configInventory", cfgInventory.writeNBT());
        if (te instanceof TileCustomMEItemInputBus) {
            TileCustomMEItemInputBus custom = (TileCustomMEItemInputBus) te;
            tag.setString("definitionId", custom.getDefinitionId() == null ? "" : custom.getDefinitionId());
        }

        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < cfgInventory.getSlots(); i++) {
            cfgInventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        spawnAsEntity(worldIn, pos, dropped);
        worldIn.removeTileEntity(pos);
    }

    @Override
    public void getDrops(@Nonnull final NonNullList<ItemStack> drops, @Nonnull final IBlockAccess world, @Nonnull final BlockPos pos, @Nonnull final IBlockState state, final int fortune) {
    }

    @Override
    public void dropBlockAsItemWithChance(@Nonnull final World worldIn, @Nonnull final BlockPos pos, @Nonnull final IBlockState state, final float chance, final int fortune) {
    }

    @Override
    public void onBlockPlacedBy(@Nonnull final World worldIn,
                                @Nonnull final BlockPos pos,
                                @Nonnull final IBlockState state,
                                @Nonnull final EntityLivingBase placer,
                                @Nonnull final ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileCustomMEItemInputBus) {
            TileCustomMEItemInputBus custom = (TileCustomMEItemInputBus) te;
            custom.setDefinitionId(getRegistryName() == null ? null : getRegistryName().toString());
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && tag.hasKey("definitionId")) {
                custom.setDefinitionId(tag.getString("definitionId"));
            }
        }
    }

    private static String normalizePath(@Nullable String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value.isEmpty() ? "custom_me_item_input_bus" : value;
    }
}
