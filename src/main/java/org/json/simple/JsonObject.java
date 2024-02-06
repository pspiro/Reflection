/*
 * $Id: JSONObject.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.json.simple.parser.JSONParser;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import reflection.SiweUtil;
import tw.util.S;

/**
 * A JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JsonObject extends HashMap<String,Object> implements JSONAware, JSONStreamAware, Comparable<JsonObject> {
	
	private static final long serialVersionUID = -503443796854799292L;
	private DecimalFormat m_doubleFormat; 
	
	
	public JsonObject() {
		super();
	}

	public JsonObject(Map<String, ? extends Object> base) {
		super(base);
	}

	/**
	 * Allows creation of a JSONObject from a Map. After that, both the
	 * generated JSONObject and the Map can be modified independently.
	 * 
	 * @param map
	 */
//	public JSONObject(Map map) {
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

	
	
	
	
	/** Converts any object type to string or returns empty string, never null. */
	public String getString(String key) {
		Object val = get(key);
		return val != null ? val.toString() : ""; 
	}

	/** Converts any object type to string or returns empty string, never null. */
	public String getLowerString(String key) {
		Object val = get(key);
		return val != null ? val.toString().toLowerCase() : ""; 
	}

	public static JsonObject parse( String text) throws Exception {
		Util.require( isObject(text), "Error: not a json object: " + text);
		return (JsonObject)new JSONParser().parse( text);
	}
	
	public static JsonObject parse(InputStream is) throws Exception {
		return (JsonObject)new JSONParser().parse( new InputStreamReader(is) );  // parseMsg() won't work here because it assumes all values are strings
	}	
	
	public static boolean isObject(String text) {
		return text != null && text.trim().startsWith("{");
	}

	/** If the key does not exist, it returns an empty array */
	public JsonArray getArray(String key) {
		JsonArray array = (JsonArray)get(key);
		return array != null ? array : new JsonArray(); 
	}

	public JsonObject getObject(String key) throws Exception {
		return (JsonObject)get(key);
	}
	
	/** Returns zero for null value. */
	public long getLong(String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Long.parseLong( str) : 0;
	}

	/** Returns zero for null value. */
	public int getInt( String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Integer.parseInt( str) : 0;
	}

	public double getDouble(String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Double.valueOf( str) : 0.;
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
		if (objIn instanceof JsonObject) {
			out( "{\n");

			JsonObject map = ((JsonObject)objIn);
			
			boolean first = true;
			for (Object key : map.keySet() ) {
				Object val = map.get( key);
				
				if (val != null && val.toString().length() > 0) {
					if (!first) {
						out( ",\n");
					}

					out( "%s%s : ", Util.tab( level+1), key);
					display( val, level + 1, false);
					first = false;
				}
			}
			out( "\n%s%s", Util.tab(level), "}");
		}
		else if (objIn instanceof JsonArray) {
			JsonArray ar = (JsonArray)objIn;
			
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

	public boolean getBool(String key) {
		return Boolean.parseBoolean( getString(key) );
	}

	public SiweMessage getSiweMessage() throws Exception {
		return SiweUtil.toSiweMessage(this);
	}

	public JsonObject getRequiredObj(String key) throws Exception {
		JsonObject obj = getObject(key);
		Util.require(obj != null, "The required key is missing from the json object: " + key);
		return obj;
	}

	public Comparable getComparable(String key) {
		return (Comparable)get(key);
	}

	public boolean isComparable(String key) {
		return get(key) instanceof Comparable;
	}

	/** Add the pair if val is not null */
	public void putIf(String key, Object val) {
		if (val != null) {
			put(key, val);
		}
	}


	/** Update the value for one specific key;
	 *  the value passed to the callback will never be null */
	public void update(String key, Function<Object,Object> updater) {
		Object obj = get(key);
		if (obj != null ) {
			put( key, updater.apply(obj) );
		}
	}

	/** Return true if val is not null */
	public boolean has(String key) {
		return S.isNotNull( getString(key) );
	}

	@Override public int compareTo(JsonObject o) {
		return toString().compareTo(o.toString() );
	}

	public String getTime(String key, SimpleDateFormat fmt) {
		long v = getLong(key);
		return v == 0 ? "" : fmt.format(v);
	}

	/** Trim all string values */
	public JsonObject trim() {
		entrySet().forEach( entry -> {
			if (entry.getValue() instanceof String) {
				entry.setValue( entry.getValue().toString().trim() );
			}
		});
		return this;
	}
	
	public String toHtml() {
		StringBuilder b = new StringBuilder();
		b.append( "<html><table>\n");
		entrySet().forEach( entry -> b.append(String.format( "<tr><td>%s</td><td>%s</td></tr>\n", 
				entry.getKey(), 
				Util.left(Util.toString(entry.getValue()), 100) ) ) );  // trim it too 100 because Cookies are really long
		b.append( "</table></html>");
		return b.toString();
	}

	/** Copy all tags from other to this object; null values are okay but not added */
	public void copyFrom(JsonObject other, String... tags) {
		for (String tag : tags) {
			if (other.get(tag) != null) {
				put( tag, other.get(tag) );
			}
		}
	}

	/** Increment the key by val; stored value must be a Double */
	public void increment(String key, double val) {
		put( key, getDouble(key) + val);
	}
	
	public void doubleFormat( DecimalFormat fmt) {
		m_doubleFormat = fmt;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getEnum( String key, T[] values) throws Exception {
		return (T)get(key);
	}
}
/** NOTE: Timestamp objects are stored as
 *  
 */