package monitor.wallet;

import java.awt.BorderLayout;

import monitor.BigWalletPanel;
import monitor.MonPanel;
import tw.util.NewTabbedPanel;

public class WalletPanel extends MonPanel {
	private final NewTabbedPanel m_tabs = new NewTabbedPanel(true);
	private final BigWalletPanel m_bigPanel = new BigWalletPanel(this);
	private final BlockDetailPanel m_blockPanel = new BlockDetailPanel();
	
	public WalletPanel() {
		super( new BorderLayout() );
		m_tabs.addTab( "Main", m_bigPanel); 
		m_tabs.addTab( "Blockchain Transactions", m_blockPanel);
		
		add( m_tabs);
	}

	@Override protected void refresh() throws Exception {
		m_blockPanel.refresh( m_bigPanel.getWallet() ); // must come first
		m_bigPanel.refresh( m_blockPanel.rows() );
	}

	/** Called by other panels */
	public void setWallet(String walletAddr) {
		m_bigPanel.setWallet( walletAddr);
	}
	

}
