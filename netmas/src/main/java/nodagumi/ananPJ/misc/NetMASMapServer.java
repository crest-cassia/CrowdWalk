package nodagumi.ananPJ.misc;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


import nodagumi.ananPJ.NetmasCuiSimulator;
import nodagumi.ananPJ.misc.NetMASIOHandler;
import nodagumi.ananPJ.misc.CommunicationHandler;

public class NetMASMapServer extends NetMASIOHandler
    implements Serializable {


    public NetMASMapServer(
            CommunicationType _type, boolean _isDebug,
            String _addr, int _port,        // network type
            String _path,                   // file type
            byte[] _buffer                  // buffer type
            ) {
        super(_type, _isDebug, _addr, _port, _path, _buffer);
    }

    public void writeStream(String obj) {
        if (getCommunicationType() != CommunicationType.RCV_NETWORK) {
            System.err.println("NetMASMapServer.writeStream writeStream " +
                    "method" + " is valid only rcv network mode.");
            return;
        }
        byte[] buf = null;
        try {
            buf = obj.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
        synchronized(linkBuffer) {
            linkBuffer.add(buf);
        }
        System.err.println("NetMASMapServer.writeStream linkBuffer len: " +
                linkBuffer.size());
    }
}

