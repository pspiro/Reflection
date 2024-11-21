package monitor;

import static monitor.Monitor.m_config;

import java.awt.BorderLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.json.simple.JsonObject;
import org.json.simple.OrderedJson;

import common.Util;
import http.MyClient;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.HorzDualPanel;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.VerticalPanel;

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
	

	CryptoPanel() throws Exception {
		super( new BorderLayout() );
		
		HtmlButton button = new HtmlButton( "Show Wallets", ev -> {
			wrap( () -> holdersPanel.refresh( config().rusd() ) );
		});

		m_rusdAddress.setText( chain().params().rusdAddr() );

		HtmlButton emptyRefWallet = new HtmlButton( "Send to owner", ev -> emptyRefWallet() );
		HtmlButton sendBusdFromRefWallet = new HtmlButton( "Send", ev -> sendBusdFromRefWallet() );
		HtmlButton sendToRefWallet = new HtmlButton( "Send to RefWallet", ev -> sendToRefWallet() );
		HtmlButton ownerSendBusd = new HtmlButton( "Send to other", ev -> ownerSendToOther() );
		HtmlButton ownerSendMatic = new HtmlButton( "Send", ev -> ownerSendMatic() );
		HtmlButton refSendMatic = new HtmlButton( "Send", ev -> refSendMatic() );

		VerticalPanel leftPanel = new VerticalPanel();
		leftPanel.addHeader( "RUSD");
		leftPanel.add( "Address", m_rusdAddress);
		leftPanel.add( "RUSD Outstanding", m_rusdOutstanding, button);
		
		String busd = config().busd().name();
		
		leftPanel.addHeader( "RefWallet");
		leftPanel.add( "Address", m_refAddress);
		leftPanel.add( "RefWallet " + busd, m_refWalletBusd, sendBusdFromRefWallet, emptyRefWallet);
		leftPanel.add( "RefWallet " + busd + " approved", m_approved, new JLabel( " for spending by RUSD"));
		leftPanel.add( "RefWallet " + MonitorConfig.nativeTokName(), m_refWalletMatic, refSendMatic);
		
		leftPanel.addHeader( "Owner Wallet");
		leftPanel.add( "Address", m_ownerAddress);
		leftPanel.add( "Owner " + busd, m_ownerBusd, sendToRefWallet, ownerSendBusd);
		leftPanel.add( "Owner " + MonitorConfig.nativeTokName(), m_ownerMatic, ownerSendMatic);
		
		leftPanel.addHeader( "Admin Accounts");
		leftPanel.add( "Admin1 " + MonitorConfig.nativeTokName(), m_admin1Matic);
		leftPanel.add( "Admin2 " + MonitorConfig.nativeTokName(), m_admin2Matic);

		leftPanel.addHeader( "Brokerage (IB)");
		leftPanel.add( "Cash in brokerage", m_cash);
		leftPanel.add( "Net liq in brokerage", m_netLiq);
		
		HorzDualPanel dualPanel = new HorzDualPanel();
		dualPanel.add( leftPanel, "1");
		dualPanel.add( holdersPanel, "2");
		
		add(dualPanel);
	}
	
	private void sendBusdFromRefWallet() {
		wrap( () -> {
			String to = Util.ask( "Enter wallet");
			double amt = Double.parseDouble( Util.ask( "Enter amount") );
			
			if (Util.confirm( this, "Are you sure you want to send %s %s from RefWallet to %s",
					amt, config().busd().name(), to) ) {
				
				config().busd().transfer( chain().params().refWalletKey(), to, amt)
					.waitForReceipt();
				
				Util.inform(this, "Done");
			}
		});
	}

	/** Send from Owner to RefWallet */
	private void sendToRefWallet() {
		wrap( () -> {
			config().busd().transfer( 
					config().ownerKey(),
					config().refWalletAddr(),
					Double.parseDouble( Util.ask( "Enter amount"))
					).waitForReceipt();
			Util.inform(this, "Done");
		});
	}

	/** Send from Owner to somewhere else */
	private void ownerSendToOther() {
		wrap( () -> {
			String name = Util.ask( "Enter name from Recipients tab");
			if (S.isNull( name) ) return;
			
			GTable recips = new GTable(NewSheet.Reflection, "Recipients", "Name", "Address");
			String address = Util.reqValidAddress( recips.get( name) );
			
			JsonObject json = new OrderedJson();
			json.put( "Date", S.USER_DATE.format( new Date() ) );
			json.put( "Payor or Payee", name);
			json.put( "Description", "");
			json.put( "Category", "");
			json.put( "Amount", "");
			json.put( "Account", "Owner wallet");
			
			// let user edit the json object
			if (!JsonDlg.edit( Monitor.m_frame, json) ) {
				return;
			}
			
			// check amount too high or too low
			double amt = json.getDouble( "Amount");
			if (amt <= 0) return;
			
			if (amt > 300 && !Util.ask( "Enter password due to high amount").equals( "1359") ) {
				Util.inform( this, "The password was invalid");
				return;
			}

			// do the transfer
			String hash = config().busd().transfer( 
					config().ownerKey(),
					address,
					amt
					).waitForReceipt();
			
			// add transaction to Reflection financial spreadsheet
			if (Monitor.m_config.isProduction() ) {
				Tab tab = NewSheet.getTab( NewSheet.ReflTransactions, "Register");
				tab.insert( json);
			}

			// copy has to clipboard
			Util.copyToClipboard( config().chain().blockchainTx( hash) );
			Util.inform(this, "Done, hash is copied to clipboard");
		});
	}

	private void ownerSendMatic() {
		wrap( () -> sendMatic( config().ownerKey() ) );
	}

	private void refSendMatic() {
		wrap( () -> sendMatic( chain().params().refWalletKey() ) );
	}

	private void sendMatic(String senderKey) throws NumberFormatException, Exception {
		config().chain().blocks().transfer( 
				senderKey,
				Util.ask("Enter dest wallet address"),
				Double.parseDouble( Util.ask( "Enter amount"))
				);
		Util.inform(this, "Done");
	}

	/** Send all from RefWallet to owner */
	private void emptyRefWallet() {
		if (Util.confirm(this, "Are you sure you want to transfer the full amount of " + config().busd().name() + " from RefWallet to Owner?") ) {
			wrap( () -> {
				double amt = Double.parseDouble( m_refWalletBusd.getText() ) - 1; // leave $1 for good luck

				config().busd().transfer(
						chain().params().refWalletKey(),
						chain().params().ownerAddr(),
						amt);
			});
		}		
	}

	@Override public void refresh() throws Exception {
		S.out( "Refreshing Crypto panel");
		m_refAddress.setText( config().refWalletAddr() );

		double busd = config().busd().getPosition( config().refWalletAddr() );
		SwingUtilities.invokeLater( () -> m_refWalletBusd.setText( S.fmt2(busd) ) );

		double nativeBal = m_config.node().getNativeBalance( config().refWalletAddr() );
		SwingUtilities.invokeLater( () -> m_refWalletMatic.setText( S.fmt2(nativeBal) ) );

		double ownerMatic = m_config.node().getNativeBalance( chain().params().ownerAddr() );
		double ownerBusd = config().busd().getPosition( chain().params().ownerAddr() );
		SwingUtilities.invokeLater( () -> {
			m_ownerAddress.setText( chain().params().ownerAddr() );
			m_ownerBusd.setText( S.fmt2(ownerBusd) );
			m_ownerMatic.setText( S.fmt2(ownerMatic) );
		});

		double admin1Bal = m_config.node().getNativeBalance( chain().params().admin1Addr() );
		SwingUtilities.invokeLater( () -> m_admin1Matic.setText( S.fmt2(admin1Bal) ) );

//		double admin2Bal = new Wallet( config().admin2Addr()").getNativeBalance();
//		SwingUtilities.invokeLater( () -> m_admin2Matic.setText( S.fmt2(admin2Bal) ) );
		
		double approved = config().busd().getAllowance(
				chain().params().refWalletAddr(),
				chain().params().rusdAddr() );
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
