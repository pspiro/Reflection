/*
 * $Id: JSONArray.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package org.json.simple;

import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.function.Supplier;

import org.json.simple.parser.JSONParser;

import common.Util;

public class JsonArray extends TJsonArray<JsonObject> {

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
		return parse( 
				reader, 
				() -> new JsonObject(),
				() -> new JsonArray()
				);
	}

	/** reader, with types  (could be moved to base class)
	 *  to read a file, pass FileReader(filename) */
	@SuppressWarnings("unchecked")
	public static <T extends JsonObject, L extends TJsonArray<T>> L parse(
			Reader reader, 
			Supplier<T> objSupplier, 
			Supplier<TJsonArray<T>> listSupplier
			) throws Exception {
		
		return (L)new JSONParser().parse( 
				reader, 
				objSupplier, 
				listSupplier
				);
	}
	

}
