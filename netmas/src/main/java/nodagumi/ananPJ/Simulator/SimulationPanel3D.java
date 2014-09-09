package nodagumi.ananPJ.Simulator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BadTransformException;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PickInfo;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import nodagumi.ananPJ.Gui.Colors;
import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkPanel3D;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Link.Lift.LiftManager;
import nodagumi.ananPJ.NetworkParts.Link.Lift.Shaft;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;

import com.sun.j3d.utils.geometry.Sphere;

public class SimulationPanel3D extends NetworkPanel3D
        implements Serializable {
    private static final long serialVersionUID = -4438166088239555983L;
    public static int MAX_AGENT_COUNT = 500000;

    public static enum gas_display {
        NONE, HSV, RED, BLUE, ORANGE;

        public static String[] getNames() {
            String[] names = new String[values().length];
            int index = 0;
            for (gas_display value : values()) {
                names[index++] = value.toString();
            }
            return names;
        }
    };
    
    public gas_display show_gas = gas_display.ORANGE;
    protected double pollutionColorSaturation = 0.0;

    private List<EvacuationAgent> agents;
    private ArrayList<PollutedArea> pollutions;
    EvacuationModelBase model = null;
    NetworkMap networkMap = null;
    private static String evacuatedCount_string = "";
    
    protected BranchGroup agent_group = null;
    public SimulationPanel3D(EvacuationModelBase _model, JFrame _parent) {
        super(_model.getNodes(), _model.getLinks(), _parent, _model.getProperties());
        model = _model;
        networkMap = model.getMap();
        agents = model.getAgents();
        pollutions = model.getPollutions();

        agent_group = new BranchGroup();
        agent_group.setCapability(BranchGroup.ALLOW_DETACH);
        agent_group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        agent_group.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        viewTrans3D = new Transform3D();
        viewMatrix = new Matrix4d();
        camera_position_list = new ArrayList<CurrentCameraPosition>();
        readProperties();
    }

    public void readProperties() {
        NetmasPropertiesHandler properties = model.getProperties();
        if (properties != null) {
            try {
                show_gas = gas_display.valueOf(properties.getString("pollution_color", "ORANGE", gas_display.getNames()).toUpperCase());
                pollutionColorSaturation = properties.getDouble("pollution_color_saturation", 0.0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
    
    public void deserialize(EvacuationModelBase _model, JFrame _parent) {
        super.deserialize(_model.getNodes(), _model.getLinks(), _parent);
        model = _model;
        networkMap = model.getMap();
        agents = model.getAgents();
        pollutions = model.getPollutions();

        if (agent_group != null)
            agent_group.detach();
        agent_group = null;
        agent_group = new BranchGroup();
        agent_group.setCapability(BranchGroup.ALLOW_DETACH);
        agent_group.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        agent_group.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        viewTrans3D = new Transform3D();
        viewMatrix = new Matrix4d();
        camera_position_list = new ArrayList<CurrentCameraPosition>();
    }

    public JPanel getControlPanel() {
        return control_panel;
    }

    public void updateClock(double time) {
        /* put some weight */
        try {
            while (canvas.hasFrameToCapture()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        update_camerawork(time);
        update_clockstring(time);
    }

    /* control view */
    protected JPanel control_panel = null;
    CheckboxMenuItem menu_item_agent_color_speed = null;
    protected MenuItem menu_item_step = null;
    protected MenuItem menu_item_start = null;
    protected MenuItem menu_item_pause = null;

    double vertical_zoom = 10.0;
    JScrollBar vertical_zoom_control;
    JLabel vertical_zoom_value;

    double agent_size = 1.0;
    float agent_transparency = 0.0f;
    JScrollBar agent_size_control;
    JLabel agent_size_value;

    //Color3f agent_color = GREEN;
    Color3f agent_color = Colors.DEFAULT_AGENT_COLOR;

    JLabel simulation_status;

    JButton record_current_camera_position = null;
    JCheckBox replay_recorded_camera_position = null;
    double scale_on_replay = 1.0;
    JList camerawork_list = null;
    DefaultListModel camerawork_list_datamodel = null;
    JScrollBar scale_on_replay_control;
    JCheckBox record_snapshots = null;

    /* -- view recording */
    Transform3D viewTrans3D = null;
    Matrix4d viewMatrix = null;
    ArrayList<CurrentCameraPosition> camera_position_list = null; 

    JCheckBox debug_mode_cb = null;
    JCheckBox hide_normallink_cb = null;
    JCheckBox density_mode_cb = null;
    JCheckBox show_logo_cb = null;
    JCheckBox show_3d_polygon_cb = null;
    protected JCheckBox change_agent_color_depending_on_speed_cb = null;
    protected JCheckBox show_status_cb = null;
    protected JRadioButton top_rb = null;
    protected JRadioButton bottom_rb = null;

    // Simulation ウィンドウのメニューにメニュー項目を追加する
    @Override
    protected void setupExtraContents() {
        // View menu

        menu_item_agent_color_speed = new CheckboxMenuItem("Change agent color depending on speed", true);
        menu_item_agent_color_speed.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                change_agent_color_depending_on_speed_cb.setSelected(menu_item_agent_color_speed.getState());
            }
        });
        menu_view.add(menu_item_agent_color_speed);

        menu_view.add("-");
        MenuItem item = new MenuItem("Change agent color");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { change_agent_color(); }
        });
        menu_view.add(item);
        item = new MenuItem("Change link color");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { change_link_color(); }
        });
        menu_view.add(item);
        
        Menu menu_pollution_color = new Menu("Change pollution color settings");
        menu_view.add(menu_pollution_color);
        item = new MenuItem("None");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { show_gas = gas_display.NONE; }
        });
        menu_pollution_color.add(item);
        item = new MenuItem("HSV");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { show_gas = gas_display.HSV; }
        });
        menu_pollution_color.add(item);
        item = new MenuItem("Red");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { show_gas = gas_display.RED; }
        });
        menu_pollution_color.add(item);
        item = new MenuItem("Blue");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { show_gas = gas_display.BLUE; }
        });
        menu_pollution_color.add(item);
        item = new MenuItem("Orange");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { show_gas = gas_display.ORANGE; }
        });
        menu_pollution_color.add(item);

        // Action menu

        menu_item_step = new MenuItem("Step");
        menu_item_step.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { model.step(); }
        });
        menu_action.add(menu_item_step);

        menu_item_start = new MenuItem("Start");
        menu_item_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setMenuActionStartEnabled(false);
                model.start();
                model.getAgentHandler().update_buttons();
            }
        });
        menu_action.add(menu_item_start);

        menu_item_pause = new MenuItem("Pause");
        menu_item_pause.setEnabled(false);
        menu_item_pause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setMenuActionStartEnabled(true);
                model.pause();
                model.getAgentHandler().update_buttons();
            }
        });
        menu_action.add(menu_item_pause);
        menu_action.add(new MenuItem("-"));
        
        item = new MenuItem("Load recorded camera");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { load_camera_position_list(); }
        });
        menu_action.add(item);
        
        item = new MenuItem("Save recorded camera");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { save_camera_position_list(); }
        });
        menu_action.add(item);
        
        /* show time */
        simulation_status = new JLabel();
        simulation_status.setText("NOT STARTED");
        simulation_status.setHorizontalAlignment(JLabel.LEFT);
        add(simulation_status, BorderLayout.NORTH);
    }
    
    // Simulation ウィンドウの View パネルに GUI パーツをセットする
    protected void setup_control_panel() {
        /* -- view control */
        control_panel = new JPanel();
        control_panel.setName("View");
        control_panel.setLayout(new BorderLayout());
        JPanel plain_panel = new JPanel();
        control_panel.add(plain_panel, BorderLayout.SOUTH);

        JPanel view_control = new JPanel(new BorderLayout());
        view_control.setPreferredSize(new Dimension(200, 500));
        control_panel.add(view_control, BorderLayout.CENTER);
        
        /* -- zoom */
        JPanel zoom_panel = new JPanel(new GridBagLayout());
        zoom_panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLoweredBevelBorder(), "Scale"));
        /* --- vertical zoom */
        GridBagConstraints c = null;
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        zoom_panel.add(new JLabel("vertical scale"), c);
        Transform3D trans = new Transform3D();
        view_trans.getTransform(trans);
        Vector3d scale_vec = new Vector3d();
        trans.getScale(scale_vec);
        vertical_zoom = scale_vec.z;
        vertical_zoom_control = new JScrollBar(JScrollBar.HORIZONTAL, (int)(vertical_zoom * 10),
                1, 1, 500);
    
        vertical_zoom_control.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {change_vertical_zoom();}
        });
        vertical_zoom_control.setPreferredSize(new Dimension(200, 20));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        zoom_panel.add(vertical_zoom_control, c);
        vertical_zoom_value = new JLabel();
        vertical_zoom_value.setHorizontalAlignment(JLabel.RIGHT);
        vertical_zoom_value.setText("" + vertical_zoom);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        zoom_panel.add(vertical_zoom_value, c);

        /* --- agent size zoom */
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        zoom_panel.add(new JLabel("agent size"), c);
        agent_size_control = new JScrollBar(JScrollBar.HORIZONTAL,
                (int)(agent_size * 10), 1, 1, 100);
        agent_size_control.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                change_agent_size();}
        });
        agent_size_control.setPreferredSize(new Dimension(200, 20));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        zoom_panel.add(agent_size_control, c);
        agent_size_value = new JLabel();
        agent_size_value.setHorizontalAlignment(JLabel.RIGHT);
        agent_size_value.setText("" + agent_size);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        zoom_panel.add(agent_size_value, c);
        view_control.add(zoom_panel, BorderLayout.NORTH);

        /* -- camera */
        JPanel camerawork_panel = new JPanel(new BorderLayout());
        camerawork_panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLoweredBevelBorder(), "Camera"));
        JPanel camerawork_switches = new JPanel(new GridLayout(1, 4));
        JButton open_camerawork = new JButton("Open");
        open_camerawork.setToolTipText("Load camerawork list from file.");
        open_camerawork.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { load_camera_position_list(); }
        });
        camerawork_switches.add(open_camerawork);
        
        JButton save_as_camerawork = new JButton("Save as");
        save_as_camerawork.setToolTipText("Save current camerawork list.");
        save_as_camerawork.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { save_camera_position_list(); }
        });
        camerawork_switches.add(save_as_camerawork);
        
        camerawork_switches.add(new JLabel(""));
        record_current_camera_position = new JButton("Record");
        record_current_camera_position.setToolTipText("Add current camera position to camerawork list");
        record_current_camera_position.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) { record_current_camera_position(); }
        });
        camerawork_switches.add(record_current_camera_position);
        camerawork_panel.add(camerawork_switches, BorderLayout.NORTH);
        
        camerawork_list_datamodel = new DefaultListModel();
        
        camerawork_list = new JList(camerawork_list_datamodel);
        camerawork_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        camerawork_list.setLayoutOrientation(JList.VERTICAL);
        JScrollPane camerawork_list_scroller = new JScrollPane(camerawork_list);
        camerawork_panel.add(camerawork_list_scroller, BorderLayout.CENTER);

        FlowLayout layout = new FlowLayout();
        layout.setHgap(0);
        layout.setVgap(0);
        JPanel _camerawork_replay = new JPanel(layout);     // センタリングのためだけに使用
        JPanel camerawork_replay = new JPanel();
        camerawork_replay.setLayout(new BoxLayout(camerawork_replay, BoxLayout.Y_AXIS));
        replay_recorded_camera_position = new JCheckBox("Replay");
        camerawork_replay.add(replay_recorded_camera_position);

        layout = new FlowLayout(FlowLayout.LEFT);
        JPanel camera_control_panel = new JPanel(layout);
        camera_control_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        camera_control_panel.add(new JLabel("Zoom"));
        scale_on_replay_control = new JScrollBar(JScrollBar.HORIZONTAL, (int)(scale_on_replay * 10),
                1, 0, 100);
        scale_on_replay_control.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                scale_on_replay = ((double)scale_on_replay_control.getValue()) / 10;
            }
        });
        scale_on_replay_control.setPreferredSize(new Dimension(200, 20));
        camera_control_panel.add(scale_on_replay_control);
        camerawork_replay.add(camera_control_panel);

        _camerawork_replay.add(camerawork_replay);
        camerawork_panel.add(_camerawork_replay, BorderLayout.SOUTH);
        view_control.add(camerawork_panel, BorderLayout.CENTER);

        /* -- other checkboxes */
        JPanel checkbox_panel = new JPanel();
        checkbox_panel.setBorder(new CompoundBorder(checkbox_panel.getBorder(), new EmptyBorder(0, 4, 0, 0)));
        checkbox_panel.setLayout(new BoxLayout(checkbox_panel, BoxLayout.Y_AXIS));
        record_snapshots = new JCheckBox("Record simulation screen");
        record_snapshots.setSelected(model.getScreenshotInterval() != 0);
        record_snapshots.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (record_snapshots.isSelected()) {
                    model.setScreenshotInterval(1);
                } else {
                    model.setScreenshotInterval(0);
                }
            }
        });
        checkbox_panel.add(record_snapshots);

        debug_mode_cb = new JCheckBox("Debug mode");
        debug_mode_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (debug_mode_cb.isSelected()) {
                    //TODO implement
                }
            }
        });
        checkbox_panel.add(debug_mode_cb);
        
        /* -- hide normal links, added by bachi */
        hide_normallink_cb = new JCheckBox("Hide links");
        hide_normallink_cb.setSelected(false);
        hide_normallink_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (hide_normallink_cb.isSelected()) {
                    link_transparency = 1.0f;
                } else {
                    link_transparency = 0.5f;
                }
                link_transparency_changed_flag = true;
            }
        });
        checkbox_panel.add(hide_normallink_cb);

        /* -- hide agents and show density by color */
        density_mode_cb = new JCheckBox("Density mode");
        density_mode_cb.setSelected(false);
        density_mode_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (density_mode_cb.isSelected()) {
                    link_draw_density_mode = true;
                    setShowAgents(false);
                    setShowStructure(false);
                } else {
                    link_draw_density_mode = false;
                    setShowAgents(true);
                    setShowStructure(true);
                }
            }
        });
        checkbox_panel.add(density_mode_cb);

        // 歩行速度に応じてエージェントの色を変える
        change_agent_color_depending_on_speed_cb = new JCheckBox("Change agent color depending on speed");
        change_agent_color_depending_on_speed_cb.setSelected(menu_item_agent_color_speed.getState());
        change_agent_color_depending_on_speed_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                menu_item_agent_color_speed.setState(change_agent_color_depending_on_speed_cb.isSelected());
            }
        });
        checkbox_panel.add(change_agent_color_depending_on_speed_cb);

        // シミュレーションビュー上に進捗状況をテキスト表示する、及び表示位置の選択
        layout = new FlowLayout(FlowLayout.LEFT);
        layout.setHgap(0);
        layout.setVgap(2);
        JPanel show_status_panel = new JPanel(layout);
        show_status_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        show_status_cb = new JCheckBox("Show status", show_message);
        top_rb = new JRadioButton("Top", (messagePosition & TOP) == TOP);
        bottom_rb = new JRadioButton("Bottom", (messagePosition & BOTTOM) == BOTTOM);
        top_rb.setEnabled(show_message);
        bottom_rb.setEnabled(show_message);
        show_status_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                show_message = show_status_cb.isSelected();
                top_rb.setEnabled(show_message);
                bottom_rb.setEnabled(show_message);
            }
        });
        show_status_panel.add(show_status_cb);
        top_rb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
               messagePosition = TOP;
            }
        });
        bottom_rb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
               messagePosition = BOTTOM;
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(top_rb);
        bg.add(bottom_rb);
        show_status_panel.add(top_rb);
        show_status_panel.add(bottom_rb);
        checkbox_panel.add(show_status_panel);

        // AIST ロゴの表示
        show_logo_cb = new JCheckBox("Show logo");
        show_logo_cb.setSelected(false);
        show_logo_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                show_logo = show_logo_cb.isSelected();
            }
        });
        checkbox_panel.add(show_logo_cb);

        // tkokada polygon
        show_3d_polygon_cb = new JCheckBox("Show 3D polygon");
        show_3d_polygon_cb.setSelected(true);
        show_3d_polygon_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (show_3d_polygon_cb.isSelected()) {
                    show_3d_polygon = true;
                } else {
                    show_3d_polygon = false;
                }
                //canvas.repaint();
                update3dPolygon();
            }
        });
        checkbox_panel.add(show_3d_polygon_cb);

        JButton set_view_home_button = new JButton("Set view to original");
        set_view_home_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) { setViewToHome(); }
        });
        checkbox_panel.add(set_view_home_button);

        view_control.add(checkbox_panel, BorderLayout.SOUTH);
    }

    private void change_vertical_zoom() {
        double prev_vertical_zoom = vertical_zoom;
        vertical_zoom = vertical_zoom_control.getValue()/10.0;
        vertical_zoom_value.setText("" + vertical_zoom);
        
        Transform3D trans = new Transform3D();
        view_trans.getTransform(trans);
        Vector3d scale_vec = new Vector3d();
        trans.getScale(scale_vec);
        scale_vec.z *= vertical_zoom/prev_vertical_zoom;
        trans.setScale(scale_vec);
        view_trans.setTransform(trans);

        repaint();
    }

    private void change_agent_size() {
        agent_size = agent_size_control.getValue()/10.0;
        agent_size_value.setText("" + agent_size);
        
        repaint();
    }

    public void setMenuActionStartEnabled(boolean b) {
        menu_item_step.setEnabled(b);
        menu_item_start.setEnabled(b);
        menu_item_pause.setEnabled(! b);
    }

    static class CurrentCameraPosition
            implements Comparable<CurrentCameraPosition> {
        double time, vertical_zoom, agent_size;
        Matrix4d matrix;
        
        public CurrentCameraPosition() {
            matrix = new Matrix4d();
        }

        public CurrentCameraPosition(double _time,
                double _vertical_zoom, double _agent_size,
                Matrix4d _matrx) {
            time = _time;
            vertical_zoom = _vertical_zoom;
            agent_size = _agent_size;
            matrix = _matrx;
        }

        static CurrentCameraPosition fromString(String line) throws Exception {
            String items[] = line.split(",");
            if (items.length != 19) {
                throw new Exception("Camera position data error: " + line);
            }
            
            CurrentCameraPosition ret = new CurrentCameraPosition();
            ret.time = Double.parseDouble(items[0]);
            ret.vertical_zoom = Double.parseDouble(items[1]);
            ret.agent_size = Double.parseDouble(items[2]);
            for (int y = 0; y < 4; ++y) {
                for (int x = 0; x < 4; ++x) {
                    double v = Double.parseDouble(items[x + y * 4 + 3]);
                    ret.matrix.setElement(x, y, v);
                }
            }

            return ret;
        }
        
        public String toString() {
            return "" + time;
        }

        public String toSaveString() {
            StringBuffer buffer = new StringBuffer();
            
            buffer.append(time);
            buffer.append(',');
            
            buffer.append(vertical_zoom);
            buffer.append(',');
            
            buffer.append(agent_size);
            
            for (int y = 0; y < 4; ++y) {
                for (int x = 0; x < 4; ++x) {
                    double v = matrix.getElement(x, y);
                    buffer.append(',');
                    buffer.append(v);
                }
            }
            
            return buffer.toString();
        }

        @Override
        public int compareTo(CurrentCameraPosition rhs) {
            double diff = this.time - rhs.time; 
            if (diff > 0) return 1;
            else if (diff < 0) return -1;
            return 0;
        }
    }

    private void record_current_camera_position() {
        view_trans.getTransform(viewTrans3D);
        viewTrans3D.get(viewMatrix);
        CurrentCameraPosition view = new CurrentCameraPosition(model.getTickCount(),
                vertical_zoom, agent_size,
                (Matrix4d)viewMatrix.clone());
        
        camera_position_list.add(view);
        Collections.sort(camera_position_list);
        update_camerawork_list();
    }
    
    public void save_camera_position_list() {
        FileDialog fd = new FileDialog(parent, "Save camera position list", FileDialog.SAVE);
        fd.setVisible (true);
        
        if (fd.getFile() == null) return;
        String filename = fd.getDirectory() + fd.getFile();
        
        try {
            FileWriter writer = new FileWriter(filename);
            PrintWriter pw = new PrintWriter(writer);
            
            for (CurrentCameraPosition position : camera_position_list) {
                pw.println(position.toSaveString());
            }
            
            pw.close();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void load_camera_position_list() {
        FileDialog fd = new FileDialog(parent, "Open camera position list",
                FileDialog.LOAD);
        fd.setVisible (true);

        if (fd.getFile() == null) return;
        String filename = fd.getDirectory() + fd.getFile();

        setReplay(loadCameraworkFromFile(filename));
    }

    public boolean loadCameraworkFromFile(String filename) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));

            camera_position_list.clear();
            String line;

            while ((line = br.readLine()) != null) {
                if (line.charAt(0) =='#') continue;
                line = line.toUpperCase();
                camera_position_list.add(CurrentCameraPosition.fromString(line));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        update_camerawork_list();
        return true;
    }
    
    private void update_camerawork_list() {
        camerawork_list_datamodel.removeAllElements();
        for (CurrentCameraPosition p : camera_position_list) {
            camerawork_list_datamodel.addElement(p);
        }
    }

    //private void change_agent_color() {
    public void change_agent_color() {
        Color color = JColorChooser.showDialog(this, "Select agent color",
                agent_color.get());
        if (color == null) return;
        agent_color = new Color3f(color);
        repaint();
    }

    //private void change_link_color() {
    public void change_link_color() {
        Color color = JColorChooser.showDialog(this, "Select link color",
                link_color.get());
        if (color == null) return;
        link_color = new Color3f(color);
        repaint();
    }
    
    /* making objects */
    protected BranchGroup simulation_map_objects;
    private BoundingSphere bounds = new BoundingSphere(new Point3d(), 200000.0);

    private TransformGroup createTransformGroup(double x, double y, double z) {
        Transform3D trans3d = new Transform3D();
        trans3d.setTranslation(new Vector3d(x, y, z));
        trans3d.setScale(new Vector3d(agent_size,
                agent_size,
                agent_size / vertical_zoom));

        TransformGroup transforms = null;
        try {
            transforms = new TransformGroup(trans3d);
        } catch (BadTransformException e){
            return null;
        }
        transforms.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        transforms.setTransform(trans3d);

        return transforms;
    }

    /* generate object for agent */
    //private void group_from_agent(EvacuationAgent agent) {
    public void group_from_agent(EvacuationAgent agent) {
        if (agent.isEvacuated())
            return;
        /* position */
        Point2D pos = agent.getPos();
        Vector3d swing = agent.getSwing();
        double height = agent.getHeight() /
            ((MapPartGroup)(agent.getCurrentLink()).getParent()).getScale();

        TransformGroup agent_transforms =
            createTransformGroup(pos.getX() + swing.getX(),
                    pos.getY() + swing.getY(),
                    height + swing.getZ());

        /* appearance */
        Appearance app = new Appearance();
        //app.setColoringAttributes(new ColoringAttributes(Colors.WHITE,
        app.setColoringAttributes(new ColoringAttributes(Colors.GREEN,
                    ColoringAttributes.FASTEST));
        app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        TransparencyAttributes ta = new TransparencyAttributes(
                TransparencyAttributes.NICEST, agent_transparency);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        app.setTransparencyAttributes(ta);

        Sphere sphere = new Sphere((float)(2), app);

        agent_transforms.addChild(sphere);
        /* color: tkokada */
        // Background background = new Background(new Color3f(1.0f, 1.0f, 1.0f));
        // background.setApplicationBounds(bounds);

        /* handling */
        BranchGroup bgroup = new BranchGroup();
        bgroup.setCapability(BranchGroup.ALLOW_DETACH);
        bgroup.addChild(agent_transforms);
        UpdateAgent tb = new UpdateAgent(agent, agent_transforms, bgroup, app);
        tb.setSchedulingBounds(bounds);
        agent_transforms.addChild(tb);
        agent_group.addChild(bgroup);
    }

    //private TransformGroup groupFromPollution(PollutedArea pollution) {
    public TransformGroup groupFromPollution(PollutedArea pollution) {
        Appearance app = new Appearance();
        app.setColoringAttributes(new ColoringAttributes(0.2f, 0.2f, 0.2f, ColoringAttributes.SHADE_FLAT));
        app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
        TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.NICEST, 0.5f);
        app.setTransparencyAttributes(ta);

        RenderingAttributes rattr = new RenderingAttributes();
        rattr.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
        rattr.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_FUNCTION_WRITE); 
        rattr.setCapability(RenderingAttributes.ALLOW_DEPTH_ENABLE_READ);
        app.setRenderingAttributes(rattr);

        TransformGroup pollutionTransforms = pollution.get3DShape(app);
        UpdatePollution tb = new UpdatePollution(pollution,
                pollutionTransforms, app);
        tb.setSchedulingBounds(bounds);
        pollutionTransforms.addChild(tb);

        return pollutionTransforms;
    }

    /* used when registering agent while running simulation */
    int agent_count = 0;
    public void registerAgentOnline(EvacuationAgent agent) {
        agent_count++;

        if (agent_count > MAX_AGENT_COUNT) return;
        group_from_agent(agent);
    }

    boolean show_agents = true;
    public void setShowAgents(boolean b) {
        if (b == show_agents) { return; }
        if (b) {
            simulation_map_objects.addChild(agent_group);
            show_agents = true;
        } else {
            agent_group.detach();
            show_agents = false;
        }
    }

    @Override
    protected void register_map_objects() {
        if (getIsInitialized()) {
            simulation_map_objects.detach();
            simulation_map_objects = null;
        }
        simulation_map_objects = new BranchGroup();
        simulation_map_objects.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        simulation_map_objects.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        simulation_map_objects.setCapability(BranchGroup.ALLOW_DETACH);
        simulation_map_objects.addChild(agent_group);
        map_objects.addChild(simulation_map_objects);

        for (EvacuationAgent agent : agents) {
            group_from_agent(agent);
        }

        if (show_gas != gas_display.NONE) {
            for (PollutedArea pollution : pollutions) {
                TransformGroup pollutionTransforms = groupFromPollution(pollution);
                if (pollutionTransforms == null) continue;
                simulation_map_objects.addChild(pollutionTransforms);
            }
        }

        LiftManager lm = LiftManager.getInstance();
        for (String key : lm.keySet()) {
            Shaft shaft = lm.get(key);

            TransformGroup shaftTransforms = new TransformGroup();

            shaftTransforms.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            Appearance app = new Appearance();
            app.setColoringAttributes(new ColoringAttributes(1.0f, 1.0f, 0.0f,
                        ColoringAttributes.FASTEST));
            Sphere sphere = new Sphere(1f, app);
            shaftTransforms.addChild(sphere);
            UpdateShaft tb = new UpdateShaft(shaft, shaftTransforms);
            tb.setSchedulingBounds(bounds);
            shaftTransforms.addChild(tb);

            simulation_map_objects.addChild(shaftTransforms);
        }
    }

    class UpdateAgent extends Behavior{
        WakeupOnElapsedTime won;
        EvacuationAgent agent;
        Appearance app;
        TransformGroup transform_group;
        BranchGroup branch_group;

        public UpdateAgent(EvacuationAgent _agent,
                TransformGroup _transform_group,
                BranchGroup _branch_group,
                Appearance _app){
            won = new WakeupOnElapsedTime(10);
            agent = _agent;
            transform_group = _transform_group;
            branch_group = _branch_group;
            app = _app;
        }

        public void initialize(){
            wakeupOn(won);
        }

        @SuppressWarnings("unchecked")
        public void processStimulus(java.util.Enumeration criteria) {
            if (agent.isEvacuated()) {
                branch_group.detach();
                return;
            }

            MapLink current_pathway = (MapLink)agent.getCurrentLink();
            MapPartGroup group = (MapPartGroup)current_pathway.getParent();
            if (group == null) {
                /* the link the agent is on was removed */
                agent.exposed(10000);
                return;
            } 
            Point2D pos = agent.getPos();
            double height = agent.getHeight();
            height /= group.getScale();;

            TransparencyAttributes ta = app.getTransparencyAttributes();
            ta.setTransparency(agent_transparency);

            /* determine color based on triage */
            switch (agent.getTriage()) {
            case 0://GREEN
            {
                if (menu_item_agent_color_speed.getState()) {
                    if (!((RunningAroundPerson)agent).isPassedNode()){
                        app.setColoringAttributes(new ColoringAttributes(
                                    Colors.YELLOW, ColoringAttributes.FASTEST));
                    } else if (agent.getCurrentLink() != null &&
                            agent.getCurrentLink().getLaneWidth(
                            agent.getDirection()) == 0) {
                        app.setColoringAttributes(new ColoringAttributes(
                                    Colors.LIGHTB, ColoringAttributes.NICEST));
                    } else {
                        /*
                        float f = ((float)(agent.getSpeed())) * 0.31f;
                        Color c_rgb = new Color(Color.HSBtoRGB(f, 0.8f, 0.8f));
                        Color3f c = new Color3f(c_rgb);
                        app.setColoringAttributes(new ColoringAttributes(c,
                                    ColoringAttributes.FASTEST));
                        */
                        app.setColoringAttributes(new ColoringAttributes(
                                    Colors.speedToColor3f(agent.getSpeed()),
                                    ColoringAttributes.FASTEST));
                    }
                } else if (agent.hasTag("BLUE")){
                    app.setColoringAttributes(new ColoringAttributes(
                                Colors.BLUE, ColoringAttributes.FASTEST));
                } else if (agent.hasTag("APINK")){
                    app.setColoringAttributes(new ColoringAttributes(
                                Colors.APINK, ColoringAttributes.FASTEST));
                } else if (agent.hasTag("YELLOW")){
                    app.setColoringAttributes(new ColoringAttributes(
                                Colors.YELLOW, ColoringAttributes.FASTEST));
                } else {
                    app.setColoringAttributes(new ColoringAttributes(
                                agent_color, ColoringAttributes.FASTEST));
                }
            }
            break;
            case 1://YELLOW -> agent_color
                app.setColoringAttributes(new ColoringAttributes(
                            Colors.YELLOW, ColoringAttributes.FASTEST));
                break;
            case 2://RED    -> PINK (Any damaged person ) -> Poisonus red
                app.setColoringAttributes(new ColoringAttributes(
                            Colors.PRED, ColoringAttributes.FASTEST));
                break;
            case 3://PURPLE -> Deeply RED ( Dead )           Poisonus red
                app.setColoringAttributes(new ColoringAttributes(
                            Colors.BLACK2, ColoringAttributes.FASTEST));
                break;
            }

            /* move a bit right or left */
            Vector3d swing = agent.getSwing();

            Transform3D trans3d = new Transform3D();
            trans3d.setTranslation(new Vector3d(pos.getX() + swing.getX(),
                    pos.getY() + swing.getY(),
                    height + swing.getZ()));
            trans3d.setScale(new Vector3d(agent_size,
                    agent_size,
                    agent_size / vertical_zoom));
            try {
                transform_group.setTransform(trans3d);
            } catch (BadTransformException e) {
                if (agent.getCurrentLink() == null) {
                    System.err.println("agent is not on a position");
                } else { 
                    final MapLink l = agent.getCurrentLink();
                    System.err.println(e.getMessage());
                    System.err.println(l.getTagString());
                    System.err.println(" " +
                            l.getFrom().getAbsoluteCoordinates());
                    System.err.println(" " +
                            l.getTo().getAbsoluteCoordinates());
                    System.err.println(" " + l.length);
                }
                System.err.println(pos.getX() + "\t"
                        + pos.getY() + "\t"
                        + agent.getHeight() + "\t");
                agent.dumpResult(System.err);
            }

            wakeupOn(won);
        }
    }

    class UpdateShaft extends Behavior{
        WakeupOnElapsedTime won;
        Shaft shaft;
        TransformGroup transformGroup;

        public UpdateShaft(Shaft _shaft,
                TransformGroup _transformGroup){
            won = new WakeupOnElapsedTime(10);
            shaft = _shaft;
            transformGroup = _transformGroup;
        }

        public void initialize(){
            wakeupOn(won);
        }

        @SuppressWarnings("unchecked")
        public void processStimulus(java.util.Enumeration criteria){
            Point2D pos;
            double height;
            pos = shaft.getPos();
            if (pos == null) return;
            height = shaft.getHeight();

            Transform3D trans3d = new Transform3D();
            trans3d.setTranslation(new Vector3d(pos.getX(), pos.getY(), height));
            try {
                transformGroup.setTransform(trans3d);
            } catch (BadTransformException e) {
                System.err.println(shaft.getPos());
            }
            wakeupOn(won);
        }
    }

    class UpdatePollution extends Behavior{
        WakeupOnElapsedTime won;
        PollutedArea pollution;
        Appearance app;
        TransformGroup transformGroup;

        public UpdatePollution(PollutedArea _pollution,
                TransformGroup _transformGroup,
                Appearance _app){
            won = new WakeupOnElapsedTime(10);
            pollution = _pollution;
            transformGroup = _transformGroup;
            app = _app;
        }

        public void initialize(){
            wakeupOn(won);
        }

        /* When a person immobilize in 20 seconds */
        static final float MAX_DENSITY = 0.01f;

        private Color3f set_color_red(float density) {
            Color c_rgb = new Color(density, 0.0f, 0.0f);
            Color3f c = new Color3f(c_rgb);
            return c;
        }

        private Color3f set_color_blue(float density) {
            Color c_rgb = new Color(0.0f, 0.0f, density);
            Color3f c = new Color3f(c_rgb);
            return c;
        }

        private Color3f set_color_hsv(float density) {
            float f = (1.0f - (float)density) * 0.65f;
            Color c_rgb = new Color(Color.HSBtoRGB(f, 1.0f, 1.0f));
            Color3f c = new Color3f(c_rgb);
            return c;
        }
        private Color3f set_color_orange(float density){
            Color c_rgb = new Color(1.0f,(float)(0.5-density) + 0.5f, 0.0f);
            Color3f c = new Color3f(c_rgb);
            return c;           
        }
        private Color3f set_color_none() {
            Color c_rgb = new Color(0.0f, 0.0f, 0.0f);
            Color3f c = new Color3f(c_rgb);
            return c;
        }       
        @SuppressWarnings("unchecked")
        public void processStimulus(java.util.Enumeration criteria){

            // float density = (float)pollution.getDensity() / MAX_DENSITY;
            float density = 0.0f;
            //System.out.println("Pollution Area ID "+pollution.ID+" density "+density);
            if (pollutionColorSaturation > 0.0) {
                density = (float)pollution.getDensity() / (float)pollutionColorSaturation;
            } else {
                float maxPollutionLevel = (float)((EvacuationSimulator)model).getMaxPollutionLevel();
                if (maxPollutionLevel > 0.0) {
                    density = (float)pollution.getDensity() / (maxPollutionLevel / 2.0f);
                }
            }
            if (density > 1.0) density = 1.0f;

            /*
             * 修正2011年9月29日
             * 有害危険物質の濃度に応じて対象範囲の矩形の色を変更するために
             * 修正点1の濃度が低い場合の矩形の色の更新処理をスキップする部分を削除します。
             * また、修正点2における歩行者がいない場合には対象範囲の矩形の色をなくす処理を削除します。
             *
             * */

            /* 修正点1
            if (density < 0.1) {
                wakeupOn(won);
                return;
            }*/

            /* 修正点2
            Color3f c = null;
            if(!pollution.getContactOfAgents()){
                c = set_color_none();
            } else {
                switch(show_gas) {
                case RED:
                    c = set_color_red(density);
                    break;
                case BLUE:
                    c = set_color_blue(density);
                    break;
                case HSV:
                    c = set_color_hsv(density);
                    break;
                case ORANGE:
                    c = set_color_orange(density);
                    break;
                default:
                    wakeupOn(won);
                    return;
                }

                app.setColoringAttributes(new ColoringAttributes(c,
                        ColoringAttributes.FASTEST));
                app.setTransparencyAttributes(
                        new TransparencyAttributes(TransparencyAttributes.NICEST,
                                1.0f - density / 3.0f));
            }
            */

            /*以下は修正点2の修正部分*/
            Color3f c = null;

            switch(show_gas) {
            case RED:
                c = set_color_red(density);
                break;
            case BLUE:
                c = set_color_blue(density);
                break;
            case HSV:
                c = set_color_hsv(density);
                break;
            case ORANGE:
                c = set_color_orange(density);
                break;
            default:
                wakeupOn(won);
                return;
            }

            app.setColoringAttributes(new ColoringAttributes(c,
                    ColoringAttributes.FASTEST));
            app.setTransparencyAttributes(
                    new TransparencyAttributes(TransparencyAttributes.NICEST,
                            1.0f - density / 1.5f));
                            //1.0f - density / 3.0f));
            
            /*以上は修正点2の修正部分*/
            
            wakeupOn(won);

        }
    }

    @Override
    protected void registerOtherObjects() {
    }
    
    protected void update_clockstring(double time) {
        String time_string =
            String.format("        Elapsed: %5.2fsec        ",
                time);
        String clock_string = model.getAgentHandler().getClockString(time);
        simulation_status.setText("  "+ clock_string + time_string + evacuatedCount_string);
        canvas.message = "Time: " + clock_string + time_string + evacuatedCount_string;
    }

    public static void updateEvacuatedCount(String evacuatedCount){
        evacuatedCount_string = evacuatedCount;
    }

    protected void update_camerawork(double time) {
        if (replay_recorded_camera_position.isSelected() &&
                camera_position_list.size() > 0) {
            CurrentCameraPosition last_camera = null;
            CurrentCameraPosition next_camera = null;
            for (CurrentCameraPosition camera : camera_position_list) {
                if (camera.time > time) {
                    next_camera = camera;
                    break;
                }
                last_camera = camera;
            }
            // tkokada agent size in camera file is ignored.
            if (last_camera == null) {
                viewTrans3D.set(next_camera.matrix);
                //agent_size = next_camera.agent_size;
                vertical_zoom = next_camera.vertical_zoom;
            } else if (next_camera == null) {
                viewTrans3D.set(last_camera.matrix);
                //agent_size = last_camera.agent_size;
                vertical_zoom = last_camera.vertical_zoom;
            } else{
                double ratio = (time - last_camera.time) /
                    (next_camera.time - last_camera.time);
                Matrix4d matrix = new Matrix4d(last_camera.matrix);
                Matrix4d diff_matrix = new Matrix4d(next_camera.matrix);
                diff_matrix.sub(last_camera.matrix);
                diff_matrix.mul(ratio);
                matrix.add(diff_matrix);

                // agent_size = (next_camera.agent_size - last_camera.agent_size)
                    // * ratio + last_camera.agent_size;
                vertical_zoom = (next_camera.vertical_zoom -
                    last_camera.vertical_zoom) * ratio +
                    last_camera.vertical_zoom;

                viewTrans3D.set(matrix);
            }
            Matrix4d matrix = new Matrix4d();
            Matrix4d scale_matrix = new Matrix4d();
            scale_matrix.setIdentity();
            scale_matrix.setScale(scale_on_replay);
            viewTrans3D.get(matrix);
            matrix.mul(scale_matrix);
            viewTrans3D.set(matrix);
            view_trans.setTransform(viewTrans3D);
        }
    }

    MapLink selected_link = null;
    Shape3D selected_shape = null;
    @Override
    protected void mouseClickedCallback(MouseEvent e) {
        if (selected_link != null && e.getButton() == MouseEvent.BUTTON2) {
            System.out.println("removing " + selected_link.getTagString());
            synchronized (model) {
                selected_link.prepareRemove();
                selected_shape.removeAllGeometries();
                int i = 0;
                for (; i < link_geoms.size(); i++) {
                    UpdateLink ul = link_geoms.get(i);
                    ul.disabled = true;
                    if (ul.link == selected_link) break;
                }
                link_geoms.remove(i);
                networkMap.removeOBNode((OBNode)selected_link.getParent(), selected_link, false);
            }
            model.recalculatePaths();
            selected_link = null;
        }
    }

    @Override
    protected void mouseMovedCallback(MouseEvent e) {
        // tkokada
        synchronized(this) {
            if (selected_link != null) {
                selected_link.removeTag("SIM_RED");
            }

            if (true) {//for mouse only
                pick_canvas.setShapeLocation(e);
                PickInfo result = pick_canvas.pickClosest();
                if (result != null &&
                        canvasobj_to_obnode.containsKey(result.getNode())) {
                    selected_shape = (Shape3D) result.getNode();
                    MapLink link = (MapLink)canvasobj_to_obnode.get(
                            selected_shape);
                    selected_link = link;
                    link.addTag("SIM_RED");
                    return;
                }
            }
            selected_link = null;
        }
    }
    @Override
    protected Color3f colors_for_link(MapLink link) {
        if (link.hasTag("SIM_RED")) {
            return Colors.RED;
        }
        return super.colors_for_link(link);
    }

    /* view change by mouse */
    @Override
    protected void view_changed() {
        notifiyViewChangeListeners();
    }

    ArrayList<SimulationController> view_change_listeners =
        new ArrayList<SimulationController>();

    public void addViewChangeListener(SimulationController controller) {
        view_change_listeners.add(controller);
    }
    protected void notifiyViewChangeListeners() {
        for (SimulationController c : view_change_listeners) {
            c.notifyViewChange(this);
        }
    }
    public void setShowLogo(boolean b) {
        show_logo = b;
    }
    
    public void setReplay(boolean b) {
        replay_recorded_camera_position.setSelected(b);
    }
    
    public void setAgentColorSpeed(boolean b) {
        menu_item_agent_color_speed.setState(b);
    }
    
    public void setLinkDrawDensity(boolean b) {
        link_draw_density_mode = b;
    }
    /* to use debug */
    public void printAgents() {
        for (EvacuationAgent ea : agents) {
            System.err.println("SimulationPanel3D.printAgents agent id: " +
                    ea.ID + "position: " + ea.getPosition() + " pos: " + 
                    ea.getPos());
        }
    }

    public List<EvacuationAgent> getAgents() {
        return agents;
    }
    public ArrayList<PollutedArea> getPollutions() {
        return pollutions;
    }
    public BoundingSphere getBoundingSphere() {
        return bounds;
    }

    public void setVerticalScale(double d) {
        vertical_zoom = d;
        vertical_zoom_control.setValue((int)(vertical_zoom * 10.0));
    }

    public void setAgentSize(double d) {
        agent_size = d;
    }

    public void setScaleOnReplay(double d) {
        scale_on_replay = d;
    }

    public JCheckBox getRecordSnapshots() {
        return record_snapshots;
    }

    public JCheckBox getDebugMode() {
        return debug_mode_cb;
    }

    public JCheckBox getHideNormalLink() {
        return hide_normallink_cb;
    }

    public JCheckBox getDensityMode() {
        return density_mode_cb;
    }

    public JCheckBox getChangeAgentColorDependingOnSpeed() {
        return change_agent_color_depending_on_speed_cb;
    }

    public JCheckBox getShowStatus() {
        return show_status_cb;
    }

    public JRadioButton getTop() {
        return top_rb;
    }

    public JRadioButton getBottom() {
        return bottom_rb;
    }

    public JCheckBox getShowLogo() {
        return show_logo_cb;
    }

    public JCheckBox getShow3dPolygon() {
        return show_3d_polygon_cb;
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
