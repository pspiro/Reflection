package org.json.simple;

import java.io.Reader;
import java.io.StringReader;
import java.util.function.Supplier;

import org.json.simple.JsonTable.Abc;
import org.json.simple.parser.JSONParser;

import tw.util.S;

/** A map of key to JsonObjects or subclss of JsonObject */
public class JsonTable<T extends JsonObject> extends TsonObject<T> {

	/** Leave out the keys, just display the values */
	@Override public String toHtml() {
		return toArray().toHtml();
	}

	static class Abc extends JsonObject {
	}
	
	public static void main(String[] args) throws Exception {
		Abc one = new Abc();
		one.put( "name", "bob");
		one.put( "age", 32);

		Abc two = new Abc();
		two.put( "name", "sam");
		two.put( "age", 44);
		
		JsonTable<Abc> table = new JsonTable<>();
		table.put( "a", one);
		table.put( "b", two);
		
		S.out( table.toHtml() );
		
		String str = table.toString();
		
		show( str);
	}

	/** the only thing not perfect is that the object supplier should only be used for the
	 *  values in the table and not any other jsonobjects further on down */
	public static <T extends JsonObject> JsonTable<T> parseTable(
			Reader reader, 
			JsonTable<T> table,
			Supplier<T> objSupplier) throws Exception {
		
		return (JsonTable<T>) new JSONParser().parse( reader, table, null, objSupplier );
	}	

	private static void show(String str) throws Exception {
		JsonTable<Abc> table = JsonTable.parseTable( 
				new StringReader( str),
				new JsonTable<Abc>(),
				() -> new Abc()
				);
		table.display();
		
	}


}
