// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

public class GenerateScenario extends JPanel implements Serializable {
    private static final long serialVersionUID = -732709810781415714L;

    JLabel fromFileName = null;
    JTextField toFileName = null;
    JFrame parent = null;
    JLabel status = null;
    Random rand = null;
    
    class GenerationRule {
        public String time;
        public String start;
        public int number, range;
        
        public GenerationRule(String _start,                
                String _time,
                int _number, int _range) {
            time = _time;
            start = _start;
            number = _number;
            range = _range;
        }
        
        public String format(int i) throws IOException {
            return start + "," +
            time + "," + 
            range + "," +
            i;
        }
    }
    
    class Node {
        public ArrayList<Node> children;
        public ArrayList<String> tags;
        public GenerationRule generate = null;
        public String exit = null;
        public int id;

        public Node(int _id) {
            id = _id;
            children = new ArrayList<Node>();
            tags = new ArrayList<String>();
        }
        
        public void addChild(Node child) {
            children.add(child);
        }
    }

    HashMap<Integer, Node> node_list = new HashMap<Integer, Node>();

    public Node getRoot() {
        return node_list.get(0);
    }

    private void make_node_tree() {
        BufferedReader br = null;
        String line;
        node_list.clear();
        try {
            br = new BufferedReader(new FileReader(fromFileName.getText()));
            while ((line = br.readLine()) != null) {
                status.setText(line);
                /* ID,NEXT_ID,TAG,TAG,...
                 */
                if (line.length() == 0) continue;
                if (line.charAt(0) =='#') continue;
                if (line.charAt(0) ==',') continue;
                line = line.toUpperCase();
                line.replace('"', ' ');
                String items[] = line.split(",");
                
                int id = Integer.parseInt(items[0]);
                Node node = node_list.get(id);
                if (node == null) {
                    node = new Node(id);
                    node_list.put(id, node);
                }

                if (Pattern.matches("\\d+", items[1])) {
                    /* shift of state */
                    int next_id = Integer.parseInt(items[1]);
                    Node next_node = node_list.get(next_id);
                    if (next_node == null) {
                        next_node = new Node(next_id);
                        node_list.put(next_id, next_node);
                    }
                    System.err.println("" + id + "->" + next_id);
                    node.addChild(next_node);
                } else if (items[1].equals("GENERATE")) {
                    /* rules on agent generation */
                    String start = items[2];
                    String time = items[3];
                    int number = Integer.parseInt(items[4]);
                    int range = Integer.parseInt(items[5]);
                    GenerationRule rule = new GenerationRule(start, time, number, range);
                    node.generate = rule;
                } else if (items[1].equals("EXIT")) {
                    /* final target */
                    node.exit = items[2];
                } else {
                    /* mid-goal */
                    StringBuffer tag_buf = new StringBuffer();
                    for (int i = 1; i < items.length; ++i) {
                        if (i != 1) tag_buf.append(",");
                        tag_buf.append(items[i]);
                    }
                    System.err.println("" + id + ":" + tag_buf.toString());
                    node.tags.add(tag_buf.toString());
                }
            }
        } catch (NullPointerException e) {
            JOptionPane.showMessageDialog(null,
            "�V�i���I��������܂���D");
            status.setText("�ǂݍ��ݎ��s");
            return;         
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
            "�V�i���I��J���̂Ɏ��s���܂����D");
            System.err.println(e);
            status.setText("�ǂݍ��ݎ��s");
            return;         
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
            "�V�i���I��J���̂Ɏ��s���܂����D");
            System.err.println(e);
            status.setText("�ǂݍ��ݎ��s");
            return;         
        }
        status.setText("�ǂݍ��ݏI��");
    }

    private void output_line(BufferedWriter bw,
            Node node,
            GenerationRule rule,
            int number_assigned,
            String exit,
            Stack<String> path) throws IOException {
        bw.write(rule.format(number_assigned));
        bw.write("," + exit);

        for (int i = 0; i < path.size(); ++i) {
            bw.write("," + path.get(i));
            status.setText(path.get(i));
        }
        bw.write("\n");
    }

    private int calc_number(int number_assigned,
            int sibling_count,
            int count) {
        double d = (double)number_assigned / (sibling_count - count);
        int number = (int)d;
        d -= number;
        //if (Math.random() < d) {
        if (rand.nextDouble() < d) {
            number += 1;
        }
        return number;
    }
    private int iterate_tree(BufferedWriter bw,
            Node node,
            GenerationRule rule,
            int number_assigned,
            String exit,
            Stack<String> path) throws IOException {

        if (node.exit != null) {
            exit = node.exit;
        }
        
        if (node.generate != null) {
            rule = node.generate;
            number_assigned = node.generate.number;
        }

        if (node.children.size() == 0) {
            /* leaf node */
            int generated = 0;
            if (node.tags.size() == 0) {
                output_line(bw, node, rule, number_assigned, exit, path);
                generated += number_assigned;
            } else {
                int count = 0;
                for (String tag : node.tags) {
                    path.push(tag);
                    int number = calc_number(number_assigned, node.tags.size(), count);
                    output_line(bw, node, rule, number, exit, path);
                    number_assigned -= number;
                    generated += number;
                    System.err.println(tag);
                    path.pop();
                }
            }
            return generated;
        }

        int child_count = 0;
        int generated = 0;
        for (Node child : node.children) {
            /* internal node */
            int each_child_number = calc_number(number_assigned, node.children.size(), child_count);
            if (node.tags.size() == 0) {
                /* special node, with empty tag*/
                generated += iterate_tree(bw, child, rule, each_child_number, exit, path);
            } else {
                int tag_count = 0;
                for (String tag : node.tags) {
                    path.push(tag);
                    int each_tag_number = calc_number(each_child_number, node.tags.size(), tag_count);
                    int tag_generated = iterate_tree(bw, child, rule, each_tag_number, exit, path);
                    each_child_number -= tag_generated;
                    generated += tag_generated;
                    path.pop();
                    tag_count += 1;
                }
            }
            child_count += 1;
        }
        return generated;
    }
    
    private void convert() {
        try {
            BufferedWriter bw = new BufferedWriter(new PrintWriter(toFileName.getText()));
            Stack<String> path = new Stack<String>();
            iterate_tree(bw, getRoot(), null, 0, null,path);
            bw.flush();
            bw.close();
            status.setText("�o�͐���");
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null,
            "�o�͐�̃t�@�C�������܂���D");
            e.printStackTrace();
            status.setText("�o�͎��s");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
            "�o�͎��ɃG���[�D");
            e.printStackTrace();
            status.setText("�o�͎��s");
        }
    }

    public void open_file() {
        FileDialog fd = new FileDialog(parent, "From file", FileDialog.LOAD);
        fd.setFile(fromFileName.getText());
        fd.setVisible (true);
        if (fd.getFile() == null) return;
        fromFileName.setText(fd.getDirectory() + fd.getFile());
        make_node_tree();
    }

    private JPanel setup_tool_panel() {
        JPanel tool_panel = new JPanel();
        
        tool_panel.setLayout(new GridLayout(3, 3));
        
        tool_panel.add(new JLabel("�ϊ����t�@�C��"));
        fromFileName = new JLabel();
        fromFileName.setBorder(new LineBorder(Color.BLACK));
        tool_panel.add(fromFileName);
        JButton fromButton = new JButton("�ǂݍ���");
        fromButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                open_file();
            }
        });
        tool_panel.add(fromButton);

        tool_panel.add(new JLabel("�o�̓t�@�C��"));
        toFileName = new JTextField(20);
        tool_panel.add(toFileName);
        JButton toButton = new JButton("�w�肷��");
        toButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(parent, "From file", FileDialog.LOAD);
                fd.setFile(fromFileName.getText());
                fd.setVisible (true);
                
                if (fd.getFile() == null) return;
                toFileName.setText(fd.getDirectory() + fd.getFile());
            }
        });
        tool_panel.add(toButton);
        
        tool_panel.add(new JLabel(""));
        JButton runButton = new JButton("�ϊ�����");
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                convert();
            }
        });
        tool_panel.add(runButton);
        
        return tool_panel;
    }

    public GenerateScenario(JFrame _parent, Random _rand) {
        super();
        rand = _rand;
        parent = _parent;
        setLayout(new BorderLayout());
        add(setup_tool_panel(), BorderLayout.NORTH);
        
        JPanel node_panel = new JPanel();
        node_panel.setSize(200, 200);
        node_panel.setBackground(Color.WHITE);
        add(node_panel, BorderLayout.CENTER);

        status = new JLabel();
        status.setBackground(Color.GRAY);
        status.setBorder(new EtchedBorder());
        add(status, BorderLayout.SOUTH);

        status.setText(" ");
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Generate agents");
        Random rand = new Random();
        final GenerateScenario generate_scenario = new GenerateScenario(frame,
                rand);

        JMenuBar menu_bar =new JMenuBar();
        JMenu file_menu = new JMenu("File");
        JMenuItem open_file = new JMenuItem("Open", 'O');
        open_file.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generate_scenario.open_file();
            }
        });
        file_menu.add(open_file);
        JMenuItem quit = new JMenuItem("Quit", 'Q');
        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        file_menu.add(quit);
        menu_bar.add(file_menu);
        frame.setJMenuBar(menu_bar);
        
        frame.add(generate_scenario, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
    // tkokada
    /*
    private void writeObject(ObjectOutputStream stream) {
        try {
            stream.defaultWriteObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream stream) {
        try {
            stream.defaultReadObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }
    */
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
