package util;

/** Max length is 32 as per postgres log table */
public enum LogType {
	AUTO_FILL,		// approveAll config setting is on
	BLOCKCHAIN_FAILED,
	COMMISSION,		// commission report received
	ERROR,			// should never happen
	FB_UPDATE,		// fireblocks update
	ORDER_FILLED,			// order was completely or partially filled
	JEDIS,			// Jedis error
	MDS,			// used only by MktDataServer
	ORDER_ERR,		// we received an error msg from TWS for this order
	ORDER_TIMEOUT,	// our timeout occurred before the IB IOC timeout
	REC_ORDER,		// received order (roundedQty has not been set yet)
	REDEEM,			// user is redeeming RUSD 
	RESTART,		// application was restarted
	SUBMITTED,		// order submitted to IB		
	TRADE,			// trade report received
	RESPOND_ORDER,	// responding OK to Frontend
	NO_SUBMIT, 
	SUBMITTED_TO_IB, 
	ORDER_STATUS, 
	CANCEL_ORDER, 
	SUBMITTED_TO_FIREBLOCKS, 
	EXCEPTION,
	UNWIND_ERR, 
	DATABASE_ERR, 
	RESPOND_ERR, 
	ORDER_FAILED, ORDER_COMPLETED, ORDER_STATUS_UPDATED
}
