package monitor;

import static monitor.Monitor.m_config;

import java.awt.BorderLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.json.simple.JsonObject;
import org.json.simple.OrderedJson;

import common.JsonModel;
import common.Util;
import http.MyClient;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.HorzDualPanel;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.UI;
import tw.util.VerticalPanel;

/** Not a json panel */
public class CryptoPanel extends MonPanel {
	final int addrSize = 28;
	
	private JTextField m_rusdOutstanding = new JTextField(10);
	private JTextField m_refWalletBusd = new JTextField(10);
	private JTextField m_ownerBusd = new JTextField(10);
	private JTextField m_refWalletMatic = new JTextField(10);
	private JTextField m_ownerMatic = new JTextField(10);
	private JTextField m_approved = new JTextField(10);
	private JTextField m_cash = new JTextField(10);
	private JTextField m_netLiq = new JTextField(10);
	private JTextField m_ownerAddress = new JTextField(addrSize);
	private JTextField m_refAddress = new JTextField(addrSize);
	private JTextField m_rusdAddress = new JTextField(addrSize);
	
	private JLabel m_refLabel = new JLabel("???");
	private JLabel m_ownLabel = new JLabel("???");
	
	private JsonModel m_adminModel = new JsonModel("name,address,nativeBalance,latestNonce,pendingNonce") {
		protected void buildMenu(JPopupMenu menu, JsonObject record, String tag, Object val) {
			menu.add( JsonModel.menuItem("Send To", ev -> sendNativeFrom( record) ) );
		}
	};
	
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
		
		VerticalPanel topLeft = new VerticalPanel();
		topLeft.addHeader( "RUSD");
		topLeft.add( "Address", m_rusdAddress);
		topLeft.add( "RUSD Outstanding", m_rusdOutstanding, button);
		
		String busd = config().busd().name();
		
		topLeft.addHeader( "RefWallet");
		topLeft.add( "Address", m_refAddress);
		topLeft.add( "RefWallet " + busd, m_refWalletBusd, sendBusdFromRefWallet, emptyRefWallet);
		topLeft.add( "RefWallet " + busd + " approved", m_approved, new JLabel( " for spending by RUSD"), new HtmlButton( "Approve", ev -> approve() ) );
		topLeft.add( m_refLabel, m_refWalletMatic, refSendMatic);
		
		topLeft.addHeader( "Owner Wallet");
		topLeft.add( "Address", m_ownerAddress);
		topLeft.add( "Owner " + busd, m_ownerBusd, sendToRefWallet, ownerSendBusd);
		topLeft.add( m_ownLabel, m_ownerMatic, ownerSendMatic);
		
		topLeft.addHeader( "Brokerage (IB)");
		topLeft.add( "Cash in brokerage", m_cash);
		topLeft.add( "Net liq in brokerage", m_netLiq);

		JPanel leftPanel = new JPanel( new BorderLayout() );
		leftPanel.add( topLeft, BorderLayout.NORTH);
		leftPanel.add( m_adminModel.createTable( "Admin Accounts") );

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
			Util.copyToClipboard( Monitor.chain().browseTx( hash) );
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
		Monitor.chain().blocks().transfer( 
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
		m_refLabel.setText( "RefWallet " + config().nativeTokName() );
		m_ownLabel.setText( "Owner " + config().nativeTokName() );
		
		m_refAddress.setText( config().refWalletAddr() );

		double busd = config().busd().getPosition( config().refWalletAddr() );
		SwingUtilities.invokeLater( () -> m_refWalletBusd.setText( S.fmt2c(busd) ) );

		double nativeBal = m_config.node().getNativeBalance( config().refWalletAddr() );
		SwingUtilities.invokeLater( () -> m_refWalletMatic.setText( S.fmt2c(nativeBal) ) );

		double ownerMatic = m_config.node().getNativeBalance( chain().params().ownerAddr() );
		double ownerBusd = config().busd().getPosition( chain().params().ownerAddr() );
		SwingUtilities.invokeLater( () -> {
			m_ownerAddress.setText( chain().params().ownerAddr() );
			m_ownerBusd.setText( S.fmt2c(ownerBusd) );
			m_ownerMatic.setText( S.fmt2c(ownerMatic) );
		});
		
		refreshAdminTable();
		
		double approved = config().busd().getAllowance(
				chain().params().refWalletAddr(),
				chain().params().rusdAddr() );
		m_approved.setText( S.fmt2c( approved) );

		double rusd = config().rusd().queryTotalSupply();
		m_rusdOutstanding.setText( S.fmt2c(rusd) );
		
		MyClient.getJson( Monitor.refApiBaseUrl() + "/api/?msg=getCashBal", obj -> {
			double cashBal = obj.getDouble("TotalCashValue");
			double netLiq = obj.getDouble("NetLiquidation");
			obj.display();
			SwingUtilities.invokeLater( () -> {
				m_cash.setText( S.fmt2c(cashBal) );
				m_netLiq.setText( S.fmt2c(netLiq) );
			});
		});
	}

	private void refreshAdminTable() throws Exception {
		m_adminModel.ar().clear();
		m_adminModel.ar().add( createAdminRow( "Admin1", chain().params().admin1Addr() ) );
		m_adminModel.ar().add( createAdminRow( "SysAdmin", chain().params().sysAdminAddr() ) );
		m_adminModel.fireTableDataChanged();
	}
	
	private JsonObject createAdminRow( String name, String address) throws Exception {
		Util.reqValidAddress(address);
		
		return Util.toJson(
				"name", name,
				"address", address,
				"nativeBalance", m_config.node().getNativeBalance( address),
				"latestNonce", chain().node().getNonceLatest( address),
				"pendingNonce", chain().node().getNoncePending( address)
				);
	}

	private void sendNativeFrom(JsonObject record) {
		wrap( () -> {
			String to = Util.ask( "Enter destination address");
			double amt = Util.askForVal( "Enter amount");
			if (S.isNotNull( to) && amt > 0) {
				chain().blocks().transfer(
						chain().getAdminKey( record.getString( "address") ),
						to,
						amt)
					.waitForReceipt();
				UI.flash( "Done");
			}
		});
	}
	
	private void approve() {
		wrap( () -> {
			double val = Util.askForVal( "Enter amount to approve, -1 to cancel");
			if (val > 0) {
				chain().busd().approve( chain().params().refWalletKey(), chain().rusd().address(), val)
					.waitForReceipt();
				UI.flash( "Done");
			}
		});
	}
}
