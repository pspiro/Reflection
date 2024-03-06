package monitor;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExRunnable;
import http.MyClient;
import redis.MyRedis;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.NewLookAndFeel;
import tw.util.NewTabbedPanel;
import tw.util.NewTabbedPanel.INewTab;
import tw.util.S;
import tw.util.UI;

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
	static SouthPanel m_southPanel;
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
		Util.iff( m_southPanel, pan -> pan.stop() );
		
		// read config
		m_config = MonitorConfig.ask();
		
		num = new JTextField(4); // number of entries to return in query
		m_frame = new JFrame();
		m_tabs = new NewTabbedPanel(true);
		m_logPanel = new LogPanel();
		m_walletPanel = new WalletPanel();
		m_southPanel = new SouthPanel();
		
		m_config.useExternalDbUrl();
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
		
		JPanel butPanel = new JPanel();
		butPanel.add(new JLabel(refApiBaseUrl() ) );
		butPanel.add(Box.createHorizontalStrut(5));
		butPanel.add(but);
		butPanel.add(Box.createHorizontalStrut(5));
		butPanel.add(num);
		
		num.setText("40");

		m_tabs.addTab( "Home", new EmptyPanel(new BorderLayout()) );
		m_tabs.addTab( "Status", new StatusPanel() );
		m_tabs.addTab( "Crypto", new CryptoPanel() );
		m_tabs.addTab( "Wallet", m_walletPanel);
		m_tabs.addTab( "Users", new UsersPanel() );
		m_tabs.addTab( "Transactions", new TransPanel() );
		m_tabs.addTab( "Trades", createTradesPanel() );
		m_tabs.addTab( "Log", m_logPanel);
		m_tabs.addTab( "Tokens", new TokensPanel() );
		m_tabs.addTab( "MDServer Prices", new MdsPricesPanel() );
		m_tabs.addTab( "RefAPI Prices", pricesPanel);
		m_tabs.addTab( "Redemptions", new RedemptionPanel() );
		m_tabs.addTab( "Live orders", new LiveOrdersPanel() );
		m_tabs.addTab( "HookServer", new HookServerPanel() );
		m_tabs.addTab( "FbServer", new FbServerPanel() );
		m_tabs.addTab( "Query", new AnyQueryPanel() );
		m_tabs.addTab( "Hot Stocks", new HotStocksPanel() );
		//m_tabs.addTab( "Coinstore", new CoinstorePanel() );
		
		m_frame.add( butPanel, BorderLayout.NORTH);
		m_frame.add( m_tabs);
		m_frame.add( m_southPanel, BorderLayout.SOUTH);
		
		m_frame.setTitle( String.format( 
				"Reflection System Monitor - %s - %s", 
				m_config.getTabName(), 
				refApiBaseUrl() ) );
		m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		m_frame.setSize( 1300, 800);
		m_frame.setVisible(true);
		
		m_frame.addWindowListener(new WindowAdapter() {
		    public void windowClosed(WindowEvent e) {
		    	Util.execute( () -> Util.wrap( () -> start() ) );
		    }
		});
	}
	
	static int num() {
		return (int)S.parseDouble( num.getText() );
	}
	
	/** called when Refresh button is clicked */
	static void refresh() {
		((MonPanel)m_tabs.current()).refreshTop();
	}
	
	// add the commission here as well
	private static JComponent createTradesPanel() {
		String names = "created_at,time,wallet_public_key,orderref,side,quantity,symbol,conid,price,token_price,cumfill,tradekey,perm_id,order_id,exchange,avgprice";
		String sql = """
				select trades.*, transactions.wallet_public_key, transactions.price as token_price
				from trades
				left join transactions
				on trades.orderref = transactions.uid
				$where
				order by created_at desc
				$limit
				""";  // you must order by desc to get the latest entries
		 
		return new QueryPanel( "trades", names, sql);
	}

	static class UsersPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,first_name,last_name,email,kyc_status,phone,aadhaar,address,city,country,id,pan_number,persona_response";
		static String sql = "select * from users $where";
		
		UsersPanel() {
			super( "users", names, sql);
		}
		
		@Override protected String getTooltip(JsonObject row, String tag) {
			String ret = null;

			if (tag.equals("persona_response")) {
				try {
					ret = JsonObject.parse( row.getString(tag) ).getObject("fields").toHtml();
				} catch (Exception e) {
					// eat it
				}
			}
			return ret;
		}
		
		// there's one slight problem here; if they haven't opened the Wallet panel yet
		// it going to activate it, which will refresh it, then refresh it again
		// with the values passed in
		
		// you have to not activate it, but mark it as activated so it doesn't
		// refresh if they click on it later
		@Override protected void onDouble(String tag, Object val) {
			if (S.notNull(tag).equals("wallet_public_key") ) {
				m_tabs.select("Wallet");
				m_walletPanel.setWallet(val.toString());
			}
		}
		
	}

	static class AnyQueryPanel extends QueryPanel {
		
		AnyQueryPanel() {
			super( "", "", "");
		}
		
		@Override protected void refresh() throws Exception {
			wrap( () -> {
				String query = where.getText().trim()
						.replaceAll( "'wallet'", "'wallet_public_key'");
				
				if (S.isNotNull( query) ) {
					JsonArray rows = Monitor.m_config.sqlQuery( query);
					HashSet<String> keys = rows.getKeys();
					String[] names = keys.toArray( new String[0]);
					String str = String.join( ",", names);
					m_model.setNames( str);
					m_model.fireTableStructureChanged();
		
					setRows( rows);
					
					m_model.resetSort();  // sort by first column if it is sortable
					m_model.fireTableDataChanged();
					
					S.out( "***Refreshed query model to %s", rows().size() );
				}
			});
		}
		
		
	}

	static abstract class MonPanel extends JPanel implements INewTab {
		public MonPanel(LayoutManager layout) {
			super(layout);
		}

		@Override public void activated() {
			refreshTop();
		}

		/** Display the message in a popup */
		public void wrap(ExRunnable runner) {
			try {
				UI.watch( Monitor.m_frame, runner);
			}
			catch (Throwable e) {
				e.printStackTrace();
				Util.inform( this, e.getMessage() );
			}
		}

		/** Display hourglass and refresh, catch exceptions */
		protected final void refreshTop() {
			UI.watch( m_frame, () -> refresh() );
		}
		
		protected abstract void refresh() throws Exception;
		
		@Override public void switchTo() {
		}

		@Override public void closed() {
		}
		
		protected Window getWindow() {
			return SwingUtilities.getWindowAncestor(this);
		}
	}
	
	static class EmptyPanel extends MonPanel {
		EmptyPanel(LayoutManager layout) {
			super(layout);
		}
		
		@Override public void refresh() throws Exception {
		}
	}
	
	static class FbServerPanel extends JsonPanel {
		FbServerPanel() {
			super( new BorderLayout(), "id,status,createdAt");
			add( m_model.createTable() );  // don't move this, WalletPanel adds to a different place
		}
		
		@Override protected Object format(String key, Object value) {
			return key.equals("createdAt") ? Util.hhmmss.format(value) : value;
		}
		
		@Override  // this is wrong, should use base url
		public void refresh() throws Exception {
			JsonArray ar = MyClient.getArray(m_config.fbBaseUrl() + "/fbserver/get-all");
			setRows( ar);
			m_model.fireTableDataChanged();
		}
	}
	
	static class HotStocksPanel extends JsonPanel {
		HotStocksPanel() {
			super( new BorderLayout(), "smartcontractid,startDate,endDate,convertsToAmt,convertsToAddress,allow,tokenSymbol,isHot,symbol,description,type,exchange,is24hour,tradingView");
			add( m_model.createTable() );  // don't move this, WalletPanel adds to a different place
		}
		
		@Override  // this is wrong, should use base url
		public void refresh() throws Exception {
			JsonArray ar = MyClient.getArray(m_config.fbBaseUrl() + "/fbserver/get-all");
			setRows( ar);
			m_model.fireTableDataChanged();
		}
	}
}
