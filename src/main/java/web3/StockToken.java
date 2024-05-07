package web3;

/** The stock token class that is used by clients */
public class StockToken extends Erc20 {  // change to MyCoreBase. pas
	public static final int stockTokenDecimals = 18;

	public StockToken(String address) throws Exception {
		super( address, stockTokenDecimals, "StockToken");
	}
}
