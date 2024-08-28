/*
 * $Id: JSONObject.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;

import org.json.simple.parser.JSONParser;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import reflection.SiweUtil;
import reflection.Stock;
import tw.util.S;
import web3.Erc20;

/**
 * A JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
 * 
 * Note that null values are supported by put(); use putIf() to avoid null values
 * 
 * @author FangYidong<fangyidong@yahoo.com.cn>
 */
public class JsonObject extends TsonObject<Object> implements Comparable<JsonObject> {
	
	private static final long serialVersionUID = -503443796854799292L;
	
	
	public JsonObject() {
		super();
	}

//	public JsonObject(Map<String, ? extends Object> base) {
//		super(base);
//	}

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
//	public static void writeJSONString(Map map, Writer out) throws IOException {
//		if(map == null){
//			out.write("null");
//			return;
//		}
//		
//		boolean first = true;
//		Iterator iter=map.entrySet().iterator();
//		
//        out.write('{');
//		while(iter.hasNext()){
//            if(first)
//                first = false;
//            else
//                out.write(',');
//			Map.Entry entry=(Map.Entry)iter.next();
//            out.write('\"');
//            out.write(escape(String.valueOf(entry.getKey())));
//            out.write('\"');
//            out.write(':');
//			JSONValue.writeJSONString(entry.getValue(), out);
//		}
//		out.write('}');
//	}
//
//	public void writeJSONString(Writer out) throws IOException{
//		writeJSONString(this, out);
//	}
	
	/**
	 * Convert a map to JSON text. The result is a JSON object. 
	 * If this map is also a JSONAware, JSONAware specific behaviours will be omitted at this top level.
	 * 
	 * @see org.json.simple.JSONValue#toJSONString(Object)
	 * 
	 * @param map
	 * @return JSON text, or "null" if map is null.
	 */
//	public static String toJSONString(Map map){
//		if(map == null)
//			return "null";
//		
//        StringBuffer sb = new StringBuffer();
//        boolean first = true;
//		Iterator iter=map.entrySet().iterator();
//		
//        sb.append('{');
//		while(iter.hasNext()){
//            if(first)
//                first = false;
//            else
//                sb.append(',');
//            
//			Map.Entry entry=(Map.Entry)iter.next();
//			toJSONString(String.valueOf(entry.getKey()),entry.getValue(), sb);
//		}
//        sb.append('}');
//		return sb.toString();
//	}
//	
//	public String toJSONString(){
//		return toJSONString(this);
//	}
//	
//	private static String toJSONString(String key,Object value, StringBuffer sb){
//		sb.append('\"');
//        if(key == null)
//            sb.append("null");
//        else
//            JSONValue.escape(key, sb);
//		sb.append('\"').append(':');
//		
//		sb.append(JSONValue.toJSONString(value));
//		
//		return sb.toString();
//	}
//	
//	public String toString(){
//		return toJSONString();
//	}
//
//	public static String toString(String key,Object value){
//        StringBuffer sb = new StringBuffer();
//		toJSONString(key, value, sb);
//        return sb.toString();
//	}
//	
//	/**
//	 * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
//	 * It's the same as JSONValue.escape() only for compatibility here.
//	 * 
//	 * @see org.json.simple.JSONValue#escape(String)
//	 * 
//	 * @param s
//	 * @return
//	 */
//	public static String escape(String s){
//		return JSONValue.escape(s);
//	}

	
	
	
	
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
		return parse( reader, new JsonObject() );
	}	
	
	/** reader with types */
	@SuppressWarnings("unchecked")
	public static <T extends JsonObject> T parse( Reader reader, T topLevel) throws Exception {
		return (T) new JSONParser().parseObject( reader, topLevel);
	}	
	
	public static boolean isObject(String text) {
		return text != null && text.trim().startsWith("{");
	}

	/** If the key does not exist, it returns an empty array; this could be an issue
	 *  if the type is TJsonArray */
	public JsonArray getArray(String key) {
		JsonArray array = (JsonArray)get(key);
		return array != null ? array : new JsonArray(); 
	}

	/** Call it like this: json.<String>getAnyArray( key) */
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
	
	public boolean getBool(String key) {
		return Boolean.parseBoolean( getString(key) );
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


	/** Copy specified tags from other to this object; null values are okay but not added */
	public void copyFrom(JsonObject other, String... tags) {
		for (String tag : tags) {
			if (other.get(tag) != null) {
				put( tag, other.get(tag) );
			}
		}
	}

	/** Add or overwrite the values of this object with the ones passed in;
	 *  null values are ignore */ 
	public JsonObject modify( Object... objs) {
		Util.toJson( objs).forEach( (key,val) -> {
			if (val != null) {
				put( key, val);
			}
		});
		return this;
	}
	

	/** Increment the key by val; stored value must be a Double */
	public void increment(String key, double val) {
		put( key, getDouble(key) + val);
	}
	
	/** Will convert a string to enum; may return null; use method below for no exceptions */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum( String key, T[] values) throws Exception {
		Object val = get(key);
		return val instanceof Enum ? (T)val : Util.getEnum(val.toString(), values);
	}

	/** Will convert a string to enum. Defaults to def. Note that this will not
	 *  report an error if the string is invalid; the caller must be assured that
	 *  the string is null or one of the valid values. */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum( String key, T[] values, T def) {
		try {
			Object val = get(key);
			return val instanceof Enum 
					? (T)val 
			: val == null || S.isNull( val.toString() )
					? def
					: Util.getEnum(val.toString(), values);
		}
		catch( Exception e) {
			e.printStackTrace();
			return def;
		}
	}

	/** Add all keys to the key set */
	public void addKeys(HashSet<String> keys) {
		keySet().forEach( key -> keys.add( key) );
	}
	
	public Stock getStock(String tag) {
		return (Stock)get( tag);
	}

	public BigInteger getBlockchain(String key, int decimals) throws Exception {
		return Erc20.toBlockchain( getDouble( key), decimals);
	}

	/** for testing only */
	public void display2() {
		put( "type", getClass().getName() );
		display( this, 0, false);
		System.out.println();
	}
	
	/** good for chaining */
	public JsonObject removeEntry( String tag) {
		remove( tag);
		return this;
	}
	

}
/** NOTE: Timestamp objects are stored as
 *  
 */