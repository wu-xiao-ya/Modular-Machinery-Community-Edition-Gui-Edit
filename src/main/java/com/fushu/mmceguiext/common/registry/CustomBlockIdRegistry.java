package com.fushu.mmceguiext.common.registry;

import com.fushu.mmceguiext.MMCEGuiExt;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

final class CustomBlockIdRegistry {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final Map<String, String> OWNER_BY_PATH = new LinkedHashMap<String, String>();

    private CustomBlockIdRegistry() {
    }

    static boolean claim(String path, String owner, String sourceId) {
        String previous = OWNER_BY_PATH.get(path);
        if (previous != null) {
            LOGGER.warn("Skipping {} id '{}' because registry name {}:{} is already claimed by {}.",
                owner, sourceId, MMCEGuiExt.MODID, path, previous);
            return false;
        }
        OWNER_BY_PATH.put(path, owner);
        return true;
    }

    static boolean isClaimed(ResourceLocation name) {
        return name != null
            && MMCEGuiExt.MODID.equals(name.getNamespace())
            && OWNER_BY_PATH.containsKey(name.getPath());
    }
}
