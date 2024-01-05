package coinstore;

class CsConfig {
	int interval = 3000;
	String mdServerBaseUrl;
	int ordersPerSide = 5;
	int startingSize = 2;
	int sizeIncrement = 2;
	double pctOffset = .001;
	double pctOffsetWhenClosed = .01;
	double pctPriceIncrement = .001; 
	int conid = 265598;
	String host = "http://localhost:6989";
	
	String mdServerBaseUrl() {
		return mdServerBaseUrl;
	}

	int interval() {
		return interval;
	}

	public int port() {
		// TODO Auto-generated method stub
		return 0;
	}
}
