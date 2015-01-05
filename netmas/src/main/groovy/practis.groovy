/** practis.groovy */

import java.io.*
import java.nio.channels.*
import javax.xml.parsers.*
import nodagumi.ananPJ.NetworkMapEditor
import nodagumi.ananPJ.NetmasCuiSimulator
import org.w3c.dom.*


/** copy files from src to dst */
def copyFile(src, dst) {
    def srcc = new FileInputStream(src).getChannel()
    def dstc = new FileOutputStream(dst).getChannel()
    try {
        srcc.transferTo(0, srcc.size(), dstc)
    } finally {
        srcc.close()
        dstc.close()
    }
}

/** delete a file or a directory */
def deleteFile(file) {
    if (file.isDirectory()) {
        String[] children = file.list()
        print children
        for (int i = 0; i < children.size(); i++)
            deleteFile(new File(file, children[i]))
    }
    file.delete()
}

variables = []

/** properties loader */
def practisPropertiesLoader(file) {
    try {
        def doc = (DocumentBuilderFactory.newInstance())
            .newDocumentBuilder()
            .parse(new BufferedInputStream(new FileInputStream(file)))
        def root = doc.getDocumentElement()
        def plist = root.getElementsByTagName("Properties")
        def properties = plist.item(0).getFirstChild().getNodeValue()
        def list = root.getElementsByTagName("Variable")
        println root.getTagName() + ", " + list.length + ", " + properties
        for (int i = 0; i < list.length; i++) {
            def element = list.item(i)
            def name = getChildren(element, "name")
            def type = getChildren(element, "type")
            def change = getChildren(element, "change")
            if (change.equals("range")) {
                def start = getChildren(element, "start")
                def end = getChildren(element, "end")
                def step = getChildren(element, "step")
                println name + ", " + type + ", " + change + ", " + start + ", " + end + ", " + step
                values = []
                //for (int j = parseInt(start); j <= parseInt(end); j += parseInt(step)) {
                for (int j = Integer.parseInt(start); j <= Integer.parseInt(end); j += Integer.parseInt(step)) {
                    values += Integer.toString(j)
                }
                variables += [[name, values]]
            } else if (change.equals("list")) {
                def llist = element.getElementsByTagName("list").item(0).getElementsByTagName("value")
                def valueList = []
                for (int j = 0; j < llist.length; j++) {
                    println "value: " + llist.item(j).getFirstChild().getNodeValue()
                    valueList += [llist.item(j).getFirstChild().getNodeValue()]
                }
                println "valueList: " + valueList
                variables += [[name, valueList]]
            }
        }
        println variables
    } catch (e) {
        e.printStackTrace()
    }
    return true
}

/** get a first child node value from an element */
def getChildren(element, tag) {
    def list = element.getElementsByTagName(tag)
    if (list.length == 0) {
        return "None"
    }
    return list.item(0).getFirstChild().getNodeValue()
}

def rand = new Random()
def nme     /** network map editor */
def cui     /** cui simulator */
def start, init, finish     /** time */
def macroTimeStep = ""      /** time step of the simulation */
def tick = 0.0      /** simulation counter */

if (args.size() < 1) {
    start = System.nanoTime()
    nme = new NetworkMapEditor(rand)
    init = System.nanoTime()
    nme.setVisible(true)
    finish = System.nanoTime()
    macroTimeStep += "0"
} else if (args.size() == 1) {
    // start = System.nanoTime()
    // nme = new NetworkMapEditor(rand)
    // nme.setProperties(args[0])
    // init = System.nanoTime()
    // nme.setVisible(true)
    // finish = System.nanoTime()
    // macroTimeStep += "0"
    practisPropertiesLoader(args[0])
    // rm, mkdir
    patterns = recursiveList(variables, [], 0)
    println patterns
    File tdict = new File('/tmp/' + args[2])
    if (tdict.isDirectory()) {
        deleteFile(tdict)
    }
    //tdict.delete()
    start = System.nanoTime()
    cui = new NetmasCuiSimulator(args[1])
    cui.setScenarioSerial("NetmasCuiSimulator")
    cui.initialize()
    init = System.nanoTime()
    def timestep = Integer.parseInt(args[2])
    cui.model.setTimeScale(timestep)
    cui.timeSeriesLogPath = '/tmp/' + args[2]
    cui.model.saveTimeSeriesLog('/tmp/' + args[2])
    cui.start()
    finish = System.nanoTime()
    tick = cui.model.getSecond()
} else if (args.size() == 2) {
    if (args[0] == 'cui') {
        start = System.nanoTime()
        cui = new NetmasCuiSimulator(args[1])
        cui.setScenarioSerial("NetmasCuiSimulator")
        cui.initialize()
        init = System.nanoTime()
        cui.start()
        finish = System.nanoTime()
        tick = cui.model.getSecond()
    } else {
        start = System.nanoTime()
        nme = new NetworkMapEditor(rand)
        nme.setProperties(args[0])
        init = System.nanoTime()
        nme.setVisible(true)
        finish = System.nanoTime()
        macroTimeStep += "0"
    }
} else if (args.size() == 3) {
    // rm, mkdir
    File tdict = new File('/tmp/' + args[2])
    if (tdict.isDirectory()) {
        deleteFile(tdict)
    }
    //tdict.delete()
    start = System.nanoTime()
    cui = new NetmasCuiSimulator(args[1])
    cui.setScenarioSerial("NetmasCuiSimulator")
    cui.initialize()
    init = System.nanoTime()
    def timestep = Integer.parseInt(args[2])
    cui.model.setTimeScale(timestep)
    cui.timeSeriesLogPath = '/tmp/' + args[2]
    cui.model.saveTimeSeriesLog('/tmp/' + args[2])
    cui.start()
    finish = System.nanoTime()
    tick = cui.model.getSecond()
}

// post processes
def n = 1000 * 1000 * 1000
def initTime = (init - start) / n
def loopTime = (finish - init) / n
println String.format(' init: %.10f', initTime)
println String.format(' loop: %.10f', loopTime)
println String.format(' tick: %f', tick)

def hogeFile = new File('/tmp/hoge.csv')
if (!hogeFile.exists()) {
    hogeFile.createNewFile()
}
def reader = new File('/tmp/hoge.csv').newReader('UTF-8')
def str = reader.getText()
def writer = new File('/tmp/hoge.csv').newWriter('UTF-8')
str += macroTimeStep + "," + initTime + "," + loopTime + "," + tick
writer.println(str)
writer.close()

