/*
 * $Id: JSONObject.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonstoneid.siwe.SiweMessage;

import chain.Stocks.Stock;
import common.Util;
import reflection.SiweUtil;
import tw.util.S;
import web3.Erc20;

/**
 * A JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
 * 
 * Note that null values are supported by put(); use putIf() to avoid null values
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JsonObject extends HashMap<String,Object> implements JSONAware, JSONStreamAware, Comparable<JsonObject> {
	
	private static final long serialVersionUID = -503443796854799292L;
	
	
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
	
	public static JsonObject parseOrdered( String text) throws Exception {
		Util.require( isObject(text), "Error: not a json object: " + text);
		return (JsonObject)new JSONParser().parse( text, new ContainerFactory() {
			@Override public Map createObjectContainer() {
				return new OrderedJson();
			}
			@Override public List creatArrayContainer() {
				return null;
			}
		});
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

	/** If the key does not exist, it adds a new array to the map */
	public JsonArray getOrAddArray(String key) {
		JsonArray array = (JsonArray)get(key);
		if (array == null) {
			array = new JsonArray();
			put( key, array);
		}
		return array; 
	}

	/** Call it like this: json.<String>getAnyArray( key)
	 *  Also works for array of lists, like this: json.<ArrayList>getAnyArray( key);
	 *  In this case, each item in the array will be of the correct json type, e.g.
	 *  string, int, or object */
	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> getArrayOf(String key) {
		ArrayList<T> array = (ArrayList<T>)get(key);
		return array != null ? array : new ArrayList<T>(); 
	}
	
	/** Can return null; caller should check */
	public JsonObject getObject(String key) throws Exception {
		Object obj = get(key);
		if (obj == null || obj.equals( "") ) {
			return null;
		}
		if (obj instanceof JsonObject) {
			return (JsonObject)obj;
		}
		if (obj instanceof String) {
			return JsonObject.parse( (String)obj);
		}
		throw new Exception( String.format( "Not a json object  key=%s  val=%s", key, obj) );
	}

	/** Throws exception if not found */
	public JsonObject getRequiredObj(String key) throws Exception {
		JsonObject obj = getObject(key);
		Util.require( obj instanceof JsonObject, "Not a json object  key=%s  val=%s", key, obj);
		return obj;
	}
	
	/** Never returns null, returns empty JsonObject  */
	public JsonObject getObjectNN(String key) throws Exception {
		Object obj = getObject(key);
		return obj != null ? (JsonObject)obj : new JsonObject();
	}
	
	/** Never returns null, creates new JsonObject and adds it to the map if necessary */
	public JsonObject getOrAddObject(String key) throws Exception {
		var obj = getObject(key);
		if (obj == null) {
			obj = new JsonObject();
			put( key, obj);
		}
		return obj;
	}
	
	/** Returns zero for null value Can handle hex calues starting with 0x. */
	public long getLong(String key) {
		return Util.getLong( getString( key) );
	}

	/** Returns zero for null value. */
	public int getInt( String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Integer.parseInt( str) : 0;
	}

	public BigInteger getBigInt(String key) {
		String str = getString( key);
		return S.isNotNull( str) ? new BigInteger( str) : BigInteger.ZERO;
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

					out( "%s\"%s\" : ", Util.tab( level+1), key);
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
		return Util.equalsIgnore( getString(key), "true", "y");
	}

	public SiweMessage getSiweMessage() throws Exception {
		return SiweUtil.toSiweMessage(this);
	}

	/** @deprecated; use putIf(); when everyone is uing putIf(), remove putIf()
	 *  and change put() to not add null values; having null values seems useless
	 *  and it can break things because the size of the map is not the same
	 *  if you skip null values */
	@Override public Object put(String key, Object value) {
		return super.put(key, value);
	}

	/** Add the pair if val is not null AND not empty string
	 *  
	 *  This should be used on newly created objects since it's not clear if you
	 *  would want to overwrite existing values with null values */
	public void putIf(String key, Object val) {
		if (val != null && S.isNotNull( val.toString() ) ) {
			put(key, val);
		}
	}
	
	public JsonObject append( String key, Object val) {
		put( key, val);
		return this;
	}

	/** add all items in other to this object using putIf;
	 *  values in other will overwrite these values UNLESS the value in other is null */
	public JsonObject append( JsonObject other) {
		forEach( (key,val) -> putIf( key, val) );
		return this;
	}


	/** Update the value for one specific key;
	 *  the value passed to the callback will never be null
	 *  
	 *  WARNING: if updater returns null, the present value will be maintained */
	public void update(String key, Function<Object,Object> updater) {
		Object obj = get(key);
		if (obj != null ) {
			put( key, updater.apply(obj) );
		}
	}

	/** Return true if any of the keys are missing or have no value */
	public boolean has(String... keys) {
		for (var key : keys) {
			if (S.isNull( getString( key) ) ) {
				return false;
			}
		}
		return true;
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
	
	/** Don't add \n's because it break JOptionPane in Util.inform */ 
	public String toHtml(boolean fancy) {
		return toHtml( fancy, keySet().toArray() );
	}
	
	public String toHtml( boolean fancy, Object[] keys) {
		StringBuilder b = new StringBuilder();
		
		if (fancy) {
			Util.wrapHtml( b, "style", fancyTable); 
		}
		
		Util.appendHtml( b, "table", () -> {
			for (var keyObj : keys) {
				String key = (String)keyObj;
				Object value = get( key);
				
				if (S.isNotNullObj( value) ) {
					Util.appendHtml( b, "tr", () -> {
						Util.wrapHtml( b, "td", key);
		
						if (value instanceof JsonArray) {
							Util.wrapHtml( b, "td", ((JsonArray)value).toHtml() );
						}
						else {
							Util.wrapHtml( b, "td", Util.left(Util.toString(value), 100) );  // trim it too 100 because Cookies are really long
						}
					});
				}
			}
		});
		
		return Util.wrapHtml( "html", b.toString() );
	}


	/** Copy all tags from other to this object; null values are okay but not added */
	public void copyFrom(JsonObject other, String... tags) {
		for (String tag : tags) {
			putIf( tag, other.get(tag) );
		}
	}

	/** Increment the key by val; stored value must be a Double */
	public void increment(String key, double val) {
		put( key, getDouble(key) + val);
	}
	
	/** Will convert a string to enum; may return null */
	public <T extends Enum<T>> T getEnum( String key, T[] values) throws Exception {
		Object val = get(key);
		return val == null ? null : Util.getEnum(val.toString(), values);
	}

	public <T extends Enum<T>> T getEnum( String key, T[] values, T defVal) throws Exception {
		Object val = get(key);
		return val == null ? null : Util.getEnum(val.toString(), values, defVal);
	}

	/** Add all keys to the key set */
	public void addKeys(HashSet<String> keys) {
		keySet().forEach( key -> keys.add( key) );
	}
	
	public static JsonObject readFromFile(String filename) throws Exception {
		return parse( new FileInputStream( filename) );
	}
	
	public void writeToFile(String filename) throws IOException {
		try (FileWriter writer = new FileWriter( filename) ) {
			writeJSONString( writer);
		}
	}

	public Stock getStock(String tag) {
		return (Stock)get( tag);
	}

	public BigInteger getBlockchain(String key, int decimals) throws Exception {
		return Erc20.toBlockchain( getDouble( key), decimals);
	}

	public void removeNulls() {
		for (Iterator<Entry<String, Object>> iter = entrySet().iterator(); iter.hasNext(); ) {
			Object val = iter.next().getValue();
			if (val == null || S.isNull( val.toString() ) ) {
				iter.remove();
			}
		}
	}
	
	/** good for chaining */
	public JsonObject removeEntry( String tag) {
		remove( tag);
		return this;
	}
	
	public static void displayMap( HashMap<String,?> map) {
		new JsonObject( map).display();
	}

	public static JsonObject toJson( Record record) throws Exception {
		JsonObject json = new JsonObject();

		// Iterate over each component (field) and add it to the JSON object
		for (var component : record.getClass().getRecordComponents() ) {
			// Make the method accessible if necessary
			component.getAccessor().setAccessible(true);

			// Get the field name
			String fieldName = component.getName();
			Object fieldValue = component.getAccessor().invoke(record);

			// Add the field and its value to the JSON object
			json.put(fieldName, fieldValue);
		}                
		return json;
	}

	/** make sure to add @JsonIgnoreProperties(ignoreUnknown = true) to the record to 
	 * ignore unknown fields so you can add new fields to the json before updating the record */
	public <T extends Record> T toRecord(Class<T> clas) throws Exception {
        return new ObjectMapper().readValue( toString(), clas);
	}

	/** or call keySet() */
	public ArrayList<String> getKeys() {
		ArrayList<String> ar = new ArrayList<>();
		keySet().forEach( key -> ar.add( key) );
		return ar;
	}

}
/** NOTE: Timestamp objects are stored as
 *  
 */