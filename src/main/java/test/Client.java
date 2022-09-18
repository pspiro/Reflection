package test;

import java.io.OutputStream;
import java.net.Socket;

import tw.util.IStream;
import tw.util.S;

public class Client {
	public static void main(String[] args) {
		try {
			new Client().run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void run() throws Exception {
		String str = "POST / HTTP/1.1\r\n"
			       + "Content-Length: 38\r\n"  // can be too high, but not too low
			       + "\r\n"
			       + "{ 'msg': 'getprice', 'conid': '8314' }";
		
		String str2 = new IStream( "c:\\temp\\file2.json").readAll(); 
		
		boolean v = str.equals(str2);

		Socket socket = new Socket("localhost", 8001);
		OutputStream os = socket.getOutputStream();
		os.write( str2.getBytes() );
		os.flush();
		S.sleep(5000);
		os.close();
		
		
	}
}


/* file contents
POST / HTTP/1.1
Content-Length: 100

{ "msg": "getprice", "conid": "8315" }
*/