package monitor;

public class TransPanel {
	static QueryPanel createUsersPanel() {
		String names = "aadhaar,active,address,city,country,created_at,email,id,is_black_listed,kyc_status,name,pan_number,persona_response,phone,updated_at,wallet_public_key";
		String sql = "select * from users";
		return new QueryPanel( names, sql);
	}

	public static QueryPanel createTransPanel() {
		String names = "tds,rounded_quantity,perm_id,fireblocks_id,price,action,commission,currency,timestamp,cumfill,side,quantity,avgprice,wallet_public_key,conid,exchange,time,order_id,tradekey,status,";
		String sql = """
				select *
				from crypto_transactions ct
				left join trades tr on ct.order_id = tr.order_id
				;""";
//				join commissions co on tr.tradekey = co.tradekey
		return new QueryPanel( names, sql);
	}
}
