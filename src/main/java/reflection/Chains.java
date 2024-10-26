package reflection;

import java.util.HashMap;

import reflection.Chain.ChainWrapper;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;
import web3.NodeInstance;

public class Chains extends HashMap<Integer,ChainWrapper> {

	void readAll() throws Exception {
		Tab tab = NewSheet.getTab( NewSheet.Reflection, "Blockchain");
		read( tab, "Polygon");
		read( tab, "PulseChain");
		read( tab, "zkSync");
	}

	private void read(Tab tab, String name) throws Exception {
		var json = new GTable( tab, "Tag", name, true).toJson();
		var chain = json.toRecord( Chain.class);
		var wrapper = new ChainWrapper( chain);
		put( chain.chainId(), wrapper);
    }
	
	public Chain chain( int chainId) {
		return get( chainId).chain();
	}
	
	public NodeInstance node( int chainId) {
		return get( chainId).node();
	}
	
	
	public static void main(String[] args) throws Exception {
		Chains chains = new Chains();
		chains.readAll();
		S.out( chains);
	}
}
