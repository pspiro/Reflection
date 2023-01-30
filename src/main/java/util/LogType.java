package util;

public enum LogType {
	RESTART,		// application was restarted
	ERROR,			// should never happen
	TRADE,			// trade report received
	COMMISSION,		// commission report received
	REJECTED,		// order was rejected
	FILLED,			// order was completely filled
	MINT,			// minted some goerli ETF for user
	PARTIAL_FILL,	// order was partially filled
	ORDER_ERR,		// we received an error msg from TWS for this order
	ORDER_TIMEOUT,
	ORDER_CONNECTION,
	MD_CONNECTION,
	TERMINATE,
	TIME,			// switched to/from IBEOS hours 
	CHECK,			// check order (what-if)
	ORDER,			// received order
	SUBMIT,
	WALLET
}
