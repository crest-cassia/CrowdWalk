// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;

import nodagumi.Itk.Itk;

/**
 * CrowdWalk の起動を司る
 */
public class CrowdWalkLauncher {
    public static String optionsFormat = "[-c] [-g|g2] [-h] [-l <LEVEL>] [-N] [-o] [-t <FILE>] [-f <FALLBACK>]* [-v]"; // これはメソッドによる取得も可能
    public static String commandLineSyntax = String.format("crowdwalk %s [properties-file]", optionsFormat);
    public static String SETTINGS_FILE_NAME = "GuiSimulationLauncher.ini";

    /**
     * インターネット応答確認用 URL (国土地理院の地理院タイル提供サイト)
     */
    public static String RESPONSE_CONFIRMATION_URL = "https://cyberjapandata.gsi.go.jp";

    /**
     * GUI 設定情報のパス設定項目(マップファイル以外)
     */
    private static String[] SETTING_PATH_ITEMS = {"generation", "scenario", "fallback", "obstructer"};

    /**
     * GUI の設定情報
     */
    private static Settings settings = null;

    /**
     * 2D GUI シミュレータを使用する
     */
    public static boolean use2dSimulator = false;

    /**
     * オフラインモード
     */
    public static boolean offline = false;

    /**
     * インターネット接続可能
     */
    public static boolean internetEnabled = false;

    /**
     * "No hint for goal" ログを出力しない
     */
    public static boolean disableNoHintForGoalLog = false;

    /**
     * コマンドラインオプションの定義
     */
    public static void defineOptions(Options options) {
        options.addOption("c", "cui", false, "CUI モードでシミュレーションを開始する\nproperties-file の指定が必須");
        options.addOption("g", "gui", false, "マップエディタウィンドウを開かずに GUI モードでシミュレーションを開始する\nproperties-file の指定が必須");
        options.addOption("2", "use-2d-simulator", false, "2D GUI シミュレータを使用する");
        options.addOption("h", "help", false, "この使い方を表示して終了する");
        options.addOption(OptionBuilder.withLongOpt("log-level")
            .withDescription("ログレベルを指定する\nLEVEL = Trace | Debug | Info | Warn | Error | Fatal")
            .hasArg().withArgName("LEVEL").create("l"));
        options.addOption("N", "disable-no-hint-for-goal-log", false, "\"No hint for goal\" ログを出力しない");
        options.addOption("o", "offline", false, "Internet への接続をおこなわない");
        options.addOption(OptionBuilder.withLongOpt("tick")
            .withDescription("tick 情報を FILE に出力する\nCUI モード時のみ有効")
            .hasArg().withArgName("FILE").create("t"));
        options.addOption(OptionBuilder.withLongOpt("fallback")
                          .withDescription("fallback の先頭に追加する")
                          .hasArg().withArgName("JSON").create("f"));
        options.addOption("v", "version", false, "バージョン情報を表示して終了する");
    }

    /**
     * コマンドラインオプションを解析して指定された処理を実行する
     * @param args : main メソッドの args 引数
     */
    public static void parseCommandLine(String[] args, Options options) {
        String propertiesFilePath = null;
        ArrayList<String> fallbackStringList = new ArrayList<String>() ;

        try {
            CommandLine commandLine = new PosixParser().parse(options, args);
            // ヘルプ表示オプションもしくはコマンドライン引数エラー
            if (commandLine.hasOption("help") || commandLine.getArgs().length > 1) {
                printHelp(options);
                Itk.quitSafely() ;
            }
            // バージョン表示
            if (commandLine.hasOption("version")) {
                System.err.println(
                    "CrowdWalk Version\n" +
                    "----------------------------------------------------------------\n" +
                    getVersion() +
                    "----------------------------------------------------------------");
                Itk.quitSafely() ;
            }

            // fallback への追加
            if (commandLine.hasOption("fallback")) {
                for(String fallback : commandLine.getOptionValues("fallback")) {
                    fallbackStringList.add(fallback) ;
                }
            }

            // プロパティファイルの指定あり
            if (commandLine.getArgs().length == 1) {
                propertiesFilePath = commandLine.getArgs()[0];
            }

            // ログレベルの指定
            if (commandLine.hasOption("log-level")) {
                setLogLevel(commandLine.getOptionValue("log-level"));
            }

            // 2D GUI シミュレータを使用する
            use2dSimulator = commandLine.hasOption("use-2d-simulator");

            // オフラインモード
            offline = commandLine.hasOption("offline");

            // "No hint for goal" ログを出力しない
            disableNoHintForGoalLog = commandLine.hasOption("disable-no-hint-for-goal-log");

            // CUI モードで実行
            if (commandLine.hasOption("cui")) {
                if (propertiesFilePath == null) {
                    printHelp(options);
                    Itk.quitByError() ;
                }
                CuiSimulationLauncher launcher
                    = launchCuiSimulator(propertiesFilePath, fallbackStringList);
                // tick 情報の出力
                if (commandLine.hasOption("tick")) {
                    String tickFilePath = commandLine.getOptionValue("tick");
                    saveTick(tickFilePath, launcher.simulator.currentTime);
                }
            }
            // GUI モードで実行
            else if (commandLine.hasOption("gui")) {
                if (propertiesFilePath == null) {
                    printHelp(options);
                    Itk.quitByError() ;
                }
                launchGuiSimulator(propertiesFilePath, fallbackStringList);
            }
            // マップエディタの実行
            else {
                launchMapEditor(propertiesFilePath, fallbackStringList);
            }
        } catch (ParseException e) {
            Itk.logError(e.getMessage());
            printHelp(options);
            Itk.quitByError() ;
        } catch (Exception e) {
            Itk.dumpStackTraceOf(e);
            Itk.logError(e.getMessage());
            Itk.quitByError() ;
        }
    }

