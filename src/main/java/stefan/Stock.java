package stefan;

import java.util.ArrayList;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.client.Types.Action;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.SecType;
import com.ib.client.Types.TimeInForce;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.Bar;

import common.MyTimer;
import common.Util;
import tw.util.OStream;
import tw.util.S;

class Stock extends TopMktDataAdapter {
	enum Status {
		Init,				// upon Stock creation
		LookingToOpen, 		// active, waiting for price penetration
		SubmittedBuy,		// buy line penetrated and buy order submitted
		SubmittedSell,		// buy order filled, sell trailing stop submitted; status is reset to LookingToOpen after this
		Error,				// an exception occurred, we don't know how to recover
	}
	
	static class OneSide {
		private double line; // e.g. buyLine
		private double trigger; // buyLine + params.excessAmt
		private double lossLmt; // e.g. buy lossLimit
		private double lossLmtTrigger; // e.g. buy lossLimit + params.excessAmt
	}
	
	private final Stefan m_stefan;
	private final String m_symbol;
	private final Contract m_contract = new Contract();
	private final Stock.OneSide m_buy = new OneSide();
	private final Stock.OneSide m_sell = new OneSide();
	private Status m_status = Status.Init;
	private Order m_openingOrder;
	private Order m_closing;
	private ArrayList<Bar> m_bars;
	private double m_bid;
	private double m_ask;
	private double m_last;
	private long m_buyTime;
	private double m_position;
	private boolean m_adjusted;
	private int m_reqId;
	private int m_index; // index of this stock in Stefan.m_stocks 

	public Stock( Stefan stefan, String symbol) {
		m_stefan = stefan;
		m_symbol = symbol;

		m_contract.symbol( m_symbol);
		m_contract.exchange( "SMART");
		m_contract.secType( SecType.STK);
		m_contract.currency( "USD");
	}
	
	public void reqTopData() {
		m_reqId = controller().reqTopMktData(m_contract, "", false, false, this); // api writes to log
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
		boolean ticked = false;
		
		switch( tickType) {
			case BID:
			case DELAYED_BID:
				m_bid = price;
				ticked = true;
				break;
			case ASK:
			case DELAYED_ASK:
				m_ask = price;
				ticked = true;
				break;
			case LAST:
			case DELAYED_LAST:
				double prevLast = m_last;
				m_last = price;
				if (m_stefan.userStatus().isActive() ) { 
					tickLast( prevLast);
				}
				ticked = true;
				break;
		}
		
		if (ticked) {
			refreshStockTable();
		}
	}
	
	private void refreshStockTable() {
		m_stefan.m_controlPanel.model().fireTableRowsUpdated(m_index, m_index);
	}
	
	private void refreshOpenOrdersTable() {
		m_stefan.ordersPanel().refreshOpenOrders();
	}

	private void refreshClosingOrdersTable() {
		m_stefan.ordersPanel().refreshClosingOrders();
	}

	/** executes in API thread; make sure it's okay; we might need to scale it back if it ticks too often */
	private void tickLast(double prevLast) {
		switch( m_status) {
			case LookingToOpen:
				considerOpening( prevLast);
				break;
			case SubmittedSell:
				checkLossLimit();
				break;
		}
	}

	/** When when we move from the candle period to the trading period */
	public void requestHistoricalData() {
		try {
			MyTimer t = new MyTimer().next( "Querying historical data for " + m_symbol);
			controller().reqHistoricalData(
					m_contract, 
					params().getEndAsDate(),
					params().duration(), 
					DurationUnit.SECOND, 
					params().getBarSize(), 
					WhatToShow.TRADES, 
					true, 
					false,
					bars -> {
				t.done("historical query for " + m_symbol);
				m_bars = bars;
				setBuyLines();
			});
		}
		catch (Exception e) {
			e.printStackTrace();  // unexpected
		}
	}
	
