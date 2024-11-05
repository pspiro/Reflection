package web3;

import chain.Allow;
import chain.Chain;

/** The stock token class that is used by clients */
public class StockToken extends Erc20 {
	public static final int stockTokenDecimals = 18;

	/** the fields read in from the symbols tab on spreadsheet */
	public static record StockTokenRec (
			int conid,
			String startDate, 
			String endDate, 
			double convertsToAmt,
			String convertsToAddress,
			Allow allow
			) {

	}

	private StockTokenRec m_params;

	public StockToken(String address, Chain chain) {
		super( address, stockTokenDecimals, "StockToken", chain	);
	}

	/** @param name is the .r name 
	 * @throws Exception */
	public StockToken(String address, String name, Chain chain, StockTokenRec params) throws Exception {
		super( address, stockTokenDecimals, name, chain);
		m_params = params;
	}

	public StockTokenRec rec() {
		return m_params;
	}

	public int conid() {
		return m_params.conid();
	}
}
