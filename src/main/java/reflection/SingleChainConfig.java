package reflection;

import chain.Chain;
import chain.ChainParams;
import chain.Chains;
import tw.google.NewSheet.Book.Tab;
import web3.Busd;
import web3.NodeInstance;
import web3.RetVal;
import web3.Rusd;

/** Used by Monitor and test scripts. This should be removed; the chain is not tied to the config. */
public class SingleChainConfig extends Config {
	private Chain m_chain;

	protected void readFromSpreadsheet(Tab tab) throws Exception {
		super.readFromSpreadsheet(tab);
		
		// let chain equal first chain in list of chains specified on tab
		String[] names = m_tab.getRequiredString( "chains").split( ",");
		m_chain = Chains.readOne( names[0], true);
	}

	public boolean isProduction() {
		return chain().params().isProduction();
	}
	
	public Chain chain() {
		return m_chain;
	}

	public int chainId() {
		return m_chain.chainId();
	}

	public Rusd rusd() {
		return chain().rusd();
	}

	public Busd busd() {
		return chain().busd();
	}

	/** for testing and Monitor only */ 
	public String rusdAddr() {
		return chain().rusd().address(); 
	}

	/** for testing and Monitor only */
	public String busdAddr() { 
		return chain().busd().address(); 
	}

	public String[] getStablecoinAddresses() throws Exception {
		return chain().getStablecoinAddresses();
	}

	public NodeInstance node() throws Exception {
		return chain().node();
	}

	public ChainParams params() {
		return chain().params();
	}

	public String refWalletAddr() {
		return params().refWalletAddr();
	}

	/** returns private key or account name */
	public String refWalletKey() throws Exception {
		return params().refWalletKey();
	}

	/** returns private key or account name */
	public String ownerKey() throws Exception {  // private key or "Owner"
		return params().ownerKey();
	}

	/** returns private key or account name */
	public String admin1Key() throws Exception {
		return params().admin1Key();
	}

	public String ownerAddr() {
		return params().ownerAddr();
	}

	/** for testing only */
	public RetVal mintBusd(String wallet, double amt) throws Exception {
		return busd().mint( ownerKey(), wallet, amt);
	}

	public String nativeTokName() {
		return chain().params().platformBase();
	}
}
