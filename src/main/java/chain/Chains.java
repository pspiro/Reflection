package chain;

import java.util.HashMap;

import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;

public class Chains extends HashMap<Integer,Chain> {

	public void readAll() throws Exception {
		Tab tab = NewSheet.getTab( NewSheet.Reflection, "Blockchain");
		read( tab, "Polygon");
		read( tab, "PulseChain");
		read( tab, "Sepolia");
		read( tab, "zkSync");
	}

	private void read(Tab tab, String name) throws Exception {
		var json = new GTable( tab, "Tag", name, true).toJson();
		var params = json.toRecord( ChainParams.class);
		var chain = new Chain( params);
		put( params.chainId(), chain);
    }
	
	public Chain chain( int chainId) {
		return get( chainId);
	}
	
	public ChainParams params( int chainId) {
		return get( chainId).params();
	}
	
	public static void main(String[] args) throws Exception {
		Chains chains = new Chains();
		chains.readAll();
		S.out( chains);
	}
	
	/** Used for requesting market data */
	public Chain polygon() {
		return chain( 137);
	}

	public Chain pulseChain() {
		return chain( 324);
	}

	public void checkKeys() throws Exception {
		for (var chain : values() ) {
			S.out( "  checking key for " + chain.params().name() );
			chain.params().admin1Key();
		}
	}
}
