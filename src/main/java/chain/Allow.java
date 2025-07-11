package chain;

import com.ib.client.Types.Action;

/** Possible values for the Config allowTrading setting, and used for blacklisting specific wallets */
public enum Allow {
	Buy, Sell, All, None;

	public boolean allow(Action side) {
		return this == All || this.toString().equalsIgnoreCase(side.toString());
	}
}
