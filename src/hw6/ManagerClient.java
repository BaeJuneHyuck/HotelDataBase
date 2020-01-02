package hw6;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class ManagerClient{
	DataBase db;
	boolean login = false;
	boolean running = true;
	Scanner scanner;
	String id;
	String pass;
	String name;
	int money;
	int type;
	
	Connection conn = null;
	PreparedStatement pstm = null;
	ResultSet rs = null;
	ResultSetMetaData rsmd = null;
	
	public ManagerClient(DataBase _DB) {
		db = _DB;
		conn = db.conn;
		pstm = db.pstm;
		rs = db.rs;
		rsmd = db.rsmd;
	}
	
	public int login(String id, String pass) {
		
		try {
			String query = "SELECT * FROM HOTEL_MANAGER M WHERE M.ID = \'"+ id + "\'";
			pstm = conn.prepareStatement(query);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			if (rs.next()) {
				if (rs.getString("password").equals(pass)) {
					login = true;
					name = rs.getString("name");
					type = rs.getInt("type");
					System.out.println("[system] Welcome "+ name);
					return 0;
				}else {
					System.out.println("[system] invalid password");
				}
			}else {
				System.out.println("[system] invalid id or password");
			}  	
		}catch (SQLException e) {
			e.printStackTrace();
		}		
		return 1;	
	}
	
	public void run() {
		while(running) {
			scanner = new Scanner(System.in);
			System.out.println("1. Room List");
			System.out.println("2. HouseKeeping List");
			System.out.println("3. Assign HouseKeeping");
			System.out.println("0. logout");
			System.out.print("-----------input:");
			
			String input = scanner.next();
			int room = 0;
			int employee = 0;
			String start_date = "";
			String end_date = "";
			
			switch(input) {
			case "1":
				showRoomlist();
				break;
			case "2":
				showKeeplist();
				break;
			case "3":
				try {
					System.out.print("input room number : ");
					room = scanner.nextInt();
					System.out.print("input employee number : ");
					employee = scanner.nextInt();
					start_date = DateSystem.getDate(scanner, true);
					end_date = DateSystem.getDate(scanner, false);
					
				}catch (Exception e) {
					System.out.println("[system] invalid input!");
				}
				if(DateSystem.checkPeriod(start_date, end_date) == true) {
					assignKeeping(employee, room, start_date, end_date);
				}else {
					System.out.println("[System] invalid period");
				}
				break;
			case "0":
				running = false;
				break;
			default:
				System.out.println("Invalid input.");
			}
		}
	}
	
	private void showRoomlist() {
		try {
			String query = "SELECT * FROM HOTEL_ROOM";
			pstm = conn.prepareStatement(query);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			int row	= 0;
			
			String header = String.format("|%-12s|%-12s|%-12s|%-32s|", 
					" Room No"," Status", " price/day", " description");
			System.out.println(header);
            while(rs.next()){     
            	row ++;
            	int status = rs.getInt("STATUS");
            	String status_text= "";
    			switch(status) {
    			case 0: status_text = "free"; break;
    			case 1: status_text = "reserved"; break;
    			case 2: status_text = "checked in"; break;
    			case 3: status_text = "checked out"; break;
    			}
    					
            	System.out.printf("|%-12s|%-12s|%-12s|%-32s|\n",
            			rs.getString("ROOMNUMBER"),status_text,
            			rs.getString("PRICE"),rs.getString("DESCRIPTION"));
            }
            System.out.println("[system] Total " + row + " rooms.");
            
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void showKeeplist() {
		try {
			String query = "SELECT * FROM HOTEL_KEEPING";
			pstm = conn.prepareStatement(query);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			int row	= 0;
			
			String header = String.format("|%-10s|%-13s|%-22s|%-22s|", 
					" Room No"," Employee ID", " Start Time", " end Time");
			System.out.println(header);
            while(rs.next()){     
            	row++;

            	System.out.printf("|%-10s|%-13s|%-22s|%-22s|\n",
            			rs.getString("ROOMNUMBER"),rs.getInt("EMPLOYEEID"),
            			rs.getString("STARTTIME"),rs.getString("ENDTIME"));
            }
            System.out.println("[system] Total " + row + " housekeepings.");
            
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
	private void assignKeeping(int employee, int roomnumber, String start_date, String end_date) {
		try {
			// 락 설정
			System.out.println("waiting other users lock");
			Statement stmt = conn.createStatement();  
			stmt.execute("lock table HOTEL_RESERVATION in exclusive mode");  
	
			// 해당 사원이 존재하는지 확인
			String query = "SELECT * FROM HOTEL_EMPLOYEE WHERE ID = ?";
			pstm = conn.prepareStatement(query);
			pstm.setInt(1,employee);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			
			if(!rs.next()){
				System.out.println("[System] Invalid Employee number");
				return;
			}
			
			// 해당 방번호가 존재하는지 확인
			query = "SELECT * FROM HOTEL_ROOM WHERE ROOMNUMBER = ?";
			pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			
			if(!rs.next()){
				System.out.println("[System] Invalid room number");
				return;
			}
						
			// 원하는 시간에 겹치는 예약이 존재하는지 찾기
			query = "SELECT * FROM HOTEL_RESERVATION WHERE " +
					"   ROOMNUMBER = ? AND (" + 
					" 	? BETWEEN STARTDATE AND ENDDATE OR " + 
					"   ? BETWEEN STARTDATE AND ENDDATE OR " + 
					"   STARTDATE BETWEEN ? AND ? OR " + 
					"   ENDDATE BETWEEN ? AND ?)";
					
			pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			pstm.setString(2, start_date);
			pstm.setString(3, end_date);
			pstm.setString(4, start_date);
			pstm.setString(5, end_date);
			pstm.setString(6, start_date);
			pstm.setString(7, end_date);
			
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			int result = 0;
			while(rs.next()){
				result ++;
				String other_start = rs.getString("STARTDATE").substring(0,10);
				String other_end = rs.getString("ENDDATE").substring(0,10);
				System.out.println("[system] conflict find, There is another reservation "+other_start +" ~ "+other_end);
				conn.rollback();
				return;
			}		
			
			// 우리가 원하는 시간에 겹치는 하우스키핑이 존재하는지 찾기
			query = "SELECT * FROM HOTEL_KEEPING WHERE " +
					"   ROOMNUMBER = ? AND (" + 
					" 	? BETWEEN STARTTIME AND ENDTIME OR " + 
					"   ? BETWEEN STARTTIME AND ENDTIME OR " + 
					"   STARTTIME BETWEEN ? AND ? OR " + 
					"   ENDTIME BETWEEN ? AND ?)";
					
			pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			pstm.setString(2, start_date);
			pstm.setString(3, end_date);
			pstm.setString(4, start_date);
			pstm.setString(5, end_date);
			pstm.setString(6, start_date);
			pstm.setString(7, end_date);
			
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			while(rs.next()){
				String other_start = rs.getString("STARTDATE").substring(0,10);
				String other_end = rs.getString("ENDDATE").substring(0,10);
				System.out.println("[system] conflict find, There is a housekeeping "+other_start +" ~ "+other_end);
				conn.rollback();
				return;
			}
			
			// 의도적으로 커밋을 지연시켜 다른 사용자가 락을 기다리는 모습 확인
			Thread.sleep(3000);
						
		    // 예약 내용 테이블에 넣기
		    query = "INSERT INTO HOTEL_KEEPING( "
		    		+ "EMPLOYEEID, ROOMNUMBER, STARTTIME,ENDTIME) VALUES("
		    		+ "?, ?, ?, ?)";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1, employee);
			pstm.setInt(2, roomnumber);
			pstm.setDate(3, java.sql.Date.valueOf(start_date));
			pstm.setDate(4, java.sql.Date.valueOf(end_date));
	
			result = pstm.executeUpdate();
			if (result == 1) {
				System.out.println("[system] assign success(" + roomnumber+", " + start_date +", "+end_date + ")");	
			}else {
				System.out.println("[system] assign failed(" + roomnumber+", " + start_date +", "+end_date + ")");	
			}
		    conn.commit();
		}catch(Exception e) {
			try {
				conn.rollback();
			}catch (SQLException e2){
				e2.printStackTrace();
			}
			e.printStackTrace();
		}
	}
}
