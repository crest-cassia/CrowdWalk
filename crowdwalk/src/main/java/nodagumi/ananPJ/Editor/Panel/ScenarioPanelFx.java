package nodagumi.ananPJ.Editor.Panel;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import nodagumi.ananPJ.Agents.Factory.AgentFactory;
import nodagumi.ananPJ.Editor.MapEditor;
import nodagumi.ananPJ.Editor.EditorFrameFx;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryList;
import nodagumi.Itk.Term;

/**
 * マップエディタの Scenario タブ用のペイン
 */
public class ScenarioPanelFx extends BorderPane {
    /**
     * データ・モデル
     */
    public static class GenerationModel {
        private AgentFactory factory;

        public GenerationModel(AgentFactory factory) {
            this.factory = factory;
        }

        public AgentFactory getAgentFactory() {
            return factory;
        }

        public String getGoal() {
            if (factory.getGoal() == null) {
                return "---";
            }
            return factory.getGoal().toString();
        }

        public String getStart() {
            if (factory.getStartInfo() == null) {
                return "---";
            }
            return factory.getStartInfo();
        }

        public String getPath() {
            if (factory.getPlannedRoute() == null) {
                return "---";
            }
            return factory.getPlannedRoute().toString();
        }
    }

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * マップエディタのウィンドウフレーム
     */
    private EditorFrameFx frame;

    private Label generationFileLabel;
    private Label scenarioFileLabel;
    private Label fallbackFileLabel;
    private Button generationFileButton = new Button("Open");
    private Button scenarioFileButton = new Button("Open");
    private Button fallbackFileButton = new Button("Open");
    private TableView<GenerationModel> tableView;
    private ObservableList<GenerationModel> generationList = FXCollections.observableArrayList();
    private TableView.TableViewSelectionModel<GenerationModel> selectionModel;

