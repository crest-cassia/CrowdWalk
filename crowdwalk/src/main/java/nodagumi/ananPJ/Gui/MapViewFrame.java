package nodagumi.ananPJ.Gui;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;

/**
 * 3D マップ表示専用のウィンドウ
 */
public class MapViewFrame {
    /**
     * ウィンドウフレーム
     */
    private Stage stage;

    /**
     * マップパネル
     */
    public SimulationPanel3D panel;

    private String linkAppearanceFile = null;

    public MapViewFrame() {}

    public MapViewFrame(String title, int width, int height, NetworkMap networkMap, CrowdWalkPropertiesHandler properties, ArrayList<HashMap> linkAppearanceConfig, ArrayList<HashMap> nodeAppearanceConfig) {
        stage = new Stage();
        stage.setTitle(title);

        // メニュー
        Node menuBar = createMenu();

        // シミュレーションパネル
        for (MapNode node : networkMap.getNodes()) {
            node.sortLinkTableByAngle();
        }
        panel = new SimulationPanel3D(width, height, networkMap, 1.0, properties, null, null, linkAppearanceConfig, nodeAppearanceConfig);
        panel.setPrefSize(width, height);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(menuBar);
        borderPane.setCenter(panel);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
    }

    /**
     * メニューの生成
     */
    private Node createMenu() {
        MenuBar menuBar = new MenuBar();

        //// File menu ////

        Menu fileMenu = new Menu("File");

        MenuItem miClose = new MenuItem("Close");
        miClose.setOnAction(e -> stage.close());
        miClose.setAccelerator(KeyCombination.valueOf("Ctrl+W"));

        fileMenu.getItems().addAll(miClose);

        //// View menu ////

        Menu viewMenu = new Menu("View");

        MenuItem miCentering = new MenuItem("Centering");
        miCentering.setOnAction(e -> {
            panel.centering(false, panel.getWidth(), panel.getHeight());
        });

        MenuItem miCenteringWithScaling = new MenuItem("Centering with scaling");
        miCenteringWithScaling.setOnAction(e -> {
            panel.centering(true, panel.getWidth(), panel.getHeight());
        });

        viewMenu.getItems().addAll(miCentering, miCenteringWithScaling);

        menuBar.getMenus().addAll(fileMenu, viewMenu);

        return menuBar;
    }

    public void show() {
        stage.show();
    }
}
