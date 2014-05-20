import java.io.*
import java.nio.channels.*

import nodagumi.ananPJ.NetworkMapEditor
import nodagumi.ananPJ.NetmasCuiSimulator
import nodagumi.ananPJ.NetworkParts.Link.*
import nodagumi.ananPJ.misc.NetmasPropertiesHandler


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

/** make a generation file */
def makeGenerationFile(file, ratioA, ratioB, ratio, model) {
    try {
        FileOutputStream fd = new FileOutputStream(file)
        OutputStreamWriter out = new OutputStreamWriter(fd)
        baseWString = "TIMEEVERY,WEST_STATION_LINKS,18:00:00,18:09:00,60,60,"
        baseEString = "TIMEEVERY,EAST_STATION_LINKS,18:00:00,18:09:00,60,60,"
        out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_N_NODES,POINT_A,E_POINT_A\n")
        out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MN_NODES,POINT_A,E_POINT_B\n")
        out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MS_NODES,POINT_A,E_POINT_C\n")
        out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_S_NODES,POINT_A,E_POINT_D\n")
        out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_N_NODES,POINT_B,W_POINT_A\n")
        out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MN_NODES,POINT_B,W_POINT_B\n")
        out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MS_NODES,POINT_B,W_POINT_C\n")
        out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_S_NODES,POINT_B,W_POINT_D\n")
        out.write(baseWString + ((int)((1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_N_NODES,POINT_C,E_POINT_A\n")
        out.write(baseWString + ((int)((1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MN_NODES,POINT_C,E_POINT_B\n")
        out.write(baseWString + ((int)((1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MS_NODES,POINT_C,E_POINT_C\n")
        out.write(baseWString + (((int)(1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_S_NODES,POINT_C,E_POINT_D\n")
        out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_N_NODES,POINT_D,W_POINT_A\n")
        out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MN_NODES,POINT_D,W_POINT_B\n")
        out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MS_NODES,POINT_D,W_POINT_C\n")
        out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_S_NODES,POINT_D,W_POINT_D\n")
        out.close()
    } catch (IOException e) {
        e.printStackTrace()
    }
}

def rand = new Random()
def nme     /** network map editor */
def cui     /** cui simulator */
def start, init, finish     /** time */
def macroTimeStep = ""      /** time step of the simulation */
def tick = 0.0      /** simulation counter */
def dirString = ""  /** the directory where the logs are saved. */

println "arg size: " + args.size() + ", args: " + args

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
    def properties = new NetmasPropertiesHandler(args[0])
    def randseed = properties.getInteger("randseed", -1)
    if (randseed != -1) {
        rand.setSeed(randseed)
    }
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
        macroTimeStep += cui.expectedDensityMacroTimeStep
        tick = cui.model.getSecond()
    } else {
        start = System.nanoTime()
        def properties = new NetmasPropertiesHandler(args[0])
        def randseed = properties.getInteger("randseed", -1)
        if (randseed != -1) {
            rand.setSeed(randseed)
        }
        nme = new NetworkMapEditor(rand)
        nme.setProperties(args[0])
        init = System.nanoTime()
        nme.setVisible(true)
        finish = System.nanoTime()
        macroTimeStep += "0"
    }
} else if (args.size() == 3) {
    dirString = '/tmp/'
    if (args[0] == 'cui') {
        start = System.nanoTime()
        cui = new NetmasCuiSimulator(args[1])
        cui.setScenarioSerial("NetmasCuiSimulator")
        cui.initialize()
        init = System.nanoTime()
        cui.start()
        finish = System.nanoTime()
        macroTimeStep += cui.expectedDensityMacroTimeStep
        tick = cui.model.getSecond()
    } else {
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
        cui.model.getAgentHandler().setExpectedDensityMacroTimeStep(timestep)
        cui.model.setTimeScale(timestep)
        cui.timeSeriesLogPath = dirString
        cui.model.saveTimeSeriesLog(dirString)
        cui.expectedDensityMacroTimeStep = timestep
        cui.start()
        finish = System.nanoTime()
        macroTimeStep += cui.expectedDensityMacroTimeStep
        tick = cui.model.getSecond()
    }
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
    macroTimeStep += cui.expectedDensityMacroTimeStep
    tick = cui.model.getSecond()
// 1: properties 2: ratioA(= 1.0 - ratioC) 3: ratioB(= 1.0 - ratioD)
// 4: ratio(for all) 5: randseed
} else if (args.size() == 6) {
    // rm, mkdir
    dirString = '/tmp/practis-ratioA' + args[2] + '-ratioB' + args[3] + '-ratio' + args[4] + '-rand' + args[5]
    File tdict = new File(dirString)
    if (tdict.isDirectory()) {
        deleteFile(tdict)
    }
    //tdict.delete()
    def ratioA = Double.parseDouble(args[2])
    def ratioB = Double.parseDouble(args[3])
    def ratio = Double.parseDouble(args[4])
    def randseed = Integer.parseInt(args[5])
    println "properties:" + args[1] + ", ratioA: " + args[2] + \
        ", ratioB: " + args[3] + ", raio: " + args[4] + ", rand: " + args[5]
    start = System.nanoTime()
    def genfile = (new File(args[1])).getParent() + "/gen.csv"
    println "genfile:" + genfile
    makeGenerationFile(genfile, ratioA, ratioB, ratio, "DENSITY")
    cui = new NetmasCuiSimulator(args[1], randseed)
    cui.setScenarioSerial("NetmasCuiSimulator")
    //cui.linerGenerateAgentRatio = ratio
    cui.initialize()
    init = System.nanoTime()
    // cui.model.getAgentHandler().setLinerGenerateAgentRatio(ratio)
    //cui.model.getAgentHandler().setExpectedDensityMacroTimeStep(timestep)
    //cui.model.setTimeScale(timestep)
    cui.timeSeriesLogPath = dirString
    cui.model.saveTimeSeriesLog(dirString)
    //cui.expectedDensityMacroTimeStep = timestep
    cui.start()
    cui.model.saveGoalLog(dirString, true);
    finish = System.nanoTime()
    // macroTimeStep += cui.expectedDensityMacroTimeStep
    tick = cui.model.getSecond()
// 1: properties 2: randseed, 3: map, 4: speed_model 5: ratio(for all) 6: none
} else if (args.size() == 7) {
    // rm, mkdir
    dirString = '/tmp/practis-rand' + args[2] + '-width' + args[3] + '-model' + args[4] + '-ratio' + args[5]
    File tdict = new File(dirString)
    if (tdict.isDirectory()) {
        deleteFile(tdict)
    }
    //tdict.delete()
    def randseed = Integer.parseInt(args[2])
    def width = Double.parseDouble(args[3])
    def model = args[4]
    def ratio = Double.parseDouble(args[5])
    println "properties:" + args[1] + ", randseed: " + args[2] + \
        ", width: " + args[3] + ", model: " + args[4] + ", ratio: " + args[5]
    start = System.nanoTime()
    def genfile = (new File(args[1])).getParent() + "/gen.csv"
    makeGenerationFile(genfile, 0.5, 0.5, ratio, model)
    cui = new NetmasCuiSimulator(args[1], randseed)
    cui.setScenarioSerial("NetmasCuiSimulator")
    //cui.linerGenerateAgentRatio = ratio
    cui.initialize()
    init = System.nanoTime()
    for (MapLink link : cui.networkMap.getLinks()) {
        link.width = width
    }
    // cui.model.getAgentHandler().setLinerGenerateAgentRatio(ratio)
    //cui.model.getAgentHandler().setExpectedDensityMacroTimeStep(timestep)
    //cui.model.setTimeScale(timestep)
    cui.timeSeriesLogPath = dirString
    cui.model.saveTimeSeriesLog(dirString)
    //cui.expectedDensityMacroTimeStep = timestep
    cui.start()
    cui.model.saveGoalLog(dirString, true);
    finish = System.nanoTime()
    // macroTimeStep += cui.expectedDensityMacroTimeStep
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

if (args.size() <= 2){
    writer = new File('tick.txt').newWriter('UTF-8')
    writer.write(Double.toString(tick) + "\n")
    writer.close()
}else{
    writer = new File(args[2] + '.json').newWriter('UTF-8')
    writer.write("{\"tick\" : " + Double.toString(tick) + "}")
    writer.close()    
}