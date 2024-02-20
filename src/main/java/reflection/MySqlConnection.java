package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONAware;
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
	
	/** Pass full sql query; don't forget single-quotes around string search params */
	public JsonArray queryToJson( String sql, Object... params) throws Exception {  // you could pass in the json labels, if you like
		ResultSet res = query(sql, params);
		
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
		String fullSql = params.length == 0 ? sql : String.format( sql, params);
		return connection.createStatement().executeQuery(fullSql);
	}
	
	/** Do not do String.format() substitutions on sql. */
	public int execute( String sql) throws Exception {
		try {
			Util.require( connection != null, "you must connect to the database");
			return connection.createStatement().executeUpdate(sql);
		}
		catch( Exception e) {
			S.out( "Database error with sql " + sql);
			throw e;
		}
	}
	
	/** Try update first, if it failes, do an insert
	 *  Do not include the word 'where' in the where clause
	 *  MAKE SURE THE FIELD FROM THE WHERE CLAUSE IS INCLUDED IN THE JSON */
	public void insertOrUpdate( String table, JsonObject json, String where, Object... params) throws Exception {
		if (updateJson( table, json, where, params) == 0) {
			insertJson( table, json);
		}
	}
	
	public int execWithParams( String sql, Object...params) throws Exception {
		return execute( String.format( sql, params) );
	}
	
	/** Do not include the word 'where' in the where clause
	 *  Don't forget single quotes around string values in where clause */
	public int updateJson( String table, JsonObject json, String where, Object... params) throws Exception {
		Util.require( !where.startsWith( "where"), "Don't include where" );

		StringBuilder values = new StringBuilder();
		for (Object key : json.keySet() ) {
			if (values.length() > 0) {
				values.append(", ");
			}
			values.append(key + " = " + toSqlValue( json.get(key) ) );
		}
		
		return execute( String.format("update %s set %s where %s", table, values, String.format(where, params) ) );
	}
	
	/** These types are supported and get quoted.
	 *  See also JSONValue.getsQuoted */
	private static boolean getsQuoted(Object val) {
		return val instanceof String || val instanceof JSONAware || val instanceof Enum;
	}
	
	/** Return true for all supported types;
	 *  note that String is both primitive and gets quoted */
	private static boolean isSupportedType(Object val) {
		return Util.isPrimitive(val.getClass()) || getsQuoted(val);
	}
	
	/** Inserts null values, with a warning */
	public void insertJson( String table, JsonObject json) throws Exception {
		Util.require( connection != null, "you must connect to the database");

		StringBuilder colStr = new StringBuilder();
		StringBuilder valStr = new StringBuilder();
		
		Util.forEach( json, (key,val) -> {
			if (val != null) {
				Util.require( isSupportedType(val) , "Cannot insert non-primitive type " + val.getClass() );

				if (valStr.length() > 0) {
					colStr.append(',');
					valStr.append(',');
				}
				colStr.append( key);
				valStr.append( toSqlValue(val) );
			}
		});
		
		String sql = String.format( "insert into %s (%s) values (%s)",
				table, colStr, valStr);
		
		execute(sql);
	}

	/** Put strings in single quotes when inserting or updating */
	private String toSqlValue(Object val) {
		return getsQuoted(val)
				? String.format( "'%s'", Util.dblQ(val.toString()))  // double-up the single-quotes 
				: val != null ? val.toString() : "null"; 
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
	
	public interface SqlCommand { // rename to command
		public void run(MySqlConnection conn) throws Exception;
	}

	public interface SqlQuery {
		public JsonArray run(MySqlConnection conn) throws Exception;
	}

	@Override public void close() throws Exception {
		connection.close();
	}
}
