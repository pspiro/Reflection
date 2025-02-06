package stefan;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.simple.JsonObject;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Execution;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IPositionHandler;
import com.ib.controller.ApiController.ITradeReportHandler;

import common.ConnectionMgrBase;
import common.Util;
import http.MyServer;
import tw.util.NewLookAndFeel;
import tw.util.NewTabbedPanel;
import tw.util.OStream;
import tw.util.S;
import tw.util.UI;

public class Stefan implements ITradeReportHandler {
	enum Time { PreCandle, Candle, Trading, PostTrading }

	enum UserStatus { 
		Inactive, Active, ClosingOnly;
		boolean isActive() { 
			return Util.equals( this, Active, ClosingOnly);
		}

		Color getColor() {
			return this == Inactive ? Color.red : this == Active ? Color.green : Color.yellow;
		}
	}
	
	private final HashMap<String,Stock> m_map = new HashMap<>(); // map stock tymbol to Stock
	final ArrayList<Stock> m_stocks = new ArrayList<>(); 
	private ConnectionMgr m_connMgr;
	private Params m_params;
	private OStream m_tradeLog;
	private double m_openedAmt; // incremented for opening orders, decremented for closing orders
	private UserStatus m_userStatus = UserStatus.Inactive;
	private Time m_time = Time.PreCandle;
	private boolean m_validParams; // once they are valid, they are always valid
	private boolean m_recValidId; // set to true when onRecNextValidId() is called
	private boolean m_requestedHistorical; // set to true when we have validParams and have received valid Id
	private long m_timeOffset; // add this to local time to get TWS time

	// UI
	private final JFrame m_frame = new JFrame();
	private final JLabel m_timeLabel = new JLabel(); // display time on screen
	private final ConfigPanel m_configPanel = new ConfigPanel( this); // move down? pas
	        final ControlPanel m_controlPanel = new ControlPanel( this, m_stocks);
	private final TradesPanel m_tradesPanel = new TradesPanel( this);
	private final OrdersPanel m_ordersPanel = new OrdersPanel( this);

	public static void main(String[] args) {
		Thread.currentThread().setName("Monitor");		
		NewLookAndFeel.register();
		
		// run only one instance
		MyServer.listen( 7890, 1, server -> {} );

		try {
			new Stefan().run();
		}
		catch (Exception e) {
			e.printStackTrace();  // unexpected
			System.exit( 1);
		}
	}
	
	void run() throws Exception {
		m_tradeLog = new OStream( "trade.log", true);

		// tabbed panel
		NewTabbedPanel tabs = new NewTabbedPanel(true);
		tabs.addTab( "Configuration", m_configPanel);
		tabs.addTab( "Control", m_controlPanel);
		tabs.addTab( "Trades", m_tradesPanel);
		tabs.addTab( "Orders", m_ordersPanel);
		
		// bottom panel, display time
		JPanel botPanel = new JPanel();
		botPanel.setLayout( new FlowLayout( FlowLayout.RIGHT) );
		botPanel.add( m_timeLabel);
		
		// build frame
		m_frame.add( tabs);
		m_frame.add( botPanel, BorderLayout.SOUTH);
		m_frame.setSize( 1200, 900);
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m_frame.setVisible( true);
		UI.centerOnScreen(m_frame);

		// listen for event - user clicks the X in upper-right
		m_frame.addWindowListener( new WindowAdapter() {
			@Override public void windowClosed(WindowEvent e) {
				if (m_tradeLog != null) {
					m_tradeLog.close();
				}
				S.out( "User terminated application");
				System.exit( 0);
			}
		});
		
		// read parameters from file
		try {
			m_params = Params.read();
			m_configPanel.refresh( m_params);
			m_params.validate();
			onParamsUpdated();
		}
		catch( Exception e) { // normal, happens on initial startup
			Util.inform( m_frame, "Configuration could not be read or was invalid - " + e.getMessage() );
		}
	}
		
	/** Called when user clicks Save on Config tab; params have already been validated */
	public void updateParams(Params params) {		
		if (m_userStatus.isActive() ) {
			Util.inform( m_frame, "Please 'Stop' the trading before updating the parameters");
			// def no not allow changing the tws connection params
		}
		else {
			m_params = params;
			onParamsUpdated();
		}
	}

