package reflection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlConnection {
	private Connection connection;

	public void connect(String url, String user, String password) throws SQLException {
		connection = DriverManager.getConnection(url, user, password);
	}
	
	public ResultSet query(String sql) throws SQLException {
		return connection .createStatement().executeQuery(sql);
	}
	
	public void execute( String sql) throws SQLException {
		connection.createStatement().executeUpdate(sql);
	}
	
	public void insert( String table, Object...values) throws SQLException {
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
}
