package monitor;

import static monitor.Monitor.m_config;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import common.LogType;
import common.MyTimer;
import common.Util;
import http.MyClient;
import monitor.AnyQueryPanel.MyComboBox;
import monitor.wallet.BlockDetailPanel;
import monitor.wallet.BlockPanelBase;
import monitor.wallet.BlockSummaryPanel;
import onramp.Onramp;
import onramp.Onramp.KycStatus;
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
import web3.Busd;
import web3.NodeInstance;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;
import web3.Rusd;
import web3.StockToken;

public class WalletPanel extends MonPanel {
	static String usersFields = "first_name,last_name,wallet_public_key,kyc_status,created_at,address,address_1,address_2,city,state,zip,country,geo_code,email,telegram,phone,pan_number,aadhaar,locked";

	HashMap<String,String> walletMap = new HashMap<>(); // map wallet address (lower case) to wallet name
	
	private JTextField m_walField = new JTextField(28);
	private String m_wallet;  // trimmed, lower case
	private UserPanel dataPanel = new UserPanel();
	private BlockchainPanel blockchainPanel = new BlockchainPanel();
	private TransWalletPanel transPanel = new TransWalletPanel();
	private CryptoPanel cryptoPanel = new CryptoPanel();
	private TokPanel tokPanel = new TokPanel();
	private LogPanel logPanel = new LogPanel();
	private OnrampUserPanel onrampPanel = new OnrampUserPanel();
	private String m_emailAddr;

	private final NewTabbedPanel m_tabs = new NewTabbedPanel(true);

