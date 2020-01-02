package hw6;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

class tableRoom {
	final public String[] fieldNames = {
			"ROOMNUMBER","Type","STATUS","DESCRIPTION","PRICE"};
	final public String[] typeNames = {
			"NUMBER","NUMBER","NUMBER","VARCHAR2","NUMBER"};
}

class tableReservation {
	final public String[] fieldNames = {
			"ROOMNUMBER","RDATE","STARTDATE","ENDDATE","GUESTNAME","CHECKIN","CHECKOUT","RSTATUS"};
	final public String[] typeNames = {
			"NUMBER", "DATE", "DATE", "DATE", "VARCHAR2", "DATE", "DATE", "NUMBER"};
	public String roomnumber, startdate;
	public tableReservation(String _roomnumber, String _startdate) {
		roomnumber = _roomnumber;
		startdate = _startdate;
	}
}

class tableGuest {
	final public String[] fieldNames = {
			"ID","PASSWORD","NAME","TYPE","MONEY","PHONE"};
	final public String[] typeNames = {
			"VARCHAR2","VARCHAR2","VARCHAR2","NUMBER","NUMBER","VARCHAR2"};
}

class tableKeeping {
	final public String[] fieldNames = {
			"EMPLOYEEID","ROOMNUMBER", "STARTTIME","ENDTIME"};
	final public String[] typeNames = {
			"NUMBER","NUMBER", "DATE", "DATE"};
}

class tableManager {
	final public String[] fieldNames = {
			"ID","PASSWORD","NAME","TYPE"};
	final public String[] typeNames = {
			"VARCHAR2","VARCHAR2","VARCHAR2","NUMBER"};
}

class tableEmployee {
	final public String[] fieldNames = {
			"ID","NAME"};
	final public String[] typeNames = {
			"NUMBER","VARCHAR2"};
}

public class DataBase {
	final static String DB_URL = 
			"jdbc:oracle:thin:@localhost:1521:xe";
	final static String USER = "hr";
	final static String PASS = "oracle";
	
	public Connection conn = null;
	public PreparedStatement pstm = null;
	public ResultSet rs = null;
	public ResultSetMetaData rsmd = null;
		
	public int connect() {
		try {
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			DatabaseMetaData dbMetaData = conn.getMetaData();
			if (dbMetaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE)) {
				conn.setTransactionIsolation(8); // 8 for TRANSACTION_SERIALIZABLE
				conn.setAutoCommit(false);
			}
			
			System.out.println("[system] Connection success");
			
		}catch(SQLException e) {
			e.printStackTrace();
			System.exit(-1);
			return 1;
		}
		return 0;
	}
	
	public int disconnect() {
		try {
			if(conn!= null)conn.close();
			if(rs!= null)rs.close();
			if(rs!= null)pstm.close();
			System.out.println("[system] Disconnected, good bye");
		}catch (SQLException e){
			e.printStackTrace();
			return 1;
		}
		return 0;
	}
}
