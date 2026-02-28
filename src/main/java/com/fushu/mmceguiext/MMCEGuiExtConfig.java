package com.fushu.mmceguiext;

import net.minecraftforge.common.config.Config;

@Config(modid = MMCEGuiExt.MODID, name = MMCEGuiExt.MODID + "/client")
public class MMCEGuiExtConfig {
    @Config.Comment("鎬诲紑鍏筹細鏄惁鍚敤鎵€鏈夋帶鍒跺櫒GUI鏇挎崲 / Master switch for all controller GUI replacements.")
    public static boolean enabled = true;

    @Config.Comment("淇℃伅鍖洪紶鏍囨粴杞闀匡紙鍍忕礌锛?/ Mouse wheel scroll step in pixels for info panels.")
    @Config.RangeInt(min = 2, max = 64)
    public static int wheelStep = 10;

    @Config.Comment("鏅€氭帶鍒跺櫒GUI璁剧疆 / Settings for normal machine controller GUI.")
    public static MachineController machineController = new MachineController();

    @Config.Comment("闆嗘垚(宸ュ巶)鎺у埗鍣℅UI璁剧疆 / Settings for integrated(factory) controller GUI.")
    public static FactoryController factoryController = new FactoryController();

    public static class MachineController {
        @Config.Comment("鏄惁鏇挎崲鏅€氭帶鍒跺櫒GUI涓哄彲鎵╁睍鐗堟湰 / Replace normal controller GUI with resizable version.")
        public boolean replaceGui = true;

        @Config.Comment("GUI瀹藉害锛屾渶灏?76 / GUI width, minimum 176.")
        @Config.RangeInt(min = 176, max = 1024)
        public int guiWidth = 320;

        @Config.Comment("GUI楂樺害锛屾渶灏?13 / GUI height, minimum 213.")
        @Config.RangeInt(min = 213, max = 1024)
        public int guiHeight = 213;

        @Config.Comment("淇℃伅鍖篨鍋忕Щ锛?涓鸿嚜鍔ㄥ竷灞€ / Info panel X offset, 0 means auto layout.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelX = 0;

        @Config.Comment("淇℃伅鍖篩鍋忕Щ / Info panel Y offset.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelY = 10;

        @Config.Comment("淇℃伅鍖哄搴︼紝0涓烘寜GUI瀹藉害鑷姩璁＄畻 / Info panel width, 0 means auto by GUI width.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelWidth = 0;

        @Config.Comment("淇℃伅鍖洪珮搴?/ Info panel height.")
        @Config.RangeInt(min = 24, max = 1024)
        public int panelHeight = 112;

        @Config.Comment("鏈啓[panel:id]鍓嶇紑鏃剁殑榛樿淇℃伅鍖篒D / Default panel id for text without [panel:id] prefix.")
        public String defaultPanelId = "main";

        @Config.Comment("浠呰嚜瀹氫箟璐村浘妯″紡浣跨敤鐨勫垎鍖猴紝鏍煎紡:id,x,y,width,height / Panels for custom-texture mode only.")
        public String[] customPanels = new String[] { "main,182,10,140,112" };

        @Config.Comment("鑷畾涔夎儗鏅创鍥捐祫婧愯矾寰勶紝鐣欑┖琛ㄧず鏃?/ Custom background texture resource location, empty means none.")
        public String backgroundTexture = "";

        @Config.Comment("鏈彁渚涜嚜瀹氫箟璐村浘鏃舵槸鍚﹂殣钘廙MCE榛樿鑳屾櫙 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("鏄惁浣跨敤9瀹牸鑳屾櫙娓叉煋閬垮厤鎷変几鍙樺舰 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = true;

        @Config.Comment("9瀹牸鑳屾櫙婧愯创鍥惧搴?/ Background texture source width for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 176;

        @Config.Comment("9瀹牸鑳屾櫙婧愯创鍥鹃珮搴?/ Background texture source height for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 213;

        @Config.Comment("9瀹牸鎷愯灏哄 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;
    }

    public static class FactoryController {
        @Config.Comment("鏄惁鏇挎崲闆嗘垚(宸ュ巶)鎺у埗鍣℅UI涓哄彲鎵╁睍鐗堟湰 / Replace integrated(factory) controller GUI with resizable version.")
        public boolean replaceGui = true;

        @Config.Comment("GUI瀹藉害锛屾渶灏?80 / GUI width, minimum 280.")
        @Config.RangeInt(min = 280, max = 1024)
        public int guiWidth = 420;

        @Config.Comment("GUI楂樺害锛屾渶灏?13 / GUI height, minimum 213.")
        @Config.RangeInt(min = 213, max = 1024)
        public int guiHeight = 213;

        @Config.Comment("淇℃伅鍖篨鍋忕Щ锛?琛ㄧず浣跨敤MMCE榛樿(113) / Info panel X offset, 0 means MMCE default (113).")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelX = 0;

        @Config.Comment("淇℃伅鍖篩鍋忕Щ / Info panel Y offset.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelY = 10;

        @Config.Comment("淇℃伅鍖哄搴︼紝0涓烘寜GUI瀹藉害鑷姩璁＄畻 / Info panel width, 0 means auto by GUI width.")
        @Config.RangeInt(min = 0, max = 1024)
        public int panelWidth = 0;

        @Config.Comment("淇℃伅鍖洪珮搴?/ Info panel height.")
        @Config.RangeInt(min = 24, max = 1024)
        public int panelHeight = 112;

        @Config.Comment("鏈啓[panel:id]鍓嶇紑鏃剁殑榛樿淇℃伅鍖篒D / Default panel id for text without [panel:id] prefix.")
        public String defaultPanelId = "main";

        @Config.Comment("浠呰嚜瀹氫箟璐村浘妯″紡浣跨敤鐨勫垎鍖猴紝鏍煎紡:id,x,y,width,height / Panels for custom-texture mode only.")
        public String[] customPanels = new String[] { "main,113,10,159,112" };

        @Config.Comment("鑷畾涔夎儗鏅创鍥捐祫婧愯矾寰勶紝鐣欑┖琛ㄧず鏃?/ Custom background texture resource location, empty means none.")
        public String backgroundTexture = "";

        @Config.Comment("鏈彁渚涜嚜瀹氫箟璐村浘鏃舵槸鍚﹂殣钘廙MCE榛樿鑳屾櫙 / Hide MMCE default background when no custom texture is provided.")
        public boolean hideDefaultBackground = false;

        @Config.Comment("鏄惁浣跨敤9瀹牸鑳屾櫙娓叉煋閬垮厤鎷変几鍙樺舰 / Use 9-slice rendering to avoid stretch distortion.")
        public boolean useNineSlice = true;

        @Config.Comment("9瀹牸鑳屾櫙婧愯创鍥惧搴?/ Background texture source width for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureWidth = 280;

        @Config.Comment("9瀹牸鑳屾櫙婧愯创鍥鹃珮搴?/ Background texture source height for 9-slice rendering.")
        @Config.RangeInt(min = 16, max = 4096)
        public int backgroundTextureHeight = 213;

        @Config.Comment("9瀹牸鎷愯灏哄 / Corner size used by 9-slice rendering.")
        @Config.RangeInt(min = 2, max = 128)
        public int backgroundCorner = 8;

        @Config.Comment("Special/core thread background tint color. Hex RGB or ARGB, e.g. B2E5FF or FFB2E5FF.")
        public String specialThreadBackgroundColor = "B2E5FF";

        @Config.Comment("Visible recipe queue rows on the left panel.")
        @Config.RangeInt(min = 1, max = 20)
        public int queueVisibleRows = 6;
    }
}
