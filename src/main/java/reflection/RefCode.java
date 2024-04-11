package reflection;

public enum RefCode {
	ACCESS_DENIED, 		// the wallet is blacklisted as per the Blacklist tab on the config spreadsheet, but we don't necessarily want the user to know that
	BLOCKCHAIN_FAILED,
	CONFIG_ERROR, 
	DELAYED_REDEMPTION,
	EXCHANGE_CLOSED,	// for order or checkorder 
	INSUFFICIENT_ALLOWANCE,
	INSUFFICIENT_STABLECOIN, 
	INSUFFICIENT_STOCK_TOKEN, 
	INVALID_NONCE,
	INVALID_PRICE,		// price too high or too low 
	INVALID_REQUEST,	// the request itself is invalid; should never happen 
	INVALID_USER_PROFILE,  // missing user record or some required field from user table
	MISSING_ENV_VAR,
	NEED_KYC,			// order is above non-KYC max order size
	NOT_CONNECTED,		// not connected to TWS or TWS is not connected to server
	NO_PRICES,			// returned by getPrice(), getAllPrices(), order(), checkOrder() 
	NO_RUSD_TO_REDEEM, 
	NO_STOCKS,
	NO_SUCH_REQUEST,  
	NO_SUCH_STOCK,		// for admin msg only  
	OK,					// success; for order, completely filled
	ORDER_TOO_LARGE,	// dollar amt is too high as per config settings
	ORDER_TOO_SMALL,	// dollar amt is too small
	OVER_REDEMPTION_LIMIT, // RUSD is locked because they won it in a contest
	PARTIAL_FILL,		// order was partially filled; should never happen, we use AON
	POST_SPLIT, 
	PRE_SPLIT, 
	REDEMPTIONS_HALTED, // redemptions are halted as per the allowRedemptions setting in config
	REDEMPTION_PENDING, // user has twice submitted redemption and there is insufficient USDC in RefWallet
	REJECTED,			// order was rejected, reason unknown
	RUSD_LOCKED, 
	STALE_DATA,   // no recent market data for order rounded to zero
	TIMED_OUT,			// order timed out before being filled or canceled, or SIWE auth was too slow
	TOO_FAST,
	TOO_SLOW,
	TRADING_HALTED, // trading is halted as per the allowTrading config setting
	UNKNOWN,			// should never happen
	UPDATE_PROFILE, 
	VALIDATION_FAILED,
	ONRAMP_FAILED,
}
