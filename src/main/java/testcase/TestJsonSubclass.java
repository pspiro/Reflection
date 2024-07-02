package testcase;

import java.io.StringReader;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.TsonArray;

import com.ib.client.Types.Action;

import common.Util;
import junit.framework.TestCase;
import tw.util.S;


public class TestJsonSubclass extends TestCase {
	static class Rec extends JsonObject {
	}
	
	static class Recs extends TsonArray<Rec> {
	}
	
	public void testEnum() throws Exception {
		JsonObject obj = Util.toJson( "name", "peter", "action", Action.Buy);

		JsonArray ar = new JsonArray();
		ar.add( obj);
		ar.add( obj);
		ar.writeToFile("file.t");
		
		JsonArray ar2 = JsonArray.readFromFile( "file.t");
		ar2.print();
	}

	/** this shows how to read back a json object that returns a JsonObject subclass 
	 * @throws Exception */
	public void test1() throws Exception {
		Rec rec = new Rec();
		rec.put( "name", "peter");
		rec.put( "age", 55);
		
		// parse object, maintain types
		String str = rec.toString();
		Rec rec2 = JsonObject.parse( new StringReader(str), () -> new Rec() );
		rec2.display2();

		// parse object, no types
		str = rec.toString();
		JsonObject obj = JsonObject.parse( str);
		obj.display2();
		
		Rec rec3 = new Rec();
		rec3.put( "name", "dam");
		rec3.put( "age", 42);

		Recs recs = new Recs();
		recs.add( rec);
		recs.add( rec3);
		
		str = recs.toString();

		// parse array, maintain types
		Recs recs2 = JsonArray.parse( new StringReader( str), () -> new Rec(), () -> new Recs() );
		S.out( recs2.getClass().getName() );
		recs2.display();
		
		S.out( "object type = " + recs2.get( 0).getClass().getName() );

		// parse array, no types
		JsonArray recs3 = JsonArray.parse( str);
		S.out( recs3.getClass().getName() );
		recs3.display();
		
		
		
		
//		TJsonArray<Rec> ar = new TJsonArray();
//		ar.put( rec);
//		
//		String str = ar.toString();
//		
//		TJsonArray<Rec> ar2 = new JSONParser().parse3( str, Rec.class);
//		rec2.display();
//		
//		TJsonArray<Rec> ar = new TJsonArray();
//		
//		
	}

}
