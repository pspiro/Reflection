package test;

import static java.lang.String.format;

import org.json.simple.JsonObject;

import http.MyClient;
import tw.util.S;

public class TestInfura {
	static String api = "bba0c8b8f46b4c6a8f740a74c6c4ad77";
	
	public static void main(String[] args) throws Throwable {
		String url = format( "https://mainnet.infura.io/v3/%s", api);
	    String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\": [{\"from\": \"0xb60e8dd61c5d32be8058bb8eb970870f07233155\",\"to\": \"0xd46e8dd67c5d32be8058bb8eb970870f07244567\",\"gas\": \"0x76c0\",\"gasPrice\": \"0x9184e72a000\",\"value\": \"0x9184e72a\",\"data\": \"0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675\"}, \"latest\"],\"id\":2}";
	    
	    JsonObject obj = JsonObject.parse(data);
	    obj.display();
	    String ret = querySync( url, data);
	    S.out( "");
	    S.out( ret);
	}		
	
	static String querySync(String url, String data) throws Throwable {
		return MyClient.create(url, data)
				.header("accept", "application/json")
				.header("X-API-Key", "test")
				.query().body();
	}
}
