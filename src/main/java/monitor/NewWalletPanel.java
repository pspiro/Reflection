package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.util.HashMap;

import javax.swing.Box;
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
import http.MyClient;
import monitor.wallet.BlockDetailPanel;
import monitor.wallet.BlockSummaryPanel;
import positions.Wallet;
import reflection.MySqlConnection;
import reflection.Stock;
import test.MyTimer;
import tw.util.DualPanel;
import tw.util.HorzDualPanel;
import tw.util.HtmlButton;
import tw.util.HtmlPane;
import tw.util.NewTabbedPanel;
import tw.util.NewTabbedPanel.INewTab;
import tw.util.S;
import tw.util.UI;
import tw.util.UpperField;
import tw.util.VerticalPanel;
import util.LogType;
import web3.StockToken;

public class NewWalletPanel extends MonPanel {
	static String usersFields = "first_name,last_name,wallet_public_key,kyc_status,created_at,address,address_1,address_2,city,state,zip,country,geo_code,email,telegram,phone,pan_number,aadhaar,locked";

	private JTextField m_walField = new JTextField(28);
	private String m_wallet;  // trimmed, lower case
	private UserPanel dataPanel = new UserPanel();
	private BlockchainPanel blockchainPanel = new BlockchainPanel();
	private TransactionsPanel transPanel = new TransactionsPanel();
	private CryptoPanel cryptoPanel = new CryptoPanel();
	private TokPanel tokPanel = new TokPanel();
	private LogPanel logPanel = new LogPanel();
	private String m_emailAddr;

	private final NewTabbedPanel m_tabs = new NewTabbedPanel(true);

	public NewWalletPanel() {
		super( new BorderLayout() );
		MyTimer t = new MyTimer();
		t.next( "NewWallet ctor");

		m_walField.addActionListener( ev -> refreshTop() );  // hourglass and refresh

		LeftFlow top = new LeftFlow();
		top.horz( 15);
		top.label( "Wallet");
		top.horz(10);
		top.add( m_walField);
		top.horz( 10);
		top.add( new HtmlButton( "Refresh", ev -> refreshTop() ) );
		top.horz( 10);
		top.add( new HtmlButton( "View on Blockchain Explorer", ev -> {
			Util.browse( config().blockchainAddress( m_wallet) );
		}));

		m_tabs.addTab( "User Data", dataPanel); 
		m_tabs.addTab( "Blockchain", blockchainPanel); 
		m_tabs.addTab( "Transactions", transPanel); 
		m_tabs.addTab( "Crypto", cryptoPanel); 
		m_tabs.addTab( "Tokens", tokPanel); 
		m_tabs.addTab( "Log", logPanel);

		add( top, BorderLayout.NORTH);
		add( m_tabs);

		t.done();
	}

	/** Called by other panels; triggers call to refresh() below */
	public void setWallet(String wallet) {
		m_walField.setText( wallet);
		refreshTop();  // hourglass, wrap, and refresh
	}

	@Override protected void refresh() throws Exception {
		wrap( () -> {
			m_wallet = m_walField.getText().trim().toLowerCase();
			Util.require( S.isNull( m_wallet) || Util.isValidAddress(m_wallet), "Invalid wallet address");
	
			if (Util.isValidAddress( m_wallet) ) {
				Monitor.m_config.sqlCommand( sql -> {
					dataPanel.refresh( sql, m_wallet);
					blockchainPanel.clear();
					transPanel.clear();
					cryptoPanel.clear();
					tokPanel.clear();
					logPanel.clear();
				});
			}
		});
	}

	class UserPanel extends MiniTab {
		HtmlPane usersPane = new HtmlPane();
		HtmlPane personaPane = new HtmlPane();
		JsonObject personaResp;

		UserPanel() {
			super( new BorderLayout() );

			JPanel left = new JPanel( new BorderLayout() );
			left.add( usersPane);
			left.setBorder( new TitledBorder( "Users Table"));

			LeftFlow topRight = new LeftFlow();
			topRight.add( new HtmlButton( "View in Persona", ev -> onShowInPersona() ) );
			
			JPanel right = new JPanel( new BorderLayout() );
			right.add( topRight, BorderLayout.SOUTH);
			right.add( personaPane);
			right.setBorder( new TitledBorder( "Persona Data"));

			HorzDualPanel dualPanel = new HorzDualPanel();
			dualPanel.add( left, "1");
			dualPanel.add( right, "2");
			add( dualPanel);
		}

		@Override protected void clear() {
		}
		
		@Override public void activated() {
		}

