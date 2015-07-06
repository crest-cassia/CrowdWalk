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

public class CuiSimulationLauncher extends BasicSimulationLauncher {

    public CuiSimulationLauncher(Random _random) {
        super(_random);
    }

    public CuiSimulationLauncher(String _propertiesPath) {
        super(null) ;
        // load properties
        setPropertiesFromFile(_propertiesPath) ;

        // check property options
        if (getNetworkMapFile() == null) {
            System.err.println("CuiSimulationLauncher: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(getNetworkMapFile())).exists()) {
            System.err.println("CuiSimulationLauncher: specified map file does " +
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
        Itk.logDebug("CuiSimulationLauncher start!");
        paused = false ;
        simulateMainLoop() ;
    }

}

