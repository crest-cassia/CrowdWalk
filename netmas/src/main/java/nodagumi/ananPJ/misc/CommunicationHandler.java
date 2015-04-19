package nodagumi.ananPJ.misc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/*
 * CommunicationHandler manages inputs and outputs from/to multiple I/O.
 * Currently it supports buffer, file, pipe and network. CommunicationType
 * determines the output type. NetMAS${CommunicationType_NAME}Handler class
 * (for example, NetMASNetworkHandler) handles input type
 * ${CommunicationType_NAME}.
 */
public abstract class CommunicationHandler extends Thread {

    public enum CommunicationType {
        RCV_BUFFER,
        SND_BUFFER,
        RCV_FILE,
        SND_FILE,
        RCV_PIPE,
        SND_PIPE,
        RCV_NETWORK,
        SND_NETWORK,
        NONE
    };

    private boolean isDebug = false;
    private boolean stopFlag = false;
    private CommunicationType type = CommunicationType.NONE;

    public CommunicationHandler(CommunicationType _type, boolean _isDebug) {
        setCommunicationType(_type);
        isDebug = _isDebug;
    }

    abstract public void close();
    abstract protected void receiveBufferRun();
    abstract protected void receiveFileRun();
    abstract protected void receivePipeRun();
    abstract protected void receiveNetworkRun();
    abstract protected void sendBufferRun();
    abstract protected void sendFileRun();
    abstract protected void sendPipeRun();
    abstract protected void sendNetworkRun();

    public void run() {
        switch (getCommunicationType()) {
            case RCV_BUFFER:
                receiveBufferRun();
                break;
            case SND_BUFFER:
                sendBufferRun();
                break;
            case RCV_FILE:
                receiveFileRun();
                break;
            case SND_FILE:
                sendFileRun();
                break;
            case RCV_PIPE:
                receivePipeRun();
                break;
            case SND_PIPE:
                sendPipeRun();
                break;
            case RCV_NETWORK:
                receiveNetworkRun();
                break;
            case SND_NETWORK:
                sendNetworkRun();
                break;
            case NONE:
                break;
            default:
                System.err.println("CommunicationHandler.run: invalid type." +
                        " run method do nothing.");
                break;
        }
        close();
    }

    public boolean getIsDebug() {
        return isDebug;
    }

    public void setIsDebug(boolean _isDebug) {
        isDebug = _isDebug;
    }

    public boolean getStopFlag() {
        return stopFlag;
    }

    public void setStopFlag(boolean _stopFlag) {
        stopFlag = _stopFlag;
    }

    public CommunicationType getCommunicationType() {
        return type;
    }

    public void setCommunicationType(CommunicationType _type) {
        switch (_type) {
        case RCV_BUFFER:
        case SND_BUFFER:
        case RCV_FILE:
        case SND_FILE:
        case RCV_PIPE:
        case SND_PIPE:
        case RCV_NETWORK:
        case SND_NETWORK:
        case NONE:
            type = _type;
            break;
        default:
            if (isDebug)
                System.err.println("CommunicationHandler." + 
                    "setCommunicationTypeString: inputted invalid type:" + 
                    _type);
        }
    }

    public static String CommunicationType2String(CommunicationType _type) {
        switch (_type) {
        case RCV_BUFFER:
            return "RCV_BUFFER";
        case SND_BUFFER:
            return "SND_BUFFER";
        case RCV_FILE:
            return "RCV_FILE";
        case SND_FILE:
            return "SND_FILE";
        case RCV_PIPE:
            return "RCV_PIPE";
        case SND_PIPE:
            return "SND_PIPE";
        case RCV_NETWORK:
            return "RCV_NETWORK";
        case SND_NETWORK:
            return "SND_NETWORK";
        case NONE:
            return "NONE";
        default:
            return "*** INVALID ***";
        }
    }

    public String getCommunicationTypeString() {
        return CommunicationType2String(type);
    }

    public void setCommunicationTypeString(String _type) {
        if (_type.equals("RCV_BUFFER"))
            type = CommunicationType.RCV_BUFFER;
        else if (_type.equals("SND_BUFFER"))
            type = CommunicationType.SND_BUFFER;
        else if (_type.equals("RCV_FILE"))
            type = CommunicationType.RCV_FILE;
        else if (_type.equals("SND_FILE"))
            type = CommunicationType.SND_FILE;
        else if (_type.equals("SND_PIPE"))
            type = CommunicationType.SND_PIPE;
        else if (_type.equals("RCV_PIPE"))
            type = CommunicationType.RCV_PIPE;
        else if (_type.equals("RCV_NETWORK"))
            type = CommunicationType.RCV_NETWORK;
        else if (_type.equals("SND_NETWORK"))
            type = CommunicationType.SND_NETWORK;
        else if (_type.equals("NONE"))
            type = CommunicationType.NONE;
        else {
            if (isDebug)
                System.err.println("CommunicationHandler." + 
                    "setCommunicationTypeString: inputted invalid type:" + 
                    _type);
        }
    }
}