	/** executes in API thread; make sure it's okay */
	private void setBuyLines() {
		double low = Double.MAX_VALUE;
		double high = 0;

		// calculate high/low and write all bars to a file
		try( OStream os = new OStream( m_symbol + ".bars") ) {
			for (var bar : m_bars) {
				high = Math.max( high, bar.high() );
				low = Math.min( low, bar.low() );
				os.writeln( bar.toString() );
			}
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		
		m_buy.line = high;
		m_buy.trigger = Util.round( high * (1 + params().excessPct() ) );
		m_buy.lossLmt = Util.round( high * (1 + params().lossLimitPct() ) );
		m_buy.lossLmtTrigger = Util.round( m_buy.lossLmt * (1 + params().excessPct() ) );
		
		m_sell.line = low;
		m_sell.trigger = Util.round( low * (1 - params().excessPct() ) );
		m_sell.lossLmt = Util.round( low * (1 - params().lossLimitPct() ) );
		m_sell.lossLmtTrigger = Util.round( m_sell.lossLmt * (1 - params().excessPct() ) );
		
		out( "buyLine=%s  sellLine=%s", high, low);
		reset();
	}
	
	private void considerOpening(double prevLast) {
		// if last price >= trigger price, and prev last was not...
		if (!Util.isGtEq( prevLast, m_buy.trigger) && Util.isGtEq( m_last, m_buy.trigger) ) {
			out( "opening buy order has triggered", m_last);
			buyNow();
		}
		else {
			// check for sell short
		}
	}
	
	/** always called in the MD thread; no sync. necessary, we are in api thread, make sure that's okay. pas */
	private void buyNow() {
		int shares = getOrderQty();
		
		// insufficient funds remaining to place order with min share size?
		if (shares < params().minShares() ) {
			out( "WARNING: order triggered but calculated shares of %s is less than the minimum", shares);
			return;
		}
		
		// check for valid bid/ask
		if (m_bid <= 0 || m_ask <= 0) {
			out( "WARNING: cannot place buy order due to invalid bid or ask");
			return;
		}
		
		try {
			double lmtPrice = m_bid + params().limitOffset();
			double holdAmt = lmtPrice * shares;

			// create order
			m_openingOrder = new Order();
			m_openingOrder.action( Action.Buy);
			m_openingOrder.roundedQty( shares);
			m_openingOrder.orderType( params().orderType() );  // mkt or lmt
			m_openingOrder.outsideRth( true);  // where to get this???
			m_openingOrder.tif( TimeInForce.DAY);

			if (m_openingOrder.orderType() == OrderType.LMT) {
				m_openingOrder.lmtPrice( lmtPrice);
			}

			// update status and counter
			m_status = Status.SubmittedBuy;
			m_stefan.incrementOpenedAmt( holdAmt);

			// add order to open orders panel
			m_stefan.ordersPanel().addOpenOrder( m_contract, m_openingOrder);
			
			out( "placing opening order: %s %s shares at %s %s", 
					m_openingOrder.action(), m_openingOrder.roundedQty(), m_openingOrder.orderType(), lmtPrice);
			
			controller().placeOrder(m_contract, m_openingOrder, new IOrderHandler() {
				@Override public void orderState(OrderState orderState) {
					out( "open order state %s", orderState);
				}
				
				@Override public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
						int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
					
					out( "order update  status=%s  filled=%s  avgPrice=%s", 
							status, filled, avgFillPrice);
					
					m_openingOrder.status( status);
					
					if (m_status == Status.SubmittedBuy) {
					
						if (status == OrderStatus.Filled) {
							out( "opening order was filled");
							
							// adjust counter, assuming fill price was different from limit price
							m_stefan.incrementOpenedAmt( m_openingOrder.roundedQty() * avgFillPrice - holdAmt ); 

							// submit sell order
							m_status = Status.SubmittedSell;
							
							if (m_stefan.userStatus().isActive() ) {
								submitSell( shares);
							}
							else {
								out( "WARNING: buy order filled, but algo has been stopped so no closing order will be placed");
							}
						}
						else if (status == OrderStatus.Cancelled) {
							out( "Error: order was canceled");
							m_stefan.incrementOpenedAmt( filled.toDouble() * avgFillPrice - holdAmt );
							reset();
						}

						refreshStockTable();
						refreshOpenOrdersTable();
					}
				}

				@Override public void onRecOrderError(int errorCode, String errorMsg) {
					out( "open order error %s %s", errorCode, errorMsg);
				}
			});

			refreshStockTable(); // let new stock status be reflected in Stock table
		}
		catch( Exception e) {
			e.printStackTrace();
			m_status = Status.Error;
		}
	}
	
