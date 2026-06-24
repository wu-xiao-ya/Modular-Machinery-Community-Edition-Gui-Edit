package com.fushu.mmceguiext.client.config;

import com.fushu.mmceguiext.MMCEGuiExt;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class MachineGuiStyleManager {
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static final long MAX_MACHINE_STYLE_FILE_BYTES = 1024L * 1024L;
    private static final String MACHINERY_DIR = "modularmachinery/machinery";
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);

    private static final Object LOCK = new Object();

    private static long lastLoadTime = 0L;
    private static boolean pinnedCache = false;
    private static final Map<String, ControllerStyle> MACHINE_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> FACTORY_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> MACHINE_SUBGUI_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> FACTORY_SUBGUI_STYLES = new HashMap<String, ControllerStyle>();

    private MachineGuiStyleManager() {
    }

    public static ControllerStyle resolveMachineController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return merge(
            resolve(machine, MACHINE_CONTROLLER_STYLES),
            resolve(machine, MACHINE_SUBGUI_STYLES)
        );
    }

    public static ControllerStyle resolveFactoryController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return merge(
            resolve(machine, FACTORY_CONTROLLER_STYLES),
            resolve(machine, FACTORY_SUBGUI_STYLES)
        );
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
        MACHINE_SUBGUI_STYLES.clear();
        FACTORY_SUBGUI_STYLES.clear();

        Path machineryDir = Loader.instance().getConfigDir().toPath().resolve(MACHINERY_DIR);
        if (Files.exists(machineryDir)) {
            try (Stream<Path> stream = Files.walk(machineryDir)) {
                stream
                    .filter(Files::isRegularFile)
                    .filter(MachineGuiStyleManager::isMachineJson)
                    .forEach(MachineGuiStyleManager::loadMachineJson);
            } catch (IOException ex) {
                LOGGER.warn("Failed to scan MMCE GUI ext machine configs under {}: {}", machineryDir, ex.getMessage());
            }
        }

        Path subGuiDir = SubGuiConfigLoader.getSubGuiRootDir();
        if (Files.exists(subGuiDir)) {
            try (Stream<Path> stream = Files.walk(subGuiDir)) {
                stream
                    .filter(Files::isRegularFile)
                    .filter(MachineGuiStyleManager::isMachineJson)
                    .forEach(MachineGuiStyleManager::loadSubGuiJson);
            } catch (IOException ex) {
                LOGGER.warn("Failed to scan MMCE GUI ext subGUI configs under {}: {}", subGuiDir, ex.getMessage());
            }
        }
    }

    private static boolean isMachineJson(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") && !name.endsWith(".var.json");
    }

    private static void loadMachineJson(Path path) {
        try {
            if (Files.size(path) > MAX_MACHINE_STYLE_FILE_BYTES) {
                LOGGER.warn("Skipping MMCE GUI ext machine config {} because it is larger than {} bytes.", path, MAX_MACHINE_STYLE_FILE_BYTES);
                return;
            }
            String content = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
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

    private static void loadSubGuiJson(Path path) {
        try {
            if (Files.size(path) > MAX_MACHINE_STYLE_FILE_BYTES) {
                LOGGER.warn("Skipping MMCE GUI ext subGUI config {} because it is larger than {} bytes.", path, MAX_MACHINE_STYLE_FILE_BYTES);
                return;
            }
            MachineGuiStyleParser.MachineFileParseResult parsed = SubGuiConfigLoader.loadSubGuiJson(path);

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
                mergeStyle(MACHINE_SUBGUI_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, machineStyle);
            }

            if (parsed.factoryNodePresent) {
                ControllerStyle factoryStyle = parsed.factoryStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.factoryStyle;
                mergeStyle(FACTORY_SUBGUI_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, factoryStyle);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to read MMCE GUI ext subGUI config {}: {}", path, ex.getMessage());
        }
    }

    private static ControllerStyle merge(@Nullable ControllerStyle base, @Nullable ControllerStyle overlay) {
        if (base == null || base == ControllerStyle.EMPTY || base.isEmpty()) {
            if (overlay == null || overlay == ControllerStyle.EMPTY || overlay.isEmpty()) {
                return ControllerStyle.EMPTY;
            }
            return ControllerStyle.copyOf(overlay);
        }
        if (overlay == null || overlay == ControllerStyle.EMPTY || overlay.isEmpty()) {
            return base;
        }
        return ControllerStyle.copyOf(base).mergeFrom(overlay);
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

    private static void mergeStyle(
        Map<String, ControllerStyle> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        ControllerStyle style
    ) {
        mergeStyleForKey(target, namespacedKey, style);
        if (allowPathFallback) {
            mergeStyleForKey(target, pathKey, style);
        }
    }

    private static void mergeStyleForKey(Map<String, ControllerStyle> target, String key, ControllerStyle style) {
        ControllerStyle previous = target.get(key);
        if (previous == null || previous == ControllerStyle.EMPTY || previous.isEmpty()) {
            target.put(key, ControllerStyle.copyOf(style));
            return;
        }
        target.put(key, ControllerStyle.copyOf(previous).mergeFrom(style));
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
        public Boolean centerFullGui;
        @Nullable
        public Boolean hideDefaultBackground;
        @Nullable
        public Integer guiWidth;
        @Nullable
        public Integer guiHeight;
        @Nullable
        public Boolean allowOffscreenGui;
        @Nullable
        public Integer coordinateWidth;
        @Nullable
        public Integer coordinateHeight;
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
        public Integer threadQueueX;
        @Nullable
        public Integer threadQueueY;
        @Nullable
        public Integer threadScrollbarX;
        @Nullable
        public Integer threadScrollbarY;
        @Nullable
        public ThreadScrollbarStyle threadScrollbar;
        @Nullable
        public Integer threadVisibleRows;
        @Nullable
        public Integer threadRowWidth;
        @Nullable
        public Integer threadRowHeight;
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
        @Nullable
        public List<ProgressBarStyle> progressBars;
        @Nullable
        public List<SliderStyle> sliders;
        @Nullable
        public List<DynamicVisualStyle> dynamicVisuals;
        @Nullable
        public List<SubGuiStyle> subGuis;

        public boolean isEmpty() {
            return (backgroundTexture == null || backgroundTexture.trim().isEmpty())
                   && backgroundTextureOffsetX == null
                   && backgroundTextureOffsetY == null
                   && centerFullGui == null
                   && hideDefaultBackground == null
                   && guiWidth == null
                   && guiHeight == null
                   && allowOffscreenGui == null
                   && coordinateWidth == null
                   && coordinateHeight == null
                   && backgroundTextureWidth == null
                   && backgroundTextureHeight == null
                   && backgroundCorner == null
                   && useNineSlice == null
                   && specialThreadBackgroundColor == null
                   && threadQueueX == null
                   && threadQueueY == null
                   && threadScrollbarX == null
                   && threadScrollbarY == null
                   && threadScrollbar == null
                   && threadVisibleRows == null
                   && threadRowWidth == null
                   && threadRowHeight == null
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
                   && (textureLayers == null || textureLayers.isEmpty())
                   && (progressBars == null || progressBars.isEmpty())
                   && (sliders == null || sliders.isEmpty())
                   && (dynamicVisuals == null || dynamicVisuals.isEmpty())
                   && (subGuis == null || subGuis.isEmpty());
        }

        public static ControllerStyle copyOf(ControllerStyle source) {
            ControllerStyle copy = new ControllerStyle();
            if (source == null) {
                return copy;
            }
            copy.backgroundTexture = source.backgroundTexture;
            copy.backgroundTextureOffsetX = source.backgroundTextureOffsetX;
            copy.backgroundTextureOffsetY = source.backgroundTextureOffsetY;
            copy.centerFullGui = source.centerFullGui;
            copy.hideDefaultBackground = source.hideDefaultBackground;
            copy.guiWidth = source.guiWidth;
            copy.guiHeight = source.guiHeight;
            copy.allowOffscreenGui = source.allowOffscreenGui;
            copy.coordinateWidth = source.coordinateWidth;
            copy.coordinateHeight = source.coordinateHeight;
            copy.backgroundTextureWidth = source.backgroundTextureWidth;
            copy.backgroundTextureHeight = source.backgroundTextureHeight;
            copy.backgroundCorner = source.backgroundCorner;
            copy.useNineSlice = source.useNineSlice;
            copy.specialThreadBackgroundColor = source.specialThreadBackgroundColor;
            copy.threadQueueX = source.threadQueueX;
            copy.threadQueueY = source.threadQueueY;
            copy.threadScrollbarX = source.threadScrollbarX;
            copy.threadScrollbarY = source.threadScrollbarY;
            copy.threadScrollbar = ThreadScrollbarStyle.copyOf(source.threadScrollbar);
            copy.threadVisibleRows = source.threadVisibleRows;
            copy.threadRowWidth = source.threadRowWidth;
            copy.threadRowHeight = source.threadRowHeight;
            copy.disableRightExtension = source.disableRightExtension;
            copy.enableSmartInterfaceEditor = source.enableSmartInterfaceEditor;
            copy.smartInterfaceEditorX = source.smartInterfaceEditorX;
            copy.smartInterfaceEditorY = source.smartInterfaceEditorY;
            copy.smartInterfaceEditorInputWidth = source.smartInterfaceEditorInputWidth;
            copy.smartInterfaceEditorVirtualKey = source.smartInterfaceEditorVirtualKey;
            copy.smartInterfaceEditorPriority = source.smartInterfaceEditorPriority;
            copy.foregroundContentPriority = source.foregroundContentPriority;
            copy.hideDefaultSmartInterfaceEditor = source.hideDefaultSmartInterfaceEditor;
            copy.hidePlayerInventory = source.hidePlayerInventory;
            copy.showBlueprintInfo = source.showBlueprintInfo;
            copy.showStructureInfo = source.showStructureInfo;
            copy.showStatusInfo = source.showStatusInfo;
            copy.showParallelismInfo = source.showParallelismInfo;
            copy.showPerformanceInfo = source.showPerformanceInfo;
            copy.defaultPageId = source.defaultPageId;
            copy.defaultPanelId = source.defaultPanelId;
            copy.customPanels = source.customPanels == null ? null : new ArrayList<String>(source.customPanels);
            copy.infoSections = source.infoSections == null ? null : new ArrayList<InfoSectionStyle>(source.infoSections);
            copy.texts = source.texts == null ? null : new ArrayList<TextStyle>(source.texts);
            copy.smartInterfaceEditors = source.smartInterfaceEditors == null ? null : new ArrayList<SmartInterfaceEditorStyle>(source.smartInterfaceEditors);
            copy.buttons = source.buttons == null ? null : new ArrayList<ButtonStyle>(source.buttons);
            copy.textureLayers = source.textureLayers == null ? null : new ArrayList<TextureLayerStyle>(source.textureLayers);
            copy.progressBars = source.progressBars == null ? null : new ArrayList<ProgressBarStyle>(source.progressBars);
            copy.sliders = source.sliders == null ? null : new ArrayList<SliderStyle>(source.sliders);
            copy.dynamicVisuals = source.dynamicVisuals == null ? null : copyDynamicVisualList(source.dynamicVisuals);
            copy.subGuis = source.subGuis == null ? null : copySubGuiList(source.subGuis);
            return copy;
        }

        public ControllerStyle mergeFrom(@Nullable ControllerStyle overlay) {
            if (overlay == null) {
                return this;
            }
            if (overlay.backgroundTexture != null) this.backgroundTexture = overlay.backgroundTexture;
            if (overlay.backgroundTextureOffsetX != null) this.backgroundTextureOffsetX = overlay.backgroundTextureOffsetX;
            if (overlay.backgroundTextureOffsetY != null) this.backgroundTextureOffsetY = overlay.backgroundTextureOffsetY;
            if (overlay.centerFullGui != null) this.centerFullGui = overlay.centerFullGui;
            if (overlay.hideDefaultBackground != null) this.hideDefaultBackground = overlay.hideDefaultBackground;
            if (overlay.guiWidth != null) this.guiWidth = overlay.guiWidth;
            if (overlay.guiHeight != null) this.guiHeight = overlay.guiHeight;
            if (overlay.allowOffscreenGui != null) this.allowOffscreenGui = overlay.allowOffscreenGui;
            if (overlay.coordinateWidth != null) this.coordinateWidth = overlay.coordinateWidth;
            if (overlay.coordinateHeight != null) this.coordinateHeight = overlay.coordinateHeight;
            if (overlay.backgroundTextureWidth != null) this.backgroundTextureWidth = overlay.backgroundTextureWidth;
            if (overlay.backgroundTextureHeight != null) this.backgroundTextureHeight = overlay.backgroundTextureHeight;
            if (overlay.backgroundCorner != null) this.backgroundCorner = overlay.backgroundCorner;
            if (overlay.useNineSlice != null) this.useNineSlice = overlay.useNineSlice;
            if (overlay.specialThreadBackgroundColor != null) this.specialThreadBackgroundColor = overlay.specialThreadBackgroundColor;
            if (overlay.threadQueueX != null) this.threadQueueX = overlay.threadQueueX;
            if (overlay.threadQueueY != null) this.threadQueueY = overlay.threadQueueY;
            if (overlay.threadScrollbarX != null) this.threadScrollbarX = overlay.threadScrollbarX;
            if (overlay.threadScrollbarY != null) this.threadScrollbarY = overlay.threadScrollbarY;
            if (overlay.threadScrollbar != null) this.threadScrollbar = ThreadScrollbarStyle.merge(this.threadScrollbar, overlay.threadScrollbar);
            if (overlay.threadVisibleRows != null) this.threadVisibleRows = overlay.threadVisibleRows;
            if (overlay.threadRowWidth != null) this.threadRowWidth = overlay.threadRowWidth;
            if (overlay.threadRowHeight != null) this.threadRowHeight = overlay.threadRowHeight;
            if (overlay.disableRightExtension != null) this.disableRightExtension = overlay.disableRightExtension;
            if (overlay.enableSmartInterfaceEditor != null) this.enableSmartInterfaceEditor = overlay.enableSmartInterfaceEditor;
            if (overlay.smartInterfaceEditorX != null) this.smartInterfaceEditorX = overlay.smartInterfaceEditorX;
            if (overlay.smartInterfaceEditorY != null) this.smartInterfaceEditorY = overlay.smartInterfaceEditorY;
            if (overlay.smartInterfaceEditorInputWidth != null) this.smartInterfaceEditorInputWidth = overlay.smartInterfaceEditorInputWidth;
            if (overlay.smartInterfaceEditorVirtualKey != null) this.smartInterfaceEditorVirtualKey = overlay.smartInterfaceEditorVirtualKey;
            if (overlay.smartInterfaceEditorPriority != null) this.smartInterfaceEditorPriority = overlay.smartInterfaceEditorPriority;
            if (overlay.foregroundContentPriority != null) this.foregroundContentPriority = overlay.foregroundContentPriority;
            if (overlay.hideDefaultSmartInterfaceEditor != null) this.hideDefaultSmartInterfaceEditor = overlay.hideDefaultSmartInterfaceEditor;
            if (overlay.hidePlayerInventory != null) this.hidePlayerInventory = overlay.hidePlayerInventory;
            if (overlay.showBlueprintInfo != null) this.showBlueprintInfo = overlay.showBlueprintInfo;
            if (overlay.showStructureInfo != null) this.showStructureInfo = overlay.showStructureInfo;
            if (overlay.showStatusInfo != null) this.showStatusInfo = overlay.showStatusInfo;
            if (overlay.showParallelismInfo != null) this.showParallelismInfo = overlay.showParallelismInfo;
            if (overlay.showPerformanceInfo != null) this.showPerformanceInfo = overlay.showPerformanceInfo;
            if (overlay.defaultPageId != null) this.defaultPageId = overlay.defaultPageId;
            if (overlay.defaultPanelId != null) this.defaultPanelId = overlay.defaultPanelId;
            this.customPanels = appendList(this.customPanels, overlay.customPanels);
            this.infoSections = appendList(this.infoSections, overlay.infoSections);
            this.texts = appendList(this.texts, overlay.texts);
            this.smartInterfaceEditors = appendList(this.smartInterfaceEditors, overlay.smartInterfaceEditors);
            this.buttons = appendList(this.buttons, overlay.buttons);
            this.textureLayers = appendList(this.textureLayers, overlay.textureLayers);
            this.progressBars = appendList(this.progressBars, overlay.progressBars);
            this.sliders = appendList(this.sliders, overlay.sliders);
            this.dynamicVisuals = appendDynamicVisualList(this.dynamicVisuals, overlay.dynamicVisuals);
            this.subGuis = appendSubGuiList(this.subGuis, overlay.subGuis);
            return this;
        }

        @Nullable
        private static <T> List<T> appendList(@Nullable List<T> base, @Nullable List<T> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<T> out = base == null ? new ArrayList<T>() : new ArrayList<T>(base);
            out.addAll(overlay);
            return out;
        }

        @Nullable
        private static List<SubGuiStyle> copySubGuiList(@Nullable List<SubGuiStyle> source) {
            if (source == null || source.isEmpty()) {
                return source;
            }
            List<SubGuiStyle> copy = new ArrayList<SubGuiStyle>(source.size());
            for (SubGuiStyle subGui : source) {
                copy.add(SubGuiStyle.copyOf(subGui));
            }
            return copy;
        }

        @Nullable
        private static List<DynamicVisualStyle> copyDynamicVisualList(@Nullable List<DynamicVisualStyle> source) {
            if (source == null || source.isEmpty()) {
                return source;
            }
            List<DynamicVisualStyle> copy = new ArrayList<DynamicVisualStyle>(source.size());
            for (DynamicVisualStyle visual : source) {
                copy.add(DynamicVisualStyle.copyOf(visual));
            }
            return copy;
        }

        @Nullable
        private static List<DynamicVisualStyle> appendDynamicVisualList(
            @Nullable List<DynamicVisualStyle> base,
            @Nullable List<DynamicVisualStyle> overlay
        ) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<DynamicVisualStyle> out = base == null ? new ArrayList<DynamicVisualStyle>() : copyDynamicVisualList(base);
            for (DynamicVisualStyle visual : overlay) {
                out.add(DynamicVisualStyle.copyOf(visual));
            }
            return out;
        }

        @Nullable
        private static List<SubGuiStyle> appendSubGuiList(@Nullable List<SubGuiStyle> base, @Nullable List<SubGuiStyle> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<SubGuiStyle> out = base == null ? new ArrayList<SubGuiStyle>() : copySubGuiList(base);
            for (SubGuiStyle subGui : overlay) {
                out.add(SubGuiStyle.copyOf(subGui));
            }
            return out;
        }
    }


    public static class ThreadScrollbarStyle {
        @Nullable public Integer x;
        @Nullable public Integer y;
        @Nullable public Integer width;
        @Nullable public Integer height;
        @Nullable public String trackTexture;
        @Nullable public String thumbTexture;
        @Nullable public Integer trackColor;
        @Nullable public Integer thumbColor;
        @Nullable public Integer textureWidth;
        @Nullable public Integer textureHeight;
        @Nullable public Integer thumbTextureWidth;
        @Nullable public Integer thumbTextureHeight;
        @Nullable public Integer thumbMinHeight;
        @Nullable public Boolean visible;

        @Nullable
        public static ThreadScrollbarStyle copyOf(@Nullable ThreadScrollbarStyle source) {
            if (source == null) {
                return null;
            }
            ThreadScrollbarStyle copy = new ThreadScrollbarStyle();
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.trackTexture = source.trackTexture;
            copy.thumbTexture = source.thumbTexture;
            copy.trackColor = source.trackColor;
            copy.thumbColor = source.thumbColor;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.thumbTextureWidth = source.thumbTextureWidth;
            copy.thumbTextureHeight = source.thumbTextureHeight;
            copy.thumbMinHeight = source.thumbMinHeight;
            copy.visible = source.visible;
            return copy;
        }

        @Nullable
        public static ThreadScrollbarStyle merge(@Nullable ThreadScrollbarStyle base, @Nullable ThreadScrollbarStyle overlay) {
            if (overlay == null) {
                return base;
            }
            ThreadScrollbarStyle out = base == null ? new ThreadScrollbarStyle() : copyOf(base);
            if (overlay.x != null) out.x = overlay.x;
            if (overlay.y != null) out.y = overlay.y;
            if (overlay.width != null) out.width = overlay.width;
            if (overlay.height != null) out.height = overlay.height;
            if (overlay.trackTexture != null) out.trackTexture = overlay.trackTexture;
            if (overlay.thumbTexture != null) out.thumbTexture = overlay.thumbTexture;
            if (overlay.trackColor != null) out.trackColor = overlay.trackColor;
            if (overlay.thumbColor != null) out.thumbColor = overlay.thumbColor;
            if (overlay.textureWidth != null) out.textureWidth = overlay.textureWidth;
            if (overlay.textureHeight != null) out.textureHeight = overlay.textureHeight;
            if (overlay.thumbTextureWidth != null) out.thumbTextureWidth = overlay.thumbTextureWidth;
            if (overlay.thumbTextureHeight != null) out.thumbTextureHeight = overlay.thumbTextureHeight;
            if (overlay.thumbMinHeight != null) out.thumbMinHeight = overlay.thumbMinHeight;
            if (overlay.visible != null) out.visible = overlay.visible;
            return out;
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
        public Float shiftValue;
        @Nullable
        public Float ctrlValue;
        @Nullable
        public Float ctrlShiftValue;
        @Nullable
        public String stringValue;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public String targetPage;
        @Nullable
        public String targetSubGui;
        @Nullable
        public String openMode;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean visible;
        @Nullable
        public List<String> hotkeys;
        @Nullable
        public Boolean consumeHotkey;
        @Nullable
        public String page;
        @Nullable
        public String texture;
        @Nullable
        public String hoverTexture;
        @Nullable
        public String disabledTexture;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer u;
        @Nullable
        public Integer v;
        @Nullable
        public Integer hoverU;
        @Nullable
        public Integer hoverV;
        @Nullable
        public Integer disabledU;
        @Nullable
        public Integer disabledV;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Integer corner;
        @Nullable
        public Integer textColor;
        @Nullable
        public Integer hoverTextColor;
        @Nullable
        public Integer disabledTextColor;
        @Nullable
        public Boolean drawLabel;
    }

    public static class SubGuiStyle {
        @Nullable
        public String id;
        @Nullable
        public String mode;
        @Nullable
        public Boolean draggable;
        @Nullable
        public Boolean dragHandle;
        @Nullable
        public Integer dragX;
        @Nullable
        public Integer dragY;
        @Nullable
        public Integer dragWidth;
        @Nullable
        public Integer dragHeight;
        @Nullable
        public Integer x;
        @Nullable
        public Integer y;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public ControllerStyle style;

        public boolean isEmpty() {
            return (id == null || id.trim().isEmpty())
                   && (mode == null || mode.trim().isEmpty())
                   && draggable == null
                   && dragHandle == null
                   && dragX == null
                   && dragY == null
                   && dragWidth == null
                   && dragHeight == null
                   && x == null
                   && y == null
                   && width == null
                   && height == null
                   && (style == null || style == ControllerStyle.EMPTY || style.isEmpty());
        }

        public static SubGuiStyle copyOf(@Nullable SubGuiStyle source) {
            SubGuiStyle copy = new SubGuiStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.mode = source.mode;
            copy.draggable = source.draggable;
            copy.dragHandle = source.dragHandle;
            copy.dragX = source.dragX;
            copy.dragY = source.dragY;
            copy.dragWidth = source.dragWidth;
            copy.dragHeight = source.dragHeight;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.style = source.style == null ? null : ControllerStyle.copyOf(source.style);
            return copy;
        }
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
        public Float alpha;
        @Nullable
        public String page;
    }

    public static class ProgressBarStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public int width;
        public int height;
        @Nullable
        public Integer backgroundColor;
        @Nullable
        public Integer fillColor;
        @Nullable
        public Integer borderColor;
        @Nullable
        public String texture;
        @Nullable
        public String backgroundTexture;
        @Nullable
        public String fillTexture;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public String direction;
        @Nullable
        public String source;
        @Nullable
        public Integer threadIndex;
        @Nullable
        public String coreThreadId;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public Boolean showText;
        @Nullable
        public Integer textColor;
    }

    public static class SliderStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public int width;
        public int height;
        @Nullable
        public String key;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float step;
        @Nullable
        public Float initialValue;
        @Nullable
        public String direction;
        @Nullable
        public Integer trackColor;
        @Nullable
        public Integer fillColor;
        @Nullable
        public Integer thumbColor;
        @Nullable
        public Integer borderColor;
        @Nullable
        public Integer thumbWidth;
        @Nullable
        public Integer thumbHeight;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public Boolean showText;
        @Nullable
        public Integer textColor;
    }

    public static class DynamicVisualStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public int width;
        public int height;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public DynamicVisualTransformStyle transform;
        @Nullable
        public DynamicVisualTransformByValueStyle transformByValue;
        @Nullable
        public DynamicVisualVisibilityByValueStyle visibleByValue;
        @Nullable
        public DynamicVisualSourceStyle source;
        @Nullable
        public DynamicVisualHistoryStyle history;
        @Nullable
        public DynamicVisualRendererStyle renderer;
        @Nullable
        public List<DynamicVisualRendererRuleStyle> rendererSwitch;
        @Nullable
        public DynamicVisualRendererByValueStyle rendererByValue;

        public static DynamicVisualStyle copyOf(@Nullable DynamicVisualStyle source) {
            DynamicVisualStyle copy = new DynamicVisualStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.priority = source.priority;
            copy.foreground = source.foreground;
            copy.visible = source.visible;
            copy.page = source.page;
            copy.transform = DynamicVisualTransformStyle.copyOf(source.transform);
            copy.transformByValue = DynamicVisualTransformByValueStyle.copyOf(source.transformByValue);
            copy.visibleByValue = DynamicVisualVisibilityByValueStyle.copyOf(source.visibleByValue);
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            copy.history = DynamicVisualHistoryStyle.copyOf(source.history);
            copy.renderer = DynamicVisualRendererStyle.copyOf(source.renderer);
            if (source.rendererSwitch != null) {
                copy.rendererSwitch = new ArrayList<DynamicVisualRendererRuleStyle>(source.rendererSwitch.size());
                for (DynamicVisualRendererRuleStyle rule : source.rendererSwitch) {
                    copy.rendererSwitch.add(DynamicVisualRendererRuleStyle.copyOf(rule));
                }
            }
            copy.rendererByValue = DynamicVisualRendererByValueStyle.copyOf(source.rendererByValue);
            return copy;
        }
    }

    public static class DynamicVisualTransformStyle {
        @Nullable
        public Float offsetX;
        @Nullable
        public Float offsetY;
        @Nullable
        public Float scale;
        @Nullable
        public Float scaleX;
        @Nullable
        public Float scaleY;
        @Nullable
        public Float rotation;
        @Nullable
        public Float alpha;
        @Nullable
        public String origin;
        @Nullable
        public Float pivotX;
        @Nullable
        public Float pivotY;
        @Nullable
        public String pivotUnit;

        @Nullable
        public static DynamicVisualTransformStyle copyOf(@Nullable DynamicVisualTransformStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualTransformStyle copy = new DynamicVisualTransformStyle();
            copy.offsetX = source.offsetX;
            copy.offsetY = source.offsetY;
            copy.scale = source.scale;
            copy.scaleX = source.scaleX;
            copy.scaleY = source.scaleY;
            copy.rotation = source.rotation;
            copy.alpha = source.alpha;
            copy.origin = source.origin;
            copy.pivotX = source.pivotX;
            copy.pivotY = source.pivotY;
            copy.pivotUnit = source.pivotUnit;
            return copy;
        }
    }

    public static class DynamicVisualTransformByValueStyle {
        @Nullable
        public DynamicVisualDrivenValueStyle offsetX;
        @Nullable
        public DynamicVisualDrivenValueStyle offsetY;
        @Nullable
        public DynamicVisualDrivenValueStyle scale;
        @Nullable
        public DynamicVisualDrivenValueStyle scaleX;
        @Nullable
        public DynamicVisualDrivenValueStyle scaleY;
        @Nullable
        public DynamicVisualDrivenValueStyle rotation;
        @Nullable
        public DynamicVisualDrivenValueStyle alpha;
        @Nullable
        public DynamicVisualDrivenValueStyle pivotX;
        @Nullable
        public DynamicVisualDrivenValueStyle pivotY;

        @Nullable
        public static DynamicVisualTransformByValueStyle copyOf(@Nullable DynamicVisualTransformByValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualTransformByValueStyle copy = new DynamicVisualTransformByValueStyle();
            copy.offsetX = DynamicVisualDrivenValueStyle.copyOf(source.offsetX);
            copy.offsetY = DynamicVisualDrivenValueStyle.copyOf(source.offsetY);
            copy.scale = DynamicVisualDrivenValueStyle.copyOf(source.scale);
            copy.scaleX = DynamicVisualDrivenValueStyle.copyOf(source.scaleX);
            copy.scaleY = DynamicVisualDrivenValueStyle.copyOf(source.scaleY);
            copy.rotation = DynamicVisualDrivenValueStyle.copyOf(source.rotation);
            copy.alpha = DynamicVisualDrivenValueStyle.copyOf(source.alpha);
            copy.pivotX = DynamicVisualDrivenValueStyle.copyOf(source.pivotX);
            copy.pivotY = DynamicVisualDrivenValueStyle.copyOf(source.pivotY);
            return copy;
        }
    }

    public static class DynamicVisualDrivenValueStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public DynamicVisualSourceStyle source;

        @Nullable
        public static DynamicVisualDrivenValueStyle copyOf(@Nullable DynamicVisualDrivenValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualDrivenValueStyle copy = new DynamicVisualDrivenValueStyle();
            copy.min = source.min;
            copy.max = source.max;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            return copy;
        }
    }

    public static class DynamicVisualVisibilityByValueStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float equals;
        @Nullable
        public Boolean invert;
        @Nullable
        public DynamicVisualSourceStyle source;

        @Nullable
        public static DynamicVisualVisibilityByValueStyle copyOf(@Nullable DynamicVisualVisibilityByValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualVisibilityByValueStyle copy = new DynamicVisualVisibilityByValueStyle();
            copy.min = source.min;
            copy.max = source.max;
            copy.equals = source.equals;
            copy.invert = source.invert;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            return copy;
        }
    }

    public static class DynamicVisualSourceStyle {
        @Nullable
        public String type;
        @Nullable
        public String combine;
        @Nullable
        public List<DynamicVisualSourceStyle> sources;
        @Nullable
        public String key;
        @Nullable
        public String metric;
        @Nullable
        public Float defaultValue;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Boolean clamp;
        @Nullable
        public Boolean invert;

        @Nullable
        public static DynamicVisualSourceStyle copyOf(@Nullable DynamicVisualSourceStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualSourceStyle copy = new DynamicVisualSourceStyle();
            copy.type = source.type;
            copy.combine = source.combine;
            if (source.sources != null) {
                copy.sources = new ArrayList<DynamicVisualSourceStyle>(source.sources.size());
                for (DynamicVisualSourceStyle child : source.sources) {
                    copy.sources.add(DynamicVisualSourceStyle.copyOf(child));
                }
            }
            copy.key = source.key;
            copy.metric = source.metric;
            copy.defaultValue = source.defaultValue;
            copy.min = source.min;
            copy.max = source.max;
            copy.clamp = source.clamp;
            copy.invert = source.invert;
            return copy;
        }
    }

    public static class DynamicVisualHistoryStyle {
        @Nullable
        public Boolean enabled;
        @Nullable
        public Integer samples;
        @Nullable
        public Integer intervalTicks;

        @Nullable
        public static DynamicVisualHistoryStyle copyOf(@Nullable DynamicVisualHistoryStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualHistoryStyle copy = new DynamicVisualHistoryStyle();
            copy.enabled = source.enabled;
            copy.samples = source.samples;
            copy.intervalTicks = source.intervalTicks;
            return copy;
        }
    }

    public static class DynamicVisualRendererStyle {
        @Nullable
        public String type;
        @Nullable
        public String direction;
        @Nullable
        public String backgroundTexture;
        @Nullable
        public String fillTexture;
        @Nullable
        public String fallbackTexture;
        @Nullable
        public Integer backgroundColor;
        @Nullable
        public Integer fillColor;
        @Nullable
        public Integer borderColor;
        @Nullable
        public Integer color;
        @Nullable
        public Integer lineColor;
        @Nullable
        public Integer gridColor;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public String mode;
        @Nullable
        public Float startAngle;
        @Nullable
        public Integer innerRadius;
        @Nullable
        public Integer segments;
        @Nullable
        public Integer lineWidth;
        @Nullable
        public Boolean showGrid;
        @Nullable
        public List<DynamicVisualFrameStyle> frames;

        @Nullable
        public static DynamicVisualRendererStyle copyOf(@Nullable DynamicVisualRendererStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualRendererStyle copy = new DynamicVisualRendererStyle();
            copy.type = source.type;
            copy.direction = source.direction;
            copy.backgroundTexture = source.backgroundTexture;
            copy.fillTexture = source.fillTexture;
            copy.fallbackTexture = source.fallbackTexture;
            copy.backgroundColor = source.backgroundColor;
            copy.fillColor = source.fillColor;
            copy.borderColor = source.borderColor;
            copy.color = source.color;
            copy.lineColor = source.lineColor;
            copy.gridColor = source.gridColor;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.mode = source.mode;
            copy.startAngle = source.startAngle;
            copy.innerRadius = source.innerRadius;
            copy.segments = source.segments;
            copy.lineWidth = source.lineWidth;
            copy.showGrid = source.showGrid;
            if (source.frames != null) {
                copy.frames = new ArrayList<DynamicVisualFrameStyle>(source.frames.size());
                for (DynamicVisualFrameStyle frame : source.frames) {
                    copy.frames.add(DynamicVisualFrameStyle.copyOf(frame));
                }
            }
            return copy;
        }
    }

    public static class DynamicVisualRendererByValueStyle {
        @Nullable
        public DynamicVisualDrivenColorStyle backgroundColor;
        @Nullable
        public DynamicVisualDrivenColorStyle fillColor;
        @Nullable
        public DynamicVisualDrivenColorStyle borderColor;
        @Nullable
        public DynamicVisualDrivenColorStyle color;
        @Nullable
        public DynamicVisualDrivenColorStyle lineColor;
        @Nullable
        public DynamicVisualDrivenColorStyle gridColor;

        @Nullable
        public static DynamicVisualRendererByValueStyle copyOf(@Nullable DynamicVisualRendererByValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualRendererByValueStyle copy = new DynamicVisualRendererByValueStyle();
            copy.backgroundColor = DynamicVisualDrivenColorStyle.copyOf(source.backgroundColor);
            copy.fillColor = DynamicVisualDrivenColorStyle.copyOf(source.fillColor);
            copy.borderColor = DynamicVisualDrivenColorStyle.copyOf(source.borderColor);
            copy.color = DynamicVisualDrivenColorStyle.copyOf(source.color);
            copy.lineColor = DynamicVisualDrivenColorStyle.copyOf(source.lineColor);
            copy.gridColor = DynamicVisualDrivenColorStyle.copyOf(source.gridColor);
            return copy;
        }
    }

    public static class DynamicVisualRendererRuleStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float equals;
        @Nullable
        public DynamicVisualSourceStyle source;
        @Nullable
        public DynamicVisualRendererStyle renderer;

        @Nullable
        public static DynamicVisualRendererRuleStyle copyOf(@Nullable DynamicVisualRendererRuleStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualRendererRuleStyle copy = new DynamicVisualRendererRuleStyle();
            copy.min = source.min;
            copy.max = source.max;
            copy.equals = source.equals;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            copy.renderer = DynamicVisualRendererStyle.copyOf(source.renderer);
            return copy;
        }
    }

    public static class DynamicVisualDrivenColorStyle {
        @Nullable
        public Integer fromColor;
        @Nullable
        public Integer toColor;
        @Nullable
        public DynamicVisualSourceStyle source;

        @Nullable
        public static DynamicVisualDrivenColorStyle copyOf(@Nullable DynamicVisualDrivenColorStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualDrivenColorStyle copy = new DynamicVisualDrivenColorStyle();
            copy.fromColor = source.fromColor;
            copy.toColor = source.toColor;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            return copy;
        }
    }

    public static class DynamicVisualFrameStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float equals;
        @Nullable
        public String texture;

        public static DynamicVisualFrameStyle copyOf(@Nullable DynamicVisualFrameStyle source) {
            DynamicVisualFrameStyle copy = new DynamicVisualFrameStyle();
            if (source == null) {
                return copy;
            }
            copy.min = source.min;
            copy.max = source.max;
            copy.equals = source.equals;
            copy.texture = source.texture;
            return copy;
        }
    }

}
