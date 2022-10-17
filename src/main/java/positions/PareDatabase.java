package positions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import reflection.MySqlConnection;
import tw.util.S;

/** Write at most one entry per wallet+token, and delete all zero balance entries.
 *  You have to leave the last block for two reasons; it could be partial,
 *  and the last block can get recalculated and that won't work for these
 *  aggregate rows */
public class PareDatabase {
	static MySqlConnection m_database = new MySqlConnection();

	public static void main(String[] args) throws SQLException, Exception {
		m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");
		
		int block = m_database.queryNext( "select max(block) from events").getInt(1) - 1;

		ResultSet res = m_database.query( "select wallet, token, sum(quantity) from events where block <= %s group by wallet, token order by wallet", block); // assume last block might be partial
		while (res.next() ) {
			String wallet = res.getString(1);
			String token = res.getString(2);
			double val = res.getDouble(3);
			String hash = genHash();
			
			S.out( "Paring %s %s", wallet, token);
			
			m_database.startTransaction();
			m_database.delete( "delete from events where wallet = '%s' and token = '%s'", wallet, token);
			m_database.insert("events", block, token, wallet, val, hash, EventFetcher.srcParer);
			m_database.commit();
		}
		
		m_database.delete( "delete from events where quantity = 0");
	}

	static Random rnd = new Random( System.currentTimeMillis() );

	/** Generate a transaction hash - 66 random characters */
	private static String genHash() {
		StringBuilder sb = new StringBuilder();
		for (int a = 0; a < 66; a++) {
			char c = (char)('a' + rnd.nextInt(26));
			sb.append(c);
		}
		return sb.toString();
	}
}
