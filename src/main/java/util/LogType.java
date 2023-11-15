package util;

/** Max length is 32 as per postgres log table */
public enum LogType {
	AUTO_FILL,			// approveAll config setting is on
	BLOCKCHAIN_FAILED,
	CANCEL_ORDER,		// canceling the IB order because we had a timeout 
	COMMISSION,			// commission report received
	DATABASE_ERROR, 
	ERROR_1,ERROR_2,ERROR_3,ERROR_4,ERROR_5,ERROR_6,  // use a dif. value depending on where it's caught
	FB_UPDATE,			// fireblocks update
	IB_ORDER_STATUS,	// received IB order status
	JEDIS,				// Jedis error
	MDS,				// used only by MktDataServer
	NO_STOCK_ORDER,		// there is no IB order because the order size is so small
	ORDER_COMPLETED,	// blockchain completed
	ORDER_ERR,			// we received an error msg from TWS for this order
	ORDER_FAILED,		// IB or blockchain failed
	ORDER_FILLED,		// IB order was completely or partially filled
	ORDER_STATUS_UPDATED,
	ORDER_TIMEOUT,		// our timeout occurred before the IB IOC timeout
	REC_ORDER,			// received order (roundedQty has not been set yet)
	REDEEM,				// user is redeeming RUSD 
	RESPOND_ERR,		// exception while sending response to Frontend 
	RESPOND_ORDER,		// responding OK to Frontend
	RESTART,			// application was restarted
	SHUTDOWN,			// received shutdown message from unix kill command
	SIGNED_IN,			// user signed in with their wallet private key
	SOCKET_ERROR,		// during quick-response
	SUBMITTED_TO_FIREBLOCKS, 
	SUBMITTED_TO_IB,	// order submitted to IB
	TRADE,				// trade report received
	TRADING_HOURS_ERROR,
	TWS_CONNECTION,		// gained/lost connection to TWS
	UNWIND_ERROR, REDEMPTION_FAILED, REDEMPTION_COMPLETED, 		// error while unwinding an order
	
}
