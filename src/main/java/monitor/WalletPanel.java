package monitor;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Accounts;
import fireblocks.Fireblocks;
import http.MyClient;
import monitor.Monitor.TransPanel;
import positions.Wallet;
import reflection.Stock;
import tw.util.DualPanel;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.UI;
import tw.util.VerticalPanel;

public class WalletPanel extends JsonPanel {
	
	
	private static final double minBalance = .0001;
	private final JTextField m_wallet = new JTextField(32); 
	private final JLabel m_rusd = new JLabel(); 
	private final JLabel m_usdc = new JLabel(); 
	private final JLabel m_approved = new JLabel(); 
	private final JLabel m_matic = new JLabel(); 
	private final JLabel m_name = new JLabel(); 
	private final JLabel m_email = new JLabel(); 
	private final JLabel m_kyc = new JLabel(); 
	private final JTextField m_username = new JTextField(8); 
	private final JTextField m_mintAmt = new JTextField(8); 
	private final JTextField m_burnAmt = new JTextField(8);
	private final JTextField m_subject = new JTextField(8);
	private final JTextArea m_text = new JTextArea(3, 40);

	private final TransPanel transPanel = new TransPanel();
	private final RedemptionPanel redemPanel = new RedemptionPanel();

	WalletPanel() throws Exception {
		super( new BorderLayout(), "Symbol,Balance");

		m_wallet.addActionListener( e -> refreshTop() );

		VerticalPanel vp = new VerticalPanel();
		vp.setBorder( new TitledBorder( "Balances") );
		vp.add( "Wallet", m_wallet);
		
		vp.addHeader( "User details");
		vp.add( "Name", m_name);
		vp.add( "Email", m_email);
		vp.add( "KYC", m_kyc);
		
		vp.addHeader( "Crypto");
		vp.add( "RUSD", m_rusd);
		vp.add( "USDT", m_usdc);
		vp.add( "Approved", m_approved);
		vp.add( "MATIC", m_matic);
		
		vp.addHeader( "Operations");
		vp.add( "Mint RUSD", m_mintAmt, new HtmlButton("Mint", e -> mint() ) ); 
		vp.add( "Burn RUSD", m_burnAmt, new HtmlButton("Burn", e -> burn() ) ); 

		vp.add( "Create", m_username, new HtmlButton("Create new user", e -> createUser() ) );
		vp.add( "Explore", new HtmlButton("View on blockchain explorer", e -> explore() ) );
		vp.add( "Give MATIC", new HtmlButton("Transfer .01 MATIC from Admin1 to this wallet", e -> giveMatic() ) );

		vp.add( "Subject", m_subject, new HtmlButton("Send", e -> send() ) );
		vp.add( "Text", m_text);
		
		transPanel.small("Transactions");
		redemPanel.small("Redemptions");


		JPanel leftPanel = new JPanel(new BorderLayout() );
		leftPanel.add( vp, BorderLayout.NORTH);
		leftPanel.add( m_model.createTable() );
		
		DualPanel rightPanel = new DualPanel();
		rightPanel.add( "1", transPanel);
		rightPanel.add( "2", redemPanel);
		
		add( leftPanel, BorderLayout.WEST);
		add( rightPanel);
	}

	private void giveMatic() {
		Util.wrap( () -> {
			if (Util.confirm( this, 
					"Are you sure you want to transfer .01 MATIC from Admin1 to " + m_wallet.getText() ) ) {
				Fireblocks.transfer(
						Accounts.instance.getId("Admin1"), 
						m_wallet.getText(), 
						Fireblocks.platformBase, 
						.01, 
						"give .01 MATIC for free"
				).waitForHash();
			}
		});
	}

	/** Open wallet in blockchain explorer */
	private void explore() {
		String url = Monitor.m_config.blockchainExplorer() + m_wallet.getText();
		Util.browse( url);
	}

	private void createUser() {
		Util.wrap( () -> {
			Monitor.m_config.sqlCommand( sql -> sql.insertJson( "users",
					Util.toJson( 
							"wallet_public_key", m_wallet.getText().toLowerCase(),
							"first_name", m_username.getText() ) ) );
			Util.inform( this, "Done");
		});
	}

