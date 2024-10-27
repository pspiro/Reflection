package web3;

import chain.Chain;

public class Busd extends Stablecoin {
	public Busd(String address, int decimals, String name, Chain chain) throws Exception {
		super( address, decimals, name, chain);
	}
}
