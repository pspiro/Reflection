package monitor;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import common.Util;
import http.MyClient;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.HorzDualPanel;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.VerticalPanel;
import web3.NodeServer;

/** Not a json panel */
public class CryptoPanel extends MonPanel {
	final int addrSize = 28;
	
	private JTextField m_rusdOutstanding = new JTextField(10);
	private JTextField m_refWalletBusd = new JTextField(10);
	private JTextField m_ownerBusd = new JTextField(10);
	private JTextField m_refWalletMatic = new JTextField(10);
	private JTextField m_admin1Matic = new JTextField(10);
	private JTextField m_admin2Matic = new JTextField(10);
	private JTextField m_ownerMatic = new JTextField(10);
	private JTextField m_approved = new JTextField(10);
	private JTextField m_cash = new JTextField(10);
	private JTextField m_netLiq = new JTextField(10);
	private JTextField m_ownerAddress = new JTextField(addrSize);
	private JTextField m_refAddress = new JTextField(addrSize);
	private JTextField m_rusdAddress = new JTextField(addrSize);
	HoldersPanel holdersPanel = new HoldersPanel();
	

	CryptoPanel() {
		super( new BorderLayout() );
		
		HtmlButton button = new HtmlButton( "Show Wallets", ev -> {
			wrap( () -> holdersPanel.refresh( config().rusd() ) );
		});

		m_rusdAddress.setText( config().rusdAddr() );

		HtmlButton emptyRefWallet = new HtmlButton( "Send to owner", ev -> emptyRefWallet() );
		HtmlButton sendToRefWallet = new HtmlButton( "Send to RefWallet", ev -> sendToRefWallet() );
		HtmlButton ownerSendBusd = new HtmlButton( "Send to other", ev -> ownerSendBusd() );
		HtmlButton ownerSendMatic = new HtmlButton( "Send", ev -> ownerSendMatic() );

		VerticalPanel leftPanel = new VerticalPanel();
		leftPanel.addHeader( "RUSD");
		leftPanel.add( "Address", m_rusdAddress);
		leftPanel.add( "RUSD Outstanding", m_rusdOutstanding, button);
		
		String busd = config().busd().name();
		
		leftPanel.addHeader( "RefWallet");
		leftPanel.add( "Address", m_refAddress);
		leftPanel.add( "RefWallet " + busd, m_refWalletBusd, emptyRefWallet);
		leftPanel.add( "RefWallet " + busd + " approved", m_approved, new JLabel( " for spending by RUSD"));
		leftPanel.add( "RefWallet MATIC", m_refWalletMatic);
		
		leftPanel.addHeader( "Owner Wallet");
		leftPanel.add( "Address", m_ownerAddress);
		leftPanel.add( "Owner " + busd, m_ownerBusd, sendToRefWallet, ownerSendBusd);
		leftPanel.add( "Owner MATIC", m_ownerMatic, ownerSendMatic);
		
		leftPanel.addHeader( "Admin Accounts");
		leftPanel.add( "Admin1 MATIC", m_admin1Matic);
		leftPanel.add( "Admin2 MATIC", m_admin2Matic);

		leftPanel.addHeader( "Brokerage (IB)");
		leftPanel.add( "Cash in brokerage", m_cash);
		leftPanel.add( "Net liq in brokerage", m_netLiq);
		
		HorzDualPanel dualPanel = new HorzDualPanel();
		dualPanel.add( leftPanel, "1");
		dualPanel.add( holdersPanel, "2");
		
		add(dualPanel);
	}
	
	/** Send from Owner to RefWallet */
	private void sendToRefWallet() {
		wrap( () -> {
			config().busd().transfer( 
					config().ownerKey(),
					config().refWalletAddr(),
					Double.parseDouble( Util.ask( "Enter amount"))
					).waitForHash();
			Util.inform(this, "Done");
		});
	}

	/** Send from Owner to somewhere else */
	private void ownerSendBusd() {
		wrap( () -> {
			String name = Util.ask( "Enter name");
			
			GTable tab = new GTable(NewSheet.Reflection, "Recipients", "Name", "Address");
			String address = Util.reqValidAddress( tab.get( name) );
			
			double amt = Double.parseDouble( Util.ask( "Enter amount"));
			if (amt > 300 && !Util.ask( "Enter password due to high amount").equals( "1359") ) {
				Util.inform( this, "The password was invalid");
				return;
			}
			
			String hash = config().busd().transfer( 
					config().ownerKey(),
					address,
					amt
					).waitForHash();
			
			//Util.browse( config().blockchainTx( hash) );
			Util.copyToClipboard( config().blockchainTx( hash) );
			Util.inform(this, "Done, hash is copied to clipboard");
		});
	}

	private void ownerSendMatic() {
		wrap( () -> {
			config().matic().transfer( 
					config().ownerKey(),
					Util.ask("Enter dest wallet address"),
					Double.parseDouble( Util.ask( "Enter amount"))
					);
			Util.inform(this, "Done");
		});
	}

	/** Send all from RefWallet to owner */
	private void emptyRefWallet() {
		if (Util.confirm(this, "Are you sure you want to transfer this amount (-1) " + config().busd().name() + " from RefWallet to Owner?") ) {
			wrap( () -> {
				double amt = Double.parseDouble( m_refWalletBusd.getText() ) - 1; // leave $1 for good luck

				config().busd().transfer(
						config().refWalletKey(),
						config().ownerAddr(),
						amt);
			});
		}		
	}

	@Override public void refresh() throws Exception {
		S.out( "Refreshing Crypto panel");
		m_refAddress.setText( config().refWalletAddr() );

		double busd = config().busd().getPosition( config().refWalletAddr() );
		SwingUtilities.invokeLater( () -> m_refWalletBusd.setText( S.fmt2(busd) ) );

		double nativeBal = NodeServer.getNativeBalance( config().refWalletAddr() );
		SwingUtilities.invokeLater( () -> m_refWalletMatic.setText( S.fmt2(nativeBal) ) );

		double ownerMatic = NodeServer.getNativeBalance( config().ownerAddr() );
		double ownerBusd = config().busd().getPosition( config().ownerAddr() );
		SwingUtilities.invokeLater( () -> {
			m_ownerAddress.setText( config().ownerAddr() );
			m_ownerBusd.setText( S.fmt2(ownerBusd) );
			m_ownerMatic.setText( S.fmt2(ownerMatic) );
		});

		double admin1Bal = NodeServer.getNativeBalance( config().admin1Addr() );
		SwingUtilities.invokeLater( () -> m_admin1Matic.setText( S.fmt2(admin1Bal) ) );

//		double admin2Bal = new Wallet( config().admin2Addr()").getNativeBalance();
//		SwingUtilities.invokeLater( () -> m_admin2Matic.setText( S.fmt2(admin2Bal) ) );
		
		double approved = config().busd().getAllowance(
				config().refWalletAddr(),
				config().rusdAddr() );
		m_approved.setText( S.fmt2( approved) );

		double rusd = config().rusd().queryTotalSupply();
		m_rusdOutstanding.setText( S.fmt2(rusd) );
		
		MyClient.getJson( Monitor.refApiBaseUrl() + "/api/?msg=getCashBal", obj -> {
			double cashBal = obj.getDouble("TotalCashValue");
			double netLiq = obj.getDouble("NetLiquidation");
			obj.display();
			SwingUtilities.invokeLater( () -> {
				m_cash.setText( S.fmt2(cashBal) );
				m_netLiq.setText( S.fmt2(netLiq) );
			});
		});
	}
}
