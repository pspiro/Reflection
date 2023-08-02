package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.postgresql.util.PSQLException;

import common.Util;
import tw.util.S;

public class MySqlConnection implements AutoCloseable {
	private Connection connection;
	
	public Connection getConnection() {
		return connection;
	}
	
	public void startTransaction() throws SQLException {
		connection.setAutoCommit(false);
	}
	
	public void commit() throws SQLException {
		connection.commit();
		connection.setAutoCommit(true);
	}

	public void connect(String host, String port, String db, String user, String password) throws SQLException {
		String url = String.format( "jdbc:postgresql://%s:%s/%s", host, port, db);
		connect( url, user, password);
	}
	
	public void connect(String url, String user, String password) throws SQLException {
		connection = DriverManager.getConnection(url, user, password);
	}
	
	/** Returns with the set ready to be read; assumes there is data. */
	public ResultSet queryNext(String sql, Object... params) throws Exception {
		ResultSet set = query( sql, params);
		if (!set.next() ) {
			throw new Exception( "No rows returned");
		}
		return set;
	}
	
	public JsonArray queryToJson( String sql, Object... params) throws Exception {  // you could pass in the json labels, if you like
		ResultSet res = query(sql, params);
		
		ResultSetMetaData meta = res.getMetaData();

		JsonArray ar = new JsonArray();
		while (res.next() ) {
			JsonObject obj = new JsonObject();
			for (int i = 1; i <= res.getMetaData().getColumnCount(); i++) {
				obj.put( res.getMetaData().getColumnLabel(i), res.getObject(i) );
			}
			ar.add(obj);
		}
		return ar;
	}
	
	public ResultSet query(String sql, Object... params) throws Exception {
		Util.require( connection != null, "you must connect to the database");
		String fullSql = String.format( sql, params);
		return connection.createStatement().executeQuery(fullSql);
	}
	
	/** Do not do String.format() substitutions on sql. */
	public int execute( String sql) throws Exception {
		Util.require( connection != null, "you must connect to the database");
		return connection.createStatement().executeUpdate(sql);
	}
	
	/** Don't call execute because the sql string could have percent signs in it
	 *  This version will break if table columns are changed
	 *  (e.g. FAQ table. */
	public void insert( String table, Object... values) throws Exception {
		insert( table, null, values);
	}
	
	/** Try update first, if it failes, do an insert
	 *  Do not include the word 'where' in the where clause */
	public void insertOrUpdate( String table, JsonObject json, String where, Object... params) throws Exception {
		if (updateJson( table, json, where, params) == 0) {
			insertJson( table, json);
		}
	}
	
	public void insertJson( String table, JsonObject json) throws Exception {
		String[] names = new String[json.size()];
		Object[] vals = new Object[json.size()];

		int i = 0;
		for (Object key : json.keySet() ) {
			names[i] = (String)key;
			vals[i++] = json.get(key);
		}
		insert(table, names, vals);
	}
	
	/** Do not include the word 'where' in the where clause
	 *  Don't forget single quotes around string values in where clause */
	public int updateJson( String table, JsonObject json, String where, Object... params) throws Exception {
		StringBuilder values = new StringBuilder();
		for (Object key : json.keySet() ) {
			if (values.length() > 0) {
				values.append(", ");
			}
			values.append(key + " = " + toSqlValue( json.get(key) ) );
		}
		
		return execute( String.format("update %s set %s where %s", table, values, String.format(where, params) ) );
	}
	
	/** Don't call execute because the sql string could have percent signs in it
	 *  (e.g. FAQ table. */
	public void insertPairs( String table, Object... pairs) throws Exception {
		Util.require( pairs.length % 2 == 0, "Error: MySqlConnection.insertPairs()");
		
		int len = pairs.length / 2;
		
		String[] names = new String[len];
		Object[] vals = new Object[len];
		
		int n = 0;
		for (int i = 0; i < len; i++) {
			names[i] = (String)pairs[n++];
			vals[i] = pairs[n++];
		}
		
		insert( table, names, vals);
	}

	/** If column names are not given, you can (must) give any number of columns starting with the first.
	 *  Single-quotes in the values are supported */  
	public void insert( String table, String[] columnNames, Object... values) throws Exception {
		Util.require( connection != null, "you must connect to the database");

		// build values string
		StringBuilder valStr = new StringBuilder();
		for (Object val : values) {
			if (valStr.length() > 0) {
				valStr.append(',');
			}
			if (val != null) {
				Util.require( Util.isPrimitive(val.getClass()), "Cannot insert non-primitive type " + val.getClass() );
				valStr.append( toSqlValue(val) );
			}
			else {
				valStr.append( "NULL");
			}
		}
		
		String sql;
		
		if (columnNames != null) {
			Util.require( columnNames.length == values.length, "mismatched column/values when inserting");

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

	/** Put strings in single quotes */
	private String toSqlValue(Object val) {
		return val instanceof String 
				? String.format( "'%s'", Util.dblQ((String)val))  // double-up the single-quotes 
				: val.toString(); 
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
	
	public interface SqlRunnable {
		public void run(MySqlConnection conn) throws Exception;
	}

	@Override public void close() throws Exception {
		connection.close();
	}
}
