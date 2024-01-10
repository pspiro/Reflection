package monitor;

import java.awt.BorderLayout;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import common.Util;
import fireblocks.Accounts;
import fireblocks.Fireblocks;
import http.MyClient;
import monitor.Monitor.MonPanel;
import positions.Wallet;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.VerticalPanel;

/** Not a json panel */
public class CryptoPanel extends MonPanel {
	private JTextField m_rusdOutstanding = new JTextField(10);
	private JTextField m_refWalletBusd = new JTextField(10);
	private JTextField m_ownerBusd = new JTextField(10);
	private JTextField m_refWalletMatic = new JTextField(10);
	private JTextField m_admin1Matic = new JTextField(10);
	private JTextField m_admin2Matic = new JTextField(10);
	private JTextField m_ownerMatic = new JTextField(10);
	private JTextField m_cash = new JTextField(10);
	HoldersPanel holdersPanel = new HoldersPanel();

	CryptoPanel() {
		super( new BorderLayout() );
		
		HtmlButton button = new HtmlButton( "Show Wallets", ev -> {
			Util.wrap( () -> holdersPanel.refresh( Monitor.m_config.rusd() ) );
		});

		HtmlButton emptyRefWallet = new HtmlButton( "Send to owner", ev -> emptyRefWallet() );

		VerticalPanel rusdPanel = new VerticalPanel();
		rusdPanel.setBorder( new TitledBorder("RUSD Analysis"));
		rusdPanel.add( "RUSD Outstanding", m_rusdOutstanding, button);
		
		rusdPanel.add( "RefWallet USDT", m_refWalletBusd, emptyRefWallet);
		rusdPanel.add( "RefWallet MATIC", m_refWalletMatic);
		
		rusdPanel.add( "Owner USDT", m_ownerBusd);
		rusdPanel.add( "Owner MATIC", m_ownerMatic);
		
		rusdPanel.add( "Admin1 MATIC", m_admin1Matic);
		rusdPanel.add( "Admin2 MATIC", m_admin2Matic);

		rusdPanel.add( "Cash in brokerage", m_cash);
		
		add(rusdPanel);
		add(holdersPanel, BorderLayout.EAST);
	}
	
	private void emptyRefWallet() {
		if (Util.confirm(this, "Are you sure you want to transfer all USDT from RefWallet to Owner?") ) {
			Util.wrap( () -> {
				int from = Accounts.instance.getId("RefWallet");
				String to = Accounts.instance.getAddress("Owner");
				String note = "empty the RefWallet of stablecoin";;
				double amt = Double.parseDouble( m_refWalletBusd.getText() ) - 1; // leave $1 for good luck
				
				Fireblocks.transfer(from, to, "USDT_POLYGON", amt, note).waitForHash();
			});
		}		
	}

	@Override public void activated() {
		Util.wrap( () -> refresh() );
	}
	
	@Override public void refresh() throws Exception {
		S.out( "Refreshing Crypto panel");
		Wallet refWallet = Fireblocks.getWallet("RefWallet");

		double busd = refWallet.getBalance(Monitor.m_config.busdAddr());
		SwingUtilities.invokeLater( () -> m_refWalletBusd.setText( S.fmt2(busd) ) );

		double nativeBal = refWallet.getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_refWalletMatic.setText( S.fmt2(nativeBal) ) );

		Wallet owner = Fireblocks.getWallet("Owner");
		double ownerBal = owner.getNativeTokenBalance();
		double busdBal = owner.getBalance(Monitor.m_config.busdAddr());
		SwingUtilities.invokeLater( () -> {
			m_ownerMatic.setText( S.fmt2(ownerBal) );
			m_ownerBusd.setText( S.fmt2(busdBal) );
		});

		double admin1Bal = Fireblocks.getWallet("Admin1").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin1Matic.setText( S.fmt2(admin1Bal) ) );

		double admin2Bal = Fireblocks.getWallet("Admin2").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin2Matic.setText( S.fmt2(admin2Bal) ) );

		double rusd = Monitor.m_config.rusd().queryTotalSupply();
		SwingUtilities.invokeLater( () -> m_rusdOutstanding.setText( S.fmt2(rusd) ) );
		
		MyClient.getJson( Monitor.refApiBaseUrl() + "/api/?msg=getCashBal", obj -> {
			double val = obj.getDouble("TotalCashValue");
			SwingUtilities.invokeLater( () -> m_cash.setText( S.fmt2(val) ) );
		});
	}
}
