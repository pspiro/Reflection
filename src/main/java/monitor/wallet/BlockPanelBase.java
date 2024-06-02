package monitor.wallet;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.json.simple.JsonObject;

import monitor.Monitor;
import reflection.Config;

public class BlockPanelBase extends JPanel {
	static final String Me = "***";
	static final String RefWallet = "RefWallet";
	static final String toAddress = "to_address";
	static final String fromAddress = "from_address";
	static final String valueDecimal = "value_decimal";
	static final String tokenSymbol = "token_symbol";
	static final String timestamp = "block_timestamp";
	static final String nullAddr = "0x0000000000000000000000000000000000000000";

	protected String refWallet; 

	BlockPanelBase() {
		super( new BorderLayout() );
		refWallet = Monitor.m_config.refWalletAddr();
	}

	protected boolean weCare(JsonObject trans) {
		return isRusd( trans) || isUsdt( trans) || isStock( trans);
	}

	protected boolean isFromMe(JsonObject trans) {
		return trans.getString( fromAddress).equals( Me);
	}

	protected boolean isToMe(JsonObject trans) {
		return trans.getString( toAddress).equals( Me);
	}

	protected boolean isFromRefWallet(JsonObject trans) {
		return trans.getString( fromAddress).equalsIgnoreCase( RefWallet);
	}

	protected boolean isToRefWallet(JsonObject trans) {
		return trans.getString( toAddress).equalsIgnoreCase( RefWallet);
	}

	protected boolean isRusd(JsonObject obj) {
		return obj.getString( tokenSymbol).equals( "RUSD");
	}

	protected boolean isUsdt(JsonObject obj) {
		return obj.getString( tokenSymbol).equals( "USDT");
	}

	protected boolean isStock(JsonObject obj) {
		return obj.getString( tokenSymbol).endsWith( ".r");
	}

	protected boolean isMint(JsonObject obj) {
		return obj.getString( fromAddress).equals( "Mint");
	}

	protected boolean isBurn(JsonObject obj) {
		return obj.getString( toAddress).equals( "Burn");
	}
	
	static Config config() {
		return Monitor.m_config;
	}
}
