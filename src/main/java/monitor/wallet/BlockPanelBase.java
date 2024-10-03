package monitor.wallet;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import monitor.Monitor;
import reflection.Config;
import web3.NodeInstance.Transfer;

public class BlockPanelBase extends JPanel {
	public static final String Me = "This wallet";
	public static final String RefWallet = "RefWallet";  // remove this. pas
	public static final String nullAddr = "0x00";
	static final String address = "address";
	static final String toAddress = "to";
	static final String fromAddress = "from";
	static final String valueDecimal = "value_decimal";
	static final String tokenSymbol = "token_symbol";
	static final String timestamp = "block_timestamp";

	protected String refWallet; 
	protected String wallet; // currently selected wallet

	BlockPanelBase() {
		super( new BorderLayout() );
		refWallet = Monitor.m_config.refWalletAddr();
	}

	// not used
	protected boolean weCare(Transfer trans) {
		return isRusd( trans) || isBusd( trans) || isStock( trans);
	}

	protected boolean isFromMe(Transfer trans) {
		return trans.from().equals( Me);
	}

	protected boolean isToMe(Transfer trans) {
		return trans.to().equals( Me);
	}

	protected boolean isFromRefWallet(Transfer trans) {
		return trans.from().equalsIgnoreCase( RefWallet);
	}

	protected boolean isToRefWallet(Transfer trans) {
		return trans.to().equalsIgnoreCase( RefWallet);
	}

	protected boolean isRusd(Transfer obj) {
		return obj.contract().equalsIgnoreCase( config().rusd().name() ); 
	}

	protected boolean isBusd(Transfer obj) {
		return obj.contract().equalsIgnoreCase( config().busd().name() );
	}

	/** this is not reliable; you need to check the token address */
	protected boolean isStock(Transfer obj) {
		return false; // . pas return obj.getString( tokenSymbol).endsWith( ".r");
	}

	protected boolean isMint(Transfer obj) {
		return obj.from().equals( nullAddr);
	}

	protected boolean isBurn(Transfer obj) {
		return obj.to().equals( nullAddr);
	}
	
	static Config config() {
		return Monitor.m_config;
	}
}
