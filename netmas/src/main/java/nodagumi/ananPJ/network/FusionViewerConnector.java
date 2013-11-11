package nodagumi.ananPJ.network;

import java.awt.Color;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import javax.vecmath.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Agents.*;

public class FusionViewerConnector {

    private final String fusionViewerLogNumberLine =
        "%s,%d,%d,,,,,,,,,,,,,,,,,,,,,,\n";
    private final String fusionViewerLogOneLine =
        ",,,%d,,,%f,%f,%f,,,,,,,,,HUMAN,,,,,WALK,,%s\n";
    private Calendar cal = null;
    private DateFormat fusionViewerLogFormat = null;
    private DateFormat fusionViewerFileNameFormat = null;
    private boolean fusionViewerLogInitialized = false;
    private String fusionViewerLogFileName = null;
    private File fileLog = null;

    public FusionViewerConnector() {
        cal = Calendar.getInstance();
        fusionViewerLogFormat = new SimpleDateFormat("yyyy:MM:dd-HH:mm:ss:SSS");
        fusionViewerFileNameFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
    }

    /** Saves the agent positions to the PTF.
     * The log format is based on Fusion Viewer log type as follows:
     *
     * DATE-TIME,frame,no,ID,x,y,lx,ly,lz,NS,wx,EW,wy,wz,subject,feature,gender,
     * attribute,group,action,state,free
     *
     * The above format is a line separated with ",". Please check README file,
     * if you want to know the details.
     * @param dir logging directory that Fusion Viewer log files are saved.
     * @param time simulation time.
     * @param count simulation step.
     */
    public void saveFusionViewerLog(String dir, double startTime, double time,
            int count, List<EvacuationAgent> agents) {
        System.err.println("saveFusionViewerLog " + count);
        updateCalendar(cal, time + startTime);
        PrintWriter writer = null;
        if (!fusionViewerLogInitialized) {  // initialize
            fusionViewerLogFileName = dir + "/" +
                fusionViewerFileNameFormat.format(cal.getTime()) +
                "-CrowdWalk001.csv";
            try {
                fileLog = new File(fusionViewerLogFileName);
                writer = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(
                                new FileOutputStream(fileLog, false) , "utf-8")),
                        true);
                writer.printf("# file version3,,,,,,,,,,,,,,,,,,,,,,,,\n");
                writer.printf("# DATE-TIME,frame,no,ID,x,y,px,py,pz,ox,oy,oz," +
                        "NS,wx,EW,wy,wz,subject,feature,gender,attribute," +
                        "group,action,state,free\n");
                writer.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            fusionViewerLogInitialized = true;
        }
        try {
            fileLog = new File(fusionViewerLogFileName);
            writer = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(fileLog, true) , "utf-8")),
                    true);
            int agentNotFinished = 0;
            for (int i = 0; i < agents.size(); i++) {
                EvacuationAgent agent = agents.get(i);
                if (!agent.finished())
                    agentNotFinished += 1;
            }
            writer.printf(fusionViewerLogNumberLine,
                    fusionViewerLogFormat.format(cal.getTime()), count,
                    agentNotFinished);
            for (int i = 0; i < agents.size(); i++) {
                EvacuationAgent agent = agents.get(i);
                if (!agent.finished()) {
                    writer.printf(fusionViewerLogOneLine,
                            agent.ID,
                            agent.getPos().getX(),
                            agent.getPos().getY() * -1.0,
                            agent.getHeight(),
                            agent.getGoal());
                }
            }
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** Update calendar from time(seconds) value.*/
    private static void updateCalendar(Calendar cal, double time) {
        cal.clear();
        cal.set(2013, 0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, (int) (time - ((int) time)) * 1000);
        cal.set(Calendar.SECOND, (int) time);
    }

    public static final int BUFSIZE = 1024000;
    private BufferedOutputStream out = null;
    private Socket sock = null;
    private byte[] buff = new byte[BUFSIZE];
    private ServerSocket ssock = null;
    private int port = 9886;

    /** Send the PTF packets to a specified address and port.
     * The functionalities are quite same with saveFusionViewerLog.
     */
    public void waitConnection() {
        try {
            ssock = new ServerSocket();
            ssock.setSoTimeout(1000);
            ssock.setReuseAddress(true);
            ssock.bind(new InetSocketAddress(port));

            for (;;) {  /* wait connection till interrupt */
                try {
                    System.err.println("    wait connection...");
                    sock = ssock.accept();
                    break;
                } catch (SocketTimeoutException ste) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    continue;
                }
            }
            System.err.println("    connected");
            this.setOutputStream(new BufferedOutputStream(
                        sock.getOutputStream()));
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public synchronized BufferedOutputStream getOutputStream() {
        return this.out;
    }

    public synchronized void setOutputStream(BufferedOutputStream out) {
        this.out = out;
    }

    private boolean reconnect = true;

    public void sendFusionViewerLog(double startTime, double time, int count,
            List<EvacuationAgent> agents) {

        int len, offset = 0;
        int agentNotFinished = 0;
        BufferedOutputStream output = this.getOutputStream();
        if (output == null) {
            System.err.println("\tconnection is not established yet.");
            return;
        }
        for (int i = 0; i < agents.size(); i++) {
            EvacuationAgent agent = agents.get(i);
            if (!agent.finished())
                agentNotFinished += 1;
        }
        /* pack header */
        if (!objectToByte.charToByte(buff, offset, (char) 0)) {
            System.err.println("fail to pack counter");
        }
        offset += Character.SIZE / Byte.SIZE * 2;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.YEAR))) {
            System.err.println("fail to pack year");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.MONTH))) {
            System.err.println("fail to pack month");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.DAY_OF_WEEK))) {
            System.err.println("fail to pack day of week");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.DATE))) {
            System.err.println("fail to pack date");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.HOUR))) {
            System.err.println("fail to pack hour");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.MINUTE))) {
            System.err.println("fail to pack minute");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.SECOND))) {
            System.err.println("fail to pack second");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.shortToByte(buff, offset,
                    (short) cal.get(Calendar.MILLISECOND))) {
            System.err.println("fail to pack milli second");
        }
        offset += Short.SIZE / Byte.SIZE;
        if (!objectToByte.intToByte(buff, offset, agentNotFinished)) {
            System.err.println("fail to pack no");
        }
        offset += Integer.SIZE / Byte.SIZE;
        System.err.println("    send " + offset + " bytes, agents " +
                agentNotFinished);
        if (reconnect) {
            for (;;) {
                try {
                    output.write(buff, 0, offset);
                    output.flush();
                    break;
                } catch (IOException ioe) {
                    try {
                        output.close();
                        ssock.close();
                        sock.close();
                    } catch (IOException ioex) {
                        ioex.printStackTrace();
                    }
                    this.waitConnection();
                    output = this.getOutputStream();
                }
            }
        } else {
            try {
                output.write(buff, 0, offset);
                output.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        offset = 0;
        for (int i = 0; i < agents.size(); i++) {
            EvacuationAgent agent = agents.get(i);
            if (!agent.finished()) {
                float fx = (float) agent.getPos().getX();
                float fy = (float) agent.getPos().getY() * -1.0f;
                float fz = (float) agent.getHeight();
                /* Conversion for Mojiko from here */
                fx -= 8350.0f;
                fy += 5800.0f;
                fz += 0.5f;
                float ffx = fx;
                float ffy = fy;
                fx = ((float) (ffx * Math.cos(Math.PI * -40.5 / 180.0) - ffy *
                            Math.sin(Math.PI * -40.5 / 180.0))) * 1.0f;
                fy = ((float) (ffx * Math.sin(Math.PI * -40.5 / 180.0) + ffy *
                            Math.cos(Math.PI * -40.5 / 180.0))) * 0.6f;
                fx -= 48.0f;
                fy += 16.0f;
                /* Conversion for Mojiko to here */

                if (!objectToByte.floatToByte(buff, offset, fx)) {
                    System.err.println("fail to pack fx");
                }
                offset += Float.SIZE / Byte.SIZE;
                if (!objectToByte.floatToByte(buff, offset, fy)) {
                    System.err.println("fail to pack fy");
                }
                offset += Float.SIZE / Byte.SIZE;
                if (!objectToByte.floatToByte(buff, offset, fz)) {
                    System.err.println("fail to pack fz");
                }
                offset += Float.SIZE / Byte.SIZE;
                float f = ((float)(agent.getSpeed())) * 0.31f;
                Color c_rgb = new Color(Color.HSBtoRGB(f, 0.8f, 0.8f));
                Color3f c = new Color3f(c_rgb);
                if (!objectToByte.charToByte(buff, offset, colorToChar(c.x))) {
                    System.err.println("fail to pack R");
                }
                offset += 1;
                if (!objectToByte.charToByte(buff, offset, colorToChar(c.y))) {
                    System.err.println("fail to pack G");
                }
                offset += 1;
                if (!objectToByte.charToByte(buff, offset, colorToChar(c.z))) {
                    System.err.println("fail to pack B");
                }
                offset += 2;    /* padding */
            }
        }
        System.err.println("    send " + offset + " bytes, agents " +
                agentNotFinished + ((char) 6.0f));
        if (reconnect) {
            try {
                output.write(buff, 0, offset);
                output.flush();
            } catch (IOException ioe) {
            }
        } else {
            try {
                output.write(buff, 0, offset);
                output.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private static char colorToChar(float f) {
        return (char) (255.0f * f);
    }
}