	/** Called at startup and/or after user creates or saves valid params */
	void onParamsUpdated() {
		try {
			// first time w/ valid params?
			if (!m_validParams) {				
				S.out( "Got valid params");
				m_validParams = true;
				
				// create timer
				S.out( "Creating timer");
				Util.executeEvery( 0, 1000, this::onTimer);
			}

			// first time only, can't be changed
			if (m_connMgr == null) {
				m_connMgr = new ConnectionMgr();
				m_connMgr.startTimer();
			}
			else if (!m_connMgr.apiParams().equals( m_params.getApiParams() ) ) {
				Util.inform( null, "The connection parameters have changed;\nplease exit and restart for this to take effect");
			}

			// create stocks; could query for conid here. pas
			for (var symbol : m_params.stockList().split( " *, *") ) {
				Stock stock = Util.getOrCreate( m_map, symbol, () -> new Stock( this, symbol) );
				if (!m_stocks.contains( stock) ) {
					S.out( "Adding new stock: " + symbol);
					stock.index( m_stocks.size() );
					m_stocks.add( stock);
					
					if (m_connMgr.isConnected() ) {
						stock.reqTopData();
					}
					
					if (m_requestedHistorical) {
						stock.requestHistoricalData();
					}
				}
			}
			
			m_controlPanel.refreshStockTable();
			m_controlPanel.refreshAmounts(m_params.totalAmount(), m_openedAmt);
		}
		catch( Exception e) {
			e.printStackTrace(); // unexpected
		}
	}
	
	/** Called every second once we have valid params */
	private void onTimer() {
		try {
			Date now = new Date( now() );
			Time oldTime = m_time;
			
			m_time =
				now.compareTo( m_params.getStartAsDate() ) < 0 ? Time.PreCandle :
				now.compareTo( m_params.getEndAsDate() ) < 0 ? Time.Candle :
				now.compareTo( m_params.getCloseAsDate() ) < 0 ? Time.Trading :
				Time.PostTrading;

			// start trading?
			if (m_time == Time.Trading && !m_requestedHistorical && m_recValidId) {  // don't check userStatus here; check it when placing an order so we can still display valid data in the stock table
				m_requestedHistorical = true;
				m_stocks.forEach( stock -> stock.requestHistoricalData() );
			}
			
			// update screen on time status change
			if (m_time != oldTime) {
				S.out( "Time period has moved from %s to %s", oldTime, m_time);
				refreshStatus();
			}

			// update time on screen
			m_timeLabel.setText( Util.yToS.format( now) );
		}
		catch (ParseException e) {
			e.printStackTrace(); // unexpected
		}
	}

	/** return TWS time */
	private long now() {
		return System.currentTimeMillis(); // currently disabled. pas + m_timeOffset;
	}

	public void onUserStatusUpdated(UserStatus status) {
		if (status != m_userStatus) {
			m_userStatus = status; // ???
			refreshStatus();
		}
	}
	
	class ConnectionMgr extends ConnectionMgrBase {

		ConnectionMgr() {
			super( m_params.getApiParams(), 10 * 1000L);

			// listen for trades
			m_controller.handleExecutions( Stefan.this);
		}
		
		@Override public void onConnected() {
			super.onConnected();
			S.out( "TWS_CONNECTION: connected");
			refreshStatus();
			
			// query TWS time and set offset
			controller().reqCurrentTime( time -> 
				m_timeOffset = time * 1000 - System.currentTimeMillis() );
		}

		@Override public void onRecNextValidId(int id) {
			S.out( "TWS_CONNECTION: received next valid id");
//			m_tradingHours.startQuery();

			
			S.out( "requesting positions");
			controller().reqPositions( new IPositionHandler() {
				@Override public void position(String account, Contract contract, Decimal pos, double avgCost) {
					S.out( "received position %s %s", contract.symbol(), pos);
					
					var stock = Util.getOrCreate( m_map, contract.symbol(), () -> 
						new Stock( Stefan.this, contract.symbol() ) );
					
					stock.position( pos.toDouble() );
					m_controlPanel.refreshStockTable(); // don't refresh whole table. pas
				}
				@Override public void positionEnd() {
				}
			});

			// request market data
			S.out( "Setting market data request type to DELAYED");
			controller().reqMktDataType(3);
			m_stocks.forEach( stock -> stock.reqTopData() );
						
			m_recValidId = true;
		}

		@Override public void onDisconnected() {
			S.out( "TWS_CONNECTION: disconnected");
			refreshStatus();
			m_recValidId = false;
		}
		
		@Override protected void onIbConnectionUpdated(boolean connected) {
			S.out( "TWS_CONNECTION: IB connection lost");
			refreshStatus();
		}
	}
	
	ApiController controller() {
		return m_connMgr.controller();
	}

	// might want to change the format to be more readable, e.g. csv
	@Override public void tradeReport(String tradeKey, Contract contract, Execution exec) {
		JsonObject json = exec.getJson();
		json.putIf( "symbol", contract.symbol() );
		json.putIf( "conid", contract.conid() );
		json.putIf( "tradekey", tradeKey);
		m_tradeLog.writeln( json.toString() );
		
		m_tradesPanel.addTrade( contract, exec);
	}

	@Override public void tradeReportEnd() {
	}

	@Override public void commissionReport(String tradeKey, CommissionReport rpt) {
		var json = Util.toJson( 
			"execId", rpt.execId(), 
			"commission", rpt.commission(), 
			"tradeKey", tradeKey);
		m_tradeLog.writeln( json.toString() ); 
	}

