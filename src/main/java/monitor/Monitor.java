package monitor;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonArray;

import chain.Chain;
import chain.Stocks;
import chain.Stocks.Stock;
import common.JsonModel;
import common.Util;
import http.MyClient;
import monitor.UsersPanel.PersonaPanel;
import onramp.Onramp;
import redis.MyRedis;
import tw.google.NewSheet;
import tw.util.DualPanel;
import tw.util.NewLookAndFeel;
import tw.util.NewTabbedPanel;
import tw.util.S;
import web3.StockToken;

// use this to query wallet balances, it is super-quick and returns all the positions for the wallet
// https://deep-index.moralis.io/api/v2/:address/erc20	
// you could use this to easily replace the Backend method that combines it with with the market data 

public class Monitor {
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";

	public static MonitorConfig m_config;
	public static final Stocks stocks = new Stocks();
	
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
		stocks.readFromSheet();
		
		num = new JTextField(4); // number of entries to return in query
		m_frame = new JFrame();
		m_tabs = new NewTabbedPanel(true);
		m_logPanel = new LogPanel();
		m_walletPanel = new WalletPanel();
		m_southPanel = new SouthPanel();
		
		m_config.useExternalDbUrl();
		S.out( "Read %s tab from google spreadsheet %s", m_config.getTabName(), NewSheet.Reflection);
		S.out( "Using database %s", m_config.postgresUrl() );

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
		m_tabs.addTab( "Signups", new SignupPanel() );
		m_tabs.addTab( "Persona", new PersonaPanel() );
		m_tabs.addTab( "Transactions", new TransPanel() );
		m_tabs.addTab( "Log", m_logPanel);
		m_tabs.addTab( "Trades", createTradesPanel() );
		m_tabs.addTab( "Tokens", new TokensPanel() );  // Stock Tokens
		m_tabs.addTab( "MDServer Prices", new MdsPricesPanel() );
		m_tabs.addTab( "RefAPI Prices", pricesPanel);
		m_tabs.addTab( "Redemptions", new RedemptionPanel() );
		m_tabs.addTab( "Live orders", new LiveOrdersPanel() );
		m_tabs.addTab( "HookServer", new HookServerPanel() );
		m_tabs.addTab( "UserTokenMgr", new UserTokenPanel() );
		m_tabs.addTab( "Query", new AnyQueryPanel() );
		m_tabs.addTab( "Hot Stocks", new HotStocksPanel() );
		m_tabs.addTab( "Email", new EmailPanel() );
		m_tabs.addTab( "OnRamp", new OnrampPanel() );
		//m_tabs.addTab( "Coinstore", new CoinstorePanel() );
		
		m_frame.add( butPanel, BorderLayout.NORTH);
		m_frame.add( m_tabs);
		m_frame.add( m_southPanel, BorderLayout.SOUTH);
		
		m_frame.setTitle( String.format( 
				"Reflection System Monitor - %s - %s", 
				m_config.getTabName(), 
				refApiBaseUrl() ) );
		m_frame.setSize( 1300, 810);
		m_frame.setVisible(true);
		
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		m_frame.addWindowListener(new WindowAdapter() {
//		    public void windowClosed(WindowEvent e) {
//		    	Util.execute( () -> Util.wrap( () -> start() ) );
//		    }
//		});
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

	static class EmptyPanel extends MonPanel {
		EmptyPanel(LayoutManager layout) {
			super(layout);
		}
		
		@Override public void refresh() throws Exception {
		}
	}
	
	static class HotStocksPanel extends JsonPanel {
		HotStocksPanel() {
			super( new BorderLayout(), "smartcontractid,startDate,endDate,convertsToAmt,convertsToAddress,allow,tokenSymbol,isHot,symbol,description,type,exchange,is24hour,tradingView");
			add( m_model.createTable() );  // don't move this, WalletPanel adds to a different place
		}
		
		@Override  // this is wrong, should use base url
		public void refresh() throws Exception {
			JsonArray ar = MyClient.getArray(m_config.baseUrl() + "/api/hot-stocks");
			setRows( ar);
			m_model.fireTableDataChanged();
		}
	}
	
	static class OnrampPanel extends MonPanel {
		private JsonModel m_apiModel = new JsonModel("abc") {
			@Override protected Object format(String key, Object value) {
				if (key.equals( "createdAt") ) {
					return Util.left( value.toString(), 19).replace( "T", " ");
				}
				return super.format(key, value);
			}
		};

		private JsonModel m_dbModel = new JsonModel("abc");
		
		OnrampPanel() {
			super( new BorderLayout() );
			
			JPanel p = new DualPanel();
			p.add( m_apiModel.createTable( "OnRamp API Transactions"), "1");
			p.add( m_dbModel.createTable( "Reflection Database Transactions"), "2");
			
			add( p);
		}
		
		@Override protected void refresh() throws Exception {
			JsonArray apiTrans = Onramp.getAllTransactions();
			m_apiModel.setNames( String.join( ",", apiTrans.getKeys() ) );
			m_apiModel.fireTableStructureChanged();
			m_apiModel.setRows( apiTrans);
			m_apiModel.fireTableDataChanged();
			
			JsonArray dbTrans = m_config.sqlQuery( "select * from onramp");
			m_dbModel.setNames( String.join( ",", dbTrans.getKeys() ) );
			m_dbModel.fireTableStructureChanged();
			m_dbModel.setRows( dbTrans);
			m_dbModel.fireTableDataChanged();
			
		}
	}
	
	/** Or you could let HookServer return the names which might be more user-friendly */
	public static String getDescription(String address) throws Exception {
		if (address.equalsIgnoreCase( m_config.rusdAddr())) {
			return m_config.rusd().name();
		}
		if (address.equalsIgnoreCase( m_config.busd().address())) {
			return m_config.busd().name();
		}
		var token = chain().getTokenByAddress(address);
		return token != null ? token.name() : "Unknown";
	}
	
	static Chain chain() {
		return m_config.chain();
	}
	
	static Collection<StockToken> tokens() { //rename. bc
		return chain().tokens();
	}
	
	static Collection<Stock> stocks() { //rename. bc
		return stocks.stocks();
	}
}
