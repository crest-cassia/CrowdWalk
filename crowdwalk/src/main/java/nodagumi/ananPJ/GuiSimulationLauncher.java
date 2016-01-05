// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JOptionPane;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;

import nodagumi.Itk.*;

public abstract class GuiSimulationLauncher extends BasicSimulationLauncher {
    /**
     * シミュレーションパネルの幅
     */
    protected int simulationPanelWidth = 800;

    /**
     * シミュレーションパネルの高さ
     */
    protected int simulationPanelHeight = 600;

    /**
     * GUI の設定情報
     */
    protected Settings settings;

    /**
     * ウィンドウクローズと共にアプリケーションを終了するか?
     */
    protected boolean exitOnClose = false;

    /**
     * シミュレーションのメインループ用 Runnable
     */
    protected transient Runnable simulationRunnable = null;

    /**
     * Properties
     */
    public static final String[] SHOW_STATUS_VALUES = {"none", "top", "bottom"};
    public static final String[] IMAGE_TYPES = {"bmp", "gif", "jpg", "png"};
    protected int deferFactor = 0;
    protected double verticalScale = 1.0;
    protected double agentSize = 1.0;
    protected String cameraFile = null;
    protected double zoom = 1.0;
    protected boolean recordSimulationScreen = false;
    protected String screenshotDir = "screenshots";
    protected boolean clearScreenshotDir = false;
    protected String screenshotImageType = "png";
    protected boolean simulationWindowOpen = false;
    protected boolean autoSimulationStart = false;
    protected boolean hideLinks = false;
    protected boolean densityMode = false;
    protected boolean changeAgentColorDependingOnSpeed = true;
    protected boolean showStatus = false;
    protected String showStatusPosition = "top";
    protected boolean showLogo = false;
    protected boolean show3dPolygon = true;
    protected boolean exitWithSimulationFinished = false;

