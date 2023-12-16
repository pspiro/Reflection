package monitor;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class Monitor {
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final Stocks stocks = new Stocks();

	static MonitorConfig m_config;
	static MyRedis m_redis;
	static NewTabbedPanel m_tabs;
	static LogPanel m_logPanel;
	static WalletPanel m_walletPanel;
	static JTextField num;
	static JFrame m_frame;
	
	static String refApiBaseUrl() { 
		return m_config.baseUrl();
	}

	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("Monitor");		
		NewLookAndFeel.register();
		start();
	}
	
	private static void start() throws Exception {
		// read config
		m_config = MonitorConfig.ask();

		num = new JTextField(4); // number of entries to return in query
		m_frame = new JFrame();
		m_tabs = new NewTabbedPanel(true);
		m_logPanel = new LogPanel();
		m_walletPanel = new WalletPanel();
		
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
		m_tabs.addTab( "Users", new UsersPanel() );
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
		m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		m_frame.setSize( 1100, 800);
		m_frame.setVisible(true);
		
		m_frame.addWindowListener(new WindowAdapter() {
		    public void windowClosed(WindowEvent e) {
		    	Util.execute( () -> Util.wrap( () -> start() ) );
		    }
		});
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
					if ( S.isNotNull(val) ) {
						JsonObject obj = JsonObject.parse(val);
						obj.update( "filter", cookie -> Util.left(cookie.toString(), 40) ); // shorten the cookie or it pollutes the view
						return obj.toHtml();
					}
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
		String names = "created_at,time,wallet_public_key,orderref,side,quantity,symbol,conid,price,cumfill,tradekey,perm_id,order_id,exchange,avgprice";
		String sql = """
				select trades.*, transactions.wallet_public_key
				from trades
				left join transactions
				on trades.orderref = transactions.uid
				$where
				order by created_at
				desc $limit
				""";
		 
		return new QueryPanel( "trades", names, sql);
	}

	static class UsersPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,first_name,last_name,email,phone,aadhaar,address,city,country,id,kyc_status,pan_number,persona_response,updated_at";
		static String sql = "select * from users $where";
		
		UsersPanel() {
			super( "users", names, sql);
		}
		
		@Override
		void onDouble(String tag, Object val) {
			if (S.notNull(tag).equals("wallet_public_key") ) {
				m_tabs.select("Wallet");
				m_walletPanel.setWallet(val.toString());
			}
		}
		
	}

	private static QueryPanel createSignupPanel() {
		String names = "created_at,name,email,phone,wallet_public_key";
		String sql = "select * from signup $where";
		return new QueryPanel( "signup", names, sql);
	}

	// move to Util?
	
	
	static abstract class MonPanel extends JPanel implements INewTab {
		public MonPanel(LayoutManager layout) {
			super(layout);
		}
		
		public void refresh() throws Exception {
		}
		
		@Override public void switchTo() {
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
