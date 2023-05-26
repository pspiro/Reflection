package http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import reflection.Util;
import tw.util.S;
import util.LogType;

public class MyHttpServer {
	static String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	
	public static void main(String[] args) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8484), 0);
		server.createContext("/", exch -> handle(exch) );
		server.setExecutor( Executors.newFixedThreadPool(20) );  // multiple threads but we are synchronized for single execution
		server.start();		
	}

	private static void handle(HttpExchange exch) {
		S.out( exch.getRequestURI().toString().toLowerCase() );
		
		Headers headers = exch.getRequestHeaders();
		show(headers);
		
		List<String> list = headers.get("Connection");
		S.out( "Connection: " + list);

		list = headers.get( "Sec-WebSocket-Key");
		if (list != null && list.size() > 0) {
			String key = list.get(0);
			String val = key + magic;
			String base64 = Base64.getEncoder().encodeToString(val.getBytes());
			S.out( "\n%s \n%s \n%s", key, val, base64);
			exch.getResponseHeaders().add("Sec-WebSocket-Accept", base64);
		}
		
		respond(exch, "OK");
	}

	private static void show(Headers headers) {
		for (Entry<String, List<String>> entry : headers.entrySet() ) {
			S.out( "%s: %s", entry.getKey(), entry.getValue() );
		}
		
	}
	
	static void respond(HttpExchange exch, String data) {
		try (OutputStream outputStream = exch.getResponseBody() ) {
			exch.getResponseHeaders().add( "Content-Type", "application/json");

			// add custom headers, if any  (add URL encoding here?)
//			if (headers != null) {
//				for (Entry<String, String> header : headers.entrySet() ) {
//					exch.getResponseHeaders().add( header.getKey(), header.getValue() );
//				}
//			}
			
			
			exch.sendResponseHeaders( 200, data.length() );
			outputStream.write(data.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, "Exception while responding with json");
		}
		
	}
}






//
//
//
//
//
//const WebSocket = require('ws');
//
//const socket = new WebSocket('ws://localhost:8484/');
//
//socket.on('open', () => {
//  console.log('WebSocket connection opened');
//});
//
//socket.on('message', (data) => {
//  console.log('Received message:', data);
//});
//
//socket.on('close', (code, reason) => {
//  console.log('WebSocket connection closed:', code, reason);
//});
//
//socket.on('error', (error) => {
//  console.error('WebSocket error:', error);
//});
//
//socket.emit("hello", "there");