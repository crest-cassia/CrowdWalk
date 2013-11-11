package nodagumi.ananPJ.misc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.PrintStream;
import java.io.PrintWriter;


public class NetmasTimer implements Serializable {

    private int interval = 0;
    private int counter = 0;
    private long startTime = 0;
    private long currentTime = 0;
    private long previousTime = 0;
    private PrintWriter writer = null;

    public NetmasTimer(int _interval, String _path) {
        this.interval = _interval;
        if (_path != null) {
            try {
                writer = new PrintWriter(new PrintStream(_path));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void start() {
        startTime = System.currentTimeMillis();
        currentTime = startTime;
        previousTime = startTime;
    }

    public void stop() {
        if (writer != null)
            writer.close();
    }

    public void tick() {
        if (counter >= interval) {
            previousTime = currentTime;
            counter = 0;
            currentTime = System.currentTimeMillis();
        }
        counter++;
    }

    public String getCurrent() {
        return millis2HMSformat(currentTime);
    }

    public String getElapsed() {
        return millis2HMSformat(currentTime - startTime);
    }

    public String getInterval() {
        return millis2HMSformat(currentTime - previousTime);
    }

    public void printInterval() {
        if (counter == 1)
            System.out.println(getInterval());
    }

    public void writeInterval() {
        if (counter == 1)
            writer.println("I," + getInterval());
        writer.flush();
    }

    public void writeElapsed() {
        writer.println("E," + getElapsed());
        writer.flush();
    }

    public String millis2HMSformat(long timeMillis) {
        long hours = timeMillis / 3600000;
        timeMillis = timeMillis % 3600000;
        long minutes = timeMillis / 60000;
        timeMillis = timeMillis % 60000;
        long seconds = timeMillis / 1000;
        long millis = timeMillis % 1000;
        return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds,
                millis);
    }
}