		void refresh( MySqlConnection sql, String wallet) throws Exception {
			var users = sql.queryToJson("select * from users where wallet_public_key = '%s'", m_wallet);
			if (users.size() > 0) {
				var user = users.get( 0).removeEntry( "persona_responsa");
				String html = user.toHtml( true, usersFields.split( ","));
				S.out( html);
				usersPane.setText( html);

				personaResp = user.getObjectNN( "persona_response");
				
				var pers = BigWalletPanel.getFullPersona( personaResp);
				personaPane.setText( pers.toHtml( true) );
				
				m_emailAddr = user.getString( "email"); // needed to send emails
			}
			else {
				usersPane.setText( null);
				personaPane.setText( null);
			}

			m_tabs.resetActivated();
			m_tabs.select( "User Data");  // this will trigger call to activated()
		}
		
		private void onShowInPersona() {
			if (personaResp != null) {
				try {
					Util.iff( personaResp.getString( "inquiryId"), id ->
						Util.browse( "https://app.withpersona.com/dashboard/inquiries/" + id) );
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	class BlockchainPanel extends MiniTab {
		BlockSummaryPanel sumPanel = new BlockSummaryPanel();
		BlockDetailPanel detPanel = new BlockDetailPanel();

		BlockchainPanel() {
			super( new BorderLayout() );

			sumPanel.setBorder( new TitledBorder( "Consolidated Transactions"));
			detPanel.setBorder( new TitledBorder( "All Transactions"));

			DualPanel dualPanel = new DualPanel();
			dualPanel.add( sumPanel, "1");
			dualPanel.add( detPanel, "2");

			add( dualPanel);
		}

		@Override public void activated() {
			wrap( () -> {
				detPanel.refresh( m_wallet);
				sumPanel.refresh( m_wallet, detPanel.rows() );
			});
		}

		protected void clear() {
			try {
				detPanel.refresh( "");
				sumPanel.refresh( "", new JsonArray() );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class TransactionsPanel extends MiniTab {
		TransPanel transPanel = new TransPanel();
		RedemptionPanel redemPanel = new RedemptionPanel();

		TransactionsPanel() {
			super( new BorderLayout() );

			transPanel.small( "Transactions");
			transPanel.setBorder( new TitledBorder( "Reflection Transactions"));

			redemPanel.setBorder( new TitledBorder( "Redemptions"));

			DualPanel dualPanel = new DualPanel();
			dualPanel.add( transPanel, "1");
			dualPanel.add( redemPanel, "2");

			add( dualPanel);
		}

		@Override public void activated() {
			wrap( () -> {
				MyTimer t = new MyTimer();
				t.next( "connecting to db");
				Monitor.m_config.sqlCommand( sql -> {
					t.next( "refreshing trans panel");
					transPanel.setWallet( m_wallet);
					transPanel.refresh( sql);

					t.next( "refreshing redeem panel");
					redemPanel.setWallet( m_wallet);
					redemPanel.refresh( sql);
					t.done();
				});
			});
		}

		protected void clear() { // you could run the query here, it might be fast
			transPanel.clear();
		}
	}

	class TokPanel extends MiniTab {
		private final JsonModel posModel = new PosModel();
		
		TokPanel() {
			super( new BorderLayout() );
			add( posModel.createTable() );
		}

		class PosModel extends JsonModel {
			public PosModel() {
				super("Symbol,Balance,Price,Value");
				justify("lrrr");
			}
			
			protected void buildMenu(JPopupMenu menu, JsonObject record, String tag, Object val) {
				menu.add( JsonModel.menuItem("Double it", ev -> wrap( () -> doubleIt( record) ) ) );
			}
		}

		public void clear() {
			posModel.ar().clear();
			posModel.fireTableDataChanged();
		}

		@Override public void activated() {
			wrap( () -> {
				var prices = MyClient.getArray( Monitor.m_config.mdBaseUrl() + "/mdserver/get-ref-prices");
				
				Wallet wallet = new Wallet( m_wallet);
				HashMap<String, Double> posMap = wallet.reqPositionsMap();
				
				for (Stock stock : Monitor.stocks) {
					double bal = Util.toDouble( posMap.get( stock.getSmartContractId().toLowerCase() ) );
					if (bal > BigWalletPanel.minBalance) {
						JsonObject obj = new JsonObject();
						obj.put( "Symbol", stock.symbol() );
						obj.put( "Balance", bal);
						obj.put( "stock", stock);
						
						var price = getPrice( prices, stock.conid() );
						if (price != null) {
							obj.put( "Price", fmt( price.getDouble( "last") ) );
							obj.put( "Value", fmt( price.getDouble( "last") * bal) );
						}
						posModel.ar().add(obj);
					}
				}
				posModel.fireTableDataChanged();
			});
		}

		private String fmt(double d) {
			return S.fmt2( d);
		}
	}
	
	private JsonObject getPrice(JsonArray prices, int conid) {
		for (var price : prices) {
			if (price.getInt( "conid") == conid) {
				return price;
			}
		}
		return null;
	}

	class CryptoPanel extends MiniTab {
		private UpperField m_rusd = new UpperField( 27);
		private UpperField m_busd = new UpperField( 27);
		private UpperField m_approved = new UpperField( 27);
		private UpperField m_matic = new UpperField( 27);
		private UpperField m_locked = new UpperField( 27);
		private UpperField m_mintAmt = new UpperField( 27);
		private UpperField m_burnAmt = new UpperField( 27);
		private UpperField m_awardAmt = new UpperField( 27);
		private UpperField m_lockFor = new UpperField( 7);
		private UpperField m_requiredTrades = new UpperField( 5);
		private UpperField m_subject = new UpperField( 27);
		private JTextArea m_emailText = new JTextArea( 10, 50);
	
		CryptoPanel() {
			super( new BorderLayout() );

			VerticalPanel vp = new VerticalPanel();
			vp.addHeader( "Crypto");
			vp.add( "RUSD", m_rusd);
			vp.add( "USDT", m_busd);
			vp.add( "Approved", m_approved);
			vp.add( "MATIC", m_matic);
			vp.add( "Locked", m_locked);

			vp.addHeader( "Operations");
			vp.add( "Set Verified", new HtmlButton( "Set KYC to Verified", ev -> setVerified() ) );
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

			vp.add( "Give MATIC", new HtmlButton("Transfer .01 MATIC from Owner to this wallet", e -> giveMatic() ) );

			vp.addHeader( "Send Email");
			vp.add( "Subject", m_subject, new HtmlButton("Send", e -> sendEmail() ) );
			vp.add( "Text", new JScrollPane( m_emailText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) );

			add( vp);
		}

		@Override public void activated() {
			MyClient.getJson(Monitor.refApiBaseUrl() + "/api/mywallet/" + m_wallet, obj -> {
				JsonArray ar = obj.getArray("tokens");
				Util.require( ar.size() == 3, "Invalid mywallet query results for wallet %s", m_wallet); 

				m_rusd.setText("" + S.formatPrice( ar.get(0).getDouble("balance")));
				m_busd.setText("" + S.formatPrice( ar.get(1).getDouble("balance")));
				m_approved.setText("" + S.formatPrice( ar.get(1).getDouble("approvedBalance")));
				m_matic.setText( S.fmt4( ar.get(2).getDouble("balance")) );			
			});
		}

		protected void clear() {
			m_rusd.setText( null);
			m_busd.setText( null);
			m_approved.setText( null);
			m_matic.setText( null);
			m_locked.setText( null);
			m_mintAmt.setText( null);
			m_burnAmt.setText( null);
			m_awardAmt.setText( null);
			m_lockFor.setText( null);
			m_requiredTrades.setText( null);
			m_subject.setText( null);
			m_emailText.setText( null);
		}

		private void setVerified() {
			wrap( () -> {
				Util.reqValidAddress( m_wallet );

				if (Util.confirm( this, "Are you sure you want to set this user to VERIFIED?")) {
					Monitor.m_config.sqlCommand( sql -> 
					sql.execWithParams( "update users set kyc_status='VERIFIED' where wallet_public_key = '%s'", m_wallet.toLowerCase() ) );

					Util.inform( this, "Done");
				}
			});
		}

		private void award() {
			wrap( () -> {
				long lockUntil = System.currentTimeMillis() + m_lockFor.getLong() * Util.DAY;
				double amt = m_awardAmt.getDouble();
				String wallet = m_wallet;

				// mint and lock?
				if ( amt > 0) {
					if (Util.confirm(this, "Awarding %s RUSD for %s", amt, m_wallet ) ) {
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

		private void mint() {
			wrap( () -> {
				double amt = m_mintAmt.getDouble();
				if ( amt > 0 && Util.confirm(this, "Minting %s RUSD for %s", amt, m_wallet ) ) {
					if (amt > 100 && !Util.ask( "Enter password due to high amount").equals( "1359") ) {
						Util.inform( this, "The password was invalid");
						return;
					}

					mint( amt);
				}
			});
		}

		private void mint(double amt) throws Exception {
			Util.require( Util.isValidAddress(m_wallet), "Invalid wallet address");

			String hash = config().rusd().mintRusd( 
					m_wallet, amt, Monitor.stocks.getAnyStockToken() ).waitForHash();

			config().sqlCommand( sql -> sql.insertJson( "log", Util.toJson(
					"type", LogType.MINT,
					"wallet_public_key", m_wallet,
					"data", Util.toJson( "amt", amt) ) ) );

			UI.flash(hash);

			m_mintAmt.setText(null);
		}

		private void burn() {
			burn( m_burnAmt.getDouble() );
		}

		private void burnAllRusd() {
			wrap( () -> { 
				double amt = new Wallet( m_wallet ).getBalance( config().rusdAddr() );
				burn( amt);
			});
		}

		private void burn(double amt) {
			if ( amt > 0 && Util.confirm(this, "Burning %s RUSD from %s", amt, m_wallet ) ) {
				wrap( () -> {
					Util.require( Util.isValidAddress(m_wallet), "Invalid wallet address");

					String hash = config().rusd().burnRusd( 
							m_wallet, amt, Monitor.stocks.getAnyStockToken() ).waitForHash();

					config().sqlCommand( sql -> sql.insertJson( "log", Util.toJson(
							"type", LogType.BURN,
							"wallet_public_key", m_wallet,
							"data", Util.toJson( "amt", amt) ) ) );

					UI.flash(hash);

					m_burnAmt.setText(null);
				});
			}
		}
		
		private void giveMatic() {
			if (Util.confirm( this, 
						"Are you sure you want to transfer .01 MATIC from Owner to " + m_wallet ) ) {
				wrap( () -> {
					config().matic().transfer(
							config().ownerKey(),
							m_wallet, 
							.01); 
				});
			}
		}
		
		private void sendEmail() {
			wrap( () -> {
				JsonObject data = Util.toJson( 
						"subject", m_subject.getText(), 
						"text", m_emailText.getText() );
				
				config().sendEmail(
						m_emailAddr,
						m_subject.getText(),
						m_emailText.getText() );
				
				config().sqlCommand( sql -> sql.insertJson( "log", Util.toJson(
						"type", LogType.EMAIL,
						"wallet_public_key", m_wallet,
						"data", data) ) );
				
				UI.flash( "Message sent");

				m_subject.setText(null);
				m_emailText.setText(null);			
			});
		}

	}
	
	class LogPanel extends MiniTab {
		JsonModel model = new JsonModel( "created_at,type,uid,data");

		LogPanel() {
			super( new BorderLayout() );
			add( model.createTable() );
		}

		public void clear() {
			model.ar().clear();
			model.fireTableDataChanged();
		}
		
		@Override public void activated() {
			wrap( () -> {
				var ar = Monitor.m_config.sqlQuery( "select * from log where wallet_public_key = '%s'", m_wallet);
				model.setRows( ar);
				model.fireTableDataChanged();
			});
		}
		
	}

	/** Used by TestCase as well as Monitor */
	public static JsonObject createLockObject( String wallet, double amt, long lockUntil, int requiredTrades) throws Exception {
		return Util.toJson( 
				"wallet_public_key", wallet, 
				"locked", Util.toJson( "amount", amt, "lockedUntil", lockUntil, "requiredTrades", requiredTrades) );
	}

	static class MyPanel extends JPanel {
		MyPanel() {
			//		super( new FlowLayout( FlowLayout.LEFT));
		}

		MyPanel( LayoutManager mgr) {
			super( mgr);
		}

		void horz( int width) {
			add( Box.createHorizontalStrut(width));
		}

		void label( String str) {
			add( new JLabel( str) );
		}
	}


	static class LeftFlow extends MyPanel {
		LeftFlow() {
			super( new FlowLayout( FlowLayout.LEFT) );
		}
	}
	
	private void doubleIt(JsonObject rec) throws Exception {
		String symbol = rec.getString("Symbol");
		double amt = rec.getDouble( "Balance");
		StockToken tok = rec.getStock( "stock").getToken();
		
		Util.reqValidAddress(m_wallet);
		
		if (Util.confirm( this, String.format("You will mint %s %s for %s", amt, symbol, m_wallet) ) ) {
			config().rusd().mintStockToken( m_wallet, tok, amt).waitForHash();
			Util.inform( this, "Done");
		}
	}
	
	static abstract class MiniTab extends JPanel implements INewTab {
		public MiniTab(LayoutManager layout) {
			super( layout);
		}
		abstract protected void clear();

		@Override public void closed() {
		}

		@Override public void switchTo() {
		}
	}
}

	// add the missing fields, reformat the user data and persona data