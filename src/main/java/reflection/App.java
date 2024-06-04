package reflection;

public class App {
	TradingHours m_tradingHours;
	private Stocks m_stocks;

	public void config(Config config, TradingHours tradingHours) throws Exception {
		m_stocks = config.readStocks();
		m_tradingHours = tradingHours;
	}

	Stock getStock( int conid) throws RefException {
		return m_stocks.stockMap().get( conid);
	}


}
