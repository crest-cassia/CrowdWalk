package nodagumi.ananPJ;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.vecmath.Vector3d;

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkParts.Link.Lift;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.OBNode.NType;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedAreaPoint;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedAreaRectangle;
import nodagumi.ananPJ.misc.NetMASSnapshot;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


public class NetworkMap extends DefaultTreeModel implements Serializable {
    private static final long serialVersionUID = -4302417008763441581L;
    private HashMap<Integer, OBNode> id_part_map =
        new HashMap<Integer, OBNode>();
    private ArrayList<MapNode> nodesCache = new ArrayList<MapNode>();
    private ArrayList<MapLink> linksCache = new ArrayList<MapLink>();
    private ArrayList<EvacuationAgent> agentsCache =
        new ArrayList<EvacuationAgent>();
    private ArrayList<EditorFrame> frames = new ArrayList<EditorFrame>();

    private String filename = null;
    private String pollutionFile = null;
    private String generationFile = null;
    private String responseFile = null;

    private OBNode selectedOBNode = null;
    private boolean hasDisplay = true;
    private Random random = null;

    /* undo related stuff */
    class UndoInformation implements Serializable {
        public boolean addition;
        public OBNode parent;
        public OBNode node;

        public UndoInformation(OBNode _parent,
                OBNode _node,
                boolean _addition) {
            parent = _parent;
            node = _node;
            addition = _addition;
        }
    }
    private ArrayList<UndoInformation> undo_list =
        new ArrayList<UndoInformation>();

    public boolean canUndo() {
        return undo_list.size() > 0;

    }
    public void undo() {
        if (undo_list.size() == 0) return;
        int i = undo_list.size() - 1;
        UndoInformation info = undo_list.remove(i);
        if (info.addition) {
            removeOBNode(info.parent, info.node, false);
        } else {
            insertOBNode(info.parent, info.node, false);
        }
        NetworkMapEditor.getInstance().updateAll();
    }

    /* constructor */
    public NetworkMap() {
        super(null, true);
        random = new Random();
        int id = assign_new_id();
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
        id_part_map.put(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);
    }

    public NetworkMap(Random _random) {
        super(null, true);
        random = _random;
        int id = assign_new_id();
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
        id_part_map.put(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);
    }

    public NetworkMap(Random _random, boolean _hasDisplay) {
        this(_random);
        setHasDisplay(_hasDisplay);
    }

    /* to restore from DOM */
    public NetworkMap(int id, Random _random) {
        super(null, true);
        random = _random;
        root = new MapPartGroup(id);
        ((MapPartGroup)root).addTag("root");
        id_part_map.put(id, (OBNode)root);

        setRoot((DefaultMutableTreeNode)root);
    }

    /* create contents */
    public void addObject(int id, OBNode node) {
        id_part_map.put(id, node);
    }

    public OBNode getObject(int id) {
        return id_part_map.get(id);
    }
    /* create contents */
    //Random rand = new Random();

    private int assign_new_id() {
        int id;
        do {
            id = Math.abs(random.nextInt());
            if (id < 0) { id = -id; }
        } while (id_part_map.containsKey(id));
        return id;
    }

    private int agentUniqueId = 1000;
    public int assignUniqueAgentId() {
        synchronized(this) {
            agentUniqueId += 1;
        }
        return agentUniqueId;
    }

    private void insertOBNode (OBNode parent,
            OBNode node,
            boolean can_undo) {
        id_part_map.put(node.ID, node);
        insertNodeInto(node, parent, parent.getChildCount());

        OBNode.NType type = node.getNodeType();
        if (type == OBNode.NType.NODE) {
            nodesCache.add((MapNode)node);
        } else     if (type == OBNode.NType.LINK) {
            linksCache.add((MapLink)node);
        } else     if (type == OBNode.NType.AGENT) {
            agentsCache.add((EvacuationAgent)node);
        } else     if (type == OBNode.NType.GROUP) {
            /* no operation */
        } else     if (type == OBNode.NType.ROOM) {
            /* no operation */
        } else     if (type == OBNode.NType.SYMLINK) {
            /* no operation */
        } else {
            System.err.println("unkown type added");
        }

        if (can_undo) {
            undo_list.add(new UndoInformation(parent, node, true));
        }
    }

    abstract class OBTreeCrawlFunctor {
        public abstract void apply(OBNode node,
                OBNode parent); 
    }

    private void applyToAllChildrenRec(OBNode node,
            OBNode parent,
            OBTreeCrawlFunctor func) {
        func.apply(node, parent);
        for (int i = 0; i < node.getChildCount(); ++i) {
            OBNode child = (OBNode)node.getChildAt(i);
            if (child instanceof OBNode) {
                applyToAllChildrenRec(child, node, func);
            }
        }
    }

    public void removeOBNode (OBNode parent,
            OBNode node,
            boolean can_undo) {
        id_part_map.remove(node.ID);

        OBNode.NType type = node.getNodeType();
        if (type != OBNode.NType.SYMLINK) {
            clearSymlinks(node);
        }

        switch (type) {
        case NODE:
            nodesCache.remove((MapNode)node);
            break;
        case LINK:
            linksCache.remove((MapLink)node);
            break;
        case AGENT:
            agentsCache.remove((EvacuationAgent)node);
            break;
        case GROUP:
            while (node.getChildCount() > 0) {
                removeOBNode(node, (OBNode)node.getChildAt(0), true);
            }
            break;
        case SYMLINK:
            break;
        default:
            System.err.println(type);
            System.err.println("unkown type removed");
        }

        if (can_undo) {
            undo_list.add(new UndoInformation(parent, node, false));
        }
        removeNodeFromParent(node);
    }

    public MapNode createMapNode(
            MapPartGroup parent,
            Point2D _coordinates,
            double _height) {
        int id = assign_new_id();
        MapNode node = new MapNode(id,
                _coordinates, _height);
        insertOBNode(parent, node, true);
        return node;
    }

    public MapLink createMapLink(
            MapPartGroup parent,
            MapNode _from,
            MapNode _to,
            double _length,
            double _width) {
        int id = assign_new_id();
        MapLink link = new MapLink(id, _from, _to, _length, _width);
        link.prepareAdd();
        insertOBNode(parent, link, true);
        return link;
    }

    public Lift createLift(MapPartGroup parent,
            MapNode _from,
            MapNode _to,
            double _length,
            double _width) {
        int id = assign_new_id();
        Lift link = new Lift(id, _from, _to, _length, _width);
        link.prepareAdd();
        insertOBNode(parent, link, true);
        return link;
    }

    public EvacuationAgent addAgent(
            MapPartGroup parent,
            EvacuationAgent agent) {
        // int id = assign_new_id();
        int id = assignUniqueAgentId();
        agent.ID = id;
        id_part_map.put(id, agent);
        agentsCache.add(agent);
        insertNodeInto(agent, parent, parent.getChildCount());
        return agent;
    }

    public PollutedArea createPollutedAreaRectangle(
            MapPartGroup parent,
            Rectangle2D bounds,
            double min_height,
            double max_height,
            double angle) {
        int id = assign_new_id();
        PollutedArea area = new PollutedAreaRectangle(id,
                bounds, min_height, max_height, angle);
        insertOBNode(parent, area, true);
        return area;
    }

    public PollutedArea createPollutedAreaPoint(
            MapPartGroup parent,
            MapNode node,
            int room_id) {
        int id = assign_new_id();
        Vector3d point = node.getPoint();
        PollutedArea area = new PollutedAreaPoint(id, room_id, point); 
        insertOBNode(parent, area, true);
        return area;
    }

    public MapPartGroup createGroupNode(MapPartGroup parent){
        int id = assign_new_id();
        MapPartGroup group = new MapPartGroup(id);
        insertOBNode(parent, group, true);
        return group; 
    }

    private MapPartGroup createGroupNode(){
        return createGroupNode((MapPartGroup)root);
    }