    /**
     * 画像ファイルかどうかをファイル名の拡張子で判別するフィルタ
     */
    protected FilenameFilter imageFileFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            for (String suffix : IMAGE_TYPES) {
                if (name.endsWith("." + suffix)) {
                    return true;
                }
            }
            return false;
        }
    };

    /**
     * アプリ起動時にシミュレーションを開始する時に用いるコンストラクタ.
     */
    public GuiSimulationLauncher(String _propertiesPath, Settings _settings,
            ArrayList<String> commandLineFallbacks) {
        super(null) ;
        // load properties
        setPropertiesFromFile(_propertiesPath, commandLineFallbacks) ;
        setPropertiesForDisplay();

        // check property options
        if (getNetworkMapFile() == null) {
            System.err.println("GuiSimulationLauncher: map file is " +
                    "required.");
            System.exit(1);
        } else if (!((File) new File(getNetworkMapFile())).exists()) {
            System.err.println("GuiSimulationLauncher: specified map file does " +
                    "not exist.");
            System.exit(1);
        }
        try {
            networkMap = readMapWithName(getNetworkMapFile()) ;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }

        settings = _settings;
        exitOnClose = true;
    }

    /**
     * マップエディタからシミュレーションを開始する時に用いるコンストラクタ.
     */
    public GuiSimulationLauncher(Random random, CrowdWalkPropertiesHandler _properties,
            SetupFileInfo _setupFileInfo, NetworkMap _networkMap, Settings _settings) {
        super(random) ;
        properties = _properties;
        setPropertiesForDisplay();
        setupFileInfo = _setupFileInfo;
        networkMap = _networkMap;
        settings = _settings;
        // ending condition
        setExitCount(properties.getExitCount()) ;
        setIsAllAgentSpeedZeroBreak(properties.getIsAllAgentSpeedZeroBreak());
    }

    protected void simulate() {
        // 既に終わっていたら、警告メッセージ
        if (!finished) {
            JOptionPane.showMessageDialog(null,
                    "Previous simulation not finished?",
                    "Could not start simulation",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.gc();

        // シミュレータの実体の初期化
        initializeSimulatorEntity() ;

        // ウィンドウとGUIの構築
        setupFrame();

        System.gc();

        // メインループの Runnable 作成。
        simulationRunnable = new Runnable() {
            public void run() {
                simulateMainLoop() ;
                paused = true ;
            }
        };
    }

    protected void quit() {
        Itk.logInfo("Simulation window closed.") ;
        if (exitOnClose) {
            System.exit(0);
        }
    }

    public void start() {
        synchronized (simulationRunnable) {
            if (paused) {
                paused = false ;
                Thread thread = new Thread(simulationRunnable);
                thread.start();
            }
        }
    }

    public void pause() {
        synchronized (simulationRunnable) {
            paused = true ;
        }
    }

    public void step() {
        synchronized (simulationRunnable) {
            simulateOneStepBare() ;
            if (exitCount > 0 && counter > exitCount) {
                finished = true;
            }
            if (finished) {
                finalize() ;
            }
        }
    }

    public boolean isRunning() {
        return !paused;
    }

    /**
     * シミュレーションの終了処理。
     */
    @Override
    protected void finalize() {
        super.finalize() ;
        if (isExitWithSimulationFinished()) {
            exitOnClose = true;
            quit();
        }
    }

    /**
     * ディスプレーを持つかどうか。
     */
    public boolean hasDisplay() {
        return true;
    }

    /**
     * シミュレーションの完了と共にアプリケーションを終了させるかどうか。
     */
    public abstract boolean isExitWithSimulationFinished();

    /**
     * ウィンドウとGUIを構築する
     */
    protected abstract void setupFrame();

    /**
     * ボタン類のアップデート
     */
    public abstract void update_buttons();

    /**
     * 画面出力用properties設定
     */
    protected void setPropertiesForDisplay() {
        try {
            deferFactor = properties.getInteger("defer_factor", deferFactor);
            if (deferFactor < 0 || deferFactor > 299) {
                throw new Exception("Property error - 設定値が範囲(0～299)外です: defer_factor:" + deferFactor);
            }
            verticalScale = properties.getDouble("vertical_scale", verticalScale);
            if (verticalScale < 0.1 || verticalScale > 49.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～49.9)外です: vertical_scale:" + verticalScale);
            }
            agentSize = properties.getDouble("agent_size", agentSize);
            if (agentSize < 0.1 || agentSize > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.1～9.9)外です: agent_size:" + agentSize);
            }
            zoom = properties.getDouble("zoom", zoom);
            if (zoom < 0.0 || zoom > 9.9) {
                throw new Exception("Property error - 設定値が範囲(0.0～9.9)外です: zoom:" + zoom);
            }
            cameraFile = properties.getFilePath("camera_file", null);
            recordSimulationScreen = properties.getBoolean("record_simulation_screen", recordSimulationScreen);
            screenshotDir = properties.getDirectoryPath("screenshot_dir", screenshotDir).replaceFirst("[/\\\\]+$", "");
            clearScreenshotDir = properties.getBoolean("clear_screenshot_dir", clearScreenshotDir);
            if (clearScreenshotDir && ! properties.isDefined("screenshot_dir")) {
                throw new Exception("Property error - clear_screenshot_dir を有効にするためには screenshot_dir の設定が必要です。");
            }
            if (recordSimulationScreen && ! clearScreenshotDir && new File(screenshotDir).list(imageFileFilter).length > 0) {
                throw new Exception("Property error - スクリーンショットディレクトリに画像ファイルが残っています: screenshot_dir: " + screenshotDir);
            }
            screenshotImageType = properties.getString("screenshot_image_type", screenshotImageType, IMAGE_TYPES);
            hideLinks = properties.getBoolean("hide_links", hideLinks);
            densityMode = properties.getBoolean("density_mode", densityMode);
            changeAgentColorDependingOnSpeed =
                properties.getBoolean("change_agent_color_depending_on_speed", changeAgentColorDependingOnSpeed);
            String show_status = properties.getString("show_status", "none", SHOW_STATUS_VALUES);
            if (show_status.equals("none")) {
                showStatus = false;
            } else {
                showStatus = true;
                showStatusPosition = show_status;
            }
            showLogo = properties.getBoolean("show_logo", showLogo);
            show3dPolygon = properties.getBoolean("show_3D_polygon", show3dPolygon);
            simulationWindowOpen = properties.getBoolean("simulation_window_open", simulationWindowOpen);
            autoSimulationStart = properties.getBoolean("auto_simulation_start", autoSimulationStart);
            exitWithSimulationFinished = properties.getBoolean("exit_with_simulation_finished",
                    exitWithSimulationFinished);
        } catch(Exception e) {
            //System.err.printf("Property file error: %s\n%s\n", _propertiesFile, e.getMessage());
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    //// properties 変数用のアクセサ ////

    public void setRecordSimulationScreen(boolean b) {
        recordSimulationScreen = b;
    }

    public boolean isRecordSimulationScreen() {
        return recordSimulationScreen;
    }
}