	private void mint() {
		try {
			Util.require( Util.isValidAddress(m_wallet.getText()), "Invalid wallet address");
			
			double amt = Double.parseDouble( m_mintAmt.getText() );
			if ( amt > 0 && Util.confirm(this, "Minting %s RUSD for %s", amt, m_wallet.getText() ) ) {
			
				String hash = Monitor.m_config.rusd().mintRusd( 
						m_wallet.getText(), amt, Monitor.stocks.getAnyStockToken() ).waitForHash();
				
				m_mintAmt.setText(null);
				Util.inform(this, hash);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void burn() {
		try {
			Util.require( Util.isValidAddress(m_wallet.getText()), "Invalid wallet address");

			double amt = Double.parseDouble( m_burnAmt.getText() );
			if ( amt > 0 && Util.confirm(this, "Burning %s RUSD from %s", amt, m_wallet.getText() ) ) {
			
				String hash = Monitor.m_config.rusd().burnRusd( 
						m_wallet.getText(), amt, Monitor.stocks.getAnyStockToken() ).waitForHash();
				
				m_burnAmt.setText(null);
				Util.inform(this, hash);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setWallet(String addr) {
		m_wallet.setText(addr);
		refreshTop();
	}

	public void refresh() throws Exception {
		String walletAddr = m_wallet.getText().toLowerCase();
		S.out( "Refreshing Wallet panel with wallet %s", walletAddr);

		S.out( "Clearing values");
		rows().clear();
		m_mintAmt.setText(null);
		m_burnAmt.setText(null);
		m_rusd.setText(null);
		m_usdc.setText(null);
		m_approved.setText(null);
		m_matic.setText(null);
		m_name.setText(null);
		m_email.setText(null);
		m_kyc.setText(null);
		transPanel.clear();
		redemPanel.clear();

		if (Util.isValidAddress(walletAddr)) {
			S.out( "Updating values");

			// query Users record
			JsonArray users = Monitor.m_config.sqlQuery( sql -> 
				sql.queryToJson("select * from users where wallet_public_key = '%s'", walletAddr) );
			if (users.size() == 1) {
				JsonObject json = users.get(0);
				m_name.setText( json.getString("first_name") + " " + json.getString("last_name") );
				m_email.setText( json.getString("email"));
				m_kyc.setText( json.getString("kyc_status"));
			}
			
			MyClient.getJson(Monitor.refApiBaseUrl() + "/api/mywallet/" + walletAddr, obj -> {
				JsonArray ar = obj.getArray("tokens");
				Util.require( ar.size() == 3, "Invalid mywallet query results for wallet %s", walletAddr ); 

				m_rusd.setText("" + S.formatPrice( ar.get(0).getDouble("balance")));
				m_usdc.setText("" + S.formatPrice( ar.get(1).getDouble("balance")));
				m_approved.setText("" + S.formatPrice( ar.get(1).getDouble("approvedBalance")));
				m_matic.setText("" + ar.get(2).getDouble("balance"));			
			});

			Wallet wallet = new Wallet( walletAddr);
			for (Stock stock : Monitor.stocks) {
				JsonObject obj = new JsonObject();
				double bal = wallet.getBalance( stock.getSmartContractId() );
				if (bal > minBalance) {
					obj.put( "Symbol", stock.symbol() );
					obj.put( "Balance", bal);
					rows().add(obj);
				}
			}

			String where = String.format("where wallet_public_key = '%s'", walletAddr);
			transPanel.where.setText( where);
			transPanel.refresh();
			
			redemPanel.where.setText( where);
			redemPanel.refresh();
		}
		else if (S.isNotNull(walletAddr)) {
			Util.inform(this, "Invalid wallet address '%s'", walletAddr);
		}

		m_model.fireTableDataChanged();
	}
	
	private void send() {
		try {
			Monitor.m_config.sendEmailEx(
					m_email.getText(),
					m_subject.getText(),
					m_text.getText(),
					false);
			UI.flash( "Message sent");
		} catch (Exception e) {
			Util.inform( this, e.getMessage() );
			e.printStackTrace();
		}
	}

}
