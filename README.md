# CrowdWalk: a multi-agent pedestrian simulator
- This is a multi-agent pedestrian simulator that uses a one-dimensional spatial model and supports large-scale crowd flow.
- As a Java application, it can be used on a variety of operating systems.
- The source code of CrowdWalk is maintained on GitHub and is available for free to anyone under the MIT license.


## Features


### Map editor

Create and edit network maps.
![networkmap_editor](https://user-images.githubusercontent.com/6541505/42435507-c4ddad6a-8391-11e8-9e43-6bc424d71e37.png)

### Simulator(CUI mode)

Runs the simulation with only the message displayed on the console.
![cui_simulator](https://user-images.githubusercontent.com/6541505/42435466-a6a9d4f4-8391-11e8-95bb-adbca4ca0d83.png)

### Simulator(2D graphic mode)

![2d_simulator](https://user-images.githubusercontent.com/6541505/42435544-daf6d28e-8391-11e8-9461-c4fbf0d2acc8.png)

### Simulator(3D graphic mode)

![3d_simulator](https://user-images.githubusercontent.com/6541505/42435555-e727083a-8391-11e8-8133-3bfc2e72f312.png)



## Installation

Installation should be done directly under your **home directory**.


### Environment

The following software environment is required.

- Command line shell
    - Execute a shell script to start CrowdWalk from the command line.
    - Any shell that can execute Bourne Shell shell scripts is acceptable, but if you do not know which one to use, use Bash. The following explanation assumes the use of Bash.
    - The following environment variables must be set for CrowdWalk to compile and run properly.  

~~~
export LANG=ja_JP.UTF-8
export JAVA_OPTS='-Dgroovy.source.encoding=UTF-8 -Dfile.encoding=UTF-8'
~~~
  

  
- Git
    - Download and update CrowdWalk.
- JDK (Java Development Kit)
    - Compiles and executes CrowdWalk source code.
    - JDK 11 or higher is required. However, if the functionality is limited to the simulator in CUI mode, JDK 1.6 or higher will work.
    - For Windows PCs, Oracle's JDK 17 is especially recommended (free updates will be available until December 2020).
- Ruby
    - Required to run special tools. Not required for normal use.
- Text Editor
    - Edits various configuration files.



### Download

Download the CrowdWalk source code from GitHub by executing the following command
~~~
git clone https://github.com/crest-cassia/CrowdWalk.git
~~~

When the download is complete, files are created with the following directory structure.

~~~
Crowdwalk/
    crowdwalk/
    floodtocrowdwalk/
    img2movie/
~~~

### Build

Go to the working directory of CrowdWalk.
(If you want to run CrowdWalk on Windows, check [the wiki page](https://github.com/crest-cassia/CrowdWalk/wiki/Windows%E3%81%A7%E3%81%AE%E5%AE%9F%E8%A1%8C%E6%96%B9%E6%B3%95).)
~~~~
cd ~/CrowdWalk/crowdwalk
~~~~

Execute the build command.
~~~
./gradlew
~~~~
The first time you run the command, it will take a while because a large number of libraries will be downloaded.  
When it finishes without any problems, the message BUILD SUCCESSFUL will be displayed.



After the build is complete,execute the following command and check if CrowdWalk help is displayed.
~~~
sh quickstart.sh -h
~~~



## Geting started
Several sample simulation files are available in the sample directory.  


### basic-sample

This is a one-dimensional spatial model of three rooms and a corridor. Agents are generated from each room and head for the exit on the far right.

Let's run it: start the simulator in 3D graphics mode.
~~~
sh quickstart.sh sample/basic-sample/properties.json -g
~~~


![basic_sample_window03](https://user-images.githubusercontent.com/6541505/42618979-9f5c7e60-85f1-11e8-87b8-f1539050613b.png)

Agents are circled in green. The reason they are not lined up in a straight line is that they are walking in parallel for the number of lanes. (Originally, there is a tendency for the display to be a little off.)  
Agents colors are changed from green to red according to their movement speed.


### stop-sample2

This is a simulation of the crowd movement of at the Fireworks Festival when they move to the nearest station. 
Traffic control (stop/release) will be conducted at several points on the road. Also, to prevent congestion in the station, entry will be restricted as trains arrive and depart. (Unfortunately, train arrivals and departures will not be displayed.) 

Because of the large number of agents, we will start the simulator in 2D graphics mode, which is lighter than 3D graphics mode. The -lError option is added to suppress the display of unimportant operation logs.

~~~
sh quickstart.sh sample/stop-sample2/properties.json -g2 -lError
~~~

![stop_sample_01](https://user-images.githubusercontent.com/6541505/42869086-0f73c656-8aaf-11e8-851e-dcc2fb8df9fd.png)



## Prepare for simulation


To start a simulation in CrowdWalk, the following files must be prepared at a minimum
- Map file
- Generation file
- Scenario file
- Property file

### Map file

A map file is a file in which a network map is saved.
- It is created using CrowdWalk's map editor.
- It can also be created from another format of map data, such as a shapefile or OSM, using a conversion tool.
- Since it is in XML format, it can also be edited using a text editor.

### Generation File

A generation file is a file that defines the characteristics of the agent, the place of occurrence, the time of occurrence, and the travel route.  
There are JSON format and CSV format (old format).


### Scenario File

A scenario file is a file that defines a schedule of events to occur during the simulation run.  
There are two formats: JSON and CSV (old format); the CSV format is deprecated.

### Properties File

A property file is a file that specifies the paths to various configuration files, including the above three files, and various simulator parameters.  
There are JSON format and XML format (old format). XML format is deprecated.


### Other files

In addition, the following files are also used as needed

- Background image file
- Fallback file - default setting values
- Camera work settings file - viewpoint settings for the simulator screen
- Pollution files - disaster situation data such as flooding, gas, etc.
- Link appearance files - link appearance settings
- Node appearance files - node appearance settings
- Agent appearance files - agent appearance settings
- Polygon appearance files - polygon appearance settings
- Coastline files - coastline data
- Ruby script files - external programs that extend the simulator's functionality


## Citation

If you use CrowdWalk in a scientific publication, we would appreciate
citations to the following paper::

    
    @ARTICLE{Yamashita2013-yj,
    title     = "Implementation of Simulation Environment for Exhaustive Analysis
                of Huge-Scale Pedestrian Flow",
    author    = "Yamashita, Tomohisa and Okada, Takashi and Noda, Itsuki",
    journal   = "SICE Journal of Control, Measurement, and System Integration",
    publisher = "Taylor \& Francis",
    volume    =  6,
    number    =  2,
    pages     = "137-146",
    month     =  mar,
    year      =  2013
    }

