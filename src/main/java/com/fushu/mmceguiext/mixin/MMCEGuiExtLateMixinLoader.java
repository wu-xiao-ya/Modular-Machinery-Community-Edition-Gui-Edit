package com.fushu.mmceguiext.mixin;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class MMCEGuiExtLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.mmceguiext.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        return "mixins.mmceguiext.json".equals(mixinConfig) && Loader.isModLoaded("modularmachinery");
    }
}
