package reflection;

public enum RefCode {
	OK,					// success; for order, completely filled
	UNKNOWN,			// should never happen
	INVALID_REQUEST,	// the request itself is invalid; should never happen 
	NOT_CONNECTED,		// not connected to TWS or TWS is not connected to server
	NO_SUCH_STOCK,		// for admin msg only  
	EXCHANGE_CLOSED,	// for order or checkorder 
	NO_PRICES,			// returned by getPrice(), getAllPrices(), order(), checkOrder() 
	INVALID_PRICE,		// price too high or too low 
	TIMED_OUT,			// order timed out before being filled or canceled
	REJECTED,			// order was rejected, reason unknown
	ORDER_TOO_LARGE,	// dollar amt is too high as per config settings
	PARTIAL_FILL,		// order was partially filled
	MISSING_ENV_VAR,
	BLOCKCHAIN_FAILED,
	INSUFFICIENT_FUNDS,
	NO_STOCKS,
}
