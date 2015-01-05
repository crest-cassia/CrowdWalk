import java.io.*
import java.nio.channels.*

import nodagumi.ananPJ.NetworkMapEditor
import nodagumi.ananPJ.NetmasCuiSimulator


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

def rand = new Random()
def nme     /** network map editor */
def cui     /** cui simulator */
def start, init, finish     /** time */
def macroTimeStep = ""      /** time step of the simulation */
def tick = 0.0      /** simulation counter */
def dirString = ""  /** the directory where the logs are saved. */

if (args.size() < 1) {
    dirString = '/tmp/'
    start = System.nanoTime()
    nme = new NetworkMapEditor(rand)
    init = System.nanoTime()
    nme.setVisible(true)
    finish = System.nanoTime()
    macroTimeStep += "0"
} else if (args.size() == 1) {
    dirString = '/tmp/'
    start = System.nanoTime()
    nme = new NetworkMapEditor(rand)
    nme.setProperties(args[0])
    init = System.nanoTime()
    nme.setVisible(true)
    finish = System.nanoTime()
    macroTimeStep += "0"
} else if (args.size() == 2) {
    dirString = '/tmp/'
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
    dirString = '/tmp/' + args[2]
    File tdict = new File(dirString)
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
    cui.timeSeriesLogPath = dirString
    cui.model.saveTimeSeriesLog(dirString)
    cui.start()
    finish = System.nanoTime()
    tick = cui.model.getSecond()
} else if (args.size() == 4) {
    // rm, mkdir
    dirString = '/tmp/' + args[2] + '-' + args[3]
    File tdict = new File(dirString)
    if (tdict.isDirectory()) {
        deleteFile(tdict)
    }
    //tdict.delete()
    def randseed = Integer.parseInt(args[2])
    def ratio = Double.parseDouble(args[3])
    println args[0] + "properties:" + args[1] + ", randseed: " + args[2] + ", raio: " +
        args[3]
    start = System.nanoTime()
    cui = new NetmasCuiSimulator(args[1], randseed)
    cui.setScenarioSerial("NetmasCuiSimulator")
    cui.linerGenerateAgentRatio = ratio
    cui.initialize()
    init = System.nanoTime()
    // cui.model.getAgentHandler().setLinerGenerateAgentRatio(ratio)
    cui.timeSeriesLogPath = dirString
    cui.model.saveTimeSeriesLog(dirString)
    cui.start()
    cui.model.saveGoalLog(dirString, true);
    finish = System.nanoTime()
    tick = cui.model.getSecond()
} else if (args.size() == 5) {
    // rm, mkdir
    dirString = '/tmp/' + args[2] + '-' + args[3] + '-' + args[4]
    File tdict = new File(dirString)
    if (tdict.isDirectory()) {
        deleteFile(tdict)
    }
    //tdict.delete()
    def timestep = Integer.parseInt(args[2])
    def randseed = Integer.parseInt(args[3])
    def ratio = Double.parseDouble(args[4])
    println "properties:" + args[1] + ", timestep: " + args[2] + \
        ", randseed: " + args[3] + ", raio: " + args[4]
    start = System.nanoTime()
    cui = new NetmasCuiSimulator(args[1], randseed)
    cui.setScenarioSerial("NetmasCuiSimulator")
    cui.linerGenerateAgentRatio = ratio
    cui.initialize()
    init = System.nanoTime()
    // cui.model.getAgentHandler().setLinerGenerateAgentRatio(ratio)
    cui.model.setTimeScale(timestep)
    cui.timeSeriesLogPath = dirString
    cui.model.saveTimeSeriesLog(dirString)
    cui.start()
    cui.model.saveGoalLog(dirString, true);
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

def timeFilePath = dirString + '/time.csv'
def timeFile = new File(timeFilePath)
if (!timeFile.exists()) {
    timeFile.createNewFile()
}
def reader = new File(timeFilePath).newReader('UTF-8')
def str = reader.getText()
def writer = new File(timeFilePath).newWriter('UTF-8')
str += macroTimeStep + "," + initTime + "," + loopTime + "," + tick
writer.println(str)
writer.close()
