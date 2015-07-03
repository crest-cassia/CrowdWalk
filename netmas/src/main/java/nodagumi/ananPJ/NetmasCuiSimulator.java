// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.*;
import java.util.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Agents.*;
import nodagumi.ananPJ.Agents.WalkAgent.*;
import nodagumi.ananPJ.Simulator.*;
import nodagumi.ananPJ.misc.*;

import nodagumi.Itk.*;

public class NetmasCuiSimulator extends BasicSimulationLauncher {

    public NetmasCuiSimulator(Random _random) {
        super(_random);
    }

    public NetmasCuiSimulator(String _propertiesPath) {
        super(null) ;
        // load properties
        setPropertiesFromFile(_propertiesPath) ;

        // check property options
        if (getNetworkMapFile() == null) {
            System.err.println("NetmasCuiSimulator: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(getNetworkMapFile())).exists()) {
            System.err.println("NetmasCuiSimulator: specified map file does " +
                    "not exist.");
            return;
        }
        try {
            networkMap = readMapWithName(getNetworkMapFile()) ;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        // プロパティファイルで指定されたパスを使用する(以下が無いとマップファイルの設定が使われる)
    }

    public void initialize() {
        initializeSimulatorEntity() ;
    }

    public void start() {
        Itk.logDebug("NetmasCuiSimulator start!");
        paused = false ;
        simulateMainLoop() ;
    }

}

