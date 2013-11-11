package nodagumi.ananPJ.misc;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;


/* This class enerates a generation file for a grid map. Grid map consists of
 * two rectangles and two links which joint the rectangles.
 *
 *
 */
public class GenCrossGenFile {

    private Random random = null;
    private String path = null;     // generation file name
    private ArrayList<MapNode> eastNodeList = new ArrayList<MapNode>();
    private ArrayList<MapNode> westNodeList = new ArrayList<MapNode>();

    private String EAST_NODE_KEYWORD = "EAST_NODES";
    private String WEST_NODE_KEYWORD = "WEST_NODES";
    private String NODE_TAG_KEYWORD = "ROOT-N";
    private String EXIT_TAG_KEYWORD = "EXIT";

    public GenCrossGenFile(String _path, Random _random,
            ArrayList<MapNode> _nodes) {
        path = _path;
        random = _random;
        for (MapNode node : _nodes) {
            // keyword of east side nodes
            if (node.hasTag(EAST_NODE_KEYWORD))
                eastNodeList.add(node);
            // keyword of west side nodes
            if (node.hasTag(WEST_NODE_KEYWORD))
                westNodeList.add(node);
        }
    }

    // numEast: the number of generated east agents
    // numWest: the number of generated west agents
    public void generate(int numEast, int numWest) {
        try {
            int[] numGenEast = new int[eastNodeList.size()];
            int[] numGenWest = new int[eastNodeList.size()];
            FileWriter fw = new FileWriter(path);
            for (int i = 0; i < numEast; i++) {
                int randInt = random.nextInt() % eastNodeList.size();
                numGenEast[randInt] += 1;
            }
            for (int i = 0; i < numWest; i++) {
                int randInt = random.nextInt() % westNodeList.size();
                numGenWest[randInt] += 1;
            }
            for (int i = 0; i < eastNodeList.size(); i++) {
                ArrayList<String> tags = ((MapNode) eastNodeList.get(i))
                    .getTags();
                String nodeTag = null;
                for (String tag : tags)
                    if (tag.contains(NODE_TAG_KEYWORD)) {
                        nodeTag = tag;
                        break;
                    }
                if (nodeTag == null)
                    continue;
                int exitNum = random.nextInt() % westNodeList.size();
                String exitTag = null;
                for (String tag : tags)
                    if (tag.contains(EXIT_TAG_KEYWORD) &&
                            !tag.equals(EXIT_TAG_KEYWORD)) {
                        exitTag = tag;
                        break;
                    }
                if (exitTag == null)
                    continue;
                fw.write("EACH," + nodeTag + ",18:00:00,1," + numGenEast[i] +
                        "," + exitTag + "\n");
            }
            for (int i = 0; i < westNodeList.size(); i++) {
                ArrayList<String> tags = ((MapNode) westNodeList.get(i))
                    .getTags();
                String nodeTag = null;
                for (String tag : tags)
                    if (tag.contains(NODE_TAG_KEYWORD)) {
                        nodeTag = tag;
                        break;
                    }
                if (nodeTag == null)
                    continue;
                int exitNum = random.nextInt() % eastNodeList.size();
                String exitTag = null;
                for (String tag : tags)
                    if (tag.contains(EXIT_TAG_KEYWORD) &&
                            !tag.equals(EXIT_TAG_KEYWORD)) {
                        exitTag = tag;
                        break;
                    }
                if (exitTag == null)
                    continue;
                fw.write("EACH," + nodeTag + ",18:00:00,1," + numGenWest[i] +
                        "," + exitTag + "\n");
            }
            fw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

