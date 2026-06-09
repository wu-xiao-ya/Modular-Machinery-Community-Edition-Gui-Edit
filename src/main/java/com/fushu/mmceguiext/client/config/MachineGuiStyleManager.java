package com.fushu.mmceguiext.client.config;

import com.fushu.mmceguiext.MMCEGuiExt;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class MachineGuiStyleManager {
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static final String MACHINERY_DIR = "modularmachinery/machinery";
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);

    private static final Object LOCK = new Object();

    private static long lastLoadTime = 0L;
    private static boolean pinnedCache = false;
    private static final Map<String, ControllerStyle> MACHINE_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> FACTORY_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();

    private MachineGuiStyleManager() {
    }

    public static ControllerStyle resolveMachineController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return resolve(machine, MACHINE_CONTROLLER_STYLES);
    }

    public static ControllerStyle resolveFactoryController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return resolve(machine, FACTORY_CONTROLLER_STYLES);
    }

    private static ControllerStyle resolve(@Nullable DynamicMachine machine, Map<String, ControllerStyle> source) {
        if (machine == null || machine.getRegistryName() == null) {
            return ControllerStyle.EMPTY;
        }

        String fullKey = machine.getRegistryName().toString().toLowerCase(Locale.ROOT);
        ControllerStyle fullMatch = source.get(fullKey);
        if (fullMatch != null) {
            return fullMatch;
        }

        String pathKey = machine.getRegistryName().getPath().toLowerCase(Locale.ROOT);
        ControllerStyle pathMatch = source.get(pathKey);
        return pathMatch == null ? ControllerStyle.EMPTY : pathMatch;
    }

    private static void ensureLoaded() {
        if (pinnedCache && lastLoadTime != 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLoadTime < RELOAD_INTERVAL_MS) {
            return;
        }
        synchronized (LOCK) {
            if (now - lastLoadTime < RELOAD_INTERVAL_MS) {
                return;
            }
            reload();
            lastLoadTime = now;
        }
    }

    public static void preloadAndPinCache() {
        synchronized (LOCK) {
            reload();
            lastLoadTime = System.currentTimeMillis();
            pinnedCache = true;
        }
    }

    public static void clearPinnedCache() {
        synchronized (LOCK) {
            pinnedCache = false;
            lastLoadTime = 0L;
        }
    }

    private static void reload() {
        MACHINE_CONTROLLER_STYLES.clear();
        FACTORY_CONTROLLER_STYLES.clear();

        Path machineryDir = Loader.instance().getConfigDir().toPath().resolve(MACHINERY_DIR);
        if (!Files.exists(machineryDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(machineryDir)) {
            stream
                .filter(Files::isRegularFile)
                .filter(MachineGuiStyleManager::isMachineJson)
                .forEach(MachineGuiStyleManager::loadMachineJson);
        } catch (IOException ex) {
            LOGGER.warn("Failed to scan MMCE GUI ext machine configs under {}: {}", machineryDir, ex.getMessage());
        }
    }

    private static boolean isMachineJson(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") && !name.endsWith(".var.json");
    }

    private static void loadMachineJson(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            MachineGuiStyleParser.MachineFileParseResult parsed =
                MachineGuiStyleParser.parseMachineJson(path.toString(), content);

            for (String warning : parsed.warnings) {
                LOGGER.warn(warning);
            }

            if (parsed.namespacedKey == null || parsed.pathKey == null) {
                return;
            }

            if (parsed.machineNodePresent) {
                ControllerStyle machineStyle = parsed.machineStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.machineStyle;
                putStyle(MACHINE_CONTROLLER_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, machineStyle, "machine", path);
            }

            if (parsed.factoryNodePresent) {
                ControllerStyle factoryStyle = parsed.factoryStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.factoryStyle;
                putStyle(FACTORY_CONTROLLER_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, factoryStyle, "factory", path);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to read MMCE GUI ext config {}: {}", path, ex.getMessage());
        }
    }

    private static void putStyle(
        Map<String, ControllerStyle> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        ControllerStyle style,
        String controllerKind,
        Path sourcePath
    ) {
        ControllerStyle previousNamespaced = target.put(namespacedKey, style);
        ControllerStyle previousPath = allowPathFallback ? target.put(pathKey, style) : null;
        if (previousNamespaced != null || previousPath != null) {
            LOGGER.warn(
                "MMCE GUI ext {} style for {} was overridden by {}.",
                controllerKind,
                namespacedKey,
                sourcePath
            );
        }
    }

    public static class ControllerStyle {
        public static final ControllerStyle EMPTY = new ControllerStyle();

        @Nullable
        public String backgroundTexture;
        @Nullable
        public Integer backgroundTextureOffsetX;
        @Nullable
        public Integer backgroundTextureOffsetY;
        @Nullable
        public Boolean hideDefaultBackground;
        @Nullable
        public Integer guiWidth;
        @Nullable
        public Integer guiHeight;
        @Nullable
        public Integer backgroundTextureWidth;
        @Nullable
        public Integer backgroundTextureHeight;
        @Nullable
        public Integer backgroundCorner;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Integer specialThreadBackgroundColor;
        @Nullable
        public Boolean disableRightExtension;
        @Nullable
        public Boolean enableSmartInterfaceEditor;
        @Nullable
        public Integer smartInterfaceEditorX;
        @Nullable
        public Integer smartInterfaceEditorY;
        @Nullable
        public Integer smartInterfaceEditorInputWidth;
        @Nullable
        public String smartInterfaceEditorVirtualKey;
        @Nullable
        public Integer smartInterfaceEditorPriority;
        @Nullable
        public Integer foregroundContentPriority;
        @Nullable
        public Boolean hideDefaultSmartInterfaceEditor;
        @Nullable
        public Boolean hidePlayerInventory;
        @Nullable
        public Boolean showBlueprintInfo;
        @Nullable
        public Boolean showStructureInfo;
        @Nullable
        public Boolean showStatusInfo;
        @Nullable
        public Boolean showParallelismInfo;
        @Nullable
        public Boolean showPerformanceInfo;
        @Nullable
        public String defaultPageId;
        @Nullable
        public String defaultPanelId;
        @Nullable
        public List<String> customPanels;
        @Nullable
        public List<InfoSectionStyle> infoSections;
        @Nullable
        public List<TextStyle> texts;
        @Nullable
        public List<SmartInterfaceEditorStyle> smartInterfaceEditors;
        @Nullable
        public List<ButtonStyle> buttons;
        @Nullable
        public List<TextureLayerStyle> textureLayers;

        public boolean isEmpty() {
            return (backgroundTexture == null || backgroundTexture.trim().isEmpty())
                   && backgroundTextureOffsetX == null
                   && backgroundTextureOffsetY == null
                   && hideDefaultBackground == null
                   && guiWidth == null
                   && guiHeight == null
                   && backgroundTextureWidth == null
                   && backgroundTextureHeight == null
                   && backgroundCorner == null
                   && useNineSlice == null
                   && specialThreadBackgroundColor == null
                   && disableRightExtension == null
                   && enableSmartInterfaceEditor == null
                   && smartInterfaceEditorX == null
                   && smartInterfaceEditorY == null
                   && smartInterfaceEditorInputWidth == null
                   && (smartInterfaceEditorVirtualKey == null || smartInterfaceEditorVirtualKey.trim().isEmpty())
                   && smartInterfaceEditorPriority == null
                   && foregroundContentPriority == null
                   && hideDefaultSmartInterfaceEditor == null
                   && hidePlayerInventory == null
                   && showBlueprintInfo == null
                   && showStructureInfo == null
                   && showStatusInfo == null
                   && showParallelismInfo == null
                   && showPerformanceInfo == null
                   && (defaultPageId == null || defaultPageId.trim().isEmpty())
                   && (defaultPanelId == null || defaultPanelId.trim().isEmpty())
                   && (customPanels == null || customPanels.isEmpty())
                   && (infoSections == null || infoSections.isEmpty())
                   && (texts == null || texts.isEmpty())
                   && (smartInterfaceEditors == null || smartInterfaceEditors.isEmpty())
                   && (buttons == null || buttons.isEmpty())
                   && (textureLayers == null || textureLayers.isEmpty());
        }
    }

    public static class InfoSectionStyle {
        @Nullable
        public String id;
        @Nullable
        public String panel;
        @Nullable
        public Boolean visible;
    }

    public static class TextStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public String value;
        @Nullable
        public Integer color;
        @Nullable
        public Float scale;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean shadow;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public String align;
    }

    public static class SmartInterfaceEditorStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        @Nullable
        public Integer inputWidth;
        public String virtualKey;
        @Nullable
        public String title;
        @Nullable
        public Boolean showTitle;
        @Nullable
        public Boolean showInfo;
        @Nullable
        public Boolean showControls;
        @Nullable
        public Boolean inputBackground;
        @Nullable
        public Integer priority;
        @Nullable
        public String page;
    }

    public static class ButtonStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public String label;
        @Nullable
        public String action;
        @Nullable
        public String buttonId;
        @Nullable
        public String key;
        @Nullable
        public Float value;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public String targetPage;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
    }

    public static class TextureLayerStyle {
        @Nullable
        public String id;
        public String texture;
        @Nullable
        public Integer offsetX;
        @Nullable
        public Integer offsetY;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer corner;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Integer priority;
        @Nullable
        public String page;
    }
}
