package com.fushu.mmceguiext;

import net.minecraftforge.common.config.Config;

@Config(modid = MMCEGuiExt.MODID, name = MMCEGuiExt.MODID + "/client")
public class MMCEGuiExtConfig {
    @Config.Comment("总开关：是否启用所有控制器 GUI 替换 / Master switch for all controller GUI replacements.")
    public static boolean enabled = true;

    @Config.Comment("信息区鼠标滚轮步长（像素） / Mouse wheel scroll step in pixels for info panels.")
    @Config.RangeInt(min = 2, max = 64)
    public static int wheelStep = 10;

    @Config.Comment("Nova Engineering Core 兼容模式：手动开启后预热并固定 MMCE GUI 样式缓存，避免首次打开 GUI 时扫描机器 JSON / NovaEngineering-Core compatibility mode: manually enable to preload and pin MMCE GUI style cache, avoiding machine JSON scans on first GUI open.")
    public static boolean novaEngCoreCompatibilityMode = false;

    @Config.Comment("普通机器控制器 GUI 设置 / Settings for normal machine controller GUI.")
    public static MachineController machineController = new MachineController();

    @Config.Comment("集成（工厂）控制器 GUI 设置 / Settings for integrated(factory) controller GUI.")
    public static FactoryController factoryController = new FactoryController();

    @Config.Comment("Item Bus GUI 设置 / Settings for Item Bus GUI.")
    public static ItemBus itemBus = new ItemBus();

    @Config.Comment("Upgrade Bus GUI 设置 / Settings for Upgrade Bus GUI.")
    public static UpgradeBus upgradeBus = new UpgradeBus();

    @Config.Comment("流体仓 GUI 设置 / Settings for Fluid Hatch GUI.")
    public static FluidHatch fluidHatch = new FluidHatch();

    @Config.Comment("AE2 仓室 GUI 设置 / Settings for AE2 bus GUIs.")
    public static AEBus aeBus = new AEBus();

    @Config.Comment("自定义仓室 TOP 显示设置 / The One Probe display settings for custom hatches.")
    public static CustomHatchTop customHatchTop = new CustomHatchTop();

    public static class MachineController {
        @Config.Comment("是否将普通控制器 GUI 替换为可扩展版本 / Replace normal controller GUI with resizable version.")
        public boolean replaceGui = true;

        @Config.Comment("GUI 宽度，最小 176 / GUI width, minimum 176.")
        @Config.RangeInt(min = 176, max = 1024)
        public int guiWidth = 320;

        @Config.Comment("GUI 高度，最小 213 / GUI height, minimum 213.")
        @Config.RangeInt(min = 213, max = 1024)
        public int guiHeight = 213;

        @Config.Comment("Allow requested GUI size to exceed the current screen. Disables the safety clamp that normally fits guiWidth/guiHeight into the Minecraft window.")
        public boolean allowOffscreenGui = false;

