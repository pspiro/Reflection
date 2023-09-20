//package logserver;
//
//import java.util.HashMap;
//import java.util.HashSet;
//
//import org.json.simple.JsonObject;
//import org.json.simple.ObjArray;
//
//import common.Util;
//import http.SimpleTransaction;
//import tw.util.IStream;
//import tw.util.S;
//
//public class MyLogServer {
//	HashMap<String,ObjArray> map = new HashMap<>();  // map a key to an array of Object (may be String or JsonObject (the data)
//	HashSet<String> m_filenames = new HashSet<>();
//	
//	public static void main(String[] args) {
////		if (args.length != 2) {
////			S.out("Usage: LogServer <folder> <tabName>");
////			return;
////		}
//
//		S.out( "Starting LogServer");
//		new MyLogServer().run(args);
//	}
//	
//	void run(String[] args) {
//		//Config config = new Config();
//		//config.readFromSpreadsheet(tab);
//
//		// don't allow two instances of the application, and give a way to 
//		// test if the application is running
//		SimpleTransaction.listen("0.0.0.0", 8585, trans -> process(trans) );
//	}
//
//	private void process(SimpleTransaction trans) {
//		try {
//			JsonObject json = trans.receiveJson();
//			read( json.getLowerString("filename") );
//			ObjArray ar = map.get( json.getLowerString("key") );
//			
//			JsonObject obj = new JsonObject();
//			obj.put( "entries", ar);
//			trans.respond(obj);
//		}
//		catch( Exception e) {
//			e.printStackTrace();
//			trans.respondJson( "error", e.getMessage());
//		}
//	}
//
//	synchronized void read(String filename) throws Exception {
//		if (!m_filenames.contains(filename) ) {
//			IStream is = new IStream(filename);
//			while ( is.hasNext() ) {
//				JsonObject obj = JsonObject.parse( is.readln() );
//				Object data = obj.get("data");
//				for (String key : obj.getString("keys").split(",") ) {
//					Util.getOrCreate( map, key, () -> new ObjArray() ).add(data);
//				}
//			}
//			m_filenames.add(filename);
//		}
//	}
//}
