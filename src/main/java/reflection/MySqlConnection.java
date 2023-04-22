package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

	public MySqlConnection connect(String host, String port, String db, String user, String password) throws SQLException {
		String url = String.format( "jdbc:postgresql://%s:%s/%s", host, port, db);
		return connect( url, user, password);
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
	
	/** json tags will be same as column names */
	public JSONArray queryToJson( String sql, Object... params) throws Exception {  // you could pass in the json labels, if you like
		ResultSet res = query(sql, params);
		
		ResultSetMetaData meta = res.getMetaData();

		JSONArray ar = new JSONArray();
		while (res.next() ) {
			JSONObject obj = new JSONObject();
			for (int i = 1; i <= res.getMetaData().getColumnCount(); i++) {
				obj.put( res.getMetaData().getColumnLabel(i), res.getObject(i) );
			}
			ar.add(obj);
		}
		return ar;
	}
	
	public ResultSet query(String sql, Object... params) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");
		String fullSql = String.format( sql, params);
		return connection.createStatement().executeQuery(fullSql);
	}
	
	/** Do not do Strin.format() substitutions on sql. */
	public void execute( String sql) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");
		connection.createStatement().executeUpdate(sql);
	}
	
	/** Don't call execute because the sql string could have percent signs in it
	 *  (e.g. FAQ table. */
	public void insert( String table, Object... values) throws Exception {
		insert( table, null, values);
	}

	/** If column names are not given, you can (must) give any number of columns starting with the first.
	 *  Single-quotes in the values are supported */  
	public void insert( String table, String[] columnNames, Object... values) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");

		// build values string
		StringBuilder valStr = new StringBuilder();
		for (Object val : values) {
			if (valStr.length() > 0) {
				valStr.append(',');
			}
			if (val != null) {
				String str = val instanceof String 
						? String.format( "'%s'", Util.dblQ((String)val))  // double-up the single-quotes 
						: val.toString(); 
				valStr.append( str);
			}
			else {
				valStr.append( "NULL");
			}
		}
		
		String sql;
		
		if (columnNames != null) {
			Main.require( columnNames.length == values.length, RefCode.UNKNOWN, "mismatched column/values when inserting");

			sql = String.format( "insert into %s (%s) values (%s)", 
					table,
					Util.concatenate( ',', columnNames),				
					valStr);
		}
		else {
			sql = String.format( "insert into %s values (%s)",
					table,
					valStr);
		}
		
		execute(sql);
	}

	public void dropTable(String table) throws Exception {
		try {
			execute( "drop table " + table);
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
