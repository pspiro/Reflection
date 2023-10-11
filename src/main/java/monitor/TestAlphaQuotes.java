package monitor;
//
//import org.asynchttpclient.AsyncHttpClient;
//import org.asynchttpclient.DefaultAsyncHttpClient;
//import org.json.simple.JsonObject;
//
//import common.Util;
//import tw.util.S;
//
public class TestAlphaQuotes {
//	final static String AlphaKey = "EKD9A4ZUSQPEPFXK";  // alphavantage API key
//	final static String AlphaUrl = "https://www.alphavantage.co";
//	JsonObject stock = getRow(row);
//	String symbol = stock.getString("symbol").split(" ")[0];
//	
//	if (col == getColumnIndex("alpha last") ) {
//		String alphaQuery = String.format(
//				"https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
//				symbol, 
//				AlphaKey);
//
////		HttpRequest req = MyAsyncClient.getBuilder( alphaQuery)
////				.header( "accept", "application/json")
////				.header( "content-type", "application/json")
////				.build();
//		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
//		client
//			.prepare("GET", alphaQuery)
//			.setHeader("accept", "application/json")
//			.setHeader("content-type", "application/json")
//		  	.execute()
//		  	.toCompletableFuture()
//		  	.thenAccept( response -> {
//		  		try {
//		  			client.close();
//		  			
//		  			Util.require( response.getStatusCode() == 200, "Error status code %s - %s", 
//		  					response.getStatusCode(), response.getStatusText() );
//		  			
//		  			S.out( response.getResponseBody() );
//		  			JsonObject obj = JsonObject.parse(response.getResponseBody());
//		  			obj.display();
//		  			double last = obj.getObject("Global Quote").getDouble("05. price");
//		  			stock.put("alpha last", last);
//		  			fireTableRowsUpdated(row, row);
//		  		}
//		  		catch (Exception e) {
//		  			e.printStackTrace();
//		  		}
//		  	});
//	}
//}
//
}
