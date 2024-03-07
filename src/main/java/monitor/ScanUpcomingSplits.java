package monitor;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;

import http.MyClient;
import reflection.Config;
import reflection.Stocks;

public class ScanUpcomingSplits {
	public static void main(String[] args) throws Exception {
		
		String body = MyClient.getString( "https://www.nasdaq.com/market-activity/stock-splits");
		
		ArrayList<String> list = new ArrayList<>(); 

		try (BufferedReader br = new BufferedReader( new StringReader( body) ) ) {
			String str;
			while ( (str=br.readLine()) != null) {
				str = str.trim();
				
				if (str.startsWith( "<td><a href=\"https://www.nasdaq.com") ) {
					str = str.substring(10);
					
					int start = str.indexOf( ">") + 1;
					int end = str.indexOf( "<");
					
					list.add( str.substring( start, end) );
				}
			}
		}
		
		Config config = Config.ask();
		Stocks stocks = config.readStocks();
		
	}
	
	
}