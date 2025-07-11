package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.simple.JSONAware;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.postgresql.util.PGobject;
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
				obj.put( res.getMetaData().getColumnLabel(i), getJsonObj( res.getObject(i) ) );
			}
			ar.add(obj);
		}
		return ar;
	}
	
	/** Convert Postgres object to json object; note that Users.persona_response is string, not json 
	 * @throws Exception */
	private Object getJsonObj(Object obj) throws Exception {
		return obj instanceof PGobject gob && gob.getType().equals("jsonb")
				? JSONAware.parse( obj.toString() )
				: obj;
	}

	/** Pass full sql query; don't forget single-quotes around string search params;
	 *  can return null */
	public JsonObject querySingleRecord( String sql, Object... params) throws Exception {
		JsonArray ar = queryToJson( sql, params);
		return ar.size() > 0 ? ar.get(0) : null;
	}
	
	public ResultSet query(String sql, Object... params) throws Exception {
		Util.require( connection != null, "you must connect to the database");
		String fullSql = params.length == 0 ? sql : String.format( sql, params);
		return connection.createStatement().executeQuery(fullSql);
	}
	

	/** @param sql is the complete query including the delete from 
	 *  @return the number of rows deleted */
	public int delete(String sql, Object... params) throws Exception {
		int num = execWithParams( sql, params);
		if (num == 0) {
			S.out( "Warning: no rows deleted");
		}
		return num;
	}
	
	/** This can be used for insert, update, or delete
	 * 
	 *  Do not do String.format() substitutions on sql. */
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
	 *  MAKE SURE THE FIELD FROM THE WHERE CLAUSE IS INCLUDED IN THE JSON
	 *  
	 *  WARNING: any table with an auto-generated ID column, the update will throw
	 *  an exception if there is no existing record (e.g. users table)
	 *  
	 *  @return true for insert, false for update */
	public boolean insertOrUpdate( String table, JsonObject json, String where, Object... params) throws Exception {
		boolean inserted = false;
		
		if (updateJson( table, json, where, params) == 0) {
			insertJson( table, json);
			inserted = true;
		}
		
		return inserted;
	}
	
	/** This can be used for insert, update, or delete */
	public int execWithParams( String sql, Object...params) throws Exception {
		return execute( String.format( sql, params) );
	}
	
	/** Do not include the word 'where' in the where clause
	 *  Don't forget single quotes around string values in where clause */
	public int updateJson( String table, JsonObject json, String where, Object... params) throws Exception {
		if (!where.startsWith( "where")) {
			where = "where " + where;
		}

		StringBuilder values = new StringBuilder();
		for (Object key : json.keySet() ) {
			if (values.length() > 0) {
				values.append(", ");
			}
			values.append(key + " = " + toSqlValue( json.get(key) ) );
		}
		
		return execute( String.format("update %s set %s %s", table, values, String.format(where, params) ) );
	}
	
	/** These types are supported and get quoted.
	 *  See also JSONValue.getsQuoted */
	private static boolean getsQuoted(Object val) {
		return val instanceof String || val instanceof JSONAware || val instanceof Enum || val instanceof Date;
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
	
	public interface SqlCommand { // rename to command
		public void run(MySqlConnection conn) throws Exception;
	}

	public interface SqlQuery {
		public JsonArray run(MySqlConnection conn) throws Exception;
	}

	@Override public void close() throws Exception {
		connection.close();
	}
	
	/** For inserts and updates; always enters the time in NY timezone. This is not so great
	 *  as there will be ambiguity around daylight savings time changes */
	public static class MySqlDate extends Date {
		public MySqlDate( long time) {
			super( time);
		}
		
		public MySqlDate() {
		}
		
		@Override public String toString() {
			return Util.yToS.format( this);
		}
	}
}
