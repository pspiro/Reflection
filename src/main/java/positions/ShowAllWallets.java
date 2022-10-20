package positions;

import java.sql.ResultSet;

import reflection.MySqlConnection;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

/** Show all balances for all wallets from database. */
public class ShowAllWallets {
	private MySqlConnection m_database = new MySqlConnection();

	public static void main(String[] args) {
		try {
			new ShowAllWallets().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void run() throws Exception {
		GTable map = new GTable(NewSheet.Reflection, "Symbols", "TokenAddress", "Symbol", false);

		S.out( "Connecting to database");
		m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");
		
		S.out( "reading wallets");
		String sql = "select wallet, token, sum(quantity) from events group by wallet, token order by wallet, token";
		ResultSet res = m_database.query( sql);
		while( res.next() ) {
			String wallet = res.getString(1);
			String token = res.getString(2);
			double val = res.getDouble(3);   // look up the symbol
			
			S.out( "%s %s %s", wallet, map.get(token), val);
		}
	}
}
