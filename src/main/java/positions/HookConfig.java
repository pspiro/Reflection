package positions;

import org.json.simple.JsonObject;

import chain.Chain;
import common.Util;
import reflection.Config.Tooltip;

/** obsolete, remove. pas */
public class HookConfig {
	public enum HookType { None, Moralis, Alchemy }
	
	private Chain m_chain;

	public void setChain(Chain chain) {
		m_chain = chain;
	}
	
	public String getTooltip(Tooltip tag) {
		return tooltips.getString( tag.toString() );
	}

	public HookType hookType() {
		return Util.getEnum( m_chain.params().hookType(), HookType.values() );
	}

	public String hookServerUrlBase() {
		return m_chain.params().hookServerUrlBase();
	}

	public boolean noStreams() {
		return m_chain.params().noStreams();
	}
	
	public String alchemyChain() {
		return m_chain.params().alchemyChain();
	}

	public String getHookNameSuffix() {
		return m_chain.params().hookNameSuffix();
	}

	public double minTokenPosition() {
		return m_chain.params().minTokenPosition();
	}

	/** obsolete, ignored by Frontend */
	public int myWalletRefresh() { 
		return m_chain.params().myWalletRefresh(); 
	}
	
	static JsonObject tooltips = Util.toJson( 
			"approveButton", "Click here to approve your stablecoin for use on the Reflection system. You can give approval at the time an order is placed, but approving now makes for a smoother trading experience",
			"baseBalance", "This is the native token of your selected blockchain. You only need native token to 'approve' your first transaction. After that, Reflection pays all the gas fees.",
			"busdBalance", "This is the stablecoin that can be used to purchase Reflection stock tokens",
			"redeemButton", "Click here to exchange your RUSD for stablecoin. The stablecoin can then be converted to cash on other platforms.",
			"rusdBalance", "RUSD is the native stablecoin of the Reflection platform. It is what you receive when you sell a stock token, and likewise it can be used to buy more stock tokens. It is backed 1-to-1 with a combination of US dollars and other US-dollar-backed stablecoins and can be redeemed at no cost at any time."
			);
}
