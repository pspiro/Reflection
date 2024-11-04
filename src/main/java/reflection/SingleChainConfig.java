package reflection;

import chain.Chain;
import chain.ChainParams;
import chain.Chains;
import common.Util;
import tw.google.NewSheet.Book.Tab;
import web3.Busd;
import web3.NodeInstance;
import web3.RetVal;
import web3.Rusd;

public class SingleChainConfig extends Config {
		private int m_chainId;
		private Chain m_chain;
		/** for display to user */
//		private String blockchainName;  // for messages
//		this.blockchainName = m_tab.get( "blockchainName");
//		require( S.isNotNull( blockchainName) || !sendTelegram, "blockchainName");
//		public String blockchainName() {
//			return blockchainName;
//		}

		protected void readFromSpreadsheet(Tab tab) throws Exception {
			super.readFromSpreadsheet(tab);

			m_chainId = m_tab.getRequiredInt( "chainId");

			Chains chains = new Chains();
			chains.readAll();
			
			m_chain = chains.get( m_chainId);
		}

		public Chain chain() {
			return m_chain;
		}
		
		public int chainId() {
			return m_chainId;
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
			return new String[] { rusdAddr(), busdAddr() };
		}
		
		public NodeInstance node() throws Exception {
			return chain().node();
		}

		private ChainParams params() {
			return chain().params();
		}
		
		public String admin1Addr() {
			return params().admin1Addr();
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

		public boolean isProduction() {  // probably need a separate setting for this
			return Util.equals(m_chainId, 137, 369, 324);  
		}
	}
