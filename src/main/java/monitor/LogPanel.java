package monitor;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

class LogPanel extends QueryPanel {
	static String names = "created_at,wallet_public_key,name,uid,type,chainid,code,data"; 
	static String sql = """
		select 
			log.created_at,
			log.wallet_public_key,
			first_name || ' ' || last_name as name,
			log.uid,
			type,
			chainid,
			data->>'code' as code,
			data
		from log
		left join users using (wallet_public_key)
		$where 
		order by created_at desc 
		$limit""";  // you must order by desc to get the latest entries

	LogPanel() {
		super( "log", names, sql);
	}

	void filterByUid( String uid) {
		where.setText( String.format( "where uid = '%s'", uid) );
		Util.wrap( () -> refresh() );
	}
	
	@Override protected void onDouble(String tag, Object val) {
		switch(tag) {
			case "wallet_public_key":
				Monitor.m_tabs.select( "Wallet");
				Monitor.m_walletPanel.setWallet( val.toString() );
				break;
			}
	}

	@Override protected String getTooltip(JsonObject row, String tag) {
		try {
			if (tag.equals("data") ) {
				String val = row.getString(tag);
				if ( S.isNotNull(val) ) {
					JsonObject obj = JsonObject.parse(val);
					obj.update( "filter", cookie -> Util.left(cookie.toString(), 40) ); // shorten the cookie or it pollutes the view
					return obj.toHtml();
				}
			}
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}