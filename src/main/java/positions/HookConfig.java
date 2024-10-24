package positions;

import common.Util;
import positions.HookServer.MoralisStreamMgr;
import positions.HookServer.StreamMgr;
import reflection.Config;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;

public class HookConfig extends Config {
	public enum HookType { None, Moralis, Alchemy }

	private HookType hookType;
	private String alchemyChain;
	private String hookServerUrlBase; // webhook url passed to Moralis
	private boolean noStreams;

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
}
