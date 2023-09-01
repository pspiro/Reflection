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
	TIMED_OUT,			// order timed out before being filled or canceled, or SIWE auth was too slow
	REJECTED,			// order was rejected, reason unknown
	ORDER_TOO_LARGE,	// dollar amt is too high as per config settings
	NEED_KYC,			// order is above non-KYC max order size
	PARTIAL_FILL,		// order was partially filled; should never happen, we use AON
	MISSING_ENV_VAR,
	BLOCKCHAIN_FAILED,
	INSUFFICIENT_FUNDS,
	INSUFFICIENT_ALLOWANCE,
	NO_STOCKS,
	VALIDATION_FAILED,
	CONFIG_ERROR, 
	TOO_FAST,
	TOO_SLOW,
	UPDATE_PROFILE, 
	POST_SPLIT, 
	PRE_SPLIT, 
	ACCESS_DENIED, 		// the wallet is blacklisted as per the Blacklist tab on the config spreadsheet, but we don't necessarily want the user to know that
	TRADING_HALTED, // trading is halted as per the allowTrading config setting
	REDEMPTIONS_HALTED, // redemptions are halted as per the allowRedemptions setting in config
	REDEMPTION_PENDING,		// user has twice submitted redemption and there is insufficient USDC in RefWallet
}
