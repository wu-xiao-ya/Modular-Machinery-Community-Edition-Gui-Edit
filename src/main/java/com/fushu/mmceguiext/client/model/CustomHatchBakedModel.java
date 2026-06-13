package com.fushu.mmceguiext.client.model;

import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.item.ItemBlockCustomHatch;
import com.fushu.mmceguiext.common.model.CustomHatchModelState;
import com.fushu.mmceguiext.common.model.CustomHatchRenderState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoaderRegistry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomHatchBakedModel extends BakedModelWrapper<IBakedModel> {
    private final ResourceLocation sourceModel;
    private final Map<String, IBakedModel> cache = new HashMap<String, IBakedModel>();
    private final ItemOverrideList overrides = new CustomHatchItemOverrides();

    public CustomHatchBakedModel(IBakedModel originalModel, ResourceLocation sourceModel) {
        super(originalModel);
        this.sourceModel = sourceModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (state != null && state.getBlock() instanceof BlockCustomHatch) {
            BlockCustomHatch block = (BlockCustomHatch) state.getBlock();
            CustomHatchRenderState renderState = extractRenderState(state, block);
            String id = renderState != null ? renderState.definitionId : (block.getDefinition() == null ? null : block.getDefinition().id);
            IBakedModel model = resolveVariantModel(id, renderState);
            if (model != null) {
                return model.getQuads(state, side, rand);
            }
        }
        return super.getQuads(state, side, rand);
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.overrides;
    }

    @Nullable
    private IBakedModel resolveVariantModel(@Nullable String id, @Nullable CustomHatchRenderState renderState) {
        ResourceLocation texture = CustomHatchModelRegistry.getTexture(id);
        ResourceLocation sourceModel = CustomHatchModelRegistry.getSourceModel(id);
        if (renderState != null) {
            CustomHatchModelRegistry.TextureLevelModel fluidLevel = CustomHatchModelRegistry.resolveTextureLevel(id, "fluid", renderState.fluidFillRatio);
            CustomHatchModelRegistry.TextureLevelModel gasLevel = CustomHatchModelRegistry.resolveTextureLevel(id, "gas", renderState.gasFillRatio);
            CustomHatchModelRegistry.TextureLevelModel energyLevel = CustomHatchModelRegistry.resolveTextureLevel(id, "energy", renderState.energyFillRatio);
            CustomHatchModelRegistry.TextureLevelModel selected = selectLevel(fluidLevel, gasLevel, energyLevel);
            if (selected != null) {
                if (selected.texture != null) {
                    texture = selected.texture;
                }
                if (selected.model != null) {
                    sourceModel = selected.model;
                }
            }
        }
        if (texture == null && sourceModel == null) {
            return null;
        }
        if (sourceModel == null) {
            sourceModel = this.sourceModel;
        }
        String key = sourceModel.toString() + "|" + (texture == null ? "" : texture.toString());
        IBakedModel cachedModel = this.cache.get(key);
        if (cachedModel != null) {
            return cachedModel;
        }
        try {
            net.minecraftforge.client.model.IModel model = ModelLoaderRegistry.getModel(sourceModel);
            if (texture != null) {
                String texturePath = texture.toString();
                model = model.retexture(com.google.common.collect.ImmutableMap.of(
                    "all", texturePath,
                    "top", texturePath,
                    "side", texturePath,
                    "bottom", texturePath
                ));
            }
            IBakedModel baked = model.bake(net.minecraftforge.common.model.TRSRTransformation.identity(), net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM, location ->
                    net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString())
                );
            this.cache.put(key, baked);
            return baked;
        } catch (Exception ignored) {
            return null;
        }
    }

    private class CustomHatchItemOverrides extends ItemOverrideList {
        private CustomHatchItemOverrides() {
            super(java.util.Collections.emptyList());
        }

        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable net.minecraft.world.World world, @Nullable net.minecraft.entity.EntityLivingBase entity) {
            if (stack.getItem() instanceof ItemBlockCustomHatch) {
                ItemBlockCustomHatch item = (ItemBlockCustomHatch) stack.getItem();
                String id = item.getDefinition() == null ? null : item.getDefinition().id;
                IBakedModel model = resolveVariantModel(id, null);
                if (model != null) {
                    return model;
                }
            }
            return CustomHatchBakedModel.this;
        }
    }

    @Nullable
    private static CustomHatchRenderState extractRenderState(IBlockState state, BlockCustomHatch block) {
        if (!(state instanceof IExtendedBlockState)) {
            return null;
        }
        IExtendedBlockState extended = (IExtendedBlockState) state;
        if (!extended.getUnlistedNames().contains(CustomHatchModelState.RENDER_STATE)) {
            return null;
        }
        CustomHatchRenderState renderState = extended.getValue(CustomHatchModelState.RENDER_STATE);
        if (renderState != null) {
            return renderState;
        }
        return new CustomHatchRenderState(block.getDefinition() == null ? null : block.getDefinition().id, 0.0D, 0.0D, 0.0D);
    }

    @Nullable
    private static CustomHatchModelRegistry.TextureLevelModel selectLevel(@Nullable CustomHatchModelRegistry.TextureLevelModel fluid,
                                                                          @Nullable CustomHatchModelRegistry.TextureLevelModel gas,
                                                                          @Nullable CustomHatchModelRegistry.TextureLevelModel energy) {
        CustomHatchModelRegistry.TextureLevelModel selected = fluid;
        if (selected == null || (gas != null && gas.minFillRatio >= selected.minFillRatio)) {
            selected = gas != null ? gas : selected;
        }
        if (selected == null || (energy != null && energy.minFillRatio >= selected.minFillRatio)) {
            selected = energy != null ? energy : selected;
        }
        return selected;
    }
}
