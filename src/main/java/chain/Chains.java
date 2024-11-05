package chain;

import java.util.HashMap;

import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;

/** maps chainId to Chain */
public class Chains extends HashMap<Integer,Chain> {
	public static final int Polygon = 137;
	public static final int ZkSync = 324;
	public static final int Sepolia = 11155111;
	public static final int PulseChain = 369;

	public Chain chain( int chainId) {
		return get( chainId);
	}

	public ChainParams params( int chainId) {
		return get( chainId).params();
	}

	/** Used for requesting market data */
	public Chain polygon() {
		return chain( Polygon);
	}

	public Chain pulseChain() {
		return chain( PulseChain);
	}

	public void checkKeys() throws Exception {
		for (var chain : values() ) {
			S.out( "  checking key for " + chain.params().name() );
			chain.params().admin1Key();
		}
	}

	void dump() throws Exception {
		for (var chain : values() ) {
			chain.dump();
		}
	}

	public void readAll() throws Exception {
		Tab bcTab = NewSheet.getTab( NewSheet.Reflection, "Blockchain");
		readChain( bcTab, "Polygon");
		readChain( bcTab, "PulseChain");
		readChain( bcTab, "Sepolia");
		readChain( bcTab, "zkSync");

		// pre-fill decimals map to avoid unnecessary queries
		// really only HookServer needs this because the other apps know how
		// many decimals there are
		//NodeInstance.setDecimals( 18, getAllContractsAddresses() );
	}

	/** Read one column (one chain) from the Blockchain tab, AND
	 *  read in all the symbols for that chain as well */
	private void readChain(Tab bcTab, String chainName) throws Exception {
		// read the params from one column on the Blockchain tab
		var params = new GTable( bcTab, "Tag", chainName, true)
				.toJson()
				.toRecord( ChainParams.class);
		
		// create the Chain and read in the symbols
		var chain = new Chain( params);
		chain.readSymbols();

		// add this new chain to the map
		put( params.chainId(), chain);

	}
}
