package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgresql.util.PSQLException;

import tw.util.S;

public class MySqlConnection {
	private Connection connection;
	
	public void startTransaction() throws SQLException {
		connection.setAutoCommit(false);
	}
	
	public void commit() throws SQLException {
		connection.commit();
		connection.setAutoCommit(true);
	}

	public MySqlConnection connect(String url, String user, String password) throws SQLException {
		connection = DriverManager.getConnection(url, user, password);
		return this;
	}
	
	/** Returns with the set ready to be read; assumes there is data. */
	public ResultSet queryNext(String sql, Object... params) throws Exception {
		ResultSet set = query( sql, params);
		if (!set.next() ) {
			throw new Exception( "No rows returned");
		}
		return set;
	}
	
	public ResultSet query(String sql, Object... params) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");
		String fullSql = String.format( sql, params);
		return connection.createStatement().executeQuery(fullSql);
	}
	
	public void execute( String sql, Object... params) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");
		String fullSql = String.format( sql, params);
		connection.createStatement().executeUpdate(fullSql);
	}
	
	/** Don't call execute because the sql string could have percent signs in it
	 *  (e.g. FAQ table. */
	public void insert( String table, Object... values) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");

		StringBuilder valStr = new StringBuilder();
		
		for (Object val : values) {
			if (valStr.length() > 0) {
				valStr.append(',');
			}
			if (val != null) {
				String str = val instanceof String 
						? String.format( "'%s'", ((String)val).replaceAll( "'", "''") )  // double-up the single-quotes 
						: val.toString(); 
				valStr.append( str);
			}
			else {
				valStr.append( "NULL");
			}
		}

		String sql = String.format( "insert into %s values (%s)", table, valStr);
		connection.createStatement().executeUpdate(sql);
	}

	public void dropTable(String table) throws Exception {
		try {
			execute( "drop table %s", table);
		}
		catch( PSQLException e) {
			if (e.getMessage() != null && e.getMessage().contains("does not exist") ) {
				S.out( "Warning: table %s does not exist", table);
			}
			else {
				throw e;
			}
		}
	}

	/** @param sql is the complete query including the delete from */
	public void delete(String sql, Object... params) throws Exception {
		try {
			query( sql, params);
		} 
		catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().equals("No results were returned by the query.") ) {
				S.out( "Warning: no rows deleted");
			}
			else {
				throw e;
			}
		}
	}
}