	void submitSell(int shares) {
		try {
			m_closing = new Order();
			m_closing.action( Action.Sell);
			m_closing.roundedQty( shares);
			m_closing.trailingPercent( params().trailingPct() );
			m_closing.outsideRth( true);  // where to get this???
			m_closing.tif( TimeInForce.DAY);
			m_closing.trailStopPrice( m_buy.line);
			
			if (params().ibOrderType() == OrderType.MKT) {
				m_closing.orderType( OrderType.TRAIL);
			}
			else {
				m_closing.orderType( OrderType.TRAIL_LIMIT);
				m_closing.lmtPriceOffset( params().limitOffset() );
			}

			m_status = Status.SubmittedSell;
			
			m_stefan.ordersPanel().addCloseOrder( m_contract, m_closing);
			
			out( "placing closing order: %s %s shares  type=%s  trailPct=%s  stop=%s", 
					m_closing.action(), m_closing.roundedQty(), m_closing.orderType(),
					m_closing.trailingPercent(), m_closing.trailStopPrice() );

			controller().placeOrder( m_contract, m_closing, new IOrderHandler() {
				@Override public void orderState(OrderState orderState) {
					out( "close order state %s", orderState);

					// if we received a valid trailing stop price that is different from the
					// current value, update the price on the order and refresh the table
					if (m_status == Status.SubmittedSell && 
							orderState.trailStopPrice() != 0 && 
							!Util.isEq( orderState.trailStopPrice(), m_closing.trailStopPrice(), 4) ) {
						
						out( "stop price has been updated to %s", orderState.trailStopPrice() );
						
						m_closing.trailStopPrice( orderState.trailStopPrice() );
						refreshClosingOrdersTable();
					}
				}
				
				@Override public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
						int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

					out( "close order status %s", status);
					
					m_closing.status( status);
					
					if (m_status == Status.SubmittedSell) {
						if (status == OrderStatus.Filled) {
							out( "closing order was filled");
							m_stefan.incrementOpenedAmt( -m_closing.roundedQty() * avgFillPrice);
							reset();
						}
					}
					else if (status == OrderStatus.Cancelled) {
						out( "ERROR: closing order was canceled");
						// what to do? re-place sell order, or reset()? pas
					}
				}
				
				@Override public void onRecOrderError(int errorCode, String errorMsg) {
					out( "close order error %s %s", errorCode, errorMsg);
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
			m_status = Status.Error;
		}
	}

	/** Prepare for a new order */
	private void reset() {
		m_status = Status.LookingToOpen;
		m_adjusted = false;
	}
	
	private void checkLossLimit() {
		if (!m_adjusted && Util.isGtEq(m_last, m_buy.lossLmtTrigger) ) {
			try {
				out( "loss-limit threshold %s reached; updating stop price to %s",
						m_buy.lossLmtTrigger, m_buy.lossLmt);
				
				m_adjusted = true;
				m_closing.trailStopPrice( m_buy.lossLmt);
				controller().modifyOrder( m_contract, m_closing);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
	}

	int getOrderQty() {
		double amount = Math.min( params().tradeAmount(), params().totalAmount() - m_stefan.openedAmt() );
		return (int)(amount / m_last + .0001); // round down
	}

	/** print string with symbol and last price in front */
	private void out(String format, Object... params) {
		S.out( "%s %s %s", m_symbol, m_last, S.format( format, params) );
	}

	public String getTableDisplay(int col) {
		return switch( col) {
			case  0 -> m_symbol;
			case  1 -> S.fmt2d( m_position);
			case  2 -> S.fmt2d( m_bid);
			case  3 -> S.fmt2d( m_ask);
			case  4 -> S.fmt2d( m_last);
			case  5 -> S.fmt2d( m_sell.lossLmtTrigger);
			case  6 -> S.fmt2d( m_sell.lossLmt);
			case  7 -> S.fmt2d( m_sell.trigger);
			case  8 -> S.fmt2d( m_sell.line);
			case  9 -> S.fmt2d( m_buy.line);
			case 10 -> S.fmt2d( m_buy.trigger);
			case 11 -> S.fmt2d( m_buy.lossLmt);
			case 12 -> S.fmt2d( m_buy.lossLmtTrigger);
			case 13 -> m_status.toString();
			default -> "";
		};
	}			
	
	public void position(double val) {
		m_position = val;
	}

	public String symbol() {
		return m_symbol;
	}
	
	private ApiController controller() {
		return m_stefan.controller();
	}

	private Params params() {
		return m_stefan.params();
	}
	
	int reqId() {
		return m_reqId;
	}

	public void index(int v) {
		m_index = v;
	}

	/** used for testing */
	public void simFill(boolean opening, double price) {
		Order order = opening ? m_openingOrder : m_closing;
		if (order != null) {
			m_stefan.controller().simFill( order.orderId(), order.roundedQty(), 0, price); 
		}
		else {
			out( "Error: cannot simFill with opening=%s", opening);
		}
	}
}
