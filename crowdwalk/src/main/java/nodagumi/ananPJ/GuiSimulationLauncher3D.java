// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import javafx.embed.swing.JFXPanel;
import javafx.application.Platform;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.AgentAppearance.view3d.AgentAppearance3D;
import nodagumi.ananPJ.Gui.SimulationFrame3D;
import nodagumi.ananPJ.Gui.SimulationViewController3D;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.Itk.*;

/**
 * 3D シミュレータを起動する
 */
public class GuiSimulationLauncher3D extends GuiSimulationLauncher {
    /**
     * シミュレーションウィンドウ
     */
    private SimulationFrame3D simulationFrame;

    /**
     * シミュレーション画面の更新を制御するコントローラ
     */
    private SimulationViewController3D viewController;

    /**
     * agent appearance のリスト
     */
    private ArrayList<AgentAppearance3D> agentAppearances = new ArrayList();

    /**
     * シミュレーションの pause 要求。
     */
    @Override
    public void pauseRequest() {
        pause();
        Platform.runLater(() -> {
            if (simulationFrame.getStartButton().isSelected()) {
                simulationFrame.getStartButton().fire();
            }
        });
    }

    /**
     * サイクル毎の画面描画
     */
    public void updateEveryTick(final SimTime currentTime) {
        // 描画のスキップ
        // [2024-06-22 S.Takami] ステップ実行時はステップ毎に描画するが，最終ステップは描画間隔に従う．
        if (currentTime.getTickCount() % displayInterval != 0 && isRunning()) {
            return;
        }

        viewController.resume();
        viewController.statusChanged("statusText", getStatusLine());
        viewController.statusChanged("displayClock", currentTime);
        viewController.statusChanged("evacuatedCount", simulator.getEvacuatedCountStatus());

        if (pauseEnabled && ! paused && currentTime.getTickCount() == simulationFrame.getPauseTime().getTickCount()) {
            pauseRequest();
        }

        if (isRecordSimulationScreen() || simulationFrame.isViewSynchronized()) {
            final CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                simulationFrame.updateCamerawork(currentTime);
                viewController.updatePanelView();
                update_buttons();
                // スクリーンショット
                if (isRecordSimulationScreen()) {
                    String path = String.format("%s/capture%06d.%s", screenshotDir, (int)currentTime.getTickCount(), screenshotImageType);
                    simulationFrame.captureScreenShot(path, screenshotImageType);
                }
                latch.countDown();
            });
            // 画面の更新が完了するまで待つ
            try {
                latch.await();
            } catch (InterruptedException e) {}
        } else {
            Platform.runLater(() -> {
                simulationFrame.updateCamerawork(currentTime);
                viewController.updatePanelView();
                update_buttons();
            });
        }
        // スクリーンショットを保存するためのスレッドが増えすぎない様にする
        while (simulationFrame.getSaveThreadCount() > 16) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        viewController.suspend();
    }

    /**
     * 画面出力用properties設定
     */
    public void setPropertiesForDisplay() {
        super.setPropertiesForDisplay();
        try {
            agentSize = properties.getDouble("agent_size_3d", agentSize);
            if (agentSize < 0.1 || agentSize > 30.0) {
                throw new Exception("Property error - 設定値が範囲(0.1～30.0)外です: agent_size or agent_size_3d:" + agentSize);
            }
        } catch(Exception e) {
            Itk.logFatal("Property file error", e.getMessage());
            Itk.quitByError() ;
        }
    }

    /**
     * エージェント登録
     */
    public void registerAgent(final AgentBase agent) {
        map.getNotifier().agentAdded(agent);
    }

    /**
     * シミュレーションの完了と共にアプリケーションを終了するかどうか。
     */
    public boolean isExitWithSimulationFinished() {
        return exitWithSimulationFinished;
    }

    /**
     * シミュレーションの完了と共にアプリケーションを終了するかどうかの設定。
     */
    public void setExitWithSimulationFinished(boolean b) {
        exitWithSimulationFinished = b;
    }

    /**
     * エージェント表示の準備
     */
    public void setupAgentView() {
        agentAppearances.clear();
        for (HashMap parameters : loadAgentAppearance()) {
            AgentAppearance3D appearance = new AgentAppearance3D(this, simulationFrame, parameters);
            if (! appearance.isValidFor3D()) {
                Itk.logFatal("3D view not defined", agentAppearanceFile);
                Itk.quitByError() ;
            }
            agentAppearances.add(appearance);
        }
    }

    /**
     * ウィンドウとGUIを構築する
     */
    public void setupFrame() {
        final int x = settings.get("simulatorPositionX", 0);
        final int y = settings.get("simulatorPositionY", 0);
        final GuiSimulationLauncher3D launcher = this;

        JFXPanel fxPanel = new JFXPanel();  // JavaFX アプリケーションスレッドを起動するために必要
        Platform.runLater(() -> {
            simulationFrame = new SimulationFrame3D("Simulation Preview",
                    simulationPanelWidth, simulationPanelHeight, launcher, properties, mapTiles);

            viewController = new SimulationViewController3D(simulationFrame);
            viewController.addNetworkMapPartsListener(map) ;
            viewController.suspend();

            // エージェント表示の準備
            setupAgentView();
            simulationFrame.getSimulationPanel().setAgentAppearances(agentAppearances);

            simulationFrame.setX(x);
            simulationFrame.setY(y);
            simulationFrame.show();
            Platform.setImplicitExit(false);
        });
    }

    /**
     * ボタン類のアップデート
     */
    public void update_buttons() {
        simulationFrame.update_buttons();
    }

    /**
     * シミュレーションウィンドウの表示位置を設定情報ファイルに保存する
     */
    public void saveSimulatorPosition(int x, int y) {
        settings.put("simulatorPositionX", x);
        settings.put("simulatorPositionY", y);
    }

    /**
     * スクリーンショット保存用のスレッド数カウンタ値を取得する
     */
    public int getSaveThreadCount() {
        return simulationFrame.getSaveThreadCount();
    }

    /**
     * アプリケーションを終了する
     */
    public void exit(int exitCode) {
        Platform.runLater(() -> Platform.exit());
    }

    /**
     * accessors
     */

    public int getDeferFactor() {
        return deferFactor;
    }

    public double getVerticalScale() {
        return verticalScale;
    }

    public double getAgentSize() {
        return agentSize;
    }

    public String getCameraFile() {
        return cameraFile;
    }

    public double getZoom() {
        return zoom;
    }

    public boolean isClearScreenshotDir() {
        return clearScreenshotDir;
    }

    public boolean isSimulationWindowOpen() {
        return simulationWindowOpen;
    }

    public boolean isAutoSimulationStart() {
        return autoSimulationStart;
    }

    public boolean isHideLinks() {
        return hideLinks;
    }

    public boolean isDensityMode() {
        return densityMode;
    }

    public boolean isChangeAgentColorDependingOnSpeed() {
        return changeAgentColorDependingOnSpeed;
    }

    public boolean isDrawingAgentByTriageAndSpeedOrder() {
        return drawingAgentByTriageAndSpeedOrder;
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public String getShowStatusPosition() {
        return showStatusPosition;
    }

    public boolean isShowLogo() {
        return showLogo;
    }

    public boolean isShow3dPolygon() {
        return show3dPolygon;
    }

    public boolean isShowBackgroundMap() {
        return showBackgroundMap;
    }

    public boolean isShowTheSea() {
        return showTheSea;
    }
}