	public Params params() {
		return m_params;
	}
	
	void incrementOpenedAmt(double amt) {
		S.out( "incrementing opened amount by %s", amt);
		m_openedAmt += amt;
		m_controlPanel.refreshAmounts( m_params.totalAmount(), m_openedAmt);
	}
	
	private void refreshStatus() {
		m_controlPanel.refreshStatus(
			m_connMgr != null && m_connMgr.isConnected(),
			m_connMgr != null && m_connMgr.ibConnection(),
			m_userStatus,
			getStatusText() );
	}

	private String getStatusText() {
		return m_userStatus.isActive()
			? switch (m_time) {
				case PreCandle -> "Waiting for candle phase to begin";
				case Candle -> "Waiting for trading phase to begin";
				case Trading -> m_userStatus == UserStatus.Active ? "Active, trading" : "Active (closing orders only)";
				case PostTrading -> "Trading has ended for the day";
				}
			: "Inactive";
	}

	public double openedAmt() {
		return m_openedAmt;
	}

	public UserStatus userStatus() {
		return m_userStatus;
	}

	public OrdersPanel ordersPanel() {
		return m_ordersPanel;
	}

	public void tick(String symbol, double price) {
		Util.ifff( m_map.get( symbol), stock -> controller().tick( stock.reqId(), price) );
	}

	public void simFill(String symbol, boolean opening, double price) {
		Util.ifff( m_map.get( symbol), stock -> stock.simFill( opening, price) );
	}
}

/* 
 * todo
 * you must close the m_tradeLog when closing the program
 */

/* time spent: 
 * 2/1 1.0 day
 * 2/2  .5 day
 * 2/3 1.2 day
*/


//write an overview of the threads

//do
//switch over to his test user
//we must handle reconnect to tws?
//implement live order handling; question: do you need to maintain a local order store? or can get get everything you need from the live order? you can store any arbitrary fields on the orderref 
//must do all the sell (reverse) functionality
//add pacing for the table updates
//let time period be divisible by bar size
//check all pas and ???
//you must display connection error on the interface

//later
//in the live orders table, display whether or not the loss limit has been hit yet or not; after you nail down how the orders will be displayed
//maybe display filled shares in the open orders tables, or just add it to the status column like tws
//you need a timer for unfilled buy or sell limit order
//must take action when user status changes/allow dynamic updates
//you can use the 'if-touched' orders to automate some of it, but you may place orders in excess of the counter
//instead of loss limit, we can use adjusted trailing amount which I think is when you touch a price you can reduce the trailing amount

//notes for Stefan
//demo: trades, comm, and bars are written to a file
//improvements: when an order triggers, you can check the SIZE to see if there is there is enough size to fill the order you are triggering
//we could use IB STOP order 
//Q: we need a name!
//Q: let's say price moves well past buy line but we don't buy due to no available funds; then funds become available; should we buy at that time?
//Q: if the program is restarted while active, do you want it to keep trading, or must user click 'Start'?
//Q: tws time seems slow; we should use pc time?
//Q: should limit price offset be applied to the trigger price or the last price, which could be higher? I would think the last price
//Q: when the last price touch the buy trigger, should we check the current ask? If it is higher than the last the buy order might not fill
//Q: do you think it's good having orders split out in two panels?
//Q: should we move orders onto control screen

//UI
//move start/stop to top, status fields to bottom
//do the visual display of a single stock for extra credit
//create better radio buttons that look more like regular buttons; use it for order type selection, or use JavaFX
//use rounded corners for the entry fields, see chat
//implement double-height table headers

//performance updates:
//you may need to split out the UI into a separate program for performance
//review all syncronization
//read the stuff he sent on java
//put some pacing on the table refresh
//for faster order placement, query for the conid up front

//test: adding a stock that already has a position

/*
The program flow for a BUY would be as follows for the settings shown in the screenshot:
If LMT and Repeat LMT are checked, then the price for the order = [Last Price] + [Offset]. If the order is not completed within 100 milliseconds, the order is updated: Order = [Last Price] + 2x[Offset]. If the order is repeatedly not executed within 100 milliseconds, then the order is updated: Order = [Last Price] + 4x[Offset]. If the order is repeatedly not executed within 100 milliseconds, then the order is updated: Order = [Last Price] + 8x[Offset]. This continues indefinitely until the order is executed. This applies to both BUY and SELL.

LMT carries the risk that we do not get all the shares or even none sold/bought. Could you provide the following algorithm: LMT uses the user-defined offset to submit a better bid. If the trade is not completed within a defined period of 100 milliseconds (it would be good if the user could enter this value), then the remaining shares are offered again with double the offset. This is repeated with the offset being doubled each time until the position is completely traded. This approach is probably similar to MKT, but it could also be used outside of trading hours. What do you think?
*/