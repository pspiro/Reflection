package monitor;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import common.Util.ExRunnable;
import http.MyClient;
import monitor.wallet.BlockSummaryPanel;
import monitor.wallet.WalletPanel;
import positions.Wallet;
import reflection.Config;
import reflection.Stock;
import tw.util.DualPanel;
import tw.util.HtmlButton;
import tw.util.S;
import tw.util.UI;
import tw.util.UI.MyTextArea;
import tw.util.UpperField;
import tw.util.VerticalPanel;
import util.LogType;
import web3.StockToken;

public class BigWalletPanel extends JPanel {  // you can safely make this a MonPanel if desired
	private static final double minBalance = .0001;
	
	private final WalletPanel m_parent;
	private final JTextField m_wallet = new JTextField(27); 
	private final JLabel m_rusd = new MyLabel(); 
	private final JLabel m_usdc = new MyLabel(); 
	private final JLabel m_approved = new MyLabel(); 
	private final JLabel m_matic = new MyLabel(); 
	private final JLabel m_name = new MyLabel(); 
	private final JLabel m_email = new MyLabel(); 
	private final JLabel m_kyc = new MyLabel(); 
	private final JLabel m_pan = new MyLabel(); 
	private final JLabel m_aadhaar = new MyLabel(); 
	private final JLabel m_locked = new MyLabel(); 
	private final JTextField m_firstName = new JTextField(8); 
	private final UpperField m_mintAmt = new UpperField(8); 
	private final UpperField m_burnAmt = new UpperField(8);
	private final UpperField m_awardAmt = new UpperField(8); 
	private final UpperField m_lockFor = new UpperField(8); 
	private final UpperField m_requiredTrades = new UpperField(8);
	private final JTextField m_subject = new JTextField(8);
	private final JTextArea m_emailText = new MyTextArea(3, 30);
	private final JsonModel posModel = new PosModel();
	private final TransPanel transPanel = new TransPanel();
	private final RedemptionPanel redemPanel = new RedemptionPanel();
	private final BlockSummaryPanel blockPanel = new BlockSummaryPanel();

	public BigWalletPanel(WalletPanel parent) {
		super( new BorderLayout() );
		m_parent = parent;

		m_wallet.addActionListener( e -> m_parent.refreshTop() );
		
		VerticalPanel vp = new VerticalPanel();
		vp.setBorder( new TitledBorder( "Balances") );
		vp.add( "Wallet", m_wallet);
		vp.add( "Explore", new HtmlButton("View on blockchain explorer", e -> explore() ) );
		
		vp.addHeader( "User details");
		vp.add( "Name", m_name); 
		vp.add( "Email", m_email);
		vp.add( "KYC", m_kyc);
		vp.add( "PAN", m_pan);
		vp.add( "Aadhaar", m_aadhaar);
		
		vp.addHeader( "Crypto");
		vp.add( "RUSD", m_rusd);
		vp.add( "USDT", m_usdc);
		vp.add( "Approved", m_approved);
		vp.add( "MATIC", m_matic);
		vp.add( "Locked", m_locked);
		
		vp.addHeader( "Operations");
		vp.add( "Mint RUSD", m_mintAmt, new HtmlButton("Mint", e -> mint() ) ); 
		vp.add( "Burn RUSD", m_burnAmt, new HtmlButton("Burn", e -> burn() ), new HtmlButton("Burn All", e -> burnAllRusd() ) ); 
		vp.add( "Award", 
				m_awardAmt, 
				new JLabel( "RUSD for "),
				m_lockFor,
				new JLabel( "days and require"),
				m_requiredTrades,
				new JLabel( "trades"),
				new HtmlButton("Go", e -> award() ) ); 

		vp.add( "First name", m_firstName, new HtmlButton("Create new user", e -> createUser() ) );
		vp.add( "Give MATIC", new HtmlButton("Transfer .01 MATIC from Admin1 to this wallet", e -> giveMatic() ) );

		vp.add( "Subject", m_subject, new HtmlButton("Send", e -> sendEmail() ) );
		vp.add( "Text", new JScrollPane( m_emailText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) );
		
		transPanel.small("Transactions");
		redemPanel.small("Redemptions");

		JPanel leftPanel = new JPanel(new BorderLayout() );
		leftPanel.add( vp, BorderLayout.NORTH);
		leftPanel.add( posModel.createTable() );
		
		DualPanel rightPanel = new DualPanel();
		rightPanel.add( "1", transPanel);
		rightPanel.add( "2", redemPanel);
		rightPanel.add( "3", blockPanel);
		
		add( leftPanel, BorderLayout.WEST);
		add( rightPanel);
	}

	public String getWallet() {
		return m_wallet.getText();
	}
	
