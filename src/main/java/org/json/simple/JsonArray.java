/*
 * $Id: JSONArray.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;

import org.json.simple.parser.JSONParser;

import common.Util;
import tw.util.OStream;

/** An array of JsonObjects */
public class JsonArray extends TsonArray<JsonObject> {

	/** string, no types */
	public static JsonArray parse( String text) throws Exception {
		Util.require( JsonArray.isArray(text), "Error: not a json array: " + text);
	
		return parse( new StringReader( text) ); 
	}
	
	public static JsonArray readFromFile( String filename) throws Exception {
		return parse( new FileReader( filename) );
	}

	/** reader, no types */
	public static JsonArray parse( Reader reader) throws Exception {
		return parse( reader, new JsonArray(), () -> new JsonObject() );
	}

	/** reader, with types  (could be moved to base class)
	 *  to read a file, pass FileReader(filename) */
	@SuppressWarnings("unchecked")
	public static <T extends JsonObject, L extends TsonArray<T>> L parse(
			Reader reader, 
			TsonArray<T> list,
			Supplier<T> objSupplier 
			) throws Exception {
		
		return (L)new JSONParser().parseArray( reader, list, objSupplier);
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
}