    public OBNodeSymbolicLink createSymLink(MapPartGroup parent,
            OBNode orig){
        int id = assign_new_id();
        OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id, orig);
        insertOBNode(parent, symlink, true);
        return symlink; 
    }

    public void clearSymlinks(final OBNode orig) {
        //System.err.println("Orig:" + orig.ID);
        applyToAllChildrenRec((OBNode)root,
                null,
                new OBTreeCrawlFunctor() {
                    @Override
                    public void apply(OBNode node, OBNode parent) {
                        if (node.getNodeType() == OBNode.NType.SYMLINK) {
                            OBNodeSymbolicLink symlink =
                                    (OBNodeSymbolicLink)node;
                            if (symlink.getOriginal() == orig) {
                                System.err.println("deleted!");
                                removeOBNode(parent, node, true);
                            }
                        }
                    }
        });
    }

    /* some stuff related to frames */
    public boolean existNodeEditorFrame(MapPartGroup _obiNode){
        for (EditorFrame frame : getFrames()) {
            if (_obiNode.equals(frame)) return true;
        }
        return false;
    }

    public EditorFrame openEditorFrame(MapPartGroup obinode){
        EditorFrame frame = new EditorFrame(obinode, random);
        obinode.setUserObject(frame);

        getFrames().add(frame);
        frame.setVisible(true);

        return frame;
    }

    public void removeEditorFrame(MapPartGroup _obinode){
        getFrames().remove(_obinode.getUserObject());
        _obinode.setUserObject(null);
    }
    
    /* accessing contents */
    public ArrayList<OBNode> getOBElements() {
        return new ArrayList<OBNode>(id_part_map.values());
    }
    
    public ArrayList<MapNode> getNodes() {
        return nodesCache;
    }

    public ArrayList<MapLink> getLinks() {
        return linksCache;
    }

    public ArrayList<EvacuationAgent> getAgents() {
        return agentsCache;
    }

    public ArrayList<MapPartGroup> getGroups() {
        ArrayList<MapPartGroup> groups = new ArrayList<MapPartGroup>();
        for (OBNode node : id_part_map.values()) {
            if (node.getNodeType() == OBNode.NType.GROUP &&
                    !node.getTags().isEmpty()) {
                groups.add((MapPartGroup)node);
            }
        }
        Collections.sort(groups, new Comparator<MapPartGroup>() {
            public int compare(MapPartGroup lhs, MapPartGroup rhs) {
                Matcher f1 = lhs.matchTag("((B?)\\d+)F"); 
                Matcher f2 = rhs.matchTag("((B?)\\d+)F");

                if (f1 != null && f2 != null) {
                    int i1;
                    if (f1.group(1).startsWith("B")) {
                        i1 = -Integer.parseInt(f1.group(1).substring(1));
                    } else {
                        i1 = Integer.parseInt(f1.group(1));
                    }
                    int i2;
                    if (f2.group(1).startsWith("B")) {
                        i2 = -Integer.parseInt(f2.group(1).substring(1));
                    } else {
                        i2 = Integer.parseInt(f2.group(1));
                    }

                    return i1 - i2;
                } else if (f1 != null) {
                    return -1;
                } else if (f2 != null) {
                    return 1;
                }

                String tc1Name = lhs.getTagString();
                String tc2Name = rhs.getTagString();
                if (tc1Name.compareTo(tc2Name) < 0) {
                    return -1;
                } else if (tc1Name.compareTo(tc2Name) > 0) {
                    return 1;
                }
                return 0;
            }
        });
        return groups;
    }

    public ArrayList<PollutedArea> getRooms() {
        ArrayList<PollutedArea> rooms = new ArrayList<PollutedArea>();
        findRoomRec((OBNode)root, rooms);
        return rooms;
    }

    private void findRoomRec(OBNode node, ArrayList<PollutedArea> rooms) {
        if (node.getNodeType() == NType.ROOM) {
            rooms.add((PollutedArea)node);
        }
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) findRoomRec((OBNode)child, rooms);
        }
    }

    public void makeStairs() {
        ArrayList<MapNode> selected_nodes = new ArrayList<MapNode>();
        for (MapNode node : getNodes()) {
            if (node.selected) selected_nodes.add(node);
        }
        if (selected_nodes.size() != 2) {
            JOptionPane.showMessageDialog(null,
                    "Number of selected nodes:"
                    + selected_nodes.size() + "\nwhere it should be 2",
                    "Fail to make stair",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        MapNode from_node = selected_nodes.get(0);
        MapNode to_node = selected_nodes.get(1);

        if (from_node.getHeight() < to_node.getHeight()) {
            from_node = selected_nodes.get(1);
            to_node = selected_nodes.get(0);
        }
        MapLink link = createMapLink((MapPartGroup)from_node.getParent(),
                from_node, to_node,    100, 6);
        link.addTag("GENERATED_STAIR");

    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String file_name) {
        filename = file_name;
    }

    public String getPollutionFile() {
        if (pollutionFile == null)
            return null;
        File pollution_file = new File(pollutionFile);
        // if (!pollution_file.isAbsolute()) {
            // File mapfile = new File(filename);
            // pollution_file = new File(mapfile.getParent()
                          // + File.separator
                          // + pollutionFile);
        // }
        // return pollution_file.getAbsolutePath();
        return pollution_file.getPath();
    }

    public void setPollutionFile(String s) {
        pollutionFile = s;
    }

    public String getGenerationFile() {
        if (generationFile == null)
            return null;
        File generation_file = new File(generationFile);
        // if (!generation_file.isAbsolute() && (filename != null)) {
            // File mapfile = new File(filename);
            // if (mapfile.getParent() != null) {
        // generation_file = new File(mapfile.getParent()
                       // + File.separator
                       // + generationFile);
            // } else {
        // return generationFile;
        // }
        // }
        // return generation_file.getAbsolutePath();
        return generation_file.getPath();
    }

    public void setGenerationFile(String s) {
        generationFile = s;
    }

    public String getResponseFile() {
        if (responseFile == null)
            return null;
        File response_file = new File(responseFile);
        // if (!response_file.isAbsolute() && (filename != null)) {
            // File mapfile = new File(filename);
            // if (mapfile.getParent() != null) {
        // response_file = new File(mapfile.getParent()
                     // + File.separator
                     // + responseFile);
            // } else {
        // return responseFile;
        // }
        // }
        // return response_file.getAbsolutePath();
        return response_file.getPath();
    }

    public void setResponseFile(String s) {
        responseFile = s;
    }

    public void testDumpNodes() {
        testDumpNodes((OBNode)root);
    }

    @SuppressWarnings("unchecked")
    public void testDumpNodes(OBNode node) {
        System.err.println(node.toString());

        for (Enumeration<OBNode> e = node.children();e.hasMoreElements();) {
            final OBNode child = e.nextElement();
            for (int i = 0; i < child.getDepth(); ++i) {
                System.err.print(" ");
            }
            testDumpNodes(child);
        }
    }

    /* converting to/from DOM */
    public boolean toDOM(Document doc) {
        Element dom_root =((OBNode)this.root).toDom(doc, "root");

        /* some attributes */
        if (pollutionFile != null) {
            dom_root.setAttribute("PollutionSettings", pollutionFile);
        }
        if (generationFile != null) {
            dom_root.setAttribute("GenerationSettings", generationFile);
        }
        if (responseFile != null) {
            dom_root.setAttribute("ResponseSettings", responseFile);
        }

        doc.appendChild(dom_root);

        return true;
    }

    public boolean fromDOM(Document doc) {
        NodeList toplevel = doc.getChildNodes();
        if (toplevel.getLength() != 1) {
            JOptionPane.showMessageDialog(null,
                    "The number of networks in the dom was "
                    + toplevel.getLength(),
                    "Fail to convert from DOM",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        Element dom_root = (Element) toplevel.item(0);

        /* some attributes */
        pollutionFile = dom_root.getAttribute("PollutionSettings");
        if (pollutionFile.isEmpty()) pollutionFile = null;
            generationFile = dom_root.getAttribute("GenerationSettings");
        if (generationFile.isEmpty()) generationFile = null;
            responseFile = dom_root.getAttribute("ResponseSettings");
        if (responseFile.isEmpty()) responseFile = null;

        setRoot(OBNode.fromDom(dom_root));
        setupNetwork((OBNode)this.root);
        return true;
    }

    private void setupNetwork(OBNode ob_node) {
        setupNodes(ob_node);
        setupLinks(ob_node);
        setupOthers(ob_node);

        checkDanglingSymlinks(ob_node);
    }

    @SuppressWarnings("unchecked")
    private void setupNodes(OBNode ob_node) {
        if (OBNode.NType.NODE == ob_node.getNodeType()) {
            id_part_map.put(ob_node.ID, ob_node);
            MapNode node = (MapNode) ob_node;

            nodesCache.add(node);
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupNodes(child);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setupLinks(OBNode ob_node) {
        if (OBNode.NType.LINK == ob_node.getNodeType()) {
            id_part_map.put(ob_node.ID, ob_node);
            MapLink link = (MapLink) ob_node;
            linksCache.add(link);

            String[] nodes = (String[])ob_node.getUserObject();
            MapNode from_node = (MapNode)id_part_map.get(Integer.parseInt(nodes[0]));
            MapNode to_node = (MapNode)id_part_map.get(Integer.parseInt(nodes[1]));

            if (from_node == null) {
                System.err.println(Integer.parseInt("from_node is null " + nodes[0]));
            }
            if (to_node == null) {
                System.err.println(Integer.parseInt("to_node is null " + nodes[1]));
            }
            from_node.addLink(link);
            to_node.addLink(link);
            try {
                link.setFromTo(from_node, to_node);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Try to set from/to of a link, which alread has it setted\n"
                        + "link ID: " + link.ID,
                        "Warning setting up network",
                        JOptionPane.WARNING_MESSAGE);
            }
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            for (Enumeration<OBNode> e = ob_node.children();e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupLinks(child);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setupOthers(OBNode ob_node) {
        if (OBNode.NType.AGENT == ob_node.getNodeType()) {
            id_part_map.put(ob_node.ID, ob_node);
            EvacuationAgent agent = (EvacuationAgent) ob_node;
            agentsCache.add(agent);
            String[] location = (String[])agent.getUserObject();
            MapLink link = (MapLink)id_part_map.get(Integer.parseInt(location[0]));
            agent.place(link, agent.getPosition());
        } else if (OBNode.NType.GROUP == ob_node.getNodeType()) {
            id_part_map.put(ob_node.ID, ob_node);
            for (Enumeration<OBNode> e = ob_node.children(); e.hasMoreElements();) {
                OBNode child = e.nextElement();
                setupOthers(child);
            }
        } else if (ob_node.getNodeType() == OBNode.NType.SYMLINK) {
            id_part_map.put(ob_node.ID, ob_node);
            Integer orig_id = (Integer)ob_node.getUserObject();
            OBNode original = (OBNode)id_part_map.get(orig_id);
            
            OBNodeSymbolicLink symlink = (OBNodeSymbolicLink)ob_node;
            symlink.setOriginal(original);
        } else if (ob_node.getNodeType() == OBNode.NType.NODE ||
                ob_node.getNodeType() == OBNode.NType.LINK ||
                ob_node.getNodeType() == OBNode.NType.AGENT ||
                ob_node.getNodeType() == OBNode.NType.ROOM
                ){
        } else {
            System.err.println("unknown type " + ob_node.getNodeType() + " in setting up network");
        }
    }

    private void checkDanglingSymlinks(OBNode node) {
        class CheckSymlink extends OBTreeCrawlFunctor {
            public int dangling_symlink_count = 0;
            boolean found = false;
            @Override
            public void apply(OBNode node,
                    OBNode parent) {
                if (node.getNodeType() == OBNode.NType.SYMLINK) {
                    OBNode original = ((OBNodeSymbolicLink)node).getOriginal();
                    if (original == null) {
                        dangling_symlink_count++;
                        found = true;
                        removeOBNode(parent, node, false);
                    }
                }
            }

            public boolean mustCheckMore() {
                boolean ret = found;
                found = false;
                return ret;
            }
        }
        CheckSymlink checker = new CheckSymlink();
        do {
            applyToAllChildrenRec(node, null, checker);
        } while(checker.mustCheckMore());
        if (checker.dangling_symlink_count != 0) {
            JOptionPane.showMessageDialog(null,
                    "Removed " + checker.dangling_symlink_count
                    + " dangling symlink(s) on loading",
                    "Corrupt file",
                    JOptionPane.WARNING_MESSAGE
                    );
        }

    }

    /* selected OBNode getter/setter
     */
    public void setSelectedOBNode(OBNode _obNode){
        selectedOBNode = _obNode;
    }
    public OBNode getSelectedOBNode(){
        return selectedOBNode;
    }
    
    public void setFrames(ArrayList<EditorFrame> frames) {
        this.frames = frames;
    }
    public ArrayList<EditorFrame> getFrames() {
        return frames;
    }
    
    public ArrayList<String> getAllTags() {
        ArrayList<String> all_tags = new ArrayList<String>();
        for (MapNode node : getNodes()) {
            for (String tag : node.getTags()) {
                if (!all_tags.contains(tag))
                    all_tags.add(tag);
            }
        }
        for (MapLink link : getLinks()) {
            for (String tag : link.getTags()) {
                if (!all_tags.contains(tag))
                    all_tags.add(tag);
            }
        }
        return all_tags;
    }
    
    private void prepare_for_save_rec(OBNode node) {
        node.prepareForSave(hasDisplay);
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) prepare_for_save_rec((OBNode)child);
        }
    }
    public void prepareForSave() {
        prepare_for_save_rec((OBNode)root);
    }
    private void setup_after_load_rec(OBNode node) {
        node.postLoad(hasDisplay);
        for (int i = 0; i < node.getChildCount(); ++i) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof OBNode) setup_after_load_rec((OBNode)child);
        }
    }
    public void setupAfterLoad() {
        setup_after_load_rec((OBNode)root);
    }

    public void printFields() {
        for (MapNode node : nodesCache)
            System.out.println("NetworkMap.printFields: nodesCache: " +
                    node.toString());
        for (MapLink link : linksCache)
            System.out.println("NetworkMap.printFields: linksCache: " +
                    link.toString());
        for (EvacuationAgent agent : agentsCache)
            System.out.println("NetworkMap.printFields: agentsCache: " +
                    agent.toString());
        for (EditorFrame frame : frames)
            System.out.println("NetworkMap.printFields: frames: " +
                    frame.toString());
        System.out.println("NetworkMap.printFields: filename: " + filename);
    }

    public Element storeToDOM(Document doc, String tag) {
        Element element = doc.createElement(tag);
        element.setAttribute("class", "NetworkMap");

        Element idElement = doc.createElement("id");
        idElement.setAttribute("class", "int");
        Text idText = doc.createTextNode("" + ((MapPartGroup) root).ID);
        idElement.appendChild(idText);
        element.appendChild(idElement);

        for (Object obj : id_part_map.keySet().toArray()) {
            Element id_part_mapElement = doc.createElement("id_part_map");
            id_part_mapElement.setAttribute("class",
                    "HashMap<Integer, OBNode>");
            Element id_part_map_keyElement = doc.createElement("key");
            id_part_map_keyElement.setAttribute("class", "Integer");
            Text id_part_map_keyText = doc.createTextNode(
                "" + ((Integer) obj));
            id_part_map_keyElement.appendChild(id_part_map_keyText);
            id_part_mapElement.appendChild(id_part_map_keyElement);

            Element id_part_map_valueElement =
                ((OBNode) id_part_map.get(obj)).storeToDOM(doc, "value");
            id_part_mapElement.appendChild(id_part_map_valueElement);

            element.appendChild(id_part_mapElement);
        }

        Element nodesCacheElement = doc.createElement("nodesCache");
        nodesCacheElement.setAttribute("class", "ArrayList<MapNode>");
        for (MapNode node : nodesCache) {
            Element nodeElement = node.storeToDOM(doc, "node");
            nodesCacheElement.appendChild(nodeElement);
        }
        element.appendChild(nodesCacheElement);

        /*element.setAttribute("filename", "" + this.getFileName());
        element.setAttribute("pollutionFile", "" + this.getPollutionFile());
        element.setAttribute("generationFile", "" + this.getGenerationFile());
        element.setAttribute("responseFile", "" + this.getResponseFile());
        element.setAttribute("selectedOBNode", "" + this.getSelectedOBNode());
        */
        return element;
    }

    public NetworkMap restoreToDOM(Element element) {
        int id = Integer.parseInt(element.getAttribute("rootID"));
        NetworkMap networkMap = new NetworkMap(id, random);

        networkMap.setFileName(element.getAttribute("filename"));
        networkMap.setGenerationFile(element.getAttribute("generationFile"));
        networkMap.setResponseFile(element.getAttribute("responseFile"));

        NodeList childNetworkMap = element.getChildNodes();
        for (int i = 0; i < childNetworkMap.getLength(); i++) {
            if (childNetworkMap.item(i) instanceof Element) {
                Element child = (Element) childNetworkMap.item(i);
                if (child.getTagName().equals(""))
                    this.setFileName(child.getAttribute(""));
            }
        }
        return networkMap;
    }

    public void setSpeedCalculationModel(RunningAroundPerson
            .SpeedCalculationModel _model) {
        for (EvacuationAgent agent : agentsCache) {
            RunningAroundPerson rap = (RunningAroundPerson) agent;
            rap.setSpeedCalculationModel(_model);
        }
    }

    public void setHasDisplay(boolean _hasDisplay) {
        hasDisplay = _hasDisplay;
    }

    public boolean getHasDisplay() {
        return hasDisplay;
    }

    public void setRandom(Random _random) {
        random = _random;
        for (EvacuationAgent agent : agentsCache)
            agent.setRandom(_random);
    }
}
// test(CRLF)
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