        @Config.Comment("信息区 X 偏移，0 表示自动布局 / Info panel X offset, 0 means auto layout.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelX = 0;

        @Config.Comment("信息区 Y 偏移 / Info panel Y offset.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelY = 10;

        @Config.Comment("信息区宽度，0 表示按 GUI 宽度自动计算 / Info panel width, 0 means auto by GUI width.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelWidth = 0;

        @Config.Comment("信息区高度 / Info panel height.")
        @Config.RangeInt(min = 24, max = 1024)
        public int panelHeight = 112;

        @Config.Comment("未写 [panel:id] 前缀时的默认信息区 ID / Default panel id for text without [panel:id] prefix.")
        public String defaultPanelId = "main";

        @Config.Comment("仅自定义贴图模式使用的分区，格式：id,x,y,width,height / Panels for custom-texture mode only. Format: id,x,y,width,height")
        public String[] customPanels = new String[] { "main,182,10,140,112" };

        @Config.Comment("自定义背景贴图资源路径，留空表示无 / Custom background texture resource location. Empty means none.")
        public String backgroundTexture = "";
        
        @Config.Comment("Custom texture origin offset X. Positive value lets texture extend to the left of GUI origin.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetX = 0;

        @Config.Comment("Custom texture origin offset Y. Positive value lets texture extend above GUI origin.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetY = 0;

        @Config.Comment("未提供自定义贴图时是否隐藏 MMCE 默认背景 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("是否隐藏底部玩家背包栏 / Hide the player inventory bar at the bottom.")
        public boolean hidePlayerInventory = false;

        @Config.Comment("是否显示蓝图信息 / Show blueprint information.")
        public boolean showBlueprintInfo = true;

        @Config.Comment("是否显示已找到结构信息 / Show found structure information.")
        public boolean showStructureInfo = true;

        @Config.Comment("是否显示控制器状态信息 / Show controller status information.")
        public boolean showStatusInfo = true;

        @Config.Comment("是否显示并行数信息 / Show parallelism information.")
        public boolean showParallelismInfo = true;

        @Config.Comment("是否显示性能信息，例如 Avg/Search/WorkMode / Show performance information, such as Avg/Search/WorkMode.")
        public boolean showPerformanceInfo = true;

        @Config.Comment("是否使用 9 宫格渲染避免拉伸变形 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = true;

        @Config.Comment("9 宫格背景源贴图宽度 / Background texture source width for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 176;

        @Config.Comment("9 宫格背景源贴图高度 / Background texture source height for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 213;

        @Config.Comment("9 宫格使用的角尺寸 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;

        @Config.Comment("Enable Smart Interface editor in controller GUI.")
        public boolean enableSmartInterfaceEditor = true;

        @Config.Comment("Smart Interface editor X in GUI. -1 = auto place at right-bottom.")
        @Config.RangeInt(min = -1, max = 2048)
        public int smartInterfaceEditorX = -1;

        @Config.Comment("Smart Interface editor Y in GUI. -1 = auto place at right-bottom.")
        @Config.RangeInt(min = -1, max = 2048)
        public int smartInterfaceEditorY = -1;

        @Config.Comment("Smart Interface editor input width.")
        @Config.RangeInt(min = 4, max = 512)
        public int smartInterfaceEditorInputWidth = 68;

        @Config.Comment("Virtual Smart Interface key when no DataPort is bound. Empty = disabled.")
        public String smartInterfaceEditorVirtualKey = "mmcege_virtual_port";
    }

    public static class FactoryController {
        @Config.Comment("是否将集成（工厂）控制器 GUI 替换为可扩展版本 / Replace integrated(factory) controller GUI with resizable version.")
        public boolean replaceGui = true;

        @Config.Comment("GUI 宽度，最小 280 / GUI width, minimum 280.")
        @Config.RangeInt(min = 280, max = 1024)
        public int guiWidth = 420;

        @Config.Comment("GUI 高度，最小 213 / GUI height, minimum 213.")
        @Config.RangeInt(min = 213, max = 1024)
        public int guiHeight = 213;

        @Config.Comment("Allow requested GUI size to exceed the current screen. Disables the safety clamp that normally fits guiWidth/guiHeight into the Minecraft window.")
        public boolean allowOffscreenGui = false;

        @Config.Comment("信息区 X 偏移，0 表示使用 MMCE 默认值（113） / Info panel X offset, 0 means MMCE default (113).")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelX = 0;

        @Config.Comment("信息区 Y 偏移 / Info panel Y offset.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelY = 10;

        @Config.Comment("信息区宽度，0 表示按 GUI 宽度自动计算 / Info panel width, 0 means auto by GUI width.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelWidth = 0;

        @Config.Comment("信息区高度 / Info panel height.")
        @Config.RangeInt(min = 24, max = 1024)
        public int panelHeight = 112;

        @Config.Comment("未写 [panel:id] 前缀时的默认信息区 ID / Default panel id for text without [panel:id] prefix.")
        public String defaultPanelId = "main";

        @Config.Comment("仅自定义贴图模式使用的分区，格式：id,x,y,width,height / Panels for custom-texture mode only. Format: id,x,y,width,height")
        public String[] customPanels = new String[] { "main,113,10,159,112" };

        @Config.Comment("自定义背景贴图资源路径，留空表示无 / Custom background texture resource location. Empty means none.")
        public String backgroundTexture = "";
        
        @Config.Comment("Custom texture origin offset X. Positive value lets texture extend to the left of GUI origin.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetX = 0;

        @Config.Comment("Custom texture origin offset Y. Positive value lets texture extend above GUI origin.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetY = 0;

        @Config.Comment("未提供自定义贴图时是否隐藏 MMCE 默认背景 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("是否隐藏底部玩家背包栏 / Hide the player inventory bar at the bottom.")
        public boolean hidePlayerInventory = false;

        @Config.Comment("是否显示蓝图信息 / Show blueprint information.")
        public boolean showBlueprintInfo = true;

        @Config.Comment("是否显示已找到结构信息 / Show found structure information.")
        public boolean showStructureInfo = true;

        @Config.Comment("是否显示控制器状态信息 / Show controller status information.")
        public boolean showStatusInfo = true;

        @Config.Comment("是否显示并行数/线程信息 / Show parallelism/thread information.")
        public boolean showParallelismInfo = true;

        @Config.Comment("是否显示性能信息，例如 Avg/Search/WorkMode / Show performance information, such as Avg/Search/WorkMode.")
        public boolean showPerformanceInfo = true;

        @Config.Comment("是否使用 9 宫格渲染避免拉伸变形 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = true;

        @Config.Comment("9 宫格背景源贴图宽度 / Background texture source width for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 280;

        @Config.Comment("9 宫格背景源贴图高度 / Background texture source height for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 213;

        @Config.Comment("9 宫格使用的角尺寸 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;

        @Config.Comment("Special/core thread background tint color. Hex RGB or ARGB, e.g. B2E5FF or FFB2E5FF.")
        public String specialThreadBackgroundColor = "B2E5FF";

        @Config.Comment("线程队列左上角 X / Thread queue top-left X.")
        @Config.RangeInt(min = 0, max = 2048)
        public int threadQueueX = 8;

        @Config.Comment("线程队列左上角 Y / Thread queue top-left Y.")
        @Config.RangeInt(min = 0, max = 2048)
        public int threadQueueY = 8;

        @Config.Comment("线程队列滚动条左侧 X / Thread queue scrollbar X.")
        @Config.RangeInt(min = 0, max = 2048)
        public int threadScrollbarX = 94;

        @Config.Comment("线程队列滚动条顶部 Y / Thread queue scrollbar Y.")
        @Config.RangeInt(min = 0, max = 2048)
        public int threadScrollbarY = 8;

        @Config.Comment("Visible recipe queue rows on the left panel.")
        @Config.RangeInt(min = 1, max = 20)
        public int queueVisibleRows = 6;

        @Config.Comment("线程队列每行宽度 / Thread queue row width.")
        @Config.RangeInt(min = 24, max = 2048)
        public int threadRowWidth = 86;

        @Config.Comment("线程队列每行高度 / Thread queue row height.")
        @Config.RangeInt(min = 16, max = 2048)
        public int threadRowHeight = 32;

        @Config.Comment("Enable Smart Interface editor in factory GUI.")
        public boolean enableSmartInterfaceEditor = true;

        @Config.Comment("Smart Interface editor X in GUI. -1 = auto place at right-bottom.")
        @Config.RangeInt(min = -1, max = 2048)
        public int smartInterfaceEditorX = -1;

        @Config.Comment("Smart Interface editor Y in GUI. -1 = auto place at right-bottom.")
        @Config.RangeInt(min = -1, max = 2048)
        public int smartInterfaceEditorY = -1;

        @Config.Comment("Smart Interface editor input width.")
        @Config.RangeInt(min = 4, max = 512)
        public int smartInterfaceEditorInputWidth = 68;

        @Config.Comment("Virtual Smart Interface key when no DataPort is bound. Empty = disabled.")
        public String smartInterfaceEditorVirtualKey = "mmcege_virtual_port";
    }

    public static class ItemBus {
        @Config.Comment("是否替换 Item Bus GUI / Replace Item Bus GUI.")
        public boolean replaceGui = true;

        @Config.Comment("GUI 宽度，最小 176 / GUI width, minimum 176.")
        @Config.RangeInt(min = 176, max = 1024)
        public int guiWidth = 176;

        @Config.Comment("GUI 高度，最小 166 / GUI height, minimum 166.")
        @Config.RangeInt(min = 166, max = 1024)
        public int guiHeight = 166;

        @Config.Comment("按 size 配置 Item Bus 背景贴图。格式：size=modid:textures/gui/xxx.png，例如 normal=yourmod:textures/gui/item_bus_normal.png。未配置的 size 回退到 MMCE 原版对应贴图 / Per-size Item Bus background textures.")
        public String[] backgroundTextures = new String[0];

        @Config.Comment("外链 JSON 样式文件名，位于 .minecraft/config/mmceguiext/styles/ 。填写后优先读取 / External JSON style file name.")
        public String styleFile = "";

        @Config.Comment("自定义贴图原点 X 偏移 / Custom texture origin offset X.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetX = 0;

        @Config.Comment("自定义贴图原点 Y 偏移 / Custom texture origin offset Y.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetY = 0;

        @Config.Comment("未提供自定义贴图时是否隐藏 MMCE 默认背景 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("是否隐藏底部玩家背包栏 / Hide the player inventory bar at the bottom.")
        public boolean hidePlayerInventory = false;

        @Config.Comment("是否使用 9 宫格渲染避免拉伸变形 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = false;

        @Config.Comment("背景源贴图宽度 / Background texture source width.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 176;

        @Config.Comment("背景源贴图高度 / Background texture source height.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 166;

        @Config.Comment("9 宫格使用的角尺寸 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;

        @Config.Comment("玩家背包左上角 X / Player inventory top-left X.")
        @Config.RangeInt(min = -1000, max = 2048)
        public int playerInventoryX = 8;

        @Config.Comment("玩家背包左上角 Y / Player inventory top-left Y.")
        @Config.RangeInt(min = -1000, max = 2048)
        public int playerInventoryY = 84;

        @Config.Comment("快捷栏左上角 Y / Player hotbar top Y.")
        @Config.RangeInt(min = -1000, max = 2048)
        public int playerHotbarY = 142;

        @Config.Comment("按尺寸覆盖 Item Bus 槽位坐标。格式：size=x1:y1;x2:y2... 例如 normal=61:18;79:18;97:18;61:36;79:36;97:36")
        public String[] slotLayouts = new String[0];

        @Config.Comment("Item Bus 纹理图层。格式：fg|bg,texture,x,y,width,height,texWidth,texHeight,corner,useNineSlice,priority,alpha。alpha 可省略，支持 0.0-1.0 或 0-255")
        public String[] textureLayers = new String[0];
    }

    public static class UpgradeBus {
        @Config.Comment("是否替换 Upgrade Bus GUI / Replace Upgrade Bus GUI.")
        public boolean replaceGui = true;

        @Config.Comment("GUI 宽度，最小 176 / GUI width, minimum 176.")
        @Config.RangeInt(min = 176, max = 1024)
        public int guiWidth = 176;

        @Config.Comment("GUI 高度，最小 213 / GUI height, minimum 213.")
        @Config.RangeInt(min = 213, max = 1024)
        public int guiHeight = 213;

        @Config.Comment("自定义背景贴图资源路径，留空表示使用原版 Upgrade Bus 背景 / Custom background texture resource location. Empty = use MMCE default Upgrade Bus texture.")
        public String backgroundTexture = "";

        @Config.Comment("外链 JSON 样式文件名，位于 .minecraft/config/mmceguiext/styles/ 。填写后优先读取 / External JSON style file name.")
        public String styleFile = "";

        @Config.Comment("自定义贴图原点 X 偏移 / Custom texture origin offset X.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetX = 0;

        @Config.Comment("自定义贴图原点 Y 偏移 / Custom texture origin offset Y.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetY = 0;

        @Config.Comment("未提供自定义贴图时是否隐藏 MMCE 默认背景 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("是否隐藏底部玩家背包栏 / Hide the player inventory bar at the bottom.")
        public boolean hidePlayerInventory = false;

        @Config.Comment("是否使用 9 宫格渲染避免拉伸变形 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = false;

        @Config.Comment("背景源贴图宽度 / Background texture source width.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 176;

        @Config.Comment("背景源贴图高度 / Background texture source height.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 213;

        @Config.Comment("9 宫格使用的角尺寸 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;

        @Config.Comment("玩家背包左上角 X / Player inventory top-left X.")
        @Config.RangeInt(min = -1000, max = 2048)
        public int playerInventoryX = 8;

        @Config.Comment("玩家背包左上角 Y / Player inventory top-left Y.")
        @Config.RangeInt(min = -1000, max = 2048)
        public int playerInventoryY = 131;

        @Config.Comment("快捷栏左上角 Y / Player hotbar top Y.")
        @Config.RangeInt(min = -1000, max = 2048)
        public int playerHotbarY = 189;

        @Config.Comment("Upgrade Bus 槽位坐标。格式：x1:y1;x2:y2...，按槽位索引顺序排列 / Upgrade Bus slot coordinates in slot-index order.")
        public String slotLayout = "";

        @Config.Comment("说明文字区域左上角 X / Description text top-left X.")
        @Config.RangeInt(min = 0, max = 2048)
        public int textX = 92;

        @Config.Comment("说明文字区域起始 Y / Description text start Y.")
        @Config.RangeInt(min = 0, max = 2048)
        public int textY = 23;

        @Config.Comment("说明文字换行宽度 / Description wrap width.")
        @Config.RangeInt(min = 20, max = 1024)
        public int textWidth = 89;

        @Config.Comment("说明文字最多可见行数 / Maximum visible description lines.")
        @Config.RangeInt(min = 1, max = 100)
        public int maxDescLines = 15;

        @Config.Comment("滚动条左侧 X / Scrollbar left X.")
        @Config.RangeInt(min = 0, max = 2048)
        public int scrollbarX = 156;

        @Config.Comment("滚动条顶部 Y / Scrollbar top Y.")
        @Config.RangeInt(min = 0, max = 2048)
        public int scrollbarY = 17;

        @Config.Comment("滚动条高度 / Scrollbar height.")
        @Config.RangeInt(min = 8, max = 2048)
        public int scrollbarHeight = 106;

        @Config.Comment("Upgrade Bus 纹理图层。格式：fg|bg,texture,x,y,width,height,texWidth,texHeight,corner,useNineSlice,priority,alpha。alpha 可省略，支持 0.0-1.0 或 0-255")
        public String[] textureLayers = new String[0];
    }

    public static class FluidHatch {
        @Config.Comment("是否替换流体仓 GUI / Replace Fluid Hatch GUI.")
        public boolean replaceGui = true;

        @Config.Comment("GUI 宽度，最小 176 / GUI width, minimum 176.")
        @Config.RangeInt(min = 176, max = 1024)
        public int guiWidth = 176;

        @Config.Comment("GUI 高度，最小 166 / GUI height, minimum 166.")
        @Config.RangeInt(min = 166, max = 1024)
        public int guiHeight = 166;

        @Config.Comment("自定义背景贴图资源路径，留空表示使用 MMCE 原版流体仓背景 / Custom background texture resource location. Empty = use MMCE default fluid hatch texture.")
        public String backgroundTexture = "";

        @Config.Comment("外链 JSON 样式文件名，位于 .minecraft/config/mmceguiext/styles/ 。填写后优先读取 / External JSON style file name.")
        public String styleFile = "";

        @Config.Comment("背景源贴图宽度 / Background texture source width.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 176;

        @Config.Comment("背景源贴图高度 / Background texture source height.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 166;

        @Config.Comment("是否使用 9 宫格渲染避免拉伸变形 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = false;

        @Config.Comment("9 宫格使用的角尺寸 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;

        @Config.Comment("自定义贴图原点 X 偏移 / Custom texture origin offset X.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetX = 0;

        @Config.Comment("自定义贴图原点 Y 偏移 / Custom texture origin offset Y.")
        @Config.RangeInt(min = 0, max = 1024)
        public int backgroundTextureOffsetY = 0;

        @Config.Comment("未提供自定义贴图时是否隐藏 MMCE 默认背景 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("Fluid Hatch 纹理图层。格式：fg|bg,texture,x,y,width,height,texWidth,texHeight,corner,useNineSlice,priority,alpha。alpha 可省略，支持 0.0-1.0 或 0-255")
        public String[] textureLayers = new String[0];
    }

    public static class AEBus {
        @Config.Comment("是否启用 AE2 仓室 GUI 替换 / Enable AE2 bus GUI replacement.")
        public boolean enabled = true;

        @Config.Comment("Item Input Bus 背景贴图 / Item Input Bus background texture.")
        public String itemInputBackgroundTexture = "";

        @Config.Comment("Item Output Bus 背景贴图 / Item Output Bus background texture.")
        public String itemOutputBackgroundTexture = "";

        @Config.Comment("Fluid Input Bus 背景贴图 / Fluid Input Bus background texture.")
        public String fluidInputBackgroundTexture = "";

        @Config.Comment("Fluid Output Bus 背景贴图 / Fluid Output Bus background texture.")
        public String fluidOutputBackgroundTexture = "";

        @Config.Comment("Gas Input Bus 背景贴图 / Gas Input Bus background texture.")
        public String gasInputBackgroundTexture = "";

        @Config.Comment("Gas Output Bus 背景贴图 / Gas Output Bus background texture.")
        public String gasOutputBackgroundTexture = "";
    }

    public static class CustomHatchTop {
        @Config.Comment("是否显示自定义仓室名称 / Show custom hatch display name.")
        public boolean showDisplayName = false;

        @Config.Comment("是否显示自定义仓室 definition id / Show custom hatch definition id.")
        public boolean showDefinitionId = false;

        @Config.Comment("是否显示物品输入/输出统计 / Show item input/output counts.")
        public boolean showItemInfo = false;

        @Config.Comment("是否显示 MMCEGE 自己绘制的流体条。注意：TOP 可能仍会自动显示 Forge Fluid 能力条 / Show MMCEGE's own fluid bar. TOP may still auto-render Forge Fluid capability.")
        public boolean showFluidInfo = true;

        @Config.Comment("是否显示 MMCEGE 自己绘制的气体条 / Show MMCEGE's own gas bar.")
        public boolean showGasInfo = true;

        @Config.Comment("是否显示 MMCEGE 自己绘制的能源条 / Show MMCEGE's own energy bar.")
        public boolean showEnergyInfo = true;
    }
}

