package com.fushu.mmceguiext.client.model;

import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.item.ItemBlockCustomHatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoaderRegistry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomHatchBakedModel extends BakedModelWrapper<IBakedModel> {
    private final Map<String, IBakedModel> cache = new HashMap<String, IBakedModel>();
    private final ItemOverrideList overrides = new CustomHatchItemOverrides();

    public CustomHatchBakedModel(IBakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (state != null && state.getBlock() instanceof BlockCustomHatch) {
            BlockCustomHatch block = (BlockCustomHatch) state.getBlock();
            String id = block.getDefinition() == null ? null : block.getDefinition().id;
            IBakedModel model = resolveVariantModel(id);
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
    private IBakedModel resolveVariantModel(@Nullable String id) {
        ResourceLocation texture = CustomHatchModelRegistry.getTexture(id);
        if (texture == null) {
            return null;
        }
        String key = texture.toString();
        IBakedModel cachedModel = this.cache.get(key);
        if (cachedModel != null) {
            return cachedModel;
        }
        try {
            IBakedModel baked = ModelLoaderRegistry
                .getModel(new ResourceLocation("mmceguiext", "block/custom_hatch"))
                .retexture(com.google.common.collect.ImmutableMap.of("all", texture.toString()))
                .bake(net.minecraftforge.common.model.TRSRTransformation.identity(), net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM, location ->
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
                IBakedModel model = resolveVariantModel(id);
                if (model != null) {
                    return model;
                }
            }
            return CustomHatchBakedModel.this;
        }
    }
}
