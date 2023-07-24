/*
 * $Id: JSONArray.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.parser.JSONParser;

import common.Util;
import tw.util.S;


/**
 * A JSON array. JSONObject supports java.util.List interface.
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JsonArray extends ArrayList<JsonObject> implements JSONAware, JSONStreamAware {
	private static final long serialVersionUID = 3957988303675231981L;

    /**
     * Encode a list into JSON text and write it to out. 
     * If this list is also a JSONStreamAware or a JSONAware, JSONStreamAware and JSONAware specific behaviours will be ignored at this top level.
     * 
     * @see org.json.simple.JSONValue#writeJSONString(Object, Writer)
     * 
     * @param list
     * @param out
     */
	public static void writeJSONString(List list, Writer out) throws IOException{
		if(list == null){
			out.write("null");
			return;
		}
		
		boolean first = true;
		Iterator iter=list.iterator();
		
        out.write('[');
		while(iter.hasNext()){
            if(first)
                first = false;
            else
                out.write(',');
            
			Object value=iter.next();
			if(value == null){
				out.write("null");
				continue;
			}
			
			JSONValue.writeJSONString(value, out);
		}
		out.write(']');
	}
	
	public void writeJSONString(Writer out) throws IOException{
		writeJSONString(this, out);
	}
	
	/**
	 * Convert a list to JSON text. The result is a JSON array. 
	 * If this list is also a JSONAware, JSONAware specific behaviours will be omitted at this top level.
	 * 
	 * @see org.json.simple.JSONValue#toJSONString(Object)
	 * 
	 * @param list
	 * @return JSON text, or "null" if list is null.
	 */
	public static String toJSONString(List list){
		if(list == null)
			return "null";
		
        boolean first = true;
        StringBuffer sb = new StringBuffer();
		Iterator iter=list.iterator();
        
        sb.append('[');
		while(iter.hasNext()){
            if(first)
                first = false;
            else
                sb.append(',');
            
			Object value=iter.next();
			if(value == null){
				sb.append("null");
				continue;
			}
			sb.append(JSONValue.toJSONString(value));
		}
        sb.append(']');
		return sb.toString();
	}

	public String toJSONString(){
		return toJSONString(this);
	}
	
	public String toString() {
		return toJSONString();
	}

	public JsonObject getJsonObj( int i) throws Exception {
		return super.get(i);
	}

	public static JsonArray parse( String text) throws Exception {
		Util.require( JsonArray.isArray(text), "Error: not a json array: " + text);
		return (JsonArray)new JSONParser().parse( text);
	}

	public static boolean isArray(String ret) {
		return ret != null && ret.trim().startsWith("[");
	}

	public void display() {
		JsonObject.display(this, 0, false);
	}

	/** Return the item in the array that has tag=value (where value is a string) */
	public JsonObject find(String tag, String value) throws Exception {
		for (JsonObject item : this) {
			if (value.equals( item.getString(tag) ) ) {
				return item;
			}
		}
		return null;
	}

	public void displayShort() {
		S.out( "[{");
		for (JsonObject item : this) {
			S.out(item + ",");  // leaves an extra , at the end 
		}
		S.out("]");
		
	}

}
