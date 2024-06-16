package web3;

/** Base class for stablecoins used by clients */
public abstract class Stablecoin extends Erc20 {
	Stablecoin( String address, int decimals, String name) throws Exception {
		super( address, decimals, name);
	}
	
	public final boolean isRusd() {
		return this instanceof Rusd;
	}
}
