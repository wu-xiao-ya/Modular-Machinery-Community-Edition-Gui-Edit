package com.fushu.mmceguiext.common.block;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.model.CustomHatchModelState;
import com.fushu.mmceguiext.common.model.CustomHatchRenderState;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import net.minecraft.block.Block;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

public class BlockCustomHatch extends BlockMachineComponent {
    public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class, EnumFacing.HORIZONTALS);
    private final com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition;
    private final String registryPath;
    private final ThreadLocal<Boolean> dropNextBreak = new ThreadLocal<Boolean>();

    public BlockCustomHatch(com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition) {
        this(definition, definition == null || definition.id == null || definition.id.trim().isEmpty()
            ? "custom_hatch"
            : CustomIdValidator.normalizePath(definition.id, "custom_hatch"));
    }

    public BlockCustomHatch(com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition, String registryPath) {
        super(resolveMaterial(definition));
        this.definition = definition;
        this.registryPath = CustomIdValidator.normalizePath(registryPath, "custom_hatch");
        applyBlockProperties(definition);
        setCreativeTabSafe(this, CommonProxy.creativeTabModularMachinery);
        String path = this.registryPath;
        setRegistryNameSafe(this, new ResourceLocation(MMCEGuiExt.MODID, path));
        setTranslationKeySafe(this, MMCEGuiExt.MODID + "." + path);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef getDefinition() {
        return this.definition;
    }

    public boolean isGenericRegistryBlock() {
        return "custom_hatch".equals(this.registryPath);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (playerIn.isSneaking() && te instanceof TileCustomHatch) {
                TileCustomHatch hatch = (TileCustomHatch) te;
                if (hatch.tryHeldItemInteraction(playerIn, hand)) {
                    return true;
                }
            }
            playerIn.openGui(MMCEGuiExt.MODID, MMCEGuiExt.GUI_CUSTOM_HATCH, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileCustomHatch) {
            TileCustomHatch hatch = (TileCustomHatch) tile;
            ItemStack picked = com.fushu.mmceguiext.common.item.ItemBlockCustomHatch.createStack(this);
            if (hatch.getDefinitionId() != null) {
                NBTTagCompound tag = picked.hasTagCompound() ? picked.getTagCompound() : new NBTTagCompound();
                tag.setString("hatchId", hatch.getDefinitionId());
                picked.setTagCompound(tag);
            }
            return picked;
        }
        return new ItemStack(net.minecraft.item.Item.getItemFromBlock(this));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileCustomHatch) {
            TileCustomHatch custom = (TileCustomHatch) tile;
            String registryId = getRegistryName() == null ? null : getRegistryName().toString();
            NBTTagCompound tag = stack.getTagCompound();
            String storedDefinition = CustomIdValidator.readSanitizedString(tag, "hatchId");
            custom.setDefinitionId(storedDefinition != null ? storedDefinition : registryId);
            if (storedDefinition != null || (registryId != null && registryId.equals(custom.getDefinitionId()))) {
                custom.readDroppedData(tag);
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        if (!worldIn.isRemote && Boolean.TRUE.equals(this.dropNextBreak.get())) {
            TileEntity tile = worldIn.getTileEntity(pos);
            ItemStack dropped = com.fushu.mmceguiext.common.item.ItemBlockCustomHatch.createStack(this);
            if (tile instanceof TileCustomHatch) {
                TileCustomHatch hatch = (TileCustomHatch) tile;
                NBTTagCompound tag = new NBTTagCompound();
                hatch.writeDroppedData(tag);
                String definitionId = hatch.getDefinitionId();
                String registryId = getRegistryName() == null ? "" : getRegistryName().toString();
                tag.setString("hatchId", definitionId == null || definitionId.trim().isEmpty() ? registryId : definitionId);
                dropped.setTagCompound(tag);
            }
            spawnAsEntity(worldIn, pos, dropped);
        }
        worldIn.removeTileEntity(pos);
    }

    @Override
    public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(@Nonnull World world, @Nonnull EntityPlayer player, @Nonnull BlockPos pos, @Nonnull IBlockState state, TileEntity te, @Nonnull ItemStack stack) {
        super.harvestBlock(world, player, pos, state, te, stack);
        if (player != null && !player.capabilities.isCreativeMode) {
            this.dropNextBreak.set(Boolean.TRUE);
            try {
                world.setBlockToAir(pos);
            } finally {
                this.dropNextBreak.remove();
            }
        } else {
            world.setBlockToAir(pos);
        }
    }

    @Override
    public void getDrops(@Nonnull NonNullList<ItemStack> drops, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune) {
    }

    @Override
    public void dropBlockAsItemWithChance(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, float chance, int fortune) {
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[]{FACING}, new net.minecraftforge.common.property.IUnlistedProperty[]{CustomHatchModelState.RENDER_STATE});
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!(state instanceof IExtendedBlockState)) {
            return state;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileCustomHatch)) {
            return ((IExtendedBlockState) state).withProperty(CustomHatchModelState.RENDER_STATE, new CustomHatchRenderState(
                this.definition == null ? null : this.definition.id, 0.0D, 0.0D, 0.0D
            ));
        }
        TileCustomHatch hatch = (TileCustomHatch) tile;
        return ((IExtendedBlockState) state).withProperty(CustomHatchModelState.RENDER_STATE, new CustomHatchRenderState(
            hatch.getDefinitionId(),
            hatch.getFluidFillRatio(),
            hatch.getGasFillRatio(),
            hatch.getEnergyFillRatio()
        ));
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCustomHatch();
    }

    private static void applyBlockProperties(BlockCustomHatch block, com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition) {
        com.fushu.mmceguiext.common.registry.CustomHatchRegistry.BlockDef props = definition == null ? null : definition.block;
        if (props == null) {
            setHardnessSafe(block, 2F);
            setResistanceSafe(block, 10F);
            setSoundTypeSafe(block, SoundType.METAL);
            setHarvestLevelSafe(block, "pickaxe", 1);
            return;
        }

        if (props.unbreakable) {
            setBlockUnbreakableSafe(block);
        } else {
            setHardnessSafe(block, props.hardness);
        }
        setResistanceSafe(block, props.resistance);
        setSoundTypeSafe(block, resolveSoundType(props.soundType));
        if (props.harvestTool != null && !props.harvestTool.trim().isEmpty()) {
            setHarvestLevelSafe(block, props.harvestTool.trim(), props.harvestLevel);
        }
        setLightLevelSafe(block, clamp(props.lightLevel, 0F, 1F));
        setLightOpacitySafe(block, Math.max(0, Math.min(255, props.lightOpacity)));
        block.slipperiness = Math.max(0F, props.slipperiness);
    }

    private void applyBlockProperties(com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition) {
        applyBlockProperties(this, definition);
    }

    private static Material resolveMaterial(com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition) {
        String material = definition == null || definition.block == null ? "iron" : definition.block.material;
        String key = material == null ? "iron" : material.trim().toLowerCase();
        if ("rock".equals(key) || "stone".equals(key)) {
            return Material.ROCK;
        }
        if ("wood".equals(key)) {
            return Material.WOOD;
        }
        if ("glass".equals(key)) {
            return Material.GLASS;
        }
        if ("ground".equals(key) || "dirt".equals(key)) {
            return Material.GROUND;
        }
        if ("cloth".equals(key) || "wool".equals(key)) {
            return Material.CLOTH;
        }
        if ("circuits".equals(key) || "circuit".equals(key)) {
            return Material.CIRCUITS;
        }
        if ("anvil".equals(key)) {
            return Material.ANVIL;
        }
        return Material.IRON;
    }

    private static SoundType resolveSoundType(String value) {
        String key = value == null ? "metal" : value.trim().toLowerCase();
        if ("stone".equals(key)) {
            return SoundType.STONE;
        }
        if ("wood".equals(key)) {
            return SoundType.WOOD;
        }
        if ("glass".equals(key)) {
            return SoundType.GLASS;
        }
        if ("cloth".equals(key)) {
            return SoundType.CLOTH;
        }
        if ("sand".equals(key)) {
            return SoundType.SAND;
        }
        if ("snow".equals(key)) {
            return SoundType.SNOW;
        }
        if ("ladder".equals(key)) {
            return SoundType.LADDER;
        }
        if ("anvil".equals(key)) {
            return SoundType.ANVIL;
        }
        return SoundType.METAL;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void setCreativeTabSafe(final Block block, @Nullable final CreativeTabs tab) {
        block.setCreativeTab(tab);
    }

    private static void setRegistryNameSafe(final Block block, final ResourceLocation name) {
        block.setRegistryName(name);
    }

    private static void setTranslationKeySafe(final Block block, final String key) {
        block.setTranslationKey(key);
    }

    private static void setHardnessSafe(final Block block, final float hardness) {
        block.setHardness(hardness);
    }

    private static void setResistanceSafe(final Block block, final float resistance) {
        block.setResistance(resistance);
    }

    private static void setSoundTypeSafe(final BlockCustomHatch block, final SoundType soundType) {
        block.blockSoundType = soundType;
    }

    private static void setHarvestLevelSafe(final Block block, final String toolClass, final int level) {
        block.setHarvestLevel(toolClass, level);
    }

    private static void setBlockUnbreakableSafe(final Block block) {
        block.setBlockUnbreakable();
    }

    private static void setLightLevelSafe(final Block block, final float level) {
        block.setLightLevel(level);
    }

    private static void setLightOpacitySafe(final Block block, final int opacity) {
        block.setLightOpacity(opacity);
    }
}
