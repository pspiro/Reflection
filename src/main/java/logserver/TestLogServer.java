package logserver;

import common.Util;
import http.MyHttpClient;

public class TestLogServer {
	public static void main(String[] args) throws Exception {
		String fname = "c:/temp/file.t";
		
		MyHttpClient cli; 
		String data;
		
		cli = new MyHttpClient("localhost", 8585);
		data = Util.toJson("filename", fname,	"key", "open").toString();
		cli.postToJson( "/", data).display();

		cli = new MyHttpClient("localhost", 8585);
		data = Util.toJson("filename", fname,	"key", "close").toString();
		cli.postToJson( "/", data).display();

		cli = new MyHttpClient("localhost", 8585);
		data = Util.toJson("filename", fname,	"key", "kdkdk").toString();
		cli.postToJson( "/", data).display();
//
//		data = Util.toJsonMsg("filename", fname,	"key", "jfjfj").toString();
//		cli.postToJson( "/", data).display();
	}
}
