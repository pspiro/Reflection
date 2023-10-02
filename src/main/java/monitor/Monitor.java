package monitor;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;
import reflection.Stock;
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
	static final String base = "https://reflection.trading";
	static final String chain = "goerli";  // or eth
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final Stocks stocks = new Stocks();
	static final Config m_config = new Config();

	static Monitor m;
	static JTextField num = new JTextField(4); // number of entries to return in query
	JFrame m_frame = new JFrame();
	NewTabbedPanel m_tabs = new NewTabbedPanel(true);
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new Exception( "You must specify a config tab name");
		}

		NewLookAndFeel.register();
		m = new Monitor( "Dt-config");
	}
	
	Monitor(String tabName) throws Exception {
		// read config
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		S.out( "  done");
		
		// read stocks
		S.out( "Reading stock list from google sheet");
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), Monitor.m_config);
		S.out( "  done");

		TokensPanel m_tokensPanel = new TokensPanel();
		PricesPanel m_pricesPanel = new PricesPanel();
		

		
		JButton but = new JButton("Refresh");
		but.addActionListener( e -> refresh() );
		num.addActionListener( e -> refresh() );
		
		JPanel butPanel = new JPanel();
		butPanel.add(but);
		butPanel.add(Box.createHorizontalStrut(5));
		butPanel.add(num);
		
		num.setText("40");
		
		m_tabs.addTab( "Home", new HomePanel() );
		m_tabs.addTab( "Misc", new MiscPanel() );
		m_tabs.addTab( "Users", createUsersPanel() );
		m_tabs.addTab( "Signup", createSignupPanel() );
		m_tabs.addTab( "Wallet", new WalletPanel() );
		m_tabs.addTab( "Transactions", createTransPanel() );
		m_tabs.addTab( "Log", createLogPanel() );
		m_tabs.addTab( "Trades", createTradesPanel() );
		m_tabs.addTab( "Tokens", new TokensPanel() );
		m_tabs.addTab( "Prices", m_pricesPanel);
		m_tabs.addTab( "Redis", new RedisPanel() );
		m_tabs.addTab( "Redemptions", new RedemptionPanel() );
		m_tabs.addTab( "Live orders", new LiveOrdersPanel() );
		
		m_frame.add( butPanel, BorderLayout.NORTH);
		m_frame.add( m_tabs);
		m_frame.add( new SouthPanel(), BorderLayout.SOUTH);
		
		m_frame.setTitle( "Reflection Monitor");
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setSize( 1000, 600);
		m_frame.setVisible(true);
		
		m_pricesPanel.initialize();
	}
	
	static int num() {
		return (int)S.parseDouble( num.getText() );
	}
	
	/** called when Refresh button is clicked */
	void refresh() {
		try {
			((RefPanel)m_tabs.current()).refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	interface RefPanel extends INewTab {
		void refresh() throws Exception; // called when Refresh button is clicked
	}

	private JComponent createTransPanel() {
		String names = "created_at,wallet_public_key,uid,action,quantity,conid,symbol,price,status,tds,rounded_quantity,order_id,perm_id,fireblocks_id,blockchain_hash,commission,currency,cumfill,side,avgprice,exchange,time,tradekey";
		// String sql = "select * from transactions ct left join trades tr on ct.order_id = tr.order_id order by ct.created_at desc limit 50";
		String sql = "select * from transactions $where order by created_at desc $limit";
		return new QueryPanel( names, sql) {
			public void adjust(JsonObject obj) {
				obj.putIf( "symbol", stocks.getStock( obj.getInt("conid") ) );
			}
		};
	}

	private JComponent createLogPanel() {
		String names = "created_at,wallet_public_key,uid,type,data"; 
		String sql = "select * from log $where order by created_at desc $limit";
		return new QueryPanel( names, sql);
	}

	// add the commission here as well
	private JComponent createTradesPanel() {
		String names = "created_at,tradekey,order_id,perm_id,time,side,quantity,symbol,price,cumfill,conid,exchange,avgprice,orderref";
		String sql = "select * from trades $where order by created_at desc $limit";
		return new QueryPanel( names, sql);
	}

	static QueryPanel createUsersPanel() {
		String names = "created_at,wallet_public_key,first_name,last_name,email,phone,aadhaar,address,city,country,id,kyc_status,pan_number,persona_response,updated_at";
		String sql = "select * from users $where";
		return new QueryPanel( names, sql);
	}

	static QueryPanel createSignupPanel() {
		String names = "created_at,name,email,phone,wallet_public_key";
		String sql = "select * from signup $where";
		return new QueryPanel( names, sql);
	}

	// move to Util?
	
	interface MyConsumer<T> {
		void accept(T param) throws Exception;
	}
	
	static void queryObj(String endpoint, MyConsumer<JsonObject> cli) {
		AsyncHttpClient client = new DefaultAsyncHttpClient();
		client.prepare("GET", Monitor.base + endpoint)
			.execute()
			.toCompletableFuture()
			.thenAccept( resp -> {
				Util.wrap( () -> {
					client.close();
					cli.accept( JsonObject.parse( resp.getResponseBody() ) );
				});
			});
	}
	
	static void queryArray(String endpoint, MyConsumer<JsonArray> cli) {
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client.prepare("GET", Monitor.base + endpoint)
			.execute()
			.toCompletableFuture()
			.thenAccept( resp -> {
				Util.wrap( () -> {
					client.close();
					cli.accept( JsonArray.parse( resp.getResponseBody() ) );
				});
			});
	}
	
	static class HomePanel extends JPanel implements RefPanel {
		@Override public void activated() {
		}
		@Override public void closed() {
		}
		@Override public void refresh() throws Exception {
		}
	}
	
}
