package hw6;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class GuestClient{
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
	
	public GuestClient(DataBase _DB) {
		db = _DB;
		conn = db.conn;
	}
	
	public int login(String _id, String _pass) {
		
		try {
			String query = "SELECT * FROM HOTEL_GUEST G WHERE G.ID = \'"+ _id + "\'";
			pstm = conn.prepareStatement(query);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			if (rs.next()) {
				if (rs.getString("password").equals(_pass)) {
					login = true;
					id = _id;
					name = rs.getString("name");
					money = rs.getInt("money");
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
			System.out.println("\n 1. Available Room List");
			System.out.println(" 2. Book Room");
			System.out.println(" 3. Check in(with reservation)");
			System.out.println(" 4. Check in(with out reservation)");
			System.out.println(" 5. Check out");
			System.out.println(" 6. My reservation");
			System.out.println(" 0. logout");
			System.out.print(" -----------input:");
			
			String input = scanner.next();
			int room = 0;
			String start_date = "", end_date="";
			switch(input) {
			case "1":
				showRoomlist();
				break;
			case "2":
				try {
					System.out.print("input room number : ");
					room = scanner.nextInt();
					start_date = DateSystem.getDate(scanner, true);
					end_date = DateSystem.getDate(scanner, false);
					
				}catch (Exception e) {
					System.out.println("[system] invalid input!");
				}
				if(DateSystem.checkPeriod(start_date, end_date) == true) {
					bookRoom(room, start_date, end_date);
				}else {
					System.out.println("[System] invalid period");
				}
				break;
			case "3":
				checkInWithR();
				break;
			case "4":
				start_date = DateSystem.getCurrentDatetime().toString().substring(0,10);
				try {
					System.out.print("input room number : ");
					room = scanner.nextInt();
					
					end_date = DateSystem.getDate(scanner, false);
				}catch (Exception e) {
					System.out.println("[system] invalid input!");
				}
				if(DateSystem.checkPeriod(start_date, end_date) == true) {
					checkInWithoutR(room, start_date, end_date);
				}else {
					System.out.println("[System] invalid period");
				}
				break;
			case "5":
				checkOut();
				break;
			case "6":
				showMyReservation(4);
				break;
			case "0":
				running = false;
				break;
			default:
				System.out.println("[system] Invalid input.");
			}
		}
	}
	
	private void showRoomlist() {
		/* 모든 방 목록을 출력 */
		try {
			String query = "SELECT * FROM HOTEL_ROOM WHERE STATUS = 0";
			pstm = conn.prepareStatement(query);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			int row	= 0;
			
			String header = String.format("|%-12s|%-12s|%-32s|", 
					" Room No", " price/day", " description");
			System.out.println(header);
            while(rs.next()){     
            	row ++;
            	System.out.printf("|%-12s|%-12s|%-32s|\n",rs.getString("ROOMNUMBER"),
            			rs.getString("PRICE"),rs.getString("DESCRIPTION"));
            }
            System.out.println("[system] Total " + row + " rooms are available");
            
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void bookRoom(int roomnumber, String start_date, String end_date) {

		/* 로직
		 * 1. rooms의 번호에 맞는 모든 reservation 선택
		 * 3. 해당 reservation의 시작시간과 종료시간 확인
		 * 3-1. 겹칠경우 사용 불가처리(rooms에서 제거)
		 * 3-2. 겹치지 않을경우 다음 reservation 확인
		 * 4.  모든 housekeeping 선택
		 * 5-1. 겹칠경우 사용불가 처리(rooms에서 제거)
		 * 5-2. 겹치지 않을경우 다음 housekeeping 확인
		 * 6. 모든 예약과 하우스킵핑이 겹치지 않음을 확인한경우 해당 방 사용가능으로 출력
		 */
		try {

			// 해당 방을 예약중인 다른 트랜잭션 있으면 대기, 없으면 락 걸고 진행
			System.out.println("waiting other users lock");
			Statement stmt = conn.createStatement();  
			stmt.execute("lock table HOTEL_RESERVATION in exclusive mode");  

			// 해당 방번호가 존재하는지 확인
			String query = "SELECT * FROM HOTEL_ROOM WHERE ROOMNUMBER = ?";
			pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			
			if(!rs.next()){
				System.out.println("[System] Invalid room number");
				return;
			}
						
			// 우리가 원하는 시간에 겹치는 예약이 존재하는지 찾기
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
		    query = "INSERT INTO HOTEL_RESERVATION( "
		    		+ "ROOMNUMBER, RDATE, STARTDATE,ENDDATE, GUESTNAME, RSTATUS) VALUES("
		    		+ "?, ?, ?, ?, ?, 0)";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			pstm.setDate(2, DateSystem.getCurrentDatetime());
			pstm.setDate(3, java.sql.Date.valueOf(start_date));
			pstm.setDate(4, java.sql.Date.valueOf(end_date));
			pstm.setString(5, id);

			result = pstm.executeUpdate();
			if (result == 1) {
				System.out.println("[system] book success(" + roomnumber+", " + start_date +", "+end_date + ")");	
			}else {
				System.out.println("[system] book failed(" + roomnumber+", " + start_date +", "+end_date + ")");	
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
	
	private void checkInWithR() {
		/* 자신의 예약중에 하나를 사용하여 checkin(RSTATUS=1)상태로 만듬 */
		ArrayList<tableReservation> list = showMyReservation(0);
		if (list.size() <= 0) {
			System.out.println("[System] You have no reservation.");
			return;
		}
		System.out.print("[System] select reservation number for check in : ");
		int input = scanner.nextInt();
		if(input <= 0 || input > list.size()) {
			System.out.println("[System] invalid reservation ");	
			return;
		}
		tableReservation selected = list.get(input-1);
		System.out.println("selected " + selected.roomnumber + " " + selected.startdate);
		try {
			System.out.println("waiting other users lock");
			Statement stmt = conn.createStatement();  
			stmt.execute("lock table HOTEL_RESERVATION in exclusive mode");  
			
			String query = "UPDATE HOTEL_RESERVATION "
		    		+ "SET CHECKIN = ?, "
				 	+ "RSTATUS = 1 "
				 	+ "WHERE ROOMNUMBER = ? AND STARTDATE = ?";
		    pstm = conn.prepareStatement(query);
			pstm.setDate(1, DateSystem.getCurrentDatetime());
			pstm.setInt(2, Integer.valueOf(selected.roomnumber));
			pstm.setDate(3, java.sql.Date.valueOf(selected.startdate));
			int result = pstm.executeUpdate();
			if (result == 0) {
				System.out.println("[system] check in failed");
				conn.rollback();
				return;
			}
			
			/* 방의 상태(STATUS)를 checked in 을 나타내는  2로 변경 */
			query = "UPDATE HOTEL_ROOM "
		    		+ "SET STATUS = 2 "
				 	+ "WHERE ROOMNUMBER = ?";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1, Integer.valueOf(selected.roomnumber));
			int result2 = pstm.executeUpdate();
			if (result2 == 0) {
				System.out.println("[system] check in failed");
				conn.rollback();
				return;
			}
			
			
			query = "UPDATE HOTEL_ROOM "
		    		+ "SET STATUS = 1 "
				 	+ "WHERE ROOMNUMBER = ?";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1, Integer.valueOf(selected.roomnumber));
			
			result = pstm.executeUpdate();
			if (result == 0) {
				System.out.println("[system] check in failed");	
			}
		
		    conn.commit();
			System.out.println("[system] check in success");
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	private void checkInWithoutR(int roomnumber, String start_date, String end_date) {
		/* 예약없이 바로 현재시간부터 종료시간까지 방을 사용*/
		try {
			// 해당 방번호가 존재하는지 확인
			String query = "SELECT * FROM HOTEL_ROOM WHERE ROOMNUMBER = ?";
			pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			
			if(!rs.next()){
				System.out.println("[System] Invalid room number");
				return;
			}
			// 해당 방을 예약중인 다른 트랜잭션 있는지 보고 멈추거나 내가 락걸거나

			System.out.println("waiting other users lock");
			Statement stmt = conn.createStatement();  
			stmt.execute("lock table HOTEL_RESERVATION in exclusive mode");  
			
			// 우리가 원하는 시간에 겹치는 예약이 존재하는지 찾기
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
				System.out.println("[system] conflict find, there is another reservation "+other_start +" ~ "+other_end);
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
		    query = "INSERT INTO HOTEL_RESERVATION( "
		    		+ "ROOMNUMBER, RDATE, STARTDATE,ENDDATE, GUESTNAME, RSTATUS) VALUES("
		    		+ "?, ?, ?, ?, ?, 1)";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			pstm.setDate(2, DateSystem.getCurrentDatetime());
			pstm.setDate(3, java.sql.Date.valueOf(start_date));
			pstm.setDate(4, java.sql.Date.valueOf(end_date));
			pstm.setString(5, id);

			result = pstm.executeUpdate();
			if (result == 1) {
				System.out.println("[system] check in success(" + roomnumber+", " + start_date +", "+end_date + ")");	
			}else {
				System.out.println("[system] check in failed(" + roomnumber+", " + start_date +", "+end_date + ")");	
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
	private void checkOut() {
		/* 자신의 예약중 checkin 상태인것을 checkout(RSTATUS=2) 상태로 만들고 결제 */
		ArrayList<tableReservation> list = showMyReservation(1);
		System.out.print("[System] select reservation number for check out : ");
		int input = scanner.nextInt();
		if(input <= 0 || input > list.size()) {
			System.out.println("[System] invalid reservation ");	
			return;
			
		}
		tableReservation selected = list.get(input-1);
		System.out.println("selected " + selected.roomnumber + " " + selected.startdate);
		int roomnumber = Integer.valueOf(selected.roomnumber);
		
		try {
			/* 락 설정 */
			System.out.println("waiting other users lock");
			Statement stmt = conn.createStatement();  
			stmt.execute("lock table HOTEL_RESERVATION in exclusive mode");  
			
			/* 예약의 상태(STATUS)를 checked out 을 나타내는  2로 변경 */
			String query = "UPDATE HOTEL_RESERVATION "
		    		+ "SET CHECKOUT = ?, "
				 	+ "RSTATUS = 2 "
				 	+ "WHERE ROOMNUMBER = ? AND STARTDATE = ?";
		    pstm = conn.prepareStatement(query);
			pstm.setDate(1, DateSystem.getCurrentDatetime());
			pstm.setInt(2, Integer.valueOf(selected.roomnumber));
			pstm.setDate(3, java.sql.Date.valueOf(selected.startdate));
			int result = pstm.executeUpdate();
			if (result == 0) {
				System.out.println("[system] check out failed");
				conn.rollback();
				return;
			}

			/* 방의 상태(STATUS)를 checked out 을 나타내는  2로 변경 */
			query = "UPDATE HOTEL_ROOM "
		    		+ "SET STATUS = 3 "
				 	+ "WHERE ROOMNUMBER = ? ";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1, Integer.valueOf(selected.roomnumber));
			result = pstm.executeUpdate();
			if (result == 0) {
				System.out.println("[system] check out failed");
				conn.rollback();
				return;
			}
			
			/* 사용 시간을 통해 고객의 money를 감소시킴 */

			int price = 0;
			int period = 0;
			query = "SELECT * "
		    		+ "FROM HOTEL_RESERVATION "
				 	+ "WHERE ROOMNUMBER = ? AND STARTDATE = ?";
		    pstm = conn.prepareStatement(query);
		    pstm.setInt(1, roomnumber);
			pstm.setDate(2, java.sql.Date.valueOf(selected.startdate));
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			while(rs.next()){
				result ++;
				Date start = rs.getDate("STARTDATE");
				Date end = rs.getDate("ENDDATE");
			    long diffInMillies = Math.abs(end.getTime() - start.getTime());
				period = (int)TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
			}
			
			query = "SELECT * "
		    		+ "FROM HOTEL_ROOM "
				 	+ "WHERE ROOMNUMBER = ?";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1,roomnumber);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			while(rs.next()){
				result ++;
				price = rs.getInt("PRICE");
			}
			
			int total_price = price * period;
			
			query = "UPDATE HOTEL_GUEST "
		    		+ "SET MONEY = MONEY - ?"
				 	+ "WHERE ID = ?";
		    pstm = conn.prepareStatement(query);
			pstm.setInt(1, total_price);
			pstm.setString(2, id);
			result = pstm.executeUpdate();
			if (result == 0) {
				System.out.println("[system] check out failed");
				conn.rollback();
				return;
			}
			System.out.println("[system] Total price: " + total_price + " for " + period +" day");
		    conn.commit();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private ArrayList<tableReservation> showMyReservation(int _status) {
		/* status 0 : reserved
		 * 1 checkined
		 * 2 out
		 * 3 cancled
		 * 4 all
		 * 방 목록을 방의 상태와 함께 출력
		 */
		String query="";
		
		switch (_status){
		case 0: query = "SELECT * FROM HOTEL_RESERVATION WHERE GUESTNAME = ? AND RSTATUS = 0"; break;
		case 1: query = "SELECT * FROM HOTEL_RESERVATION WHERE GUESTNAME = ? AND RSTATUS = 1"; break;
		case 2: query = "SELECT * FROM HOTEL_RESERVATION WHERE GUESTNAME = ? AND RSTATUS = 2"; break;
		case 3: query = "SELECT * FROM HOTEL_RESERVATION WHERE GUESTNAME = ? AND RSTATUS = 4"; break;
		case 4: query = "SELECT * FROM HOTEL_RESERVATION WHERE GUESTNAME = ?"; break;
		}
		ArrayList<tableReservation> list = new ArrayList<>();
		try {
			
			pstm = conn.prepareStatement(query);
			pstm.setString(1,id);
			rs = pstm.executeQuery();
			rsmd = rs.getMetaData();
			int row	= 0;
			String header = String.format("|%2s|%-12s|%-12s|%-12s|%-12s|%-11s|", 
					"no"," Room No", " reserved", " start", " end", " status");
			System.out.println(header);
			
			
            while(rs.next()){     
            	row ++;
            	String status = "";
            	String roomnumber = rs.getString("ROOMNUMBER");
            	String startdate = rs.getString("STARTDATE").substring(0,10);
            	switch (rs.getInt("RSTATUS")) {
            	case 0:status = "reserved"; break;
            	case 1:status = "checked in"; break;
            	case 2:status = "checked out"; break;
            	case 3:status = "cancled"; break;
            	}

            	list.add(new tableReservation(roomnumber, startdate));
            	System.out.printf("|%2s|%-12s|%-12s|%-12s|%-12s|%-11s|\n",
            			row,
            			roomnumber,
            			rs.getString("RDATE").substring(0,10),
            			startdate,
            			rs.getString("ENDDATE").substring(0,10),
            			status);
            }
            System.out.println("[system] Total " + row + " reservations for " + id + " .");
            
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return list;      
	}
}
