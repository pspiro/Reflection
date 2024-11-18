package positions;

import chain.Chain;
import common.Util;
import positions.HookConfig.HookType;
import reflection.Config.Tooltip;
import tw.google.GTable;
import tw.google.NewSheet;

/** obsolete, remove. pas */
public class HookConfig2 {
	private GTable table; 
	private Chain m_chain;
	
	protected void readFromSpreadsheet(String tabName) throws Exception {
		var tab = NewSheet.getTab(NewSheet.Reflection, tabName);
		table = new GTable( tab, "Tag", "Value", true);
	}		

	public String getTooltip(Tooltip tag) {
		return ""; // fix this. pastable.get(tag.toString());
	}

	public HookType hookType() {
		return Util.getEnum( m_chain.params().hookType(), HookType.values() );
	}

	public String hookServerUrlBase() {
		return m_chain.params().hookServerUrlBase();
	}

	public boolean noStreams() {
		return m_chain.params().noStreams();
	}
	
	public String alchemyChain() {
		return m_chain.params().alchemyChain();
	}

	public String getHookNameSuffix() {
		return m_chain.params().hookNameSuffix();
	}

	public double minTokenPosition() {
		return m_chain.params().minTokenPosition();
	}

	public int myWalletRefresh() { 
		return m_chain.params().myWalletRefresh(); 
	}

	public void chain(Chain chain) {
		m_chain = chain;
	}
}
