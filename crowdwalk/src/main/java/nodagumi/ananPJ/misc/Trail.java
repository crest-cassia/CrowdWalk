// -*- mode: java; indent-tabs-mode: nil -*-
/** Trail class
 * @author:: Itsuki Noda
 * @version:: 0.0 2017/06/20 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2017/06/20]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.misc ;

import java.util.ArrayList;
import nodagumi.Itk.*;

//======================================================================
/**
 * Trail class.
 * store trail (history) information for log output.
 * used in AgentTrailLog. 
 */
public class Trail {
    //============================================================
    /**
     *  Content of Trail.
     *  should have method getJsonObject().
     */
    static public interface Content {
	//----------------------------------------
	/**
	 *  return a Number, a String, a Map, a List, or 
	 *  an Object that can be translated to JSON.
	 *  In the case of Number or String, it will be translated to the value.
	 *  In the case of a List, it will be translated to a JSON array.
	 *  In the case of a Map, it will be traslated to a JSON object.
	 *  In the case of an Object, it will be traslated to a JSON object,
	 *  which consists of pair of name and value of public field.
	 */
	public Object getJsonObject() ;
    } // interface Content
    
    //============================================================
    /**
     *  General Object Content for Trail.
     */
    static public class ContentObject implements Content {
	//----------------------------------------
	/**
         * constructor
	 */
        public ContentObject() {} ;
        
	//----------------------------------------
	/**
         * return itself
	 */
	public Object getJsonObject() {
            return this ;
        }
    } // class ContentObject

    //============================================================
    /**
     *  General Message Content for Trail.
     */
    static public class ContentMessage implements Content {
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
         * message itself
	 */
        private String message = null ;

	//----------------------------------------
	/**
         * constructor
	 */
        public ContentMessage(String _message) {
            message = _message ;
        }
        
	//----------------------------------------
	/**
         * return message.
	 */
	public Object getJsonObject() {
            return message ;
        }
    } // class ContentObject
    
    //============================================================
    /**
     *  One record of Trail.
     */
    static public class Record {
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * timestamp
	 */
	public SimTime timestamp ;
	
	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	/**
	 * content of the record.
	 */
	public Content content ;
	
	//----------------------------------------
	/**
	 *  initialize.
	 */
	public Record(SimTime _timestamp, Content _content) {
	    timestamp = _timestamp ;
	    content = _content ;
	}
	    
	//----------------------------------------
	/**
	 *  get json object.
	 */
	public Object getJsonObject() {
	    ArrayList<Object> jsonObject = new ArrayList<Object>(2) ;
	    
	    jsonObject.add(Integer.valueOf((int)timestamp.getRelativeTime())) ;
	    jsonObject.add(content.getJsonObject()) ;
	    
	    return jsonObject ;
	}
    } // class Record
    
    //============================================================
    //============================================================
    //============================================================

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     *  Array of Trail Record
     */
    public ArrayList<Record> recordList = new ArrayList<Record>() ;

    //------------------------------------------------------------
    /**
     * initialization.
     */
    public Trail(){ } ;

    //------------------------------------------------------------
    /**
     * get JsonObject, which consists of a list of JsonObject of each item.
     */
    public ArrayList<Object> getJsonObject() {
        ArrayList<Object> jsonObject = new ArrayList<Object>() ;

	for(Record record : recordList) {
	    jsonObject.add(record.getJsonObject()) ;
	}

	return jsonObject ;
    }

    //------------------------------------------------------------
    /**
     * push JsonObject, which consists of a list of JsonObject of each item.
     */
    public Record add(SimTime timestamp, Content content) {
	Record record = new Record(timestamp, content) ;

	recordList.add(record) ;

	return record ;
    }

} // class Trail

