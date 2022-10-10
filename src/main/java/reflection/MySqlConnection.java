package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgresql.util.PSQLException;

import tw.util.S;

public class MySqlConnection {
	private Connection connection;

	public MySqlConnection connect(String url, String user, String password) throws SQLException {
		connection = DriverManager.getConnection(url, user, password);
		return this;
	}
	
	public ResultSet query(String sql, Object... format) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");
		String fullSql = String.format( sql, format);
		return connection.createStatement().executeQuery(fullSql);
	}
	
	public void execute( String sql, Object... format) throws Exception {
		Main.require( connection != null, RefCode.UNKNOWN, "you must connect to the database");
		String fullSql = String.format( sql, format);
		connection.createStatement().executeUpdate(fullSql);
	}
	
	public void insert( String table, Object...values) throws Exception {
		StringBuilder valStr = new StringBuilder();
		
		for (Object val : values) {
			if (valStr.length() > 0) {
				valStr.append(',');
			}
			if (val != null) {
				String str = val instanceof String ? String.format( "'%s'", val) : val.toString(); 
				valStr.append( str);
			}
			else {
				valStr.append( "NULL");
			}
		}
		
		String sql = String.format( "insert into %s values (%s)", table, valStr);
		execute( sql);
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
				throw( e);
			}
		}
	}
}
