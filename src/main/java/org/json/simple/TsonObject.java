/*
 * $Id: TsonObject.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.awt.Component;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.json.simple.parser.JSONParser;

import common.Util;
import tw.util.S;

/**
 * A JSON object. Key value pairs are unordered. TsonObject supports java.util.Map interface.
 * 
 * Note that null values are supported by put(); use putIf() to avoid null values
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class TsonObject<T> extends HashMap<String,T> implements JSONAware, JSONStreamAware {
	
	private static final long serialVersionUID = -503443796854799292L;
	
	
	public TsonObject() {
		super();
	}

	public TsonObject(Map<String,T> base) {
		super(base);
	}

	/**
	 * Allows creation of a TsonObject from a Map. After that, both the
	 * generated TsonObject and the Map can be modified independently.
	 * 
	 * @param map
	 */
//	public TsonObject(Map map) {
//		super(map);
//	}


    /**
     * Encode a map into JSON text and write it to out.
     * If this map is also a JSONAware or JSONStreamAware, JSONAware or JSONStreamAware specific behaviours will be ignored at this top level.
     * 
     * @see org.json.simple.JSONValue#writeJSONString(Object, Writer)
     * 
     * @param map
     * @param out
     */
	public static void writeJSONString(Map map, Writer out) throws IOException {
		if(map == null){
			out.write("null");
			return;
		}
		
		boolean first = true;
		Iterator iter=map.entrySet().iterator();
		
        out.write('{');
		while(iter.hasNext()){
            if(first)
                first = false;
            else
                out.write(',');
			Map.Entry entry=(Map.Entry)iter.next();
            out.write('\"');
            out.write(escape(String.valueOf(entry.getKey())));
            out.write('\"');
            out.write(':');
			JSONValue.writeJSONString(entry.getValue(), out);
		}
		out.write('}');
	}

	public void writeJSONString(Writer out) throws IOException{
		writeJSONString(this, out);
	}
	
	/**
	 * Convert a map to JSON text. The result is a JSON object. 
	 * If this map is also a JSONAware, JSONAware specific behaviours will be omitted at this top level.
	 * 
	 * @see org.json.simple.JSONValue#toJSONString(Object)
	 * 
	 * @param map
	 * @return JSON text, or "null" if map is null.
	 */
	public static String toJSONString(Map map){
		if(map == null)
			return "null";
		
        StringBuffer sb = new StringBuffer();
        boolean first = true;
		Iterator iter=map.entrySet().iterator();
		
        sb.append('{');
		while(iter.hasNext()){
            if(first)
                first = false;
            else
                sb.append(',');
            
			Map.Entry entry=(Map.Entry)iter.next();
			toJSONString(String.valueOf(entry.getKey()),entry.getValue(), sb);
		}
        sb.append('}');
		return sb.toString();
	}
	
	public String toJSONString(){
		return toJSONString(this);
	}
	
	private static String toJSONString(String key,Object value, StringBuffer sb){
		sb.append('\"');
        if(key == null)
            sb.append("null");
        else
            JSONValue.escape(key, sb);
		sb.append('\"').append(':');
		
		sb.append(JSONValue.toJSONString(value));
		
		return sb.toString();
	}
	
	public String toString(){
		return toJSONString();
	}

	public static String toString(String key,Object value){
        StringBuffer sb = new StringBuffer();
		toJSONString(key, value, sb);
        return sb.toString();
	}
	
	/**
	 * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
	 * It's the same as JSONValue.escape() only for compatibility here.
	 * 
	 * @see org.json.simple.JSONValue#escape(String)
	 * 
	 * @param s
	 * @return
	 */
	public static String escape(String s){
		return JSONValue.escape(s);
	}

	/** string only */
	public static JsonObject parse( String text) throws Exception {
		Util.require( isObject(text), "Error: not a json object: " + text);

		return parse( new StringReader(text) );
	}
	
	/** reader stream only */
	public static JsonObject parse(InputStream is) throws Exception {
		return parse( new InputStreamReader(is) ); 
	}	
	
	/** reader only */
	public static JsonObject parse(Reader reader) throws Exception {
		return new JSONParser().parseObject( reader, new JsonObject() ); 
	}	
	
	public static boolean isObject(String text) {
		return text != null && text.trim().startsWith("{");
	}

	public void display(String title) {
		S.out(title);
		display( this, 0, false);
		System.out.println();
	}

	public void display() {
		display( this, 0, false);
		System.out.println();
	}

	public static void display(Object objIn, int level, boolean arrayItem) {
		if (objIn instanceof TsonObject) {
			out( "{\n");

			TsonObject map = ((TsonObject)objIn);
			
			boolean first = true;
			for (Object key : map.keySet() ) {
				Object val = map.get( key);
				
				if (val != null && val.toString().length() > 0) {
					if (!first) {
						out( ",\n");
					}

					out( "%s\"%s\" : ", Util.tab( level+1), key);
					display( val, level + 1, false);
					first = false;
				}
			}
			out( "\n%s%s", Util.tab(level), "}");
		}
		else if (objIn instanceof TsonArray) {
			TsonArray ar = (TsonArray)objIn;
			
			if (ar.size() == 0) {
				out( "[ ]");
			}
			else {
				out( "[\n%s", Util.tab(level+1) );
				
				boolean first = true;
				for (Object obj : ar) {
					if (!first) {
						out( ", ");
					}
					display( obj, level + 1, true);
					first = false;
				}
				out( "\n%s]", Util.tab(level) );
			}
		}
		else {
			out( JSONValue.toJSONString(objIn) );
		}
	}
	
	static void out( String format, Object... params) {
		System.out.print( String.format( format, params) );
	}

	static void out( Object str) {
		System.out.print( str);
	}
//
//	public boolean getBool(String key) {
//		return Boolean.parseBoolean( getString(key) );
//	}

	/** Add the pair if val is not null AND not empty string
	 *  
	 *  This should be used on newly created objects since it's not clear if you
	 *  would want to overwrite existing values with null values */
	public void putIf(String key, T val) {
		if (val != null && S.isNotNull( val.toString() ) ) {
			put(key, val);
		}
	}


	/** Update the value for one specific key;
	 *  the value passed to the callback will never be null
	 *  
	 *  WARNING: if updater returns null, the present value will be maintained */
	public void update(String key, Function<Object,T> updater) {
		Object obj = get(key);
		if (obj != null ) {
			put( key, updater.apply(obj) );
		}
	}

	public static JsonObject readFromFile(String filename) throws Exception {
		return parse( new FileInputStream( filename) );
	}

	public void writeToFile(String filename) throws IOException {
		try (FileWriter writer = new FileWriter( filename) ) {
			writeJSONString( writer);
		}
	}

	public void removeNulls() {
		for (Iterator<Entry<String, T>> iter = entrySet().iterator(); iter.hasNext(); ) {
			Object val = iter.next().getValue();
			if (val == null || S.isNull( val.toString() ) ) {
				iter.remove();
			}
		}
	}

	public void show( Component parent) {
		Util.inform( parent, toString() );
	}
	
	@Override public String toHtml() {
		return "not implemented";
	}

	/** This assumes that this object is a map of key to JsonObject */
	public JsonArray toArray() {
		JsonArray ar = new JsonArray();
		for (var obj : values() ) {
			ar.add( (JsonObject)obj);
		}
		return ar;
	}
}
/** NOTE: Timestamp objects are stored as
 *  
 */