	private void award() {
		m_parent.wrap( () -> {
			long lockUntil = System.currentTimeMillis() + m_lockFor.getLong() * Util.DAY;
			double amt = m_awardAmt.getDouble();
			String wallet = m_wallet.getText().toLowerCase();
			
			// mint and lock?
			if ( amt > 0) {
				if (Util.confirm(this, "Awarding %s RUSD for %s", amt, m_wallet.getText() ) ) {
					if (amt > 500 && !Util.ask( "Enter password due to high amount").equals( "1359") ) {
						Util.inform( this, "The password was invalid");
						return;
					}
					
					mint( amt);
					JsonObject obj = createLockObject( wallet, amt, lockUntil, m_requiredTrades.getInt() );
					config().sqlCommand( sql -> sql.insertOrUpdate("users", obj, "wallet_public_key = '%s'", wallet) );
	
					Util.inform( this, "Done");
				}
			}
			// mint nothing and lock all of it
			else if (Util.confirm(this, "Lock all of this user's RUSD?") ) {
				JsonObject obj = createLockObject( wallet, 1000000, lockUntil, m_requiredTrades.getInt() );
				config().sqlCommand( sql -> sql.insertOrUpdate("users", obj, "wallet_public_key = '%s'", wallet) );

				Util.inform( this, "Done");
			}
		});
	}
	
	/** Used by TestCase as well as Monitor */
	public static JsonObject createLockObject( String wallet, double amt, long lockUntil, int requiredTrades) throws Exception {
		return Util.toJson( 
				"wallet_public_key", wallet, 
				"locked", Util.toJson( "amount", amt, "lockedUntil", lockUntil, "requiredTrades", requiredTrades) );
	}
	
	public void setWallet(String addr) {
		m_wallet.setText(addr);
		m_parent.refreshTop();
	}

	public void refresh(JsonArray blockRows) throws Exception {
		String walletAddr = m_wallet.getText().toLowerCase();
		S.out( "Refreshing Wallet panel with wallet %s", walletAddr);

		S.out( "Clearing values");
		posModel.ar().clear();
		m_mintAmt.setText(null);
		m_burnAmt.setText(null);
		m_rusd.setText(null);
		m_usdc.setText(null);
		m_approved.setText(null);
		m_matic.setText(null);
		m_name.setText(null);
		m_email.setText(null);
		m_kyc.setText(null);
		m_pan.setText(null);
		m_aadhaar.setText(null);
		m_locked.setText(null);
		transPanel.clear();
		redemPanel.clear();
		blockPanel.clear();

		if (Util.isValidAddress(walletAddr)) {
			S.out( "Updating values");

			// query Users record for north-west panel
			JsonArray users = config().sqlQuery( "select * from users where wallet_public_key = '%s'", walletAddr);
			if (users.size() == 1) {
				JsonObject json = users.get(0);
				m_name.setText( json.getString("first_name") + " " + json.getString("last_name") );
				m_email.setText( json.getString("email"));
				m_kyc.setText( json.getString("kyc_status"));
				m_pan.setText( Util.isValidPan(json.getString("pan_number") ) ? "VALID" : null); 
				m_aadhaar.setText( Util.isValidAadhaar( json.getString("aadhaar") ) ? "VALID": null);
				
				JsonObject obj = json.getObject("locked");
				if (obj != null) {
					m_locked.setText( String.format( 
							"%s locked until %s", 
							obj.getDouble( "amount"), 
							obj.getTime( "lockedUntil", Util.yToS) ) );
				}
			}
			
			MyClient.getJson(Monitor.refApiBaseUrl() + "/api/mywallet/" + walletAddr, obj -> {
				JsonArray ar = obj.getArray("tokens");
				Util.require( ar.size() == 3, "Invalid mywallet query results for wallet %s", walletAddr ); 

				m_rusd.setText("" + S.formatPrice( ar.get(0).getDouble("balance")));
				m_usdc.setText("" + S.formatPrice( ar.get(1).getDouble("balance")));
				m_approved.setText("" + S.formatPrice( ar.get(1).getDouble("approvedBalance")));
				m_matic.setText( S.fmt4( ar.get(2).getDouble("balance")) );			
			});

			Wallet wallet = new Wallet( walletAddr);
			HashMap<String, Double> posMap = wallet.reqPositionsMap();
			
			for (Stock stock : Monitor.stocks) {
				double bal = Util.toDouble( posMap.get( stock.getSmartContractId().toLowerCase() ) );
				if (bal > minBalance) {
					JsonObject obj = new JsonObject();
					obj.put( "Symbol", stock.symbol() );
					obj.put( "Balance", bal);
					obj.put( "stock", stock);
					posModel.ar().add(obj);
				}
			}

			// refresh transactions panel
			String where = String.format("where wallet_public_key = '%s'", walletAddr);
			transPanel.where.setText( where);
			transPanel.refresh();
			
			// refresh redemptions panel
			redemPanel.where.setText( where);
			redemPanel.refresh();
			
			// refresh block summary panel
			blockPanel.refresh( walletAddr, blockRows);
		}
		else if (S.isNotNull(walletAddr)) {
			Util.inform(this, "Invalid wallet address '%s'", walletAddr);
		}

		posModel.fireTableDataChanged();
	}
	
