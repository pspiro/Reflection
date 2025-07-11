/*
 * $Id: JSONArray.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.json.simple.parser.JSONParser;

import common.Util;
import tw.util.OStream;
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
	public JsonArray() {
		super();
	}
	
	public JsonArray( JsonArray other) {
		super( other);
	}
	
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

	public JsonObject getJsonObj( int i) throws Exception {  // this is not needed, remove. pas
		return super.get(i);
	}

	public static JsonArray parse( String text) throws Exception {
		Util.require( JsonArray.isArray(text), "Error: not a json array: " + text);
		return (JsonArray)new JSONParser().parse( text);
	}

	public static JsonArray parse(InputStream is) throws Exception {
		return (JsonArray)new JSONParser().parse( new InputStreamReader(is) );  // parseMsg() won't work here because it assumes all values are strings
	}	
	
	public static boolean isArray(String ret) {
		return ret != null && ret.trim().startsWith("[");
	}

	public void display() {
		JsonObject.display(this, 0, false);
	}
	

	/** Print one record per line */
	public void print() {
		forEach( record -> S.out( record) );
	}

	/** Return the item in the array that has tag=value (where value is a string) */
	// you could create more generic version that takes a method that takes
	// a value object and returns true or false?
	// Function<Object,Boolean>
	public JsonObject find(String tag, String value) throws Exception {
		for (JsonObject item : this) {
			if (value.equals( item.getString(tag) ) ) {
				return item;
			}
		}
		return null;
	}
	
	/** return true if any object in the list has tag=val;
	 *  uses .equals()  */
	public boolean has(String tag, Object val) {
		for (var item : this) {
			if (item.get( "id").equals( val) ) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void sortJson(String tag, boolean forward) {
		sort( (a, b) -> {
			Object obj1 = a.get( tag);
			Object obj2 = b.get( tag);
			
			int ret;
			
			if (obj1 == null && obj2 == null) {
				ret = 0;
			}
			else if (obj1 == null) {  // sort nulls to the end
				ret = 1;
			}
			else if (obj2 == null) {
				ret = -1;
			}
			else if (obj1.getClass().equals( obj2.getClass() ) && obj1 instanceof Comparable) {
				ret = ((Comparable)obj1).compareTo( obj2);
			}
			else if (obj1 instanceof Number && obj2 instanceof Number) {
				ret = Double.valueOf( obj1.toString() ).compareTo( Double.valueOf( obj2.toString() ) );
			}
			else {
				ret = obj1.toString().compareTo( obj2.toString() );
			}
			
			return forward ? ret : -ret;
		});
	}
	
	public void filter( Predicate<JsonObject> tester) {
		for (Iterator<JsonObject> iter = iterator(); iter.hasNext(); ) {
			if (!tester.test(iter.next() ) ) {
				iter.remove();
			}
		}
	}

	/** Update all members of the array */
	public void update(String key, Function<Object,Object> updater) {
		forEach( row -> row.update( key, updater) );
	}

	/** Convert some field from String or Integer to Double */ 
	public void convertToDouble(String key) {
		update( key, value -> Double.valueOf( value.toString() ) );
	}
	
	/** @param fancy true for browser and JEditorPane; false for tooltips */ 
	public String toHtml(boolean fancy) {
		StringBuilder b = new StringBuilder();
		
		String[] keys = getKeys().toArray(new String[0] );
		
		// add borders?
		if (fancy) {
			Util.wrapHtml( b, "style", fancyTable);  
		}
		
		Util.appendHtml( b, "table", () -> {
			// add header row
			Util.appendHtml( b, "tr", () -> {
				for (String key : keys) {
					Util.wrapHtml( b, "td", key);
				}
			});
		
			// add a row for each item in the array
			forEach( item -> {
				if (item instanceof JsonObject) {
					JsonObject obj = (JsonObject)item;

					Util.appendHtml( b, "tr", () -> {
						for (String key : keys) {
							Util.wrapHtml( b, "td", obj.getString(key) );
						}
					});
				}
			});
		});
		
		return Util.wrapHtml("html", b.toString() );
	}

	/** Return sorted set of all keys of all JsonObjects in this array */
	public ArrayList<String> getKeys() {
		HashSet<String> keys = new HashSet<>();
		forEach( item -> {
			if (item instanceof JsonObject) {
				((JsonObject)item).addKeys( keys);
			}
		});
		
		ArrayList<String> list = new ArrayList<>( keys);
		Collections.sort( list);
		return list;
	}
	
	public String[] getValues( String key) {
		HashSet<String> set = new HashSet<>();
		forEach( item -> set.add( item.getString( key) ) );
		return set.toArray( new String[0]);
	}

	public void writeToCsv(String filename, char sep) throws FileNotFoundException {
		writeToCsv( filename, sep, getKeys().toArray( new String[0]) );
	}
	
	public void writeToCsv(String filename, char sep, String keysString) throws FileNotFoundException {
		writeToCsv( filename, sep, keysString.split(",") );
	}
	
	private void writeToCsv(String filename, char sep, String[] keys) throws FileNotFoundException {
		try( OStream os = new OStream(filename) ) {
			
			// write header row
			for (String key : keys) {
				os.write( key.toString() + sep);
			}
			os.writeln();
			
			// write data rows
			forEach( obj -> {
				for (String key : keys) {
					String str = String.format(	"\"%s\"%s", 
							obj.getString( key)
								.replaceAll( "\\\"", "'")
								.replaceAll( "\\t", " ")
								.replaceAll( "\\n", " "),
							sep);
					os.write( str);
				}
				os.writeln();
			});
			
		}	
	}

	public void writeToFile(String filename) throws IOException {
		try (FileWriter writer = new FileWriter( filename) ) {
			writeJSONString( writer);
		}
	}

	/** get rid of null values and empty strings */
	public void removeNulls() {
		forEach( rec -> rec.removeNulls() );
	}

	/** Create a map of key to record 
	 * @throws Exception */
	public HashMap<String, JsonObject> getMap(String key) throws Exception {
		HashMap<String, JsonObject> map = new HashMap<>();
		Util.forEach( this, rec -> Util.iff( rec.getString( key), val -> map.put( val, rec) ) );;
		return map;
	}
	
	/** Create array from the same tag of each element 
	 * @throws Exception */
	public ArrayList<String> getArrayOf( String key) throws Exception {
		ArrayList<String> list = new ArrayList<String>();
		Util.forEach( this, rec -> list.add( rec.getString( key)));
		return list;
	}
	
	static public JsonArray toJson( ArrayList<? extends Record> list) throws Exception {
		JsonArray json = new JsonArray();
		for (var item : list) {
			json.add( JsonObject.toJson( item) );
		}
		return json;
	}
	
	/** convert this JsonArray to a list of Records of type T */ 
	public <T extends Record> ArrayList<T> toRecord(Class<T> clas) throws Exception {
		return Util.mapEx( this, item -> item.toRecord( clas) );
	}
}

