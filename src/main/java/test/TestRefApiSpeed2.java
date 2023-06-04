package test;

import http.MyHttpClient;
import tw.util.S;

/** For testing speed of Backend API vs RefAPI */

public class TestRefApiSpeed2 {
	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		
		int n = 10;
		
		for (int a = 0; a < n; a++) {
			reqBackend();
		}
		long end = System.currentTimeMillis();
		S.out( (end - start) + " ms");
	
	
		start = System.currentTimeMillis();
		for (int a = 0; a < n; a++) {
			reqRefAPI();
		}
		end = System.currentTimeMillis();
		S.out( (end - start) + " ms");
	}

	private static void reqBackend() throws Exception {
		MyHttpClient client = new MyHttpClient("34.125.38.193", 8383);
		client.get( "api/get-stock-with-price/8314");
		S.out( client.readString() );
		
	}
	
	/** THIS EXECUTES IN HALF THE TIME!!! */
	private static void reqRefAPI() throws Exception {
		MyHttpClient client = new MyHttpClient("34.125.38.193", 8383);
		client.get( "api/get-all-stocks");
		S.out( client.readString() );
		
	}
	
	// backend: 2247
	// Refapi:  1906
}

// problem: get-stock-with-price/8314 is significantly slower than get-all-stocks
// which makes no sense

// RefAPI getStock() is never called by Backend; get-stock calls RefAPI.getAllStocks()



