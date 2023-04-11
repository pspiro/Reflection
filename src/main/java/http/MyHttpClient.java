package http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import json.MyJsonArray;
import json.MyJsonObject;
import reflection.Util;
import tw.util.IStream;
import tw.util.S;

/** Good for testing, don't use this in production, there are many things not handled.
 *  Use AsyncHttpClient. */
public class MyHttpClient {
	private Socket m_socket;
	private ArrayList<String> m_reqHeaders = new ArrayList<String>();
	private HashMap<String,String> m_respHeaders = new HashMap<>();
	private int m_responseCode;
	
	public static void main(String[] args) throws Exception {
		MyHttpClient cli = new MyHttpClient( "34.125.124.211", 5001);
		cli.get();
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

	/** This is redundant and doesn't add anything except the types.
	 *  @deprecated use readMyJsonObj */
	public HashMap<String,Object> readJsonMap() throws Exception {
		JSONObject jsonObject = readJsonObject();

		HashMap<String,Object> map = new HashMap<String,Object>();
        for (Object key : jsonObject.keySet() ) {
        	map.put( (String)key, jsonObject.get(key) );
        }
        
        return map;
	}

	public MyJsonObject readMyJsonObject() throws Exception {
		return MyJsonObject.parse( readString() );
	}
	
	public MyJsonArray readMyJsonArray() throws Exception {
		return MyJsonArray.parse( readString() );
	}
	
	public JSONObject readJsonObject() throws Exception {
		return (JSONObject)new JSONParser().parse( readString() );
	}
	
	public String readString() throws Exception {
		BufferedReader br = new BufferedReader( new InputStreamReader( m_socket.getInputStream() ) );

		String first = br.readLine();
		String[] main = first.split(" ");
		Util.require( main.length >= 2, "Wrong format for first line of http response: " + first);
		m_responseCode = Integer.parseInt(main[1]);

		// build map of headers until a blank line is read
		String str;
		while (S.isNotNull( (str=br.readLine()))) {
			String[] ar = str.split( ":");
			if (ar.length == 2) {
				m_respHeaders.put( ar[0].toLowerCase().trim(), ar[1].trim() );
			}
		}

		// content-length found?
		String lenStr = m_respHeaders.get( "content-length");
		if (S.isNotNull( lenStr) ) {
			int len = Integer.valueOf( lenStr); 
			char[] ar = new char[len];
			br.read( ar, 0, len);
			return new String( ar);
		}

		// this is technically wrong; chunked code has length
		// parameters in it, but this will work if there are
		// no empty lines
		if ("chunked".equals( m_respHeaders.get( "transfer-encoding") ) ) {
			StringBuilder sb= new StringBuilder();
			while (S.isNotNull( (str=br.readLine()))) {
				sb.append( str);
				sb.append( "\r\n");
			}
			return sb.toString();
		}
		
		throw new Exception("illprepared");

		// this might work
		// no content-length found; read until end of stream
//		StringBuilder sb= new StringBuilder();
//		char[] ar = new char[1024];
//		while (br.read(ar) != -1) {
//			sb.append( ar);
//		}
//		return new String( ar);
	}
	
	public void post( String data) throws Exception {
		post( "/", data);
	}

	/** e.g. post2( "/api", */
	public void post( String url, String data) throws Exception {
		String contLen = String.format( "Content-length: %s\r\n", data.length() );

		StringBuilder sb = new StringBuilder();
		sb.append( "POST " + url + " HTTP/1.1\r\n");
		sb.append( contLen);
		sb.append( "\r\n");
		sb.append( data);
		
		write( sb.toString() );
	}

	public void get() throws Exception {
		get( "");
	}
	
	/** @param data may or not start with / 
	 * @return */
	public MyHttpClient get( String data) throws Exception {
		if (!data.startsWith("/") ) {
			data = "/" + data;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append( "GET " + data + " HTTP/1.1\r\n");
		for (String header : m_reqHeaders) {
			sb.append( header + "\r\n");
		}
		sb.append( "\r\n");

		write( sb.toString() );
		return this;
	}

	public void addHeader(String val) {
		m_reqHeaders.add( val);
	}

	public MyHttpClient addHeader(String key, String val) {
		m_reqHeaders.add( String.format( "%s: %s", key, val) );
		return this;
	}

	public void dumpHeaders() {
		// TODO Auto-generated method stub
		
	}

	public HashMap<String,String> getHeaders() {
		return m_respHeaders;
	}

	public int getResponseCode() {
		return m_responseCode;
	}
}
