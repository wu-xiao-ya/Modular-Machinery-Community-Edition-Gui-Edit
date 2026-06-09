package com.fushu.mmceguiext.client.model;

import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.item.ItemBlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomAEBusBakedModel extends BakedModelWrapper<IBakedModel> {
    private final ResourceLocation sourceModel;
    private final Map<String, IBakedModel> cache = new HashMap<String, IBakedModel>();
    private final ItemOverrideList overrides = new CustomAEBusItemOverrides();

    public CustomAEBusBakedModel(IBakedModel originalModel, ResourceLocation sourceModel) {
        super(originalModel);
        this.sourceModel = sourceModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        ResourceLocation texture = getTexture(state);
        IBakedModel model = resolveVariantModel(texture);
        return model == null ? super.getQuads(state, side, rand) : model.getQuads(state, side, rand);
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.overrides;
    }

    @Nullable
    private ResourceLocation getTexture(@Nullable IBlockState state) {
        if (state == null) {
            return null;
        }
        if (state.getBlock() instanceof BlockCustomMEItemInputBus) {
            CustomAEItemInputBusRegistry.Def def = ((BlockCustomMEItemInputBus) state.getBlock()).getDefinition();
            return def == null ? null : CustomBlockTextureParser.parse(def.blockTexture);
        }
        if (state.getBlock() instanceof BlockCustomAEMixedInputBus) {
            CustomAEMixedInputBusRegistry.Def def = ((BlockCustomAEMixedInputBus) state.getBlock()).getDefinition();
            return def == null ? null : CustomBlockTextureParser.parse(def.blockTexture);
        }
        if (state.getBlock() instanceof BlockCustomAEMixedOutputBus) {
            CustomAEMixedOutputBusRegistry.Def def = ((BlockCustomAEMixedOutputBus) state.getBlock()).getDefinition();
            return def == null ? null : CustomBlockTextureParser.parse(def.blockTexture);
        }
        return null;
    }

    @Nullable
    private IBakedModel resolveVariantModel(@Nullable ResourceLocation texture) {
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
                .getModel(this.sourceModel)
                .retexture(com.google.common.collect.ImmutableMap.of("bg_all", texture.toString()))
                .bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, location ->
                    Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString())
                );
            this.cache.put(key, baked);
            return baked;
        } catch (Exception ignored) {
            return null;
        }
    }

    private class CustomAEBusItemOverrides extends ItemOverrideList {
        private CustomAEBusItemOverrides() {
            super(java.util.Collections.emptyList());
        }

        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable net.minecraft.world.World world, @Nullable net.minecraft.entity.EntityLivingBase entity) {
            ResourceLocation texture = null;
            if (stack.getItem() instanceof ItemBlockCustomMEItemInputBus) {
                CustomAEItemInputBusRegistry.Def def = ((ItemBlockCustomMEItemInputBus) stack.getItem()).getDefinition();
                texture = def == null ? null : CustomBlockTextureParser.parse(def.blockTexture);
            } else if (stack.getItem() instanceof ItemBlockCustomAEMixedInputBus) {
                CustomAEMixedInputBusRegistry.Def def = ((ItemBlockCustomAEMixedInputBus) stack.getItem()).getDefinition();
                texture = def == null ? null : CustomBlockTextureParser.parse(def.blockTexture);
            } else if (stack.getItem() instanceof ItemBlockCustomAEMixedOutputBus) {
                CustomAEMixedOutputBusRegistry.Def def = ((ItemBlockCustomAEMixedOutputBus) stack.getItem()).getDefinition();
                texture = def == null ? null : CustomBlockTextureParser.parse(def.blockTexture);
            }
            IBakedModel model = resolveVariantModel(texture);
            return model == null ? CustomAEBusBakedModel.this : model;
        }
    }
}
