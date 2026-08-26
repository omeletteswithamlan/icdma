package mtu.construction.interpreter.databaseconnector;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection
{
	protected Connection conn;
	protected Statement sql;
	
	public DBConnection(String databasetype, String server, int port, String datasource, String user, String pass) throws SQLException
	{
		try
		{
			String url = "jdbc:" + databasetype + "://" + server + ":" + port + "/" + datasource;
			Driver driver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
			if(!driver.acceptsURL(url))
				throw new SQLException("Unable to connect to the database");

			conn = DriverManager.getConnection(url, user, pass);
		}
		catch(InstantiationException e)
		{
			throw new SQLException("The program could not execute because the database failed to instantiate.");
		}
		catch(ClassNotFoundException e)
		{
			throw new SQLException("The program could not execute because the database driver was not found");
		}
		catch(IllegalAccessException e)
		{
			throw new SQLException("The program could not execute because you do not have access to the database");
		}
	}
	
	public PreparedStatement getPreparedStatement(String s) throws SQLException
	{
		return conn.prepareStatement(s);
	}
	
	public Statement createStatement() throws SQLException
	{
		return conn.createStatement();
	}
}