    /**
     * コンストラクタ
     */
    public ScenarioPanelFx(MapEditor editor, EditorFrameFx frame) {
        this.editor = editor;
        this.frame = frame;

        // ジェネレーションファイル
        String fileName = editor.getGenerationFile();
        generationFileLabel = new Label("Generation file: " + (fileName == null ? "None" : fileName));
        generationFileLabel.setPadding(new Insets(4, 0, 0, 0));
        generationFileLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        generationFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Generation File");
            frame.setInitialPath(fileChooser, editor.getGenerationFile());
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All", "*.*"),
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("JSON", "*.json")
            );
            File file = fileChooser.showOpenDialog(frame.getStage());
            if (file != null) {
                editor.setGenerationFile(editor.getRelativePath(file));
                reset();
            }
        });
        BorderPane generationFilePane = new BorderPane();
        generationFilePane.setLeft(generationFileLabel);
        generationFilePane.setRight(generationFileButton);

        // シナリオファイル
        fileName = editor.getScenarioFile();
        scenarioFileLabel = new Label("Scenario file: " + (fileName == null ? "None" : fileName));
        scenarioFileLabel.setPadding(new Insets(4, 0, 0, 0));
        scenarioFileLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        scenarioFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Scenario File");
            frame.setInitialPath(fileChooser, editor.getScenarioFile());
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All", "*.*"),
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("JSON", "*.json")
            );
            File file = fileChooser.showOpenDialog(frame.getStage());
            if (file != null) {
                editor.setScenarioFile(editor.getRelativePath(file));
                scenarioFileLabel.setText("Scenario file: " + editor.getRelativePath(file));
            }
        });
        BorderPane scenarioFilePane = new BorderPane();
        scenarioFilePane.setLeft(scenarioFileLabel);
        scenarioFilePane.setRight(scenarioFileButton);

        // フォールバックファイル
        fileName = editor.getFallbackFile();
        fallbackFileLabel = new Label("Fallback file: " + (fileName == null ? "None" : fileName));
        fallbackFileLabel.setPadding(new Insets(4, 0, 0, 0));
        fallbackFileLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        fallbackFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Fallback File");
            frame.setInitialPath(fileChooser, editor.getFallbackFile());
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All", "*.*"),
                new FileChooser.ExtensionFilter("JSON", "*.json")
            );
            File file = fileChooser.showOpenDialog(frame.getStage());
            if (file != null) {
                editor.setFallbackFile(editor.getRelativePath(file), null);
                fallbackFileLabel.setText("Fallback file: " + editor.getRelativePath(file));
            }
        });
        BorderPane fallbackFilePane = new BorderPane();
        fallbackFilePane.setLeft(fallbackFileLabel);
        fallbackFilePane.setRight(fallbackFileButton);

        VBox configPane = new VBox(2, generationFilePane, scenarioFilePane, fallbackFilePane);
        configPane.setPadding(new Insets(4, 20, 4, 8));

        tableView = createTableView();

        this.setTop(configPane);
        this.setCenter(tableView);
    }

    /**
     * テーブルビューの生成
     */
    private TableView<GenerationModel> createTableView() {
        TableView<GenerationModel> tableView = new TableView<>();
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<GenerationModel, String> goalCol = new TableColumn("Goal");
        TableColumn<GenerationModel, String> startCol = new TableColumn("Start");
        TableColumn<GenerationModel, String> pathCol = new TableColumn("Path");

        goalCol.setCellValueFactory(new PropertyValueFactory<>("goal"));
        startCol.setCellValueFactory(new PropertyValueFactory<>("start"));
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));

        reset();
        tableView.setItems(generationList);
        tableView.getColumns().addAll(goalCol, startCol, pathCol);
 
        selectionModel.getSelectedItems().addListener(new ListChangeListener<GenerationModel>() {
            public void onChanged(ListChangeListener.Change<? extends GenerationModel> changed) {
                editor.deselectNodes();
                editor.deselectLinks();
                // 選択状態の要素すべて
                for (GenerationModel model : changed.getList()) {
                    if (model.getAgentFactory().getStartObject() != null) {
                        int index = getIndex(model);
                        model.getAgentFactory().getStartObject().selected = selectionModel.isSelected(index);
                    }
                }
                frame.getCanvas().repaintLater();
            }
        });

        return tableView;
    }

    /**
     * テーブルの要素を再設定する
     */
    public void reset() {
        String fileName = editor.getGenerationFile();
        generationFileLabel.setText("Generation file: " + (fileName == null ? "None" : fileName));

        fileName = editor.getScenarioFile();
        scenarioFileLabel.setText("Scenario file: " + (fileName == null ? "None" : fileName));

        fileName = editor.getFallbackFile();
        fallbackFileLabel.setText("Fallback file: " + (fileName == null ? "None" : fileName));

        generationList.clear();
        ArrayList<GenerationModel> generationModels = new ArrayList();
        fileName = editor.getGenerationFile();
        if (fileName != null && ! fileName.isEmpty()) {
            try {
                Term fallbackParameters = editor.getSetupFileInfo().fallbackParameters;
                AgentFactoryList factoryList =
		    new AgentFactoryList(fileName, editor.getMap(),
					 fallbackParameters, true, 1.0,
					 new Random());
                for (AgentFactory factory : factoryList) {
                    generationModels.add(new GenerationModel(factory));
                }
            } catch(Exception ex) {
                editor.displayMessage("Error", ex.getMessage(), "Illegal AgentGenerationFile: " + fileName);
                return;
            }
        }
        generationList.addAll(generationModels);
    }

    /**
     * スプリットペインのディバイダが移動した時に呼び出されるコールバック
     */
    public void widthChanged(double width) {
        generationFileLabel.setPrefWidth(width - 28 - generationFileButton.getWidth());
        scenarioFileLabel.setPrefWidth(width - 28 - scenarioFileButton.getWidth());
        fallbackFileLabel.setPrefWidth(width - 28 - fallbackFileButton.getWidth());
    }

    /**
     * 指定された要素のインデックスを取得する
     */
    public int getIndex(GenerationModel model) {
        for (int index = 0; index < generationList.size(); index++) {
            if (generationList.get(index) == model) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 表示を更新する
     */
    public void refresh() {
        tableView.refresh();
    }
}
