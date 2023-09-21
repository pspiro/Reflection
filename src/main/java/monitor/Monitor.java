package monitor;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;
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
	JFrame m_frame = new JFrame();
	NewTabbedPanel m_tabs = new NewTabbedPanel(true);
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new Exception( "You must specify a config tab name");
		}

		NewLookAndFeel.register();
		new Monitor( "Dt-config");
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
		
		JPanel butPanel = new JPanel();
		butPanel.add(but);
		
		m_tabs.addTab( "Home", new HomePanel() );
		m_tabs.addTab( "Misc", new MiscPanel() );
		m_tabs.addTab( "Users", createUsersPanel() );
		m_tabs.addTab( "Wallet", new WalletPanel() );
		m_tabs.addTab( "Transactions", new TransPanel() );
		m_tabs.addTab( "Log", new LogPanel() );
		m_tabs.addTab( "Trades", createTradesPanel() );
		m_tabs.addTab( "Tokens", m_tokensPanel);
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
		
		m_tokensPanel.initialize();
		m_pricesPanel.initialize();
	}
	
	/** called when Refresh button is clicked */
	private void refresh() {
		try {
			((RefPanel)m_tabs.current()).refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	interface RefPanel extends INewTab {
		void refresh() throws Exception; // called when Refresh button is clicked
	}

	// add the commission here as well
	private JComponent createTradesPanel() {
		String names = "uid,action,quantity,conid,pricewallet_public_key,first_name,last_name,email,phone,aadhaar,address,city,country,created_at,id,kyc_status,pan_number,persona_response,updated_at";
		String sql = "select * from users limit 100";
		return new QueryPanel( names, sql);
	}

	static QueryPanel createUsersPanel() {
		String names = "wallet_public_key,first_name,last_name,email,phone,aadhaar,address,city,country,created_at,id,kyc_status,pan_number,persona_response,updated_at";
		String sql = "select * from users";
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
