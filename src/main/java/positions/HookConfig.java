package positions;

import common.Util;
import reflection.SingleChainConfig;
import tw.google.NewSheet.Book.Tab;

public class HookConfig extends SingleChainConfig {
	public enum HookType { None, Moralis, Alchemy }

	private HookType hookType;
	private String alchemyChain;
	private String hookServerUrlBase; // webhook url passed to Moralis
	private boolean noStreams;
	private int hookServerPort;

	protected void readFromSpreadsheet(Tab tab) throws Exception {
		super.readFromSpreadsheet(tab);
		
		this.hookType = Util.getEnum( m_tab.getRequiredString( "hookType"), HookType.values(), HookType.None);
		
		this.hookServerUrlBase = m_tab.getRequiredString("hookServerUrlBase");
		require( 
				hookServerUrlBase.equals( "ngrok") || 
				hookServerUrlBase.startsWith( "https://"), "hookServerUrlBase");

		this.noStreams = m_tab.getBoolean( "noStreams");
		
		if (hookType == HookType.Alchemy) {
			this.alchemyChain = m_tab.getRequiredString( "alchemyChain");
		}
		
		this.hookServerPort = m_tab.getRequiredInt( "hookServerPort");
	}		

	public HookType hookType() {
		return hookType;
	}

	public String hookServerUrlBase() {
		return hookServerUrlBase;
	}

	public boolean noStreams() {
		return noStreams;
	}
	
	public String alchemyChain() {
		return alchemyChain;
	}

	public int hookServerPort() {
		return hookServerPort;
	}
}
