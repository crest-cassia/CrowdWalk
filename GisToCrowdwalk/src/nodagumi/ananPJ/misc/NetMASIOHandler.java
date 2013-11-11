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
import nodagumi.ananPJ.misc.CommunicationHandler;

public class NetMASIOHandler extends CommunicationHandler
    implements Serializable {

    protected static int bufsize = 8192;
    // network type
    protected int port = -1;
    protected String addr = null;
    // file type
    protected String path = null;
    // buffer type
    protected byte[] buffer = null;
    protected int len = -1;

    protected ByteBuffer sharedBuffer = null;
    protected byte[] sbuf = null;
    protected ArrayList<byte[]> linkBuffer = null;
    protected ServerSocket ssock = null;
    protected Socket sock = null;
    protected BufferedInputStream in = null;
    protected BufferedOutputStream out = null;
    protected ObjectInputStream ois = null;
    protected ObjectOutputStream oos = null;
    // inputed|outputed buffer is eof or not (used by SND|RCV_FILE mode)
    protected boolean file_eof = false;

    public NetMASIOHandler(
            CommunicationType _type, boolean _isDebug,
            String _addr, int _port,        // network type
            String _path,                   // file type
            byte[] _buffer                  // buffer type
            ) {
        super(_type, _isDebug);
        addr = _addr;
        port = _port;
        path = _path;
        buffer = _buffer;
        sbuf = new byte[bufsize];
        sharedBuffer = ByteBuffer.wrap(sbuf);
        linkBuffer = new ArrayList<byte[]>();

        switch (getCommunicationType()) {
            case RCV_BUFFER:
                break;
            case SND_BUFFER:
                break;
            case RCV_FILE:
                break;
            case SND_FILE:
                break;
            case RCV_PIPE:
                break;
            case SND_PIPE:
                break;
            case RCV_NETWORK:
                try {
                    ssock = new ServerSocket();
                    ssock.setReuseAddress(true);
                    ssock.bind(new InetSocketAddress(port));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                break;
            case SND_NETWORK:
                break;
            default:
                break;
        }
    }

    public Object readStream() {
        synchronized(linkBuffer) {
            Object obj = null;
            byte[] nullCheck = null;
            if (getCommunicationType() == CommunicationType.NONE) {
                return obj;
            } else if (getCommunicationType() != CommunicationType.RCV_BUFFER
                    && getCommunicationType() != CommunicationType.RCV_FILE
                    && getCommunicationType() != CommunicationType.RCV_PIPE
                    && getCommunicationType() != CommunicationType.RCV_NETWORK)
            {
                System.err.println("NetMASIOHandler.readStream readStream " +
                        "method is valid only receive mode.");
                return obj;
            }
            //try {
            int linkBufferSize = 0;
            // get index of byte[] null object stored in linkBuffer
            int index = -1;
            int size = 0;
            for (byte[] b : linkBuffer) {
                // calc byte size of linkBuffer from head to index
                if (b == nullCheck) {
                    index = linkBuffer.indexOf(b);
                    break;
                }
                size += b.length;
            }
            /*
            if (getIsDebug())
                System.err.println("\tNetMASIOHandler.readStream null " +
                        "index: " + index);
            */
            if (index < 0)
                return obj;
            // copy bytes (from head to index) in linkBuffer to retval
            byte[] retval = new byte[size];
            int offset = 0;
            for (byte[] b : linkBuffer) {
                if (b == nullCheck)
                    break;
                System.arraycopy(b, 0, retval, offset, b.length);
                offset += b.length;
            }
            for (int i = 0; i <= index; i++)
                linkBuffer.remove(0);

            //byte[] linkedBuffer = new byte[linkBufferSize];
            /*
            if (getIsDebug())
                System.err.println("NetMASIOHandler.readStream returned" +
                        "object size: " + size);
            */
            obj = bytes2Object(retval);
            return obj;
        }
        // } catch (InterruptedException ie) {
            // ie.printStackTrace();
        // }
    }

    public void writeStream(Object obj) {
        if (getCommunicationType() == CommunicationType.NONE) {
            return;
        } else if (getCommunicationType() != CommunicationType.SND_BUFFER &&
                getCommunicationType() != CommunicationType.SND_FILE &&
                getCommunicationType() != CommunicationType.SND_PIPE &&
                getCommunicationType() != CommunicationType.SND_NETWORK) {
            System.err.println("NetMASIOHandler.writeStream writeStream " +
                    "method" + " is valid only receive mode.");
            return;
        }
        byte[] buf = object2Bytes(obj);
        synchronized(linkBuffer) {
            linkBuffer.add(buf);
        }
    }

    public void writeStream(String obj) {
        if (getCommunicationType() == CommunicationType.NONE) {
            return;
        } else if (getCommunicationType() != CommunicationType.SND_BUFFER &&
                getCommunicationType() != CommunicationType.SND_FILE &&
                getCommunicationType() != CommunicationType.SND_PIPE &&
                getCommunicationType() != CommunicationType.SND_NETWORK) {
            System.err.println("NetMASIOHandler.writeStream writeStream " +
                    "method" + " is valid only receive mode.");
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
    }

    @Override
    public void close() {
        setStopFlag(true);
        try {
            if (out != null)
                out.close();
            if (sock != null)
                sock.shutdownOutput();
            if (oos != null)
                oos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    protected void receiveBufferRun() {
    }

    @Override
    protected void receiveFileRun() {
        try {
            int offset = 0;
            in = new BufferedInputStream(new FileInputStream(path));
            while (true) {
                synchronized (sharedBuffer) {
                    sharedBuffer.wait();
                    sharedBuffer.clear();
                    if (getStopFlag())
                        break;
                    len = in.read(sbuf, offset, sbuf.length);
                    offset += len;
                    if (len < 0)
                        break;
                    sharedBuffer.notify();
                }
            }
            in.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    @Override
    protected void receivePipeRun() {
    }

    @Override
    protected void receiveNetworkRun() {
        try {
            boolean connected = false;
            int tmpSize = 0;
            int sizeLen = 0;
            while (true) {
                    if (!connected) {
                        sock = ssock.accept();
                        in = new BufferedInputStream(sock.getInputStream());
                        connected = true;
                        /*
                        if (getIsDebug())
                            System.out.println("NetMASIOHandler." +
                                    "receiveNetworkRun connected addr " + 
                                    sock.getRemoteSocketAddress());
                        */
                    }
                    sharedBuffer.clear();
                    //len = in.read(sharedBuffer, 0 , sharedBuffer.limit());
                    len = in.read(sbuf, 0 , sbuf.length);
                    if (len <= 0) {
                        sock.shutdownInput();
                        in.close();
                        connected = false;
                        byte[] nullByte = null;
                        synchronized(linkBuffer) {
                            linkBuffer.add(nullByte);
                        /*
                        if (getIsDebug())
                            System.out.println("NetMASIOHandler." +
                                    "receiveNetworkRun disconnected." +
                                    "recevie len: " + tmpSize + " len:" +
                                    sizeLen + " linkBuffer len: " + 
                                    linkBuffer.size());
                        */
                            tmpSize = 0;
                            sizeLen = 0;
                            continue;
                        }
                    }
                    byte[] storedBytes = new byte[len];
                    tmpSize += sharedBuffer.position();
                    sizeLen += len;
                    sharedBuffer.get(storedBytes, 0, len);
                    synchronized(linkBuffer) {
                        linkBuffer.add(storedBytes.clone());
                    }
                    /*
                    if (getIsDebug())
                        System.out.println("NetMASIOHandler." +
                                "receiveNetworkRun receive " + len + " bytes");
                    */
                //}
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    protected void sendBufferRun() {
            byte[] buf = new byte[bufsize];
            while (true) {
                if (getStopFlag())
                    break;
                //len = readBuffer(buf, 0, buf.length);
                if (len < 0)
                    break;
                int rlen = len;   // remaining not written buffer
                int offset = 0;
                while (rlen > 0) {
                    if (rlen > buffer.length) {
                        System.arraycopy(buf, offset, buffer, 0,
                                buffer.length);
                        rlen -= buffer.length;
                        offset += buffer.length;
                    } else {
                        System.arraycopy(buf, offset, buffer, 0, rlen);
                        rlen = 0;
                        offset += rlen;
                    }
                }
                break;
            }
    }

    @Override
    protected void sendFileRun() {
        while (true) {
            try {
                /*
                if (getIsDebug())
                    System.out.println("NetMASIOHandler.sendFileRun: " +
                            "wait buffer I/O.");
                */
                if (getStopFlag())
                    break;
                    /*
                    if (getIsDebug())
                        System.out.println("NetMASIOHandler.sendFileRun " +
                                "create file");
                    */
                /*
                if (getIsDebug())
                    System.out.println("NetMASIOHandler.sendFileRun " +
                            "write len:" + sharedBuffer.length);
                */
                //out.write(sharedBuffer, 0, sharedBuffer.limit());
                if (linkBuffer.size() <= 0)
                    continue;
                synchronized(linkBuffer) {
                    for (byte[] b : linkBuffer) {
                        out = new BufferedOutputStream(
                                new FileOutputStream(path));
                        out.write(b, 0, b.length);
                        out.flush();
                        out.close();
                    }
                }
                    /*
                    if (getIsDebug())
                        System.out.println("NetMASIOHandler.sendFileRun " +
                                "close file.");
                    */
            } catch (IOException ioe) {
                ioe.printStackTrace();
            //} catch (InterruptedException ie) {
            //    ie.printStackTrace();
            }
        }
    }

    @Override
    protected void sendPipeRun() {
    }

    @Override
    protected void sendNetworkRun() {
        try {
            //byte[] buf = new byte[bufsize];
            boolean connected = false;
            if (getIsDebug()) {
                System.out.println("NetMASIOHandler.sendNetworkRun try to " +
                        "connect(" + addr + ", " + port + ")");
            }
            byte[] sendBuf = null;
            while (true) {
                if (linkBuffer.size() > 0) {
                    synchronized(linkBuffer) {
                        sendBuf = linkBuffer.get(0);
                        linkBuffer.remove(0);
                    }
                    if (!connected) {
                        sock = new Socket(addr, port);
                        out = new BufferedOutputStream(sock.getOutputStream());
                        /*
                        if (getIsDebug())
                            System.out.println("NetMASIOHandler." +
                                    "sendNetworkRun  connected server(" + 
                                    addr + ", " + port + ")");
                        */
                        connected = true;
                    }
                    if ((sock == null) || (getStopFlag())) {
                        //System.err.println("NetMASIOHandler.networkRun " +
                        //        "socket returns null.");
                        break;
                    }
                    out.write(sendBuf, 0, sendBuf.length);
                    System.err.println("NetMASIOHandler.networkRun " +
                            "send len " + sendBuf.length + ".");
                    out.flush();
                    sock.shutdownOutput();
                    out.close();
                    connected = false;
                    //if (getIsDebug())
                    //    System.out.println("NetMASIOHandler." +
                    //            "sendNetworkRun disconnected.");
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                /*
                synchronized(sharedBuffer) {
                    System.err.println("NetMASIOHandler.networkRun 1");
                    sharedBuffer.wait();
                    System.err.println("NetMASIOHandler.networkRun 2");
                    if (!connected) {
                        sock = new Socket(addr, port);
                        out = new BufferedOutputStream(sock.getOutputStream());
                        if (getIsDebug())
                            System.out.println("NetMASIOHandler." +
                                    "sendNetworkRun  connected server(" + 
                                    addr + ", " + port + ")");
                        connected = true;
                    }
                    System.err.println("NetMASIOHandler.networkRun 3");
                    if ((sock == null) || (getStopFlag())) {
                        System.err.println("NetMASIOHandler.networkRun " +
                                "socket returns null.");
                        break;
                    }
                    System.err.println("NetMASIOHandler.networkRun 4");
                    len = sharedBuffer.position();
                    if (len <= 0)
                        continue;
                    if (getIsDebug())
                        System.err.println("NetMASIOHandler.networkRun send " +
                                "len:" + len + ", str:" + 
                                sharedBuffer.toString());
                    //out.write(sharedBuffer, 0, sharedBuffer.position());
                    out.write(sbuf, 0, sharedBuffer.position());
                    out.flush();
                    sharedBuffer.clear();
                    System.err.println("NetMASIOHandler.networkRun 5");
                    if (file_eof) {
                        sock.shutdownOutput();
                        out.close();
                        connected = false;
                        file_eof = false;
                        if (getIsDebug())
                            System.out.println("NetMASIOHandler." +
                                    "sendNetworkRun disconnected.");
                        continue;
                    }
                    System.err.println("NetMASIOHandler.networkRun 6");
                    sharedBuffer.notify();
                    System.err.println("NetMASIOHandler.networkRun 7");
                }
                */
            }
        //} catch (SocketTimeoutException ste) {
        } catch (IOException ioe) {
            ioe.printStackTrace();
        //} catch (InterruptedException ie) {
        //    ie.printStackTrace();
        }
    }

    // convert from byte array to object
    private static Object bytes2Object(byte[] bytes) {
        Object obj = null;
        /*
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        byte[] convertedByte = new byte[bytes.length];
        bb.order(ByteOrder.LITTLE_ENDIAN);
        //bb.order(ByteOrder.BIG_ENDIAN);
        bb.get(convertedByte, 0, convertedByte.length);
        */
        try {
            //obj = new ObjectInputStream(
                    // new ByteArrayInputStream(convertedByte)).
            obj = new ObjectInputStream(
                    new ByteArrayInputStream(bytes)).readObject();
            //XMLDecoder xmlDecoder = new XMLDecoder(
            //        new ByteArrayInputStream(bytes));
            //obj = xmlDecoder.readObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        //XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(bytes));
        //Object obj = (Object) decoder.readObject();

        return obj;
    }

    // convert from object to byte array
    private static byte[] object2Bytes(Object obj) {
        /*
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            //ObjectOutput oos = new ObjectOutputStream(baos);
            //oos.writeObject(obj);
            //oos.close();
            XMLEncoder xmlEncoder = new XMLEncoder(baos);
            xmlEncoder.writeObject(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
        */
        byte[] bytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //XMLEncoder encoder = new XMLEncoder(baos);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            baos.flush();
            oos.flush();
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            //encoder.writeObject((NetmasCuiSimulator) obj);
            //encoder.close();
            bytes = baos.toByteArray();
            baos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.err.println("object2Bytes len: " + bytes.length);
        return bytes;
    }

    private static Object string2Object(String str) {
        XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(str.
                    getBytes()));
        Object obj = (Object) decoder.readObject();
        return obj;
    }

    private static String object2String(Object obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(baos);
        encoder.writeObject(obj);
        encoder.close();
        String xmlString = baos.toString();
        return xmlString;
    }
}

