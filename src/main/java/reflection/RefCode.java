package reflection;

public enum RefCode {
	OK,
	UNKNOWN,			// should never happen
	INVALID_REQUEST,	// the request itself is invalid; should never happen 
	NOT_CONNECTED,		// not connected to TWS or TWS is not connected to server
	NO_SUCH_STOCK,		// for admin msg only  
	EXCHANGE_CLOSED,	// for order or checkorder 
	NO_PRICES,			// returned by getPrice(), getAllPrices(), order(), checkOrder() 
	INVALID_PRICE, 
	TIMED_OUT,
	REJECTED,
	ORDER_TOO_LARGE, 
	PARTIAL_FILL		// order was partially filled
}
