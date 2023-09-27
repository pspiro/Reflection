package util;

/** Max length is 32 as per postgres log table */
public enum LogType {
	AUTO_FILL,		// approveAll config setting is on
	BLOCKCHAIN_FAILED,
	CANCEL_ORDER, 
	COMMISSION,		// commission report received
	DATABASE_ERR, 
	ERROR,			// should never happen
	EXCEPTION,
	FB_UPDATE,		// fireblocks update
	JEDIS,			// Jedis error
	MDS,			// used only by MktDataServer
	NO_SUBMIT, 
	ORDER_COMPLETED, 
	ORDER_ERR,		// we received an error msg from TWS for this order
	ORDER_FAILED, 
	ORDER_FILLED,			// order was completely or partially filled
	ORDER_STATUS, 
	ORDER_STATUS_UPDATED, 
	ORDER_TIMEOUT,	// our timeout occurred before the IB IOC timeout
	REC_ORDER,		// received order (roundedQty has not been set yet)
	REDEEM,			// user is redeeming RUSD 
	RESPOND_ERR, 
	RESPOND_ORDER,	// responding OK to Frontend
	RESTART,		// application was restarted
	SHUTDOWN,
	SUBMITTED_TO_FIREBLOCKS, 
	SUBMITTED_TO_IB, 
	TRADE,			// trade report received
	TWS_CONNECTION, 
	UNWIND_ERR, 
}
