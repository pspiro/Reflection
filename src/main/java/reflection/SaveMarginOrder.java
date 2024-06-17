//package reflection;
//
//import com.ib.client.Order;
//import com.ib.client.Types.Action;
//import com.ib.controller.ApiController;
//
//public class SaveMarginOrder {
//	static MarginOrderMgr mgr = new MarginOrderMgr();
//	
//	ApiController controller;
//
//	// config
//	double feePct;
//	double lastBuffer;  // as percent, try 
//	double bidBuffer;
//	double maxLeverage;
//		
//	// user fields
//	int conid;
//	double userAmt;
//	double leverage;
//	double entryPrice;
//	double stopLoss;
//	double profitTaker;
//	
//	// state
//	Order order;
//	double avgPrice;
//	double bid;
//	double ask;
//	double last;
//	int permId;
//	
//	double maxFee() {
//		return totalAmt() * feePct;
//	}
//	
//
//	double totalAmt() {
//		return userAmt * leverage;
//	}
//	
//	double maxOrderAmt() {
//		return totalAmt() - maxFee();
//	}
//	
//	double desiredQty() {
//		return maxOrderAmt() / entryPrice;
//	}
//	
//	void placeOrder() {
//		
//	}
//	
//	double loanAmt() {
//		return spentAmt() - userAmt();
//	}
//	
//	double userAmt() {
//		return Math.min( userAmt, spentAmt() );
//	}
//	
//	double spentAmt() {
//		return filledQty() * avgPrice * (1 + feePct);
//	}
//	
//	double value() {
//		return markPrice() * filledQty();
//	}
//	
//	double bidValue() {
//		return markPrice() * filledQty();
//	}
//	
//	private double filledQty() {
//		return order.filledQuantity() == order.roundedQty() ? desiredQty() : order.filledQuantity();
//	}
//
//	double markPrice() {
//		return bounded( bid, last, ask);
//	}
//	
//	double bounded( double low, double mid, double high) {
//		return Math.max( low,  Math.min( mid, high) );
//	}
//	
//	void oncheck() {
//		if (loanAmt() > 0) {
//			if (value() / loanAmt() < (1 + lastBuffer) ) {
//				liquidate();
//			}
//			else if (bidValue() / loanAmt() < (1 + bidBuffer) ) {
//				liquidate();
//			}
//		}
//	}
//
//	private void liquidate() {
////		if (order.isActive() ) {
////			controller.cancelOrder( order.orderId(), null, null);
////		}
//		
//		Order order = new Order();
//		order.action( Action.Sell);
//		order.roundedQty(); // calculate using agg braiSn
//		
//		
//	}
//	
//}
