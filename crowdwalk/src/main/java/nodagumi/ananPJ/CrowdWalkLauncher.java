// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import nodagumi.ananPJ.misc.SimTime;

import nodagumi.Itk.Itk;

/**
 * CrowdWalk の起動を司る
 */
public class CrowdWalkLauncher {
    public static String optionsFormat = "[-c] [-g] [-h] [-l <LEVEL>] [-s] [-t <FILE>] [-f <FALLBACK>]* [-v]"; // これはメソッドによる取得も可能
    public static String commandLineSyntax = String.format("crowdwalk %s [properties-file]", optionsFormat);
    public static String SETTINGS_FILE_NAME = "GuiSimulationLauncher.ini";

    /**
     * GUI 設定情報のパス設定項目(マップファイル以外)
     */
    private static String[] SETTING_PATH_ITEMS = {"generation", "scenario", "fallback", "obstructer"};

    /**
     * GUI の設定情報
     */
    private static Settings settings = null;

    /**
     * 経路探索結果の保存フラグ
     */
    public static boolean routesSaving = false;

    /**
     * コマンドラインオプションの定義
     */
    public static void defineOptions(Options options) {
        options.addOption("c", "cui", false, "CUI モードでシミュレーションを開始する\nproperties-file の指定が必須");
        options.addOption("g", "gui", false, "マップエディタウィンドウを開かずに GUI モードでシミュレーションを開始する\nproperties-file の指定が必須");
        options.addOption("h", "help", false, "この使い方を表示して終了する");
        options.addOption(OptionBuilder.withLongOpt("log-level")
            .withDescription("ログレベルを指定する\nLEVEL = Trace | Debug | Info | Warn | Error | Fatal")
            .hasArg().withArgName("LEVEL").create("l"));
        options.addOption("s", "save-routes", false, "経路探索の結果をファイルに保存する\nproperties-file の指定が必須");
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
        CommandLineParser parser = new BasicParser();
        String propertiesFilePath = null;
        ArrayList<String> fallbackStringList = new ArrayList<String>() ;

        try {
            CommandLine commandLine = parser.parse(options, args);
            // ヘルプ表示オプションもしくはコマンドライン引数エラー
            if (commandLine.hasOption("help") || commandLine.getArgs().length > 1) {
                printHelp(options);
                System.exit(0);
            }
            // バージョン表示
            if (commandLine.hasOption("version")) {
                System.err.println("CrowdWalk Version " + GuiSimulationEditorLauncher.getVersion());
                System.exit(0);
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

            // 経路探索結果の保存
            if (commandLine.hasOption("save-routes")) {
                if (propertiesFilePath == null) {
                    printHelp(options);
                    System.exit(1);
                }
                routesSaving = true;
            }

            // CUI モードで実行
            if (commandLine.hasOption("cui")) {
                if (propertiesFilePath == null) {
                    printHelp(options);
                    System.exit(1);
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
                    System.exit(1);
                }
                launchGuiSimulator(propertiesFilePath, fallbackStringList);
            }
            // マップエディタの実行
            else {
                launchGuiSimulationEditorLauncher(propertiesFilePath,
                                                  fallbackStringList);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
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
                           ArrayList<String> commandLineFallbacks)
    {
        settings = Settings.load(SETTINGS_FILE_NAME);
        GuiSimulationLauncher launcher =
            new GuiSimulationLauncher3D(propertiesFilePath,
                    settings, commandLineFallbacks);
        launcher.simulate();
        return launcher;
    }

    /**
     * マップエディタを実行する
     */
    public static GuiSimulationEditorLauncher
        launchGuiSimulationEditorLauncher(String propertiesFilePath,
                                          ArrayList<String> commandLineFallbacks)
        throws Exception
    {
        Random random = new Random();
        settings = Settings.load(SETTINGS_FILE_NAME);
        GuiSimulationEditorLauncher mapEditor
            = new GuiSimulationEditorLauncher(random, settings);
        if (propertiesFilePath != null) {
            mapEditor.setPropertiesFromFile(propertiesFilePath,
                                            commandLineFallbacks);
            mapEditor.setPropertiesForDisplay();
        } else {
            mapEditor.initProperties(commandLineFallbacks);
        }
        mapEditor.updateAll();
        mapEditor.setVisible(true);
        return mapEditor;
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
