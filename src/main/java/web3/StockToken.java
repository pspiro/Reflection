package web3;

import chain.Chain;

/** The stock token class that is used by clients */
public class StockToken extends Erc20 {
	public static final int stockTokenDecimals = 18;

	public StockToken(String address, Chain chain) {
		super( address, stockTokenDecimals, "StockToken", chain	);
	}
}

