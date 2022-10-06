package http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import tw.util.IStream;
import tw.util.S;

public class MyHttpClient {
	private Socket m_socket;
	
	public static void main(String[] args) throws Exception {
		MyHttpClient cli = new MyHttpClient( "192.168.1.11", 80);
		cli.get( "hello");
		S.out( cli.readString() );
	}

	public MyHttpClient( String host, int port) throws Exception {
		m_socket = new Socket( host, port);
	}
	
	public void writeFile( String filename) throws Exception {
		IStream is = new IStream( filename);
		write( is.readAll() );
	}
	
	public void write( String str) throws Exception {
		write( str.getBytes() );
	}
	
	public void write( byte[] bytes) throws Exception {
		m_socket.getOutputStream().write( bytes);
	}

	public HashMap<String,Object> readJsonMap() throws Exception {
		JSONObject jsonObject = readJsonObject();

		HashMap<String,Object> map = new HashMap<String,Object>();
        for (Object key : jsonObject.keySet() ) {
        	map.put( (String)key, jsonObject.get(key) );
        }
        
        return map;
	}

	public JSONObject readJsonObject() throws Exception {
		return (JSONObject)new JSONParser().parse( readString() );
	}
	
	public String readString() throws Exception {
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
	
	public void post( String data) throws Exception {
		String contLen = String.format( "Content-length: %s\r\n", data.length() );

		StringBuilder sb = new StringBuilder();
		sb.append( "POST / HTTP/1.1\r\n");
		sb.append( contLen);
		sb.append( "\r\n");
		sb.append( data);
		
		write( sb.toString() );
	}

	// this doesn't work. pas needs ?
	public void get( String data) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append( "GET /" + data + " HTTP/1.1\r\n");
		sb.append( "\r\n");

		write( sb.toString() );
	}
}
