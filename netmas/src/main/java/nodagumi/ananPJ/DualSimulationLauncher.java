package nodagumi.ananPJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.vecmath.Vector3d;

//import sun.management.resources.agent;

import nodagumi.ananPJ.Simulator.EvacuationModelBase;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.SimulationController;
import nodagumi.ananPJ.Simulator.SimulationPanel3D;

public class DualSimulationLauncher extends BasicSimulationLauncher
    implements SimulationController {
    /**
     * A simple launcher of simulation. 
     */
//  private static final long serialVersionUID = 3014238657739616307L;

    protected Settings settings;
    protected JFrame frame;
    private static long randseed = 0;
    private Random random = null;

    public DualSimulationLauncher(String[] args, long _randseed)
    throws Exception {
        randseed = _randseed;
        random = new Random(randseed);
        frame = new JFrame("NetMAS Dual Simulation Launhcer");
        settings = Settings.load("NetworkMapEditor.ini");
        parse_args(args);
        setup_scenarios();
        setup_frame();
        frame.setVisible(true);
    }

    
    SimulationPanel3D[] panels = new SimulationPanel3D[2];
    Scenario[] selected_scenarios = new Scenario[2];
    JComboBox scenario_lists[] = new JComboBox[2];

    Container main_panels = new Container();

    class Scenario {
        public Scenario(String _title,
                 String _model,
                 String _generation,
                 String _reaction) {
            title = _title;
            model = _model;
            generation = _generation;
            reaction = _reaction;
        }
        public String title;
        public String model;
        public String generation;
        public String reaction;
    }
    String[] scenario_titles = {"", ""};
    Map<String, Scenario> scenarios = new HashMap<String, Scenario>();
    private void setup_scenarios() {
        String scenario_list_file = settings.get("scenariolist", "scenariolist.ini");
        ArrayList<String> titles = new ArrayList<String>();
        try {
            FileReader fr = new FileReader (scenario_list_file);
            BufferedReader br = new BufferedReader (fr);
            
            String line = null;
            while ((line = br.readLine()) != null) {
                String elems[] = line.split(",");
                if (elems.length == 4) {
                    Scenario scenario = new Scenario(elems[3], elems[0], elems[1], elems[2]);
                    scenarios.put(elems[3], scenario);
                    titles.add(elems[3]);
                }
            }
            
            br.close ();
            fr.close ();
        } catch (FileNotFoundException e) {
            /* No setting file, not a problem */
        } catch (IOException e) {
            e.printStackTrace();
        }
        scenario_titles = titles.toArray(scenario_titles);
    }

    JTabbedPane control_panel = new JTabbedPane();
    Container selection_control = new Container();
    Container waiting_control = new Container();
    Container simulation_control = new Container();

    JTextField console = new JTextField();
    boolean waiting_calculation = false;
    Thread console_thread;
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    private void setup_frame() {
        int width = settings.get("dual_launcher_width", 640);
        int height = settings.get("dual_launcher_height", 100);
        frame.setSize(width, height);
        frame.setLayout(new BorderLayout());
        
        setup_menu();

        /* area to put simulation preview*/
        main_panels.setLayout(new GridLayout(1, 2));
        frame.add(main_panels, BorderLayout.CENTER);
        int w = settings.get("dual_3dpanel_width", 400);
        int h = settings.get("dual_3dpanel_height", 300);
        JLabel dummy_label = new JLabel();
        dummy_label.setBounds(0, 0, w, h);
        dummy_label.setBackground(Color.BLACK);
        dummy_label.setOpaque(true);
        dummy_label.setBorder(new LineBorder(Color.white, 2));
        main_panels.add(dummy_label);
        JLabel dummy_label2 = new JLabel();
        dummy_label2.setBounds(0, 0, w, h);
        dummy_label2.setBackground(Color.BLACK);
        dummy_label2.setOpaque(true);
        dummy_label2.setBorder(new LineBorder(Color.white, 2));
        main_panels.add(dummy_label2);

        /* lists and switches */
        /* selection list */
        Container selection_panel = new Container();
        selection_panel.setLayout(new GridLayout(2, 2));

        for (int i = 0; i < 2; i++) {
            selection_panel.add(new JLabel((i + 1) + "番目のシナリオ"));
        }
            for (int i = 0; i < 2; i++) {
            scenario_lists[i] = new JComboBox(scenario_titles);
            scenario_lists[i].setBorder(new LineBorder(Color.white, 2));
            selection_panel.add(scenario_lists[i]);
        }
        frame.add(selection_panel, BorderLayout.NORTH);

        /* Controls */
        selection_control.setLayout(new GridLayout(1, 5));
        selection_control.add(new JLabel());
        JButton quit_button = new JButton("終了する");
        quit_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) { quit();}}); 
        selection_control.add(quit_button);
        selection_control.add(new JLabel());
        JButton simulate_button = new JButton("この組み合わせで実行する");
        simulate_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) { simulate(false);}}); 
        selection_control.add(simulate_button);
        selection_control.add(new JLabel());
        selection_control.setName("シナリオ選択");
        control_panel.add(selection_control);
    
        waiting_control.setName("計算中");
        waiting_control.setLayout(new BorderLayout());
        PrintStream pstream = new PrintStream(stream);
        console.setEditable(false);
        console.setForeground(Color.red);
        waiting_control.add(console, BorderLayout.CENTER);
        control_panel.add(waiting_control);
        System.setErr(pstream);

        console_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(waiting_calculation) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String message = stream.toString();
                    if (!message.equals("")) {
                        console.setText(message);
                    }
                    stream.reset();
                }
            }
        });
        
        simulation_control.setLayout(new GridLayout(1, 5));
        simulation_control.add(new JLabel());
        JButton pause_button = new JButton("実行を中断する");
        pause_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) { pause();}}); 
        simulation_control.add(pause_button);
        simulation_control.add(new JLabel());
        JButton start_button = new JButton("実行を開始する");
        start_button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) { start();}}); 
        simulation_control.add(start_button);
        simulation_control.add(new JLabel());
        simulation_control.setName("シミュレーションの操作");
        control_panel.add(simulation_control);
        frame.add(control_panel, BorderLayout.SOUTH);
        
        Container view_panel = setup_view_panel();
        frame.add(view_panel, BorderLayout.EAST);
    }
    
    protected int simulation_weight = 0;
    protected JCheckBox hide_normallink_cb;
    protected JCheckBox agent_color_speed;
    protected JCheckBox link_draw_density;
    protected Camerawork selected_camerawork;
    protected JList predefined_camerawork;
    

    private Container setup_view_panel() {
        Container view_panel = new Container();
        view_panel.setLayout(new BorderLayout());

        setup_camerawork_list();

        predefined_camerawork = new JList(camerawork_titles);
        predefined_camerawork.setPreferredSize(new Dimension(100, frame.getHeight() - 300));
        predefined_camerawork.setBorder(BorderFactory.createTitledBorder("カメラワーク"));
        predefined_camerawork.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        predefined_camerawork.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                selected_camerawork = (Camerawork)cameraworks.get(predefined_camerawork.getSelectedValue());
                if (panels[0] != null) {
                    update_camerawork();
                }
            }
        });
        view_panel.add(predefined_camerawork, BorderLayout.SOUTH);

        JPanel view_control = new JPanel();
        view_control.setLayout(new GridLayout(6, 1));
        view_panel.add(view_control, BorderLayout.CENTER);
        
        /* --- simulation weight */
        view_control.add(new JLabel("simulation weight"));
        JScrollBar simulation_weight_control = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, 1000);
    
        simulation_weight_control.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                simulation_weight = e.getValue();
            }
        });
        view_control.add(simulation_weight_control);

        hide_normallink_cb = new JCheckBox("Hide links");
        hide_normallink_cb.setSelected(false);
        hide_normallink_cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (panels[0] == null) return;
                if (hide_normallink_cb.isSelected()) {
                    for (int i = 0; i < 2; i++) {
                        panels[i].link_transparency = 1.0f;
                    }
                } else {
                    for (int i = 0; i < 2; i++) {
                        panels[i].link_transparency = 0.5f;
                    }
                }
                for (int i = 0; i < 2; i++) {
                    panels[i].link_transparency_changed_flag = true;
                }
            }
        });
        view_control.add(hide_normallink_cb);

        agent_color_speed = new JCheckBox("Agent Color by Speed");
        agent_color_speed.setSelected(true);
        agent_color_speed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                update_agent_color_speed();
            }
        });
        view_control.add(agent_color_speed);

        link_draw_density = new JCheckBox("Link draw density");
        link_draw_density.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                update_link_draw_density();
            }
        });
        view_control.add(link_draw_density);

        return view_panel;
    }
    
    class Camerawork{
        public Camerawork(String _title,
                 String _file) {
            title = _title;
            file = _file;
        }
        public String title;
        public String file;
    }

    String[] camerawork_titles = {"", ""};
    Map<String, Camerawork> cameraworks = new HashMap<String, Camerawork>();
    private void setup_camerawork_list() {
        String camerawork_list_file = settings.get("cameraworklist", "cameraworklist.ini");
        ArrayList<String> titles = new ArrayList<String>();
        titles.add("（カメラワークを利用しない）");
        try {
            FileReader fr = new FileReader (camerawork_list_file);
            BufferedReader br = new BufferedReader (fr);
            
            String line = null;
            while ((line = br.readLine()) != null) {
                String elems[] = line.split(",");
                if (elems.length == 2) {
                    Camerawork camerawork = new Camerawork(elems[1], elems[0]);
                    cameraworks.put(elems[1], camerawork);
                    titles.add(elems[1]);
                }
            }
            
            br.close ();
            fr.close ();
        } catch (FileNotFoundException e) {
            /* No setting file, not a problem */
        } catch (IOException e) {
            e.printStackTrace();
        }
        camerawork_titles = titles.toArray(camerawork_titles);
    }

    private void setup_menu() {
        MenuBar menubar = new MenuBar();
        /* file menu {*/
        Menu filemenu = new Menu("Files");
        MenuShortcut ms = new MenuShortcut('Q');
        MenuItem mi = new MenuItem("Quit", ms);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { quit(); } });
        filemenu.add(mi);
        menubar.add(filemenu);

        /* action menu */
        Menu actionmenu = new Menu("Action");
        mi = new MenuItem("Run simulation");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulate(false); } });
        actionmenu.add(mi);
        actionmenu.add(new MenuItem("-"));
        mi = new MenuItem("Start");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { start(); } });
        actionmenu.add(mi);
        mi = new MenuItem("Pause");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pause(); } });
        actionmenu.add(mi);
        menubar.add(actionmenu);
        
        frame.setMenuBar(menubar);
    }
    
    private boolean finished = false;
    private Boolean run_thread = false;
    private Runnable run_simulation;
    protected EvacuationSimulator[] models = new EvacuationSimulator[2];

    class MakeModel implements Runnable {
        int i;
        String scenario_name;
        SimulationController parent;
        public MakeModel(int _i, SimulationController _parent, String _scenario_name) {
            i = _i;
            parent = _parent;
            scenario_name = _scenario_name;
        }
        
        public void run() {
            synchronized (selected_scenarios) {
                NetworkMap network_map = null;
                try {
                    System.out.println(selected_scenarios[i].model);
                    network_map = readMapWithName(selected_scenarios[i].model,
                            random);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage(), "構造物モデルファイルを開けません", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                    return;
                }

                if (!selected_scenarios[i].generation.equals("")) {
                    network_map.setGenerationFile(selected_scenarios[i].generation);
                }
                if (!selected_scenarios[i].reaction.equals("")) {
                    network_map.setResponseFile(selected_scenarios[i].reaction);
                }
                models[i] = new EvacuationSimulator(network_map, parent,
                        scenario_name, random);
            }
            models[i].setup();
            models[i].begin(true);  // tkokada
        }
    }
    
    DualSimulationLauncher launcher = this;
    protected Thread[] updater = new Thread[2];
    
    class Updater implements Runnable {
        int i;
        public boolean finished_single = false;
        public Updater(int _i) {
            i = _i;
        }

        public void run() {
            if (!finished_single) {
                finished_single = models[i].updateEveryTick();
            }
        }
    }

    protected void simulate(boolean isDeserialized) {
        control_panel.setSelectedComponent(waiting_control);
        if (!waiting_calculation) {
            waiting_calculation = true;
            console_thread.start();
        }

        Runnable setup_simulator = new Runnable() {
            @Override
            public void run() {
                Date date = new Date();
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
                String scenario_name = "dual" + format.format(date);
                
                for (int i = 0; i < 2; i++) {
                    String title = (String)scenario_lists[i].getSelectedItem();
                    Scenario scenario = scenarios.get(title);
                    selected_scenarios[i] = scenario;
                }
                /* setting up model */
                ArrayList<Thread> threads = new ArrayList<Thread>();
                
                main_panels.removeAll();
                count = 0;
                
                for (int i = 0; i < 2; i++) {
                    try {
                        Thread thread = new Thread(new MakeModel(i, launcher, scenario_name));
                        thread.start();
                        threads.add(thread);
                        threads.get(i).join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                update_camerawork();
                update_link_draw_density();

                run_simulation = new Runnable() {
                    public void run() {
                        while(!finished && run_thread) {
                            try {
                                Thread.sleep(simulation_weight);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }   
                            /*
                                for (int i = 0; i < 2; i++) {
                                    if (models[i].getAgentHandler().isFinished()) { continue; }
                                    finished &= models[i].updateEveryTick();
                                }*/
                            for (int i = 0; i < 2; i++) {
                                updater[i] = new Thread(new Updater(i));
                                updater[i].start();
                            }
                            for (int i = 0; i < 2; i++) {
                                try {
                                    updater[i].join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }       
                        }
                        
                        synchronized (run_thread) {
                            if (finished) {
                                control_panel.setSelectedComponent(selection_control);
                            }
                            run_thread = false; 
                        }
                    }
                };
                control_panel.setSelectedComponent(simulation_control);
            }
        };
        Thread thread = new Thread(setup_simulator);
        thread.start();
    }
    private void quit() {
        settings.put("dual_launcher_width", frame.getWidth());
        settings.put("dual_launcher_height", frame.getHeight());
        if (panels[0] != null) {
            settings.put("dual_3dpanel_width", panels[0].getWidth());
            settings.put("dual_3dpanel_height", panels[0].getHeight());
        }
        run_thread = false;
        Settings.save();
        frame.dispose();
        
        System.exit(0);
    }

    protected void update_camerawork() {
        for (int i = 0; i < 2; i++) {
            if (selected_camerawork == null) {
                panels[i].setReplay(false);
            } else {
                panels[i].setReplay(true);
                panels[i].loadCameraworkFromFile(selected_camerawork.file);
            }
        }
    }
    
    protected void update_link_draw_density() {
        if (panels[0] == null) return;
        if (link_draw_density.isSelected()) {
            for (int i = 0; i < 2; i++) {
                panels[i].setLinkDrawDensity(true);
                panels[i].setShowAgents(false);
                panels[i].setShowStructure(false);
                panels[i].setLinkDrawWith(4);
            }
            hide_normallink_cb.setEnabled(false);
            agent_color_speed.setEnabled(false);
        } else {
            for (int i = 0; i < 2; i++) {
                panels[i].setLinkDrawDensity(false);
                panels[i].setShowAgents(true);
                panels[i].setShowStructure(true);
                panels[i].setLinkDrawWith(1);
            }
            hide_normallink_cb.setEnabled(true);
            agent_color_speed.setEnabled(true);
        }
    }
    
    protected void update_agent_color_speed() {
        if (panels[0] == null) return;
        if (agent_color_speed.isSelected()) {
            for (int i = 0; i < 2; i++) {
                panels[i].setAgentColorSpeed(true);
            }
        } else {
            for (int i = 0; i < 2; i++) {
                panels[i].setAgentColorSpeed(false);
            }
        }

    }

    private void parse_args(String[] args) throws Exception {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].charAt(0) == '-' && args[i].charAt(1) == '-') {
                if (i == args.length - 1) {
                    throw new Exception("ERROR in parsing command line options:\n No value given for option " + args[i]);
                }
                final String key = args[i].substring(2);
                final String val = args[++i];
                settings.put(key, val);
            } else {
                throw new Exception("ERROR in parsing command line options:\n Cannot understand " + args[i]);
            }
        }
    }
    
    /* @override */
    public void windowClosing(WindowEvent e) {
        run_thread = false;
        quit();
    }

    @Override
    public void start() {
        synchronized (run_thread) {
            if (run_thread == false) {
                run_thread = true;
                finished = false;
                Thread thread = new Thread(run_simulation);
                thread.start();
            }
        }
    }

    @Override
    public void pause() {
        synchronized (run_thread) {
            run_thread = false;
        }
    }

    @Override
    public void step() {
        synchronized (run_thread) {
            for (int i = 0; i < 2; i++) {
                updater[i] = new Thread(new Updater(i));
                updater[i].start();
            }
            for (int i = 0; i < 2; i++) {
                try {
                    updater[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }       
        }
    }

    @Override
    public boolean isRunning() {
        return run_thread;
    }

    Integer count = 0;
    @Override
    public SimulationPanel3D setupFrame(EvacuationModelBase model) {
        SimulationPanel3D panel = new SimulationPanel3D(model, frame);
        int w = settings.get("dual_3dpanel_width", 400);
        int h = settings.get("dual_3dpanel_height", 300);
        panel.setCanvasSize(w, h);
        panel.initialize();
        main_panels.add(panel, BorderLayout.CENTER);
        panel.addViewChangeListener(this);
        panel.setShowLogo(false);
        panel.setBorder(new LineBorder(Color.white, 2));
        
        //panel.setReplay(replay_recorded_camera_position.isSelected());
        panel.setAgentColorSpeed(agent_color_speed.isSelected());
        frame.setVisible(true);
            
        synchronized (count) {
            panels[count] = panel;
            count++;
        }
        return panel;
    }

    @Override
    public SimulationPanel3D setupFrame(EvacuationModelBase model,
            SimulationPanel3D panel) {
        return null;
    }

    @Override
    public void notifyViewChange(SimulationPanel3D panel) {
        for (int i = 0; i < 2; ++i) {
            if (panel == panels[i]) continue;
            
            panels[i].rot_x = panel.rot_x;
            panels[i].rot_y = panel.rot_y;
            panels[i].rot_z = panel.rot_z;
            panels[i].trans_trans = (Vector3d) panel.trans_trans.clone();
            panels[i].zoom_scale = panel.zoom_scale;
            panels[i].update_viewtrans();
        }
    }
    
    public static void main(String[] args) throws Exception {
        // TODO: implement handling args and get randseed
        long _randseed = 0;
        new DualSimulationLauncher(args, _randseed);
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
