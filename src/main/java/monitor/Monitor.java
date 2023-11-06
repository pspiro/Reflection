package monitor;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Transactions;
import http.MyClient;
import redis.MyRedis;
import reflection.Config;
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
	static String base = "https://reflection.trading";
	static final String mdsBase = base;
	//static final String mdsBase = "http://localhost:6999";
	static final String chain = "goerli";  // or eth
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final Stocks stocks = new Stocks();
	static Config m_config;

	static Monitor instance;
	static JTextField num = new JTextField(4); // number of entries to return in query
	static MyRedis m_redis;
	JFrame m_frame = new JFrame();
	NewTabbedPanel m_tabs = new NewTabbedPanel(true);
	LogPanel m_logPanel = new LogPanel();
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new Exception( "You must specify a config tab name");
		}

		NewLookAndFeel.register();

		// read config
		m_config = Config.ask();
		S.out( "Read %s tab from google spreadsheet %s", m_config.getTabName(), NewSheet.Reflection);
		
		instance = new Monitor();
	}
	
	Monitor() throws Exception {
		// read stocks
		S.out( "Reading stock list from google sheet");
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), Monitor.m_config);
		S.out( "  done");

		m_redis = Monitor.m_config.newRedis();
		Util.require( S.isNotNull(m_config.baseUrl()), "baseUrl setting missing from config");
		base = m_config.baseUrl();
		
		PricesPanel m_pricesPanel = new PricesPanel();
		
		// monitor won't work until the certificate is fixed
		
		
		JButton but = new JButton("Refresh");
		but.addActionListener( e -> refresh() );
		num.addActionListener( e -> refresh() );
		
		JPanel butPanel = new JPanel();
		butPanel.add(but);
		butPanel.add(Box.createHorizontalStrut(5));
		butPanel.add(num);
		
		num.setText("40");

		// disconnect redis when application is terminated
		m_frame.addWindowListener( new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				m_redis.disconnect();
			}
		});
		
		m_tabs.addTab( "Home", new MonPanel(new BorderLayout()) );
		m_tabs.addTab( "Status", new StatusPanel() );
		m_tabs.addTab( "Crypto", new CryptoPanel() );
		m_tabs.addTab( "Users", createUsersPanel() );
		m_tabs.addTab( "Signup", createSignupPanel() );
		m_tabs.addTab( "Wallet", new WalletPanel() );
		m_tabs.addTab( "Transactions", new TransPanel() );
		m_tabs.addTab( "Trades", createTradesPanel() );
		m_tabs.addTab( "Log", m_logPanel);
		m_tabs.addTab( "Tokens", new TokensPanel() );
		m_tabs.addTab( "REF Prices", m_pricesPanel);
		m_tabs.addTab( "MDS Prices", new MdsPricesPanel() );
		m_tabs.addTab( "Redis", new RedisPanel() );
		m_tabs.addTab( "Redemptions", new RedemptionPanel() );
		m_tabs.addTab( "Live orders", new LiveOrdersPanel() );
		
		m_frame.add( butPanel, BorderLayout.NORTH);
		m_frame.add( m_tabs);
		m_frame.add( new SouthPanel(), BorderLayout.SOUTH);
		
		m_frame.setTitle( "Reflection System Monitor");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 1000, 800);
		m_frame.setVisible(true);
		
		
		m_pricesPanel.initialize();
		

	}
	
	static int num() {
		return (int)S.parseDouble( num.getText() );
	}
	
	/** called when Refresh button is clicked */
	void refresh() {
		try {
			((MonPanel)m_tabs.current()).refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class TransPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,uid,status,action,quantity,conid,symbol,price,tds,rounded_quantity,order_id,perm_id,fireblocks_id,blockchain_hash,commission,currency,cumfill,side,avgprice,exchange,time";
		static String sql = "select * from transactions $where order by created_at desc $limit";
		
		TransPanel() {
			super( "transactions", names, sql);
		}

		@Override void onDouble(String tag, Object val) {
			switch(tag) {
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
		
		public void adjust(JsonObject obj) {
			obj.putIf( "symbol", stocks.getStock( obj.getInt("conid") ) );
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
	}

	// add the commission here as well
	private JComponent createTradesPanel() {
		String names = "created_at,time,orderref,side,quantity,symbol,conid,price,cumfill,tradekey,perm_id,order_id,exchange,avgprice";
		String sql = "select * from trades $where order by created_at desc $limit";
		return new QueryPanel( "trades", names, sql);
	}

	static QueryPanel createUsersPanel() {
		String names = "created_at,wallet_public_key,first_name,last_name,email,phone,aadhaar,address,city,country,id,kyc_status,pan_number,persona_response,updated_at";
		String sql = "select * from users $where";
		return new QueryPanel( "users", names, sql);
	}

	static QueryPanel createSignupPanel() {
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
			
			add( p);
		}
		
		@Override public void refresh() throws Exception {
			long now = System.currentTimeMillis();

			MyClient.getJson( Monitor.base + "/api/status", json -> {
				f1.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
				f2.setText( json.getBool("TWS") ? "OK" : "ERROR" );
				f3.setText( json.getBool("IB") ? "OK" : "ERROR" );
				f4.setText( json.getTime("started", Util.yToS) );
				f4a.setText( json.getString("built") );
			});

			MyClient.getJson( Monitor.base + "/mdserver/status", json -> {
				f5.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
				f6.setText( json.getBool("TWS") ? "OK" : "ERROR" );
				f7.setText( json.getBool("IB") ? "OK" : "ERROR" );
				f8.setText( json.getTime("started", Util.yToS) );
			});
		}
	}
	
	static class MonPanel extends JPanel implements INewTab {
		public MonPanel(LayoutManager layout) {
			super(layout);
		}

		@Override public void activated() {
			Util.wrap( () -> refresh() );
		}

		@Override public void closed() {
		}
		
		public void refresh() throws Exception {
		}
	}
	
}