	public WalletPanel() {
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
			Util.browse( chain().browseAddress( m_wallet) );
		}));
		top.add( new HtmlButton( "Spiro wallet", ev -> m_walField.setText( NodeInstance.prod) ) );

		m_tabs.addTab( "User Data", dataPanel); 
		m_tabs.addTab( "Blockchain", blockchainPanel); 
		m_tabs.addTab( "Transactions", transPanel); 
		m_tabs.addTab( "Crypto", cryptoPanel); 
		m_tabs.addTab( "Tokens", tokPanel); 
		m_tabs.addTab( "Log", logPanel);
		m_tabs.addTab( "OnRamp", onrampPanel);

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
			dataPanel.clear();
			blockchainPanel.clear();
			transPanel.clear();
			cryptoPanel.clear();
			tokPanel.clear();
			logPanel.clear();

			m_wallet = m_walField.getText().trim().toLowerCase();
			Util.require( S.isNull( m_wallet) || Util.isValidAddress(m_wallet), "Invalid wallet address");
	
			if (Util.isValidAddress( m_wallet) ) {
				m_tabs.resetActivated();  // set activated flag to false
				m_tabs.reactivateCurrent();  // this will trigger call to activated() on the current tab
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
			usersPane.setText( null);
			personaPane.setText( null);
		}
		
		@Override public void activated() {
			wrap( () -> {
				if (S.isNull( m_wallet) ) {
					return;
				}
				
				if (!Util.isValidAddress(m_wallet)) {
					Util.inform( this, "Wallet address is invalid");
					return;
				}
				
				var users = m_config.sqlQuery("select * from users where wallet_public_key = '%s'", m_wallet);
				if (users.size() > 0) {
					var user = users.get( 0).removeEntry( "persona_responsa");
					String html = user.toHtml( true, usersFields.split( ","));
					S.out( html);
					usersPane.setText( html);
	
					personaResp = user.getObjectNN( "persona_response");
					
					var pers = getFullPersona( personaResp);
					personaPane.setText( pers.toHtml( true) );
					
					m_emailAddr = user.getString( "email"); // needed to send emails
				}
				else {
					Util.inform( this, "No entry in users table for this wallet");
					usersPane.setText( null);
					personaPane.setText( null);
				}
			});
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
		private BlockDetailPanel m_allTransPanel = new BlockDetailPanel();
		private BlockSummaryPanel m_sumPanel = new BlockSummaryPanel();

		BlockchainPanel() {
			super( new BorderLayout() );

			m_allTransPanel.setBorder( new TitledBorder( "All Transactions (Ctrl+click on 'Block' to see timestamp)"));
			m_sumPanel.setBorder( new TitledBorder( "Consolidated Transactions"));

			DualPanel dualPanel = new DualPanel();
			dualPanel.add( m_allTransPanel, "2");
			dualPanel.add( m_sumPanel, "1");

			add( dualPanel);
		}

		@Override public void activated() {
			wrap( () -> {
				walletMap.clear();
				walletMap.put( m_config.chain().params().refWalletAddr().toLowerCase(), BlockPanelBase.RefWallet);
				walletMap.put( m_config.chain().params().admin1Addr().toLowerCase(), "Admin1");
				walletMap.put( m_config.chain().params().sysAdminAddr().toLowerCase(), "Sys Admin");
				walletMap.put( m_config.chain().params().ownerAddr().toLowerCase(), "Owner");
				walletMap.put( NodeInstance.prod, "My prod wallet");
				walletMap.put( NodeInstance.nullAddr, BlockPanelBase.nullAddr);
				walletMap.put( m_wallet, BlockPanelBase.Me);

				// get all relevant transfers
				var transfers = chain().getWalletTransfers( m_wallet, Monitor.chain().getAllAddresses() );

				// build new list with substitutions
				var altered = new Transfers();
				for (var transfer : transfers) altered.add( adjust( transfer) );

				// filter based on contract and transfer size
				// not necessary anymore, only necessary if using Moralis
				// weCare( ts) and amount() 

				m_allTransPanel.refresh( m_wallet, altered);
				m_sumPanel.refresh( m_wallet, altered);
			});
		}

		private Transfer adjust(Transfer t) throws Exception {
			String from = Util.valOr( walletMap.get( t.from() ), t.from() ); 
			String to = Util.valOr( walletMap.get( t.to() ), t.to() );

			String contract = t.contract();
			var token = Monitor.chain().getTokenByAddress(contract);
			if (token != null) {
				contract = token.name();
			}
			else if (contract.equalsIgnoreCase( m_config.chain().rusd().address() ) ) {
				contract = m_config.rusd().name();
			}
			else if (contract.equalsIgnoreCase( m_config.chain().busd().address() ) ) {
				contract = m_config.busd().name();
			}
			if (contract.startsWith( "0xc21")) {
				S.out( "yes");
			}
			
			return new Transfer( contract, from, to, t.amount(), t.block(), t.hash(), t.timestamp() );  
		}

		protected void clear() {
			try {
				m_allTransPanel.refresh( "", null);
//				m_sumPanel.refresh( "", new JsonArray() );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class TransWalletPanel extends MiniTab {
		TransPanel transPanel = new TransPanel();
		RedemptionPanel redemPanel = new RedemptionPanel();

		TransWalletPanel() {
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
				m_config.sqlCommand( sql -> {
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
		private static final double minBalance = .0001;
		private final JsonModel posModel = new PosModel();
		UpperField m_rusd = new UpperField();
		UpperField m_busd = new UpperField();
		UpperField m_busdAppr = new UpperField();
		UpperField m_stock = new UpperField();
		UpperField m_total = new UpperField();
		
		TokPanel() {
			super( new BorderLayout() );
			
			VerticalPanel p = new VerticalPanel();
			p.add( "RUSD balance", m_rusd);
			p.add( chain().busd().name() + " balance", m_busd);
			p.add( chain().busd().name() + " approved", m_busdAppr);
			p.add( "Stock value", m_stock);
			p.addVSpace( 10);
			p.add( "Total", m_total);
			p.addVSpace( 20);
			
			add( p, BorderLayout.NORTH);
			add( posModel.createTable() );
		}

		class PosModel extends JsonModel {
			public PosModel() {
				super("Symbol,Balance,Price,Value");
				justify("lrrr");
			}
		}

		public void clear() {
			m_rusd.setText(null);
			m_stock.setText(null);
			m_total.setText(null);
			posModel.ar().clear();
			posModel.fireTableDataChanged();
		}

		@Override public void activated() {
			wrap( () -> {
				double rusdBal = m_config.rusd().getPosition( m_wallet);
				m_rusd.setText( fmt( rusdBal) );

				m_busd.setText( fmt( 
						m_config.busd().getPosition( m_wallet) ) );
				
				m_busdAppr.setText( fmt( 
						m_config.busd().getAllowance( m_wallet, m_config.rusd().address() ) ) );
				
				var prices = MyClient.getArray( m_config.mdBaseUrl() + "/mdserver/get-ref-prices");
				
				HashMap<String, Double> posMap = m_config.node().reqPositionsMap(m_wallet, Monitor.chain().getAllContractsAddresses(), StockToken.stockTokenDecimals);
				
				double stockBal = 0;
				
				for (var token : Monitor.tokens() ) {
					double bal = Util.toDouble( posMap.get( token.address().toLowerCase() ) );
					if (bal > minBalance) {
						JsonObject obj = new JsonObject();
						obj.put( "Symbol", token.name() );
						obj.put( "Balance", bal);
						obj.put( "stock", token);
						
						var price = getPrice( prices, token.conid() );
						if (price != null) {
							obj.put( "Price", fmt( price.getDouble( "last") ) );
							
							double val = price.getDouble( "last") * bal;
							obj.put( "Value", fmt( val) );
							
							stockBal += val;
						}
						posModel.ar().add(obj);
					}
				}
				posModel.fireTableDataChanged();
				
				m_stock.setText( fmt( stockBal) );
				m_total.setText( fmt( rusdBal + stockBal) );
			});
		}

		private String fmt(double d) {
			return S.fmt2c( d);
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
		final int wid = 10; 

		private UpperField m_rusd = new UpperField( wid);
		private UpperField m_busd = new UpperField( wid);
		private UpperField m_approved = new UpperField( wid);
		private UpperField m_nativeAmt = new UpperField( wid);
		private UpperField m_locked = new UpperField( wid);
		private UpperField m_mintAmt = new UpperField( wid);
		private UpperField m_burnAmt = new UpperField( wid);
		private UpperField m_awardAmt = new UpperField( wid);
		private UpperField m_lockFor = new UpperField( 7);
		private UpperField m_requiredTrades = new UpperField( 5);
		private JTextField m_subject = new JTextField( 27);
		private JLabel m_nativeName = new JLabel( "POL");
		private JTextArea m_emailText = new JTextArea( 10, 50);
	
		CryptoPanel() {
			super( new BorderLayout() );
			

			VerticalPanel vp = new VerticalPanel();
			vp.addHeader( "Crypto");
			vp.add( "RUSD", m_rusd);
			vp.add( m_config.busd().name(), m_busd);
			vp.add( "Approved", m_approved);
			vp.add( Util.toArray( m_nativeName, m_nativeAmt) );
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

			vp.add( "Give " + config().nativeTokName(), new HtmlButton("Transfer .01 " + config().nativeTokName() + " from Owner to this wallet", e -> giveMatic() ) );
			vp.add( "Redeem RUSD", new HtmlButton( "Redeem all RUSD", ev -> redeemAll() ) );

			vp.addHeader( "Send Email");
			vp.add( "Subject", m_subject, new HtmlButton("Send", e -> sendEmail() ) );
			vp.add( "Text", new JScrollPane( m_emailText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) );

			add( vp);
		}

		private void redeemAll() {
			wrap( () -> {
				Rusd rusd = Monitor.m_config.rusd();
				Busd busd = Monitor.m_config.busd();
	
				// nothing to redeem?
				double rusdPos = rusd.getPosition(m_wallet);  // make sure that rounded amt is not slightly more or less
				if (rusdPos < .005) {
					Util.inform(this, "User has no RUSD to redeem");
					return;
				}
	
				// insufficient stablecoin in RefWallet?
				double ourStablePos = busd.getPosition( Monitor.m_config.refWalletAddr() );
				if (ourStablePos < rusdPos) {
					String str = String.format( 
							"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
							m_wallet, rusdPos, ourStablePos, (rusdPos - ourStablePos) );
					Util.inform( this, str);
					return;
				}
	
				// confirm
				if (!Util.confirm(
						this, 
						String.format("Are you sure you want to redeem %s RUSD for %s?",
								RedemptionPanel.six.format(rusdPos), m_wallet) ) ) {
					return;
				}
	
				// don't tie up the UI thread
				Util.executeAndWrap( () -> {
					String hash = rusd.sellRusd(m_wallet, busd, rusdPos)
							.waitForReceipt();
	
					// insert into redemptions table in DB and screen
					JsonObject obj = Util.toJson( 
							"uid", Util.uid(8),
							"wallet_public_key", m_wallet.toLowerCase(),
							 "blockchain_hash", hash,
							 "status", "Completed",
							 "stablecoin", chain().busd().name(),
							 "amount", rusdPos,
							 "chainid", chain().chainId() );  
							
					Monitor.m_config.sqlCommand( conn -> conn.insertJson( "redemptions",  obj) );
	
					Util.inform( this, "Completed");
				});
			});
		}

		@Override public void activated() {
			MyClient.postToJson(
					Monitor.refApiBaseUrl() + "/api/mywallet/" + m_wallet,
					Util.toJson( "chainId", m_config.chain().chainId() ).toString(),
					obj -> {
				JsonArray ar = obj.getArray("tokens");
				Util.require( ar.size() == 3, "Invalid mywallet query results for wallet %s", m_wallet); 

				m_rusd.setText("" + S.formatPrice( ar.get(0).getDouble("balance")));
				m_busd.setText("" + S.formatPrice( ar.get(1).getDouble("balance")));
				m_approved.setText("" + S.formatPrice( ar.get(1).getDouble("approvedBalance")));
				m_nativeAmt.setText( S.fmt4( ar.get(2).getDouble("balance")) );			
			});
		}

		protected void clear() {
			m_rusd.setText( null);
			m_busd.setText( null);
			m_approved.setText( null);
			m_nativeAmt.setText( null);
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
					m_config.sqlCommand( sql -> 
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
					m_wallet, amt, Monitor.chain().getAnyStockToken() ).waitForReceipt();

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
				double amt = config().rusd().getPosition( m_wallet);
				if (amt > 0) {
					burn( amt);
				}
				else {
					Util.inform( this, "No RUSD to burn");
				}
			});
		}

		private void burn(double amt) {
			if ( amt > 0 && Util.confirm(this, "Burning %s RUSD from %s", amt, m_wallet ) ) {
				wrap( () -> {
					Util.require( Util.isValidAddress(m_wallet), "Invalid wallet address");

					String hash = config().rusd().burnRusd( 
							m_wallet, amt, Monitor.chain().getAnyStockToken() ).waitForReceipt();

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
					config().chain().blocks().transfer(
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
		JsonModel model = new JsonModel( "created_at,type,chainid,uid,data");

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
				var ar = m_config.sqlQuery( "select * from log where wallet_public_key = '%s'", m_wallet);
				model.setRows( ar);
				model.fireTableDataChanged();
			});
		}
		
	}
	
	class OnrampUserPanel extends MiniTab {
		int size = 13;
		private JTextField m_phone = new JTextField( size);
		private JTextField m_id = new JTextField( size);
		private JTextField m_kycStatus = new JTextField( size + 5);
		private MyComboBox m_kycCombo = new MyComboBox(KycStatus.values() );
		private JsonModel m_logModel = new JsonModel( "created_at,type,currency,buyAmt,recAmt,text");
		private JsonModel m_dbModel = new JsonModel( "created_at,fiat_amount,crypto_amount,hash,state,uid");
		
		private JsonModel m_apiModel = new JsonModel( "phone,onramp_id") {
			@Override protected Object format(String key, Object value) {
				if (key.equals( "createdAt") ) {
					return Util.left( value.toString(), 19).replace( "T", " ");
				}
				return super.format(key, value);
			}

		};
		
		OnrampUserPanel() {
			super( new BorderLayout() );
			
			VerticalPanel p = new VerticalPanel();
			p.add( "Phone", m_phone, new HtmlButton( "Update", this::updatePhone), 
					new JLabel("(Required format is: XX-XXXXXXXXX)") );
			p.add( "OnRamp ID", m_id, new HtmlButton( "Update", this::updateId) );
			p.add( "KYC Status", m_kycStatus, m_kycCombo, new HtmlButton( "Set", this::setKyc) );
			add( p, BorderLayout.NORTH);
			
			DualPanel left = new DualPanel();
			left.add( m_apiModel.createTable( "OnRamp API Transactions"), "1");
			left.add( m_dbModel.createTable( "Database Onramp Table"), "2");
			
			add( left, BorderLayout.CENTER);
			add( m_logModel.createTable("Database Log Table"), BorderLayout.EAST);
		}
		
		void setKyc(ActionEvent e) {
			wrap( () -> { 
				Onramp.prodRamp.updateKycStatus( m_id.getText(), (KycStatus)m_kycCombo.getSelected() );
				
				m_kycStatus.setText( Onramp.prodRamp.getKycStatus( m_id.getText() ) );
				
				Util.inform( this, "Done");
			});
		}
		
		void updatePhone( ActionEvent e) {
			wrap( () -> {
				m_config.sqlCommand( sql -> sql.execWithParams(
						"update users set phone = '%s' where wallet_public_key = '%s'",
							m_phone.getText(), m_wallet.toLowerCase() ) );;
				Util.inform( this, "Done");
			});
		}
		
		void updateId( ActionEvent e) {
			wrap( () -> {
				m_config.sqlCommand( sql -> sql.execWithParams(
						"update users set onramp_id = '%s' where wallet_public_key = '%s'",
							m_id.getText(), m_wallet.toLowerCase() ) );;
				Util.inform( this, "Done");
			});
		}
		
		@Override protected void clear() {
		}

		@Override public void activated() {
			wrap( () -> {				
				var users = m_config.sqlQuery("select phone, onramp_id from users where wallet_public_key = '%s'", m_wallet);
				if (users.size() > 0) {					
					var phone = users.get( 0).getString( "phone");
					m_phone.setText( phone);

					var id = users.get( 0).getString( "onramp_id");
					m_id.setText( id);
					
					if (S.isNotNull( id) ) {
						String status = Onramp.prodRamp.getKycStatus( id);
						m_kycStatus.setText( status);
						
						var trans = Onramp.prodRamp.getUserTransactions( id);
						m_apiModel.setNames( String.join( ",", trans.getKeys() ) );
						m_apiModel.fireTableStructureChanged();
	
						m_apiModel.setRows( trans);
						m_apiModel.fireTableDataChanged();
					}
				}
				
				// read log entries
				var logs = m_config.sqlQuery( "select * from log where wallet_public_key = '%s' and type = '%s'",
						m_wallet, LogType.ONRAMP);

				// update log record with data from the json field
				var data = new JsonArray();
				for (var log : logs) {
					data.add( log
							.getObject( "data")
							.append( "created_at", log.getString("created_at").substring( 0, 19) ) );
				}
				
				m_logModel.setRows( data);
				m_logModel.fireTableDataChanged();
				
				var dbs = m_config.sqlQuery( "select * from onramp where wallet_public_key = '%s'", m_wallet);
				m_dbModel.setRows( dbs);
				m_dbModel.fireTableDataChanged();
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

	public static JsonObject getFullPersona(JsonObject personaResp) throws Exception {
		JsonObject resp = new JsonObject();
		resp.copyFrom( personaResp, "inquiryId", "status");
		
		personaResp.getObjectNN( "fields").forEach( (key,val) -> {
			try {
				JsonObject valObj = JsonObject.parse( val.toString() );
				resp.putIf( key, valObj.getString( "value"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return resp; 
	}
}

	// add the missing fields, reformat the user data and persona data