	private void giveMatic() {
		if (Util.confirm( this, 
					"Are you sure you want to transfer .01 MATIC from Admin1 to " + m_wallet.getText() ) ) {
			wrap( () -> {
				config().matic().transfer(
						config().admin1Addr(),
						m_wallet.getText(), 
						.01); 
			});
		}
	}

	/** Open wallet in blockchain explorer */
	private void explore() {
		Util.browse( config().blockchainAddress( m_wallet.getText() ) );
	}

	private void createUser() {
		wrap( () -> {
			config().sqlCommand( sql -> sql.insertJson( "users",
					Util.toJson( 
							"wallet_public_key", m_wallet.getText().toLowerCase(),
							"first_name", m_firstName.getText() ) ) );
			UI.flash("Done");
		});
	}

	private void mint() {
		wrap( () -> {
			double amt = m_mintAmt.getDouble();
			if ( amt > 0 && Util.confirm(this, "Minting %s RUSD for %s", amt, m_wallet.getText() ) ) {
				if (amt > 100 && !Util.ask( "Enter password due to high amount").equals( "1359") ) {
					Util.inform( this, "The password was invalid");
					return;
				}
				
				mint( amt);
			}
		});
	}
	
	private void mint(double amt) throws Exception {
			Util.require( Util.isValidAddress(m_wallet.getText()), "Invalid wallet address");

			String hash = config().rusd().mintRusd( 
					m_wallet.getText(), amt, Monitor.stocks.getAnyStockToken() ).waitForHash();
			
			config().sqlCommand( sql -> sql.insertJson( "log", Util.toJson(
					"type", LogType.MINT,
					"wallet_public_key", m_wallet.getText().toLowerCase(),
					"data", Util.toJson( "amt", amt) ) ) );

			UI.flash(hash);

			m_mintAmt.setText(null);
	}

	private void burn() {
		burn( m_burnAmt.getDouble() );
	}
	
	private void burnAllRusd() {
		wrap( () -> { 
			double amt = new Wallet( m_wallet.getText() ).getBalance( config().rusdAddr() );
			burn( amt);
		});
	}

	private void burn(double amt) {
		if ( amt > 0 && Util.confirm(this, "Burning %s RUSD from %s", amt, m_wallet.getText() ) ) {
			wrap( () -> {
					Util.require( Util.isValidAddress(m_wallet.getText()), "Invalid wallet address");
			
				String hash = config().rusd().burnRusd( 
						m_wallet.getText(), amt, Monitor.stocks.getAnyStockToken() ).waitForHash();
				
				config().sqlCommand( sql -> sql.insertJson( "log", Util.toJson(
						"type", LogType.BURN,
						"wallet_public_key", m_wallet.getText().toLowerCase(),
						"data", Util.toJson( "amt", amt) ) ) );
				
				UI.flash(hash);

				m_burnAmt.setText(null);
			});
		}
	}

	/** Send an email from Josh@reflection */
	private void sendEmail() {
		wrap( () -> {
			JsonObject data = Util.toJson( 
					"subject", m_subject.getText(), 
					"text", m_emailText.getText() );
			
			config().sendEmail(
					m_email.getText(),
					m_subject.getText(),
					m_emailText.getText() );
			
			config().sqlCommand( sql -> sql.insertJson( "log", Util.toJson(
					"type", LogType.EMAIL,
					"wallet_public_key", m_wallet.getText().toLowerCase(),
					"data", data) ) );
			
			UI.flash( "Message sent");

			m_subject.setText(null);
			m_emailText.setText(null);			
		});
	}

	public void wrap(ExRunnable runner) {
		try {
			UI.watch( Monitor.m_frame, runner); // display hourglass and catch exceptions
		}
		catch (Throwable e) {
			e.printStackTrace();
			Util.inform( this, e.getMessage() );
		}
	}
	
	static class MyLabel extends JLabel {
		MyLabel() {
			addMouseListener( new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					Util.copyToClipboard( getText() );
					java.awt.Toolkit.getDefaultToolkit().beep();
				}
			});
		}
	}
	
	class PosModel extends JsonModel {
		public PosModel() {
			super("Symbol,Balance");
		}
		
		protected void buildMenu(JPopupMenu menu, JsonObject record, String tag, Object val) {
			menu.add( JsonModel.menuItem("Double it", ev -> wrap( () -> doubleIt( record) ) ) );
		}
	}
	
	private void doubleIt(JsonObject rec) throws Exception {
		String symbol = rec.getString("Symbol");
		double amt = rec.getDouble( "Balance");
		StockToken tok = rec.getStock( "stock").getToken();
		String wallet = m_wallet.getText().toLowerCase();
		
		Util.reqValidAddress(wallet);
		
		if (Util.confirm( this, String.format("You will mint %s %s for %s", amt, symbol, wallet) ) ) {
			config().rusd().mintStockToken( wallet, tok, amt).waitForHash();
			Util.inform( this, "Done");
		}	
		
	static Config config() {
		return Monitor.m_config;
	}
}
