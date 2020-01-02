package hw6;
import java.io.File;
import java.util.*;

import javax.sound.sampled.AudioInputStream; 
import javax.sound.sampled.AudioSystem; 
import javax.sound.sampled.Clip;


public class HotelManager{
	static boolean running = true;
	static Scanner scanner = new Scanner(System.in);
	static String input;
	static DataBase db;
	static Clip audioclip;
	
	static void playSound(String file) {
		try {
	        AudioInputStream in = AudioSystem.getAudioInputStream(new File(file).getAbsoluteFile());
			audioclip = AudioSystem.getClip();
	        audioclip.open(in);
	        audioclip.start();
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	}

	public static void main(String[] args) {
		
		// connect db
		db = new DataBase();
		java.sql.DatabaseMetaData dbmd;
		System.out.println("[system] Connecting DB...");
		if(db.connect()==1) {
			System.out.println("[system] Connection failed");
			System.exit(1);
		}
		// play logo sound
				playSound("logo.wav");
				
		// Main logic
		String id, pass;
		while(running) {
			System.out.println("1. Login as Manager");
			System.out.println("2. Login as Guest");
			System.out.println("0. Quit");
			System.out.print("-----------input:");
							
			input = scanner.next();
			switch(input) {
			case "1":
				// Manager Mode
				ManagerClient manager = new ManagerClient(db);
				
				do {
					System.out.print("login id :");
					id = scanner.next().toLowerCase();
					System.out.print("login pass :");
					pass = scanner.next();
				} while(manager.login(id, pass) == 1);
				manager.run();
				break;
			case "2":
				// guest mode
				GuestClient guest = new GuestClient(db);
				do {
					System.out.print("login id :");
					id = scanner.next().toLowerCase();
					System.out.print("login pass :");
					pass = scanner.next();
				} while(guest.login(id, pass) == 1);
				guest.run();
				break;
			case "0":
				running = false;
				break;
			default:
				System.out.println("Invalid input.");
			}
		}
		db.disconnect();
		audioclip.stop();
	}
}
