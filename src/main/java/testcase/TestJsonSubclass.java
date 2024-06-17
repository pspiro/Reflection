package testcase;

import org.json.simple.JsonObject;
import org.json.simple.parser.JSONParser;

public class TestJsonSubclass {
	static class UserRec extends JsonObject {
	}
	

	/** this shows how to read back a json object that returns a JsonObject subclass */
	public static void main(String[] args) throws Exception {
		UserRec rec = new UserRec();
		rec.put( "name", "peter");
		rec.put( "age", 55);
		
		String str = rec.toString();
		
		UserRec rec2 = new JSONParser().parse2( str, () -> new UserRec() );
		rec2.display();
		
	}

}
