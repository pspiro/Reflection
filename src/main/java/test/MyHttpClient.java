package test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import tw.util.IStream;
import tw.util.S;

class MyHttpClient {
	private Socket m_socket;

	MyHttpClient( String host, int port) throws Exception {
		m_socket = new Socket( host, port);
	}
	
	void writeFile( String filename) throws Exception {
		IStream is = new IStream( filename);
		write( is.readAll() );
	}
	
	void write( String str) throws Exception {
		write( str.getBytes() );
	}
	
	void write( byte[] bytes) throws Exception {
		m_socket.getOutputStream().write( bytes);
	}

	HashMap<String,Object> readJsonMap() throws Exception {
		JSONObject jsonObject = readJson();

		HashMap<String,Object> map = new HashMap<String,Object>();
        for (Object key : jsonObject.keySet() ) {
        	map.put( (String)key, jsonObject.get(key) );
        }
        
        return map;
	}

	JSONObject readJson() throws Exception {
		return (JSONObject)new JSONParser().parse( readAll() );
	}
	
	String readAll() throws Exception {
		BufferedReader br = new BufferedReader( new InputStreamReader( m_socket.getInputStream() ) );

		// build map of headers until a blank line is read
		HashMap<String,String> map = new HashMap<String,String>(); 

		String str;
		while (S.isNotNull( (str=br.readLine()))) {
			String[] ar = str.split( ":");
			if (ar.length == 2) {
				map.put( ar[0].toLowerCase().trim(), ar[1].trim() );
			}
		}
		
		int len = Integer.valueOf( map.get( "content-length") );
		char[] ar = new char[len];
		br.read( ar, 0, len);
		return new String( ar);
	}
	
	void send( String data) throws Exception {
		String contLen = String.format( "Content-length: %s\r\n", data.length() );

		/*
		GET / HTTP/1.1
		 */
		StringBuilder sb = new StringBuilder();
		sb.append( "POST / HTTP/1.1\r\n");
		sb.append( contLen);
		sb.append( "\r\n");
		sb.append( data);
		
		write( sb.toString() );
	}
}
