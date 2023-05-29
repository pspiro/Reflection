//package http;
//
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.Reader;
//import java.net.InetSocketAddress;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.concurrent.Executors;
//
//import com.sun.net.httpserver.Headers;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpServer;
//
//import fireblocks.Encrypt;
//import reflection.Util;
//import tw.util.S;
//import util.LogType;
//
//public class MyHttpServer {
//	static String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
//	
//	public static void main(String[] args) throws IOException {
//		HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8484), 0);
//		S.out( server.getClass() );
//		server.createContext("/", exch -> {
//			try {
//				handle(exch);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
//		server.setExecutor( Executors.newFixedThreadPool(20) );  // multiple threads but we are synchronized for single execution
//		server.start();		
//	}
//
//	private static void handle(HttpExchange exch) throws Exception {
//		S.out( exch.getRequestURI().toString().toLowerCase() );
//		
//		Headers headers = exch.getRequestHeaders();
//		show(headers);
//		
//		S.out( "Connection: " + headers.get("Connection") );
//		S.out( "Upgrade: " + headers.get("Upgrade") );
//		S.out( "Sec-websocket-key: " + headers.get("Sec-websocket-key") );
//
//		List<String> list = headers.get( "Sec-WebSocket-Key");
//		if (list != null && list.size() > 0) {
//			String key = headers.get("Sec-websocket-key").get(0);
//			String val = key + magic;
//			String sha = Encrypt.getSHA1(val);
//			S.out( "\n%s\n%s\n%s", key, val, sha);
//			
//			
//			exch.getResponseHeaders().add("Connection", "upgrade");
//			exch.getResponseHeaders().add("Upgrade", "websocket");
//			exch.getResponseHeaders().add("Sec-WebSocket-Accept", sha);
//
//			//exch.getResponseHeaders().add( "Content-Type", "application/json");
//
//				// add custom headers, if any  (add URL encoding here?)
//				
//			exch.sendResponseHeaders( 101, 10000);
//			
//			while (true) {
//				byte[] ar = new byte[1024];
//	            int read = exch.getRequestBody().read(ar);
//	            if (read > 0) {
//		            String str = new String(ar, read);
//		            S.out("Received: " + str);
//	            }
//	            S.sleep(100);
//			}
//			
//		}
//	}
//
//	private static void show(Headers headers) {
//		for (Entry<String, List<String>> entry : headers.entrySet() ) {
//			S.out( "%s: %s", entry.getKey(), entry.getValue() );
//		}
//		
//	}
//	
//	static void respond(HttpExchange exch, String data, HashMap<String,String> headers) {
//		try (OutputStream outputStream = exch.getResponseBody() ) {
//			exch.getResponseHeaders().add( "Content-Type", "application/json");
//
//			// add custom headers, if any  (add URL encoding here?)
//			if (headers != null) {
//				for (Entry<String, String> header : headers.entrySet() ) {
//					exch.getResponseHeaders().add( header.getKey(), header.getValue() );
//				}
//			}
//			
//			
//			exch.sendResponseHeaders( 101, 0);
//			//outputStream.write(data.getBytes());
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
//}
//
//
//
//
//
//
////
////
////
////
////
