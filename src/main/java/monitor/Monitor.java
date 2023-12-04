package monitor;

import java.awt.BorderLayout;
import java.awt.LayoutManager;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Transactions;
import http.MyClient;
import redis.MyRedis;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.NewLookAndFeel;
import tw.util.NewTabbedPanel;
import tw.util.NewTabbedPanel.INewTab;
import tw.util.S;
import tw.util.VerticalPanel;

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class Monitor {
	static MonitorConfig m_config;
	static MyRedis m_redis;
	static NewTabbedPanel m_tabs;
	static LogPanel m_logPanel;
	static WalletPanel m_walletPanel;
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final Stocks stocks = new Stocks();
	static final JTextField num = new JTextField(4); // number of entries to return in query
	static final JFrame m_frame = new JFrame();
	
	static String refApiBaseUrl() { 
		return m_config.baseUrl();
	}

	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Monitor");
		
		NewLookAndFeel.register();
		
		m_tabs = new NewTabbedPanel(true);
		m_logPanel = new LogPanel();
		m_walletPanel = new WalletPanel();
		
		// read config
		m_config = MonitorConfig.ask();
		m_config.useExteranDbUrl();
		S.out( "Read %s tab from google spreadsheet %s", m_config.getTabName(), NewSheet.Reflection);
		S.out( "Using database %s", m_config.postgresUrl() );

		// read stocks
		S.out( "Reading stock list from google sheet");
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), Monitor.m_config);
		S.out( "  done");

		Util.require( S.isNotNull(m_config.baseUrl()), "baseUrl setting missing from config");
		
		PricesPanel pricesPanel = new PricesPanel();
		
		JButton but = new JButton("Refresh");
		but.addActionListener( e -> refresh() );
		num.addActionListener( e -> refresh() );
		
		JButton but2 = new JButton("Refresh Config");
		but2.addActionListener( e -> refreshConfig() );
		
		JPanel butPanel = new JPanel();
		butPanel.add(new JLabel(refApiBaseUrl() ) );
		butPanel.add(Box.createHorizontalStrut(5));
		butPanel.add(but);
		butPanel.add(Box.createHorizontalStrut(5));
		butPanel.add(num);
		butPanel.add(Box.createHorizontalStrut(15));
		butPanel.add(but2);
		
		num.setText("40");

		m_tabs.addTab( "Home", new EmptyPanel(new BorderLayout()) );
		m_tabs.addTab( "Status", new StatusPanel() );
		m_tabs.addTab( "Crypto", new CryptoPanel() );
		m_tabs.addTab( "Users", createUsersPanel() );
		m_tabs.addTab( "Signup", createSignupPanel() );
		m_tabs.addTab( "Wallet", m_walletPanel);
		m_tabs.addTab( "Transactions", new TransPanel() );
		m_tabs.addTab( "Trades", createTradesPanel() );
		m_tabs.addTab( "Log", m_logPanel);
		m_tabs.addTab( "Tokens", new TokensPanel() );
		m_tabs.addTab( "MDServer Prices", new MdsPricesPanel() );
		m_tabs.addTab( "RefAPI Prices", pricesPanel);
		m_tabs.addTab( "Redemptions", new RedemptionPanel() );
		m_tabs.addTab( "Live orders", new LiveOrdersPanel() );
		
		m_frame.add( butPanel, BorderLayout.NORTH);
		m_frame.add( m_tabs);
		m_frame.add( new SouthPanel(), BorderLayout.SOUTH);
		
		m_frame.setTitle( String.format( 
				"Reflection System Monitor - %s - %s", 
				m_config.getTabName(), 
				refApiBaseUrl() ) );
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 1100, 800);
		m_frame.setVisible(true);
	}
	
	private static void refreshConfig() {
		Util.wrap( () -> S.inform( 
					m_frame,
					MyClient.getJson(refApiBaseUrl() + "/api/?msg=refreshconfig").toString() ) );
	}

	static int num() {
		return (int)S.parseDouble( num.getText() );
	}
	
	/** called when Refresh button is clicked */
	static void refresh() {
		try {
			((MonPanel)m_tabs.current()).refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static class TransPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,uid,status,action,quantity,conid,symbol,price,tds,rounded_quantity,order_id,perm_id,fireblocks_id,blockchain_hash,commission,currency,cumfill,side,avgprice,exchange,time";
		static String sql = "select * from transactions $where order by created_at desc $limit";
		
		TransPanel() {
			super( "transactions", names, sql);
		}

		@Override void onDouble(String tag, Object val) {
			switch(tag) {
			case "wallet_public_key":
				m_tabs.select( "Wallet");
				m_walletPanel.filter( val.toString() );
				break;
			case "uid":
				m_tabs.select( "Log");
				m_logPanel.filterByUid(val.toString());
				break;
			case "fireblocks_id":
				Util.wrap( () -> Transactions.getTransaction(val.toString()).display() );
				break;
			case "blockchain_hash":
				// show in explorer
				break;
				default:
					super.onDouble(tag, val);
			}
		}
	}
	
	static class LogPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,uid,type,data"; 
		static String sql = "select * from log $where order by created_at desc $limit";

		LogPanel() {
			super( "log", names, sql);
		}
		
		void filterByUid( String uid) {
			where.setText( String.format( "where uid = '%s'", uid) );
			Util.wrap( () -> refresh() );
		}
		
		@Override protected String getTooltip(int row, String tag) {
			try {
				if (tag.equals("data") ) {
					String val = m_model.m_ar.get(row).getString(tag);
					return S.isNotNull(val) ? JsonObject.parse(val).toHtml() : null;
				}
			}
			catch( Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	// add the commission here as well
	private static JComponent createTradesPanel() {
		String names = "created_at,time,orderref,side,quantity,symbol,conid,price,cumfill,tradekey,perm_id,order_id,exchange,avgprice";
		String sql = "select * from trades $where order by created_at desc $limit";
		return new QueryPanel( "trades", names, sql);
	}

	private static QueryPanel createUsersPanel() {
		String names = "created_at,wallet_public_key,first_name,last_name,email,phone,aadhaar,address,city,country,id,kyc_status,pan_number,persona_response,updated_at";
		String sql = "select * from users $where";
		return new QueryPanel( "users", names, sql);
	}

	private static QueryPanel createSignupPanel() {
		String names = "created_at,name,email,phone,wallet_public_key";
		String sql = "select * from signup $where";
		return new QueryPanel( "signup", names, sql);
	}

	// move to Util?
	
	
	static class StatusPanel extends MonPanel {
		JTextField f1 = new JTextField(7);
		JTextField f2 = new JTextField(7);
		JTextField f3 = new JTextField(7);
		JTextField f4 = new JTextField(14);
		JTextField f4a = new JTextField(14);
		JTextField f5 = new JTextField(7);
		JTextField f6 = new JTextField(7);
		JTextField f7 = new JTextField(7);
		JTextField f8 = new JTextField(14);
		JTextField f10 = new JTextField(14);
		JTextField f11 = new JTextField(14);
		JTextField f12 = new JTextField(14);

		StatusPanel() {
			super( new BorderLayout() );
			
			VerticalPanel p = new VerticalPanel();
			p.add( "RefAPI", f1);
			p.add( "TWS", f2);
			p.add( "IB", f3);
			p.add( "Started", f4);
			p.add( "Built", f4a);
			p.add( Box.createVerticalStrut(10) );
			p.add( "MktData", f5);
			p.add( "TWS", f6);
			p.add( "IB", f7);
			p.add( "Started", f8);
			p.add( Box.createVerticalStrut(10) );
			p.add( "FbServer", f10);
			p.add( "Started", f11);
			p.add( "Map size", f12);
			
			add( p);
		}
		
		@Override public void refresh() throws Exception {
			long now = System.currentTimeMillis();

			MyClient.getJson( m_config.baseUrl() + "/api/status", json -> {
				f1.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
				f2.setText( json.getBool("TWS") ? "OK" : "ERROR" );
				f3.setText( json.getBool("IB") ? "OK" : "ERROR" );
				f4.setText( json.getTime("started", Util.yToS) );
				f4a.setText( json.getString("built") );
			});

			MyClient.getJson( m_config.mdBaseUrl() + "/mdserver/status", json -> {
				f5.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
				f6.setText( json.getBool("TWS") ? "OK" : "ERROR" );
				f7.setText( json.getBool("IB") ? "OK" : "ERROR" );
				f8.setText( json.getTime("started", Util.yToS) );
			});
			
			MyClient.getJson( m_config.fbBaseUrl() + "/fbserver/status", json -> {
				f10.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
				f11.setText( json.getTime("started", Util.yToS) );
				f12.setText( json.getString("mapSize").toString() );
			});
		}
	}
	
	static abstract class MonPanel extends JPanel implements INewTab {
		public MonPanel(LayoutManager layout) {
			super(layout);
		}

		@Override public void activated() {
		}

		@Override public void closed() {
		}
	}
	
	static class EmptyPanel extends MonPanel {
		EmptyPanel(LayoutManager layout) {
			super(layout);
		}
		
		@Override public void refresh() throws Exception {
		}
	}
}
