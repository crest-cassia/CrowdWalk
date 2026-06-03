// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.AgentAppearance.view2d.AgentAppearance2D;
import nodagumi.ananPJ.Gui.SimulationFrame2D;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.Itk.*;

public class GuiSimulationLauncher2D extends GuiSimulationLauncher {
    /**
     * シミュレーションウィンドウのフレーム
     */
    private SimulationFrame2D simulationFrame;

    /**
     * agent appearance のリスト
     */
    private ArrayList<AgentAppearance2D> agentAppearances = new ArrayList();

    /**
     * シミュレーションの pause 要求。
     */
    @Override
    public void pauseRequest() {
        pause();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (simulationFrame.getStartButton().isSelected()) {
                    simulationFrame.getStartButton().doClick();
                }
            }
        });
    }

    /**
     * サイクル毎の画面描画
     */
    public void updateEveryTick(SimTime currentTime) {
        // 描画のスキップ
        // [2024-06-22 S.Takami] ステップ実行時はステップ毎に描画するが，最終ステップは描画間隔に従う．
        if (currentTime.getTickCount() % displayInterval != 0 && isRunning()) {
            return;
        }

        // 表示の更新
        simulationFrame.updateCamerawork(currentTime);
        simulationFrame.panel.setUpdated(false);
        simulationFrame.setStatusText(getStatusLine());
        updatePanel(simulator.getWalkingAgentCollection());
        update_buttons();
        displayClock(currentTime);
        updateEvacuatedCount();

        if (pauseEnabled && ! paused && currentTime.getTickCount() == simulationFrame.getPauseTime().getTickCount()) {
            pauseRequest();
        }

        if (simulationFrame.isRecordSimulationScreen()) {
            // スクリーンショット
            final String filename = String.format("capture%06d", (int)currentTime.getTickCount());
            while (simulationFrame.getSaveThreadCount() > 16) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Itk.dumpStackTraceOf(e) ;
                }
            }
            if (SwingUtilities.isEventDispatchThread()) {
                simulationFrame.captureScreenShot(filename);
            } else {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            simulationFrame.captureScreenShot(filename);
                        }
                    });
                } catch (Exception e) {
                    Itk.dumpStackTraceOf(e) ;
                }
            }
        } else {
            // 表示の更新が完了するまで待機する
            if (simulationFrame.isViewSynchronized()) {
                synchronized (simulationFrame.panel) {
                    if (! simulationFrame.panel.isUpdated()) {
                        try {
                            simulationFrame.panel.wait(1000);   // 念のため1秒で解除
                        } catch (InterruptedException e) {}
                    }
                }
            }
        }
    }

    /**
     * 画面出力用properties設定
     */
    public void setPropertiesForDisplay() {
        super.setPropertiesForDisplay();
        try {
            agentSize = properties.getDouble("agent_size_2d", agentSize);
            if (agentSize < 0.1 || agentSize > 30.0) {
                throw new Exception("Property error - 設定値が範囲(0.1～30.0)外です: agent_size or agent_size_2d:" + agentSize);
            }
        } catch(Exception e) {
            Itk.logFatal("Property file error", e.getMessage());
            Itk.quitByError() ;
        }
    }

    /**
     * エージェント登録
     */
    public void registerAgent(AgentBase agent) {
        // 何もしない
    }

    /**
     * シミュレーションの完了と共にアプリケーションを終了するかどうか。
     */
    public boolean isExitWithSimulationFinished() {
        return simulationFrame.getExitWithSimulationFinishedCheckBox().isSelected();
    }

    /**
     * エージェント表示の準備
     */
    public void setupAgentView() {
        agentAppearances.clear();
        for (HashMap parameters : loadAgentAppearance()) {
            AgentAppearance2D appearance = new AgentAppearance2D(this, simulationFrame, parameters);
            if (! appearance.isValidFor2D()) {
                Itk.logFatal("2D view not defined", agentAppearanceFile);
                Itk.quitByError() ;
            }
            agentAppearances.add(appearance);
        }
    }

    /**
     * ウィンドウとGUIを構築する
     */
    public void setupFrame() {
        simulationFrame = new SimulationFrame2D("Simulation Preview",
		simulationPanelWidth, simulationPanelHeight, this, properties, mapTiles);

        // エージェント表示の準備
        setupAgentView();
        simulationFrame.panel.setAgentAppearances(agentAppearances);

        int x = settings.get("simulatorPositionX", 0);
        int y = settings.get("simulatorPositionY", 0);
        simulationFrame.setLocation(x, y);
        simulationFrame.setVisible(true);
    }

    /**
     * ボタン類のアップデート
     */
    public void update_buttons() {
        simulationFrame.update_buttons();
    }

    /**
     * 時計表示
     */
    public void displayClock(SimTime currentTime) {
        simulationFrame.displayClock(currentTime);
    }

    /**
     * evacuation count の計算
     */
    public void updateEvacuatedCount() {
        simulationFrame.updateEvacuatedCount(simulator);
    }

    /**
     * シミュレーションパネルの表示を最新の状態に更新する
     */
    public void updatePanel(Collection<AgentBase> walkingAgentCollection) {
        simulationFrame.setWalkingAgents(walkingAgentCollection);
        simulationFrame.panel.repaint();
    }

    // シミュレーションウィンドウが最初に表示された時に呼び出される
    public void simulationWindowOpenedOperation(final SimulationFrame2D frame) {
        // プロパティファイルに設定された情報に従ってシミュレーションウィンドウの各コントロールの初期設定をおこなう
        if (properties == null) {
            return;
        }
        if (autoSimulationStart) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Itk.logInfo("auto simulation start");
                    frame.getStartButton().doClick();
                }
            });
        }
    }

    /**
     * GUI コントロールの初期値をセットする
     */
    public void setGuiValues(SimulationFrame2D frame) {
        frame.setSimulationDeferFactor(deferFactor);
        frame.setAgentSize(agentSize);
        frame.setShowBackgroundImage(showBackgroundImage);
        frame.setShowBackgroundMap(showBackgroundMap);
        frame.setShowTheSea(showTheSea);
        frame.setRecordSimulationScreen(recordSimulationScreen);
        frame.setScreenshotDir(screenshotDir);
        frame.setClearScreenshotDir(clearScreenshotDir);
        frame.setScreenshotImageType(screenshotImageType);
        frame.setChangeAgentColorDependingOnSpeed(changeAgentColorDependingOnSpeed);
        frame.setDrawingAgentByTriageAndSpeedOrder(drawingAgentByTriageAndSpeedOrder);
        frame.setShowStatus(showStatus);

        int statusPosition = 0;
        for (String position : SHOW_STATUS_VALUES) {
            if (position.equals(showStatusPosition)) {
                break;
            }
            statusPosition++;
        }

        frame.setStatusPosition(statusPosition);
        frame.setShowLogo(showLogo);
        frame.setExitWithSimulationFinished(exitWithSimulationFinished);

        String filePath = properties.getFurnishedPath("camera_2d_file", null);
        if (filePath != null) {
            frame.loadCamerawork(filePath);
        }
    }

    /**
     * シミュレーションウィンドウの表示位置を設定情報ファイルに保存する
     */
    public void saveSimulatorPosition(SimulationFrame2D frame) {
        settings.put("simulatorPositionX", frame.getLocationOnScreen().x);
        settings.put("simulatorPositionY", frame.getLocationOnScreen().y);
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
        Itk.quitByCode(exitCode) ;
    }
}