    /**
     * バージョン情報の取得(最終コミットログの出力で代用)
     */
    public static String getVersion() {
        return ObstructerBase.resourceToString("/commit_version.txt");
    }

    /**
     * ログレベルを設定する
     */
    public static void setLogLevel(String logLevelName) throws Exception {
        for (Map.Entry<Itk.LogLevel, String> entry : Itk.LogTag.entrySet()) {
            if (entry.getValue().equals(Itk.LogTagPrefix + logLevelName)) {
                Itk.logLevel = entry.getKey();
                return;
            }
        }
        throw new Exception("Option error - ログレベル名が間違っています: "
                            + logLevelName);
    }

    /**
     * CUI シミュレータを実行する
     */
    public static CuiSimulationLauncher
        launchCuiSimulator(String propertiesFilePath,
                           ArrayList<String> commandLineFallbacks)
    {
        CuiSimulationLauncher launcher =
            new CuiSimulationLauncher(propertiesFilePath,
                                      commandLineFallbacks);
        launcher.initialize();
        launcher.start();
        return launcher;
    }

    /**
     * GUI シミュレータを実行する
     */
    public static GuiSimulationLauncher
        launchGuiSimulator(String propertiesFilePath,
                           ArrayList<String> commandLineFallbacks) throws Exception
    {
        settings = Settings.load(SETTINGS_FILE_NAME);
        GuiSimulationLauncher launcher;
        if (use2dSimulator) {
            launcher = GuiSimulationLauncher.createInstance("GuiSimulationLauncher2D");
        } else {
            if (! GuiSimulationLauncher.isUsableClass("GuiSimulationLauncher3D")) {
                throw new Exception("このビルドには 3D シミュレータ機能が組み込まれていません。");
            }
            launcher = GuiSimulationLauncher.createInstance("GuiSimulationLauncher3D");
        }
        launcher.init(propertiesFilePath, settings, commandLineFallbacks);
        launcher.simulate();
        return launcher;
    }

    /**
     * マップエディタを実行する
     */
    public static void launchMapEditor(String propertiesFilePath,
            ArrayList<String> commandLineFallbacks) throws Exception {
        if (! offline) {
            if (isInternetEnabled()) {
                internetEnabled = true;
            } else {
                offline = true;
            }
        }
        settings = Settings.load(SETTINGS_FILE_NAME);
        MapEditor editor = new MapEditor(settings);
        if (propertiesFilePath != null) {
            editor.setPropertiesFromFile(propertiesFilePath, commandLineFallbacks);
            if (! editor.loadNetworkMap()) {
                Itk.quitByError() ;
            }
        } else {
            editor.initProperties(commandLineFallbacks);
            editor.initNetworkMap();
        }
        editor.show();
    }

    /**
     * tick 情報をファイルに出力する
     */
    public static void saveTick(String filePath, SimTime currentTime) throws IOException {
        PrintWriter writer = new PrintWriter(filePath);
        if (filePath.toLowerCase().endsWith(".json")) {
            writer.write("{\"tick\" : " + currentTime.getRelativeTime() + "}");
        } else {
            writer.println(currentTime.getRelativeTime());
        }
        writer.close();
    }

    /**
     * コマンドラインヘルプを標準出力に表示する
     */
    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        int usageWidth = 7 + commandLineSyntax.length();    // "usege: " + commandLineSyntax
        formatter.setWidth(Math.max(usageWidth, 80));       // 行の折りたたみ最小幅は80桁
        formatter.printHelp(commandLineSyntax, options);
    }

    /**
     * 3D シミュレータが使用可能か?
     */
    public static boolean isUsable3dSimulator() {
        return GuiSimulationLauncher.isUsableClass("GuiSimulationLauncher3D");
    }

    /**
     * インターネットにつながるか?
     */
    public static boolean isInternetEnabled() {
        try {
            Itk.logInfo("Trying HTTP connection to:",
                        RESPONSE_CONFIRMATION_URL) ;
            URL url = new URL(RESPONSE_CONFIRMATION_URL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(1000);    // 接続タイムアウト: 1sec
            con.setReadTimeout(1000);       // 読み取りタイムアウト: 1sec
            con.connect();
            if (con.getResponseCode() == -1) {
                Itk.logWarn("HTTP connection got an unknown error") ;
                Itk.logWarn_("HTTP Error", con.getResponseMessage()) ;
                return false;
            }
            Itk.logInfo("HTTP connection succeeded.") ;
        } catch (SocketTimeoutException e) {
            Itk.logWarn("HTTP GET", e.getMessage());
            return false;
        } catch (IOException e) {
            Itk.logWarn("HTTP GET", e.getMessage());
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        Options options = new Options();
        defineOptions(options);
        parseCommandLine(args, options);

        // Settings の保存
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (settings != null) {
                    // ディレクトリがマップファイルと異なる場合は危険なので保存しない
                    String mapDir = settings.get("mapDir", "");
                    for (String item : SETTING_PATH_ITEMS) {
                        if (! settings.get(item + "Dir", "").equals(mapDir)) {
                            settings.put(item + "Dir", "");
                            settings.put(item + "File", "");
                        }
                    }

                    Settings.save();
                }
            }
        });
    }
}
