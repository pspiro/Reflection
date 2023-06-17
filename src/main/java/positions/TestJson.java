package positions;

import org.json.simple.JsonObject;

import reflection.Util;
import tw.util.S;

public class TestJson {
	public static void main(String[] args) throws Exception {
		String json = Util.toJson( "{'a': 3, 'b': 'bob' }");
		
		JsonObject obj = JsonObject.parse(json);
		
		A a = new A(obj);
		
		S.out( a.a() );
		S.out( a.b() );
	}

	static class A {
		JsonObject obj;

		A(JsonObject o) {
			obj = o;
		}
		
		int a() { 
			return obj.getInt("a");
		}
		
		String b() { 
			return obj.getString("b"); 
		}
	}

//	record Bal(MyJsonObject obj) {
//		String symbol() { return obj.getString("symbol"); }
//		String balance() { return obj.getString("balance"); }
//		int decimals() { return obj.getInt("decimals"); }
//		String token_address() { return obj.getString("token_address"); }
//		
//		double decBalance() { 
//			return Erc20.fromBlockchain(balance(), decimals() );
//		}
//	}
//	
	
		
	
}
