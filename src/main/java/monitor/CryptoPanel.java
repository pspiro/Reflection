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
import tw.util.VerticalPanel.Header;

/** Not a json panel */
public class CryptoPanel extends MonPanel {
	private JTextField m_rusdOutstanding = new JTextField(10);
	private JTextField m_refWalletBusd = new JTextField(10);
	private JTextField m_ownerBusd = new JTextField(10);
	private JTextField m_refWalletMatic = new JTextField(10);
	private JTextField m_admin1Matic = new JTextField(10);
	private JTextField m_admin2Matic = new JTextField(10);
	private JTextField m_ownerMatic = new JTextField(10);
	private JTextField m_approved = new JTextField(10);
	private JTextField m_cash = new JTextField(10);
	HoldersPanel holdersPanel = new HoldersPanel();

	CryptoPanel() {
		super( new BorderLayout() );
		
		HtmlButton button = new HtmlButton( "Show Wallets", ev -> {
			Util.wrap( () -> holdersPanel.refresh( Monitor.m_config.rusd() ) );
		});

		HtmlButton emptyRefWallet = new HtmlButton( "Send to owner", ev -> emptyRefWallet() );
		HtmlButton sendToRefWallet = new HtmlButton( "Send to RefWallet", ev -> sendToRefWallet() );
		HtmlButton ownerSendBusd = new HtmlButton( "Send to other", ev -> ownerSendBusd() );
		HtmlButton ownerSendMatic = new HtmlButton( "Send", ev -> ownerSendMatic() );

		VerticalPanel rusdPanel = new VerticalPanel();
		rusdPanel.addHeader( "RUSD");
		rusdPanel.add( "RUSD Outstanding", m_rusdOutstanding, button);
		rusdPanel.add( "RefWallet has approved RUSD to spend BUSD", m_approved);
		
		rusdPanel.addHeader( "RefWallet");
		rusdPanel.add( "RefWallet USDT", m_refWalletBusd, emptyRefWallet);
		rusdPanel.add( "RefWallet MATIC", m_refWalletMatic);
		
		rusdPanel.addHeader( "Owner Wallet");
		rusdPanel.add( "Owner USDT", m_ownerBusd, sendToRefWallet, ownerSendBusd);
		rusdPanel.add( "Owner MATIC", m_ownerMatic, ownerSendMatic);
		
		rusdPanel.addHeader( "Fireblocks");
		rusdPanel.add( "Admin1 MATIC", m_admin1Matic);
		rusdPanel.add( "Admin2 MATIC", m_admin2Matic);

		rusdPanel.addHeader( "Brokerage (IB)");
		rusdPanel.add( "Cash in brokerage", m_cash);
		
		add(rusdPanel);
		add(holdersPanel, BorderLayout.EAST);
	}
	
	/** Send from Owner to RefWallet */
	private void sendToRefWallet() {
		Util.wrap( () -> {
			Fireblocks.transfer( 
					Accounts.instance.getId("Owner"),
					Accounts.instance.getAddress("RefWallet"),
					Monitor.m_config.fbStablecoin(),
					Double.parseDouble( Util.ask( "Enter amount")),
					"Move USDT from Owner to RefWallet"
			).waitForHash();
			Util.inform(this, "Done");
		});
	}

	/** Send from Owner to somewhere else */
	private void ownerSendBusd() {
		Util.wrap( () -> {
			Fireblocks.transfer( 
					Accounts.instance.getId("Owner"),
					Util.ask("Enter dest wallet address"),
					Monitor.m_config.fbStablecoin(),
					Double.parseDouble( Util.ask( "Enter amount")),
					Util.ask("Enter note")
			).waitForHash();
			Util.inform(this, "Done");
		});
	}

	private void ownerSendMatic() {
		Util.wrap( () -> {
			Fireblocks.transfer( 
					Accounts.instance.getId("Owner"),
					Util.ask("Enter dest wallet address"),
					Fireblocks.platformBase,
					Double.parseDouble( Util.ask( "Enter amount")),
					Util.ask("Enter note")
			).waitForHash();
			Util.inform(this, "Done");
		});
	}

	/** Send all from RefWallet to owner */
	private void emptyRefWallet() {
		if (Util.confirm(this, "Are you sure you want to transfer all USDT from RefWallet to Owner?") ) {
			Util.wrap( () -> {
				int from = Accounts.instance.getId("RefWallet");
				String to = Accounts.instance.getAddress("Owner");
				double amt = Double.parseDouble( m_refWalletBusd.getText() ) - 1; // leave $1 for good luck
				String note = "Move all USDT from RefWallet to Owner";;
				Fireblocks.transfer(from, to, "USDT_POLYGON", amt, note).waitForHash();
			});
		}		
	}

	@Override public void refresh() throws Exception {
		S.out( "Refreshing Crypto panel");
		Wallet refWallet = Fireblocks.getWallet("RefWallet");

		double busd = refWallet.getBalance(Monitor.m_config.busd().address());
		SwingUtilities.invokeLater( () -> m_refWalletBusd.setText( S.fmt2(busd) ) );

		double nativeBal = refWallet.getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_refWalletMatic.setText( S.fmt2(nativeBal) ) );

		Wallet owner = Fireblocks.getWallet("Owner");
		double ownerMatic = owner.getNativeTokenBalance();
		double ownerBusd = owner.getBalance(Monitor.m_config.busd().address());
		SwingUtilities.invokeLater( () -> {
			m_ownerBusd.setText( S.fmt2(ownerBusd) );
			m_ownerMatic.setText( S.fmt2(ownerMatic) );
		});

		double admin1Bal = Fireblocks.getWallet("Admin1").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin1Matic.setText( S.fmt2(admin1Bal) ) );

		double admin2Bal = Fireblocks.getWallet("Admin2").getNativeTokenBalance();
		SwingUtilities.invokeLater( () -> m_admin2Matic.setText( S.fmt2(admin2Bal) ) );
		
		double approved = Monitor.m_config.busd().getAllowance(
				Accounts.instance.getAddress("RefWallet"),
				Monitor.m_config.rusdAddr() );
		m_approved.setText( "" + approved);

		double rusd = Monitor.m_config.rusd().queryTotalSupply();
		SwingUtilities.invokeLater( () -> m_rusdOutstanding.setText( S.fmt2(rusd) ) );
		
		MyClient.getJson( Monitor.refApiBaseUrl() + "/api/?msg=getCashBal", obj -> {
			double val = obj.getDouble("TotalCashValue");
			SwingUtilities.invokeLater( () -> m_cash.setText( S.fmt2(val) ) );
		});
	}
}
