package hw6;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

public class DateSystem {

	public static boolean checkDate(String date) {
		// very simple date type checker
		if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
		int month = Integer.parseInt(date.substring(5,7));
		int day = Integer.parseInt(date.substring(8,10));
		if(month > 12) return false;
		if(day > 31) return false;
		return true;
	}
	
	public static boolean checkPeriod(String start, String end) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date date1 = sdf.parse(start);
	        Date date2 = sdf.parse(end);
	        if(date1.compareTo(date2) < 0) return true;
	        else return false;
		}catch (Exception e){
			e.printStackTrace();
		}
		return false;
	}


	public static java.sql.Date getCurrentDatetime() {
	    java.util.Date today = new java.util.Date();
	    return new java.sql.Date(today.getTime());
	}
	
	public static String getDate(Scanner scanner, boolean start) {
		String date = "";
		while(true) {
			if(start)System.out.print("input start date(yyyy-mm-dd) : ");
			else System.out.print("input end date(yyyy-mm-dd) : ");
			date = scanner.next();
			if(DateSystem.checkDate(date)) break;
			else System.out.println("[system] invalid input!");
		}
		return date;
	}

}
