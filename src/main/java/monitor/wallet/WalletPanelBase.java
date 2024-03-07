package monitor.wallet;

import java.awt.BorderLayout;

import monitor.MonPanel;
import monitor.WalletPanel;
import tw.util.NewTabbedPanel;

public class WalletPanelBase extends MonPanel {
	private final NewTabbedPanel m_tabs = new NewTabbedPanel(true);
	private final WalletPanel walletPanel = new WalletPanel(this);
	private final BlockPanel blockPanel = new BlockPanel();
	
	public WalletPanelBase() {
		super( new BorderLayout() );
		m_tabs.addTab( "Main", walletPanel); 
		m_tabs.addTab( "Blockchain Transactions", blockPanel);
		
		add( m_tabs);
	}

	@Override protected void refresh() throws Exception {
		walletPanel.refresh();
		blockPanel.refresh( walletPanel.getWallet() );
	}

	/** Called by other panels */
	public void setWallet(String walletAddr) {
		walletPanel.setWallet( walletAddr);
	}
	

}
