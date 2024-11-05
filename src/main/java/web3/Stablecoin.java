package web3;

import chain.Chain;

/** Base class for stablecoins used by clients */
public abstract class Stablecoin extends Erc20 {
	Stablecoin( String address, int decimals, String name, Chain chain) throws Exception {
		super( address, decimals, name, chain);
	}
	
	public final boolean isRusd() {
		return this instanceof Rusd;
	}
}
