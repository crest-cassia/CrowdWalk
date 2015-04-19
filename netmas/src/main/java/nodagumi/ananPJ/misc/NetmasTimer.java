package nodagumi.ananPJ.misc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

// シミュレーションの実行にかかった実時間をミリ秒精度で出力する
// (プログラムのパフォーマンス計測に用いる)
public class NetmasTimer {

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

    // interval分のステップ(10)進むのにかかった実時間
    public void writeInterval() {
        if (counter == 1)
            writer.println("Interval," + getInterval());
        writer.flush();
    }

    // シミュレーション開始からの実経過時間(60ステップ進むごとに出力)
    public void writeElapsed() {
        writer.println("Elapsed," + getElapsed());
        writer.flush();
    }

    public String millis2HMSformat(long timeMillis) {
        long hours = timeMillis / 3600000;
        timeMillis = timeMillis % 3600000;
        long minutes = timeMillis / 60000;
        timeMillis = timeMillis % 60000;
        long seconds = timeMillis / 1000;
        long millis = timeMillis % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds,
                millis);
    }
}

