package com.fushu.mmceguiext.common.block;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;

public class BlockCustomHatch extends BlockMachineComponent {
    public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class, EnumFacing.HORIZONTALS);
    private final com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition;

    public BlockCustomHatch(com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition) {
        super(resolveMaterial(definition));
        this.definition = definition;
        applyBlockProperties(definition);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        String path = definition == null || definition.id == null || definition.id.trim().isEmpty()
            ? "custom_hatch"
            : normalizePath(definition.id);
        setRegistryName(MMCEGuiExt.MODID, path);
        setTranslationKey(MMCEGuiExt.MODID + "." + path);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef getDefinition() {
        return this.definition;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileCustomHatch) {
                ItemStack held = playerIn.getHeldItem(hand);
                if (!held.isEmpty() && FluidUtil.getFluidHandler(held) != null) {
                    TileCustomHatch hatch = (TileCustomHatch) te;
                    FluidActionResult result = FluidUtil.tryEmptyContainer(held, hatch.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null), Fluid.BUCKET_VOLUME, playerIn, true);
                    if (result.isSuccess()) {
                        playerIn.setHeldItem(hand, result.getResult());
                        hatch.onPlayerInteract(playerIn, hand);
                        return true;
                    }
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
            return com.fushu.mmceguiext.common.item.ItemBlockCustomHatch.createStack(this);
        }
        return new ItemStack(net.minecraft.item.Item.getItemFromBlock(this));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileCustomHatch) {
            ((TileCustomHatch) tile).setDefinitionId(getRegistryName() == null ? null : getRegistryName().toString());
        }
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
        return new BlockStateContainer(this, FACING);
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
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileCustomHatch();
    }

    private static String normalizePath(String id) {
        String value = id == null ? "" : id.trim().toLowerCase();
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        return value.isEmpty() ? "custom_hatch" : value;
    }

    private static void applyBlockProperties(BlockCustomHatch block, com.fushu.mmceguiext.common.registry.CustomHatchRegistry.CustomHatchDef definition) {
        com.fushu.mmceguiext.common.registry.CustomHatchRegistry.BlockDef props = definition == null ? null : definition.block;
        if (props == null) {
            block.setHardness(2F);
            block.setResistance(10F);
            block.setSoundType(SoundType.METAL);
            block.setHarvestLevel("pickaxe", 1);
            return;
        }

        if (props.unbreakable) {
            block.setBlockUnbreakable();
        } else {
            block.setHardness(props.hardness);
        }
        block.setResistance(props.resistance);
        block.setSoundType(resolveSoundType(props.soundType));
        if (props.harvestTool != null && !props.harvestTool.trim().isEmpty()) {
            block.setHarvestLevel(props.harvestTool.trim(), props.harvestLevel);
        }
        block.setLightLevel(clamp(props.lightLevel, 0F, 1F));
        block.setLightOpacity(Math.max(0, Math.min(255, props.lightOpacity)));
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
}
