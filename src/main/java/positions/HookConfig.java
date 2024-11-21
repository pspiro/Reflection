package positions;

import common.Util;
import reflection.Config.Tooltip;
import tw.google.GTable;
import tw.google.NewSheet;

public class HookConfig {
	public enum HookType { None, Moralis, Alchemy }

	private HookType hookType;
	private String alchemyChain;
	private String hookServerUrlBase; // webhook url passed to Moralis
	private boolean noStreams;
	private String chain;
	private String hookNameSuffix;
	private GTable table; 
	private int myWalletRefresh;  // "My Wallet" panel refresh interval
	private double minTokenPosition;
	
	protected void readFromSpreadsheet(String tabName) throws Exception {
		var tab = NewSheet.getTab(NewSheet.Reflection, tabName);
		table = new GTable( tab, "Tag", "Value", true);
		
		hookType = Util.getEnum( table.getRequiredString( "hookType"), HookType.values(), HookType.None);
		hookServerUrlBase = table.getRequiredString("hookServerUrlBase");
		noStreams = table.getBoolean( "noStreams");
		chain = table.getRequiredString( "chain");
		hookNameSuffix = table.getRequiredString("hookNameSuffix");
		myWalletRefresh = table.getRequiredInt("myWalletRefresh");
		minTokenPosition = table.getRequiredDouble( "minTokenPosition");

		Util.require(
				hookServerUrlBase.equals( "ngrok") || 
				hookServerUrlBase.startsWith( "https://"), "hookServerUrlBase",
				"invalid url base");
		
		if (hookType == HookType.Alchemy) {
			alchemyChain = table.getRequiredString( "alchemyChain");  // not sure where to pull this
		}		
	}		

	String chain() {
		return chain;
	}

	public String getTooltip(Tooltip tag) {
		return table.get(tag.toString());
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

	public String getHookNameSuffix() {
		return hookNameSuffix;
	}

	public double minTokenPosition() {
		return minTokenPosition;
	}

	public int myWalletRefresh() { 
		return myWalletRefresh; 
	}
}
