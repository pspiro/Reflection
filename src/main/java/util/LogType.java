package util;

public enum LogType {
	AUTO_FILL,		// approveAll config setting is on
	CHECK,			// check order (what-if)
	COMMISSION,		// commission report received
	ERROR,			// should never happen
	FILLED,			// order was completely filled
	INFO,
	MD_CONNECTION,
	MINT,			// minted some goerli ETF for user
	ORDER,			// received order
	ORDER_CONNECTION,
	ORDER_ERR,		// we received an error msg from TWS for this order
	ORDER_TIMEOUT,
	PARTIAL_FILL,	// order was partially filled
	REJECTED,		// order was rejected
	RESTART,		// application was restarted
	SUBMIT,
	TERMINATE,
	TIME,			// switched to/from IBEOS hours 
	TRADE,			// trade report received
	WALLET, 
}
