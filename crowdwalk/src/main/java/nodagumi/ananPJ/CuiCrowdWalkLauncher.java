// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import nodagumi.ananPJ.CuiSimulationLauncher;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;

import nodagumi.Itk.Itk;

/**
 * CrowdWalk の起動を司る(CUI シミュレータ専用)
 */
public class CuiCrowdWalkLauncher {
    public static String optionsFormat = "[-c] [-h] [-l <LEVEL>] [-N] [-t <FILE>] [-f <FALLBACK>]* [-v] [-V]"; // これはメソッドによる取得も可能
    public static String commandLineSyntax = String.format("crowdwalk %s <properties-file>", optionsFormat);

    /**
     * バージョン情報の取得(最終コミットログの出力で代用)
     */
    public static String getVersion() {
        return ObstructerBase.resourceToString("/commit_version.txt");
    }

    /**
     * コマンドラインオプションの定義
     */
    public static void defineOptions(Options options) {
        options.addOption("c", "cui", false, "互換性維持のためのオプション(動作には影響しない)");
        options.addOption("h", "help", false, "この使い方を表示して終了する");
        options.addOption(OptionBuilder.withLongOpt("log-level")
            .withDescription("ログレベルを指定する\nLEVEL = Trace | Debug | Info | Warn | Error | Fatal")
            .hasArg().withArgName("LEVEL").create("l"));
        options.addOption("N", "disable-no-hint-for-goal-log", false, "\"No hint for goal\" ログを出力しない");
        options.addOption(OptionBuilder.withLongOpt("tick")
            .withDescription("tick 情報を FILE に出力する")
            .hasArg().withArgName("FILE").create("t"));
        options.addOption(OptionBuilder.withLongOpt("fallback")
                          .withDescription("fallback の先頭に追加する")
                          .hasArg().withArgName("JSON").create("f"));
        options.addOption("v", "version", false, "バージョン情報を表示して終了する");
        options.addOption("V", "validate", false, "マップデータを検証する\n問題があればシミュレーションを実行せずに終了する");
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

            // "No hint for goal" ログを出力しない
            CrowdWalkPropertiesHandler.setDisableNoHintForGoalLog(commandLine.hasOption("disable-no-hint-for-goal-log"));

            // マップデータを検証する
            CrowdWalkPropertiesHandler.setValidation(commandLine.hasOption("validate"));

            if (propertiesFilePath == null) {
                printHelp(options);
                Itk.quitByError() ;
            }
            CuiSimulationLauncher launcher
                = launchCuiSimulator(propertiesFilePath, fallbackStringList);
            // tick 情報の出力
            if (launcher != null && commandLine.hasOption("tick")) {
                String tickFilePath = commandLine.getOptionValue("tick");
                saveTick(tickFilePath, launcher.simulator.currentTime);
            }
        } catch (ParseException e) {
            Itk.logError(e.getMessage()) ;
            printHelp(options);
            Itk.quitByError() ;
        } catch (Exception e) {
            Itk.quitWithStackTrace(e) ;
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
        if (CrowdWalkPropertiesHandler.validation() && ! launcher.getMap().validate()) {
            Itk.logError_("Failed to validate the map data.");
            return null;
        }
        launcher.initialize();
        launcher.start();
        return launcher;
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
        System.out.println("CrowdWalk: CUI シミュレータ専用版");
        HelpFormatter formatter = new HelpFormatter();
        int usageWidth = 7 + commandLineSyntax.length();    // "usege: " + commandLineSyntax
        formatter.setWidth(Math.max(usageWidth, 80));       // 行の折りたたみ最小幅は80桁
        formatter.printHelp(commandLineSyntax, options);
    }

    public static void main(String[] args) {
        Options options = new Options();
        defineOptions(options);
        parseCommandLine(args, options);
    }
}
