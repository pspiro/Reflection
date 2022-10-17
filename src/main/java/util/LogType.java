package util;

public enum LogType {
	RESTART,		// application was restarted
	ERROR,			// should never happen
	TRADE,			// trade report received
	COMMISSION,		// commission report received
	REJECTED,		// order was rejected
	FILLED,			// order was completely filled
	PARTIAL_FILL,	// order was partially filled
	ORDER_ERR,		// we received an error msg from TWS for this order
	ORDER_TIMEOUT,
	CONNECTION,
	TERMINATE,
	CHECK,			// check order (what-if)
	ORDER,			// received order
	SUBMIT,
	WALLET
}
