package lights;

import java.util.*;
import java.time.Clock;
import java.io.*;

public class Lights {

	public static void main(String[] args) throws Exception {

		Scanner s = new Scanner(System.in);
		Boolean x = false;
		int select;
		File cfg = new File("config.txt");

		// This array is purely symbolic of real lights.
		// It serves no functional purpose in the program.
		// 0 north, 1 south, 2 east, 3 west
		// Based on position, not direction.
		// Color codes same as parseColor uses.
		int[] lightStatus = new int[4];

		if (cfg.exists() && !cfg.isDirectory()) {
			do {
				System.out.printf("Run setup (1) or load config(2)?: ");
				select = s.nextInt();

				if (select == 1 || select == 2) {
					x = true;
				} else {
					System.out.println("Invalid selection.");
				}
			} while (!x);
		} else {
			select = 1;
		}

		if (select == 1) {
			int[] settings = new int[4];
			setup(settings, s);
			makeConfig(settings, s);
			Clock clock = Clock.systemUTC();
			File log = new File("log.txt");
			new Thread(() -> runLights(clock, settings, true, lightStatus)).start();
			new Thread(() -> runLights(clock, settings, false, lightStatus)).start();
			new Thread(() -> checkLight(lightStatus)).start();

		} else if (select == 2) {
			int[] settings = new int[4];
			Scanner sFile = new Scanner(cfg);
			String z = sFile.nextLine();
			sFile.close();
			String[] split = z.split("\\.");
			for (int i = 0; i < settings.length; i++) {
				  settings[i] = Integer.parseInt(split[i]);
				}
			Clock clock = Clock.systemUTC();
			File log = new File("log.txt");
			new Thread(() -> runLights(clock, settings, true, lightStatus)).start();
			new Thread(() -> runLights(clock, settings, false, lightStatus)).start();
			new Thread(() -> checkLight(lightStatus)).start();

		} else {
			System.out.println("ERROR: THERE SHOULD BE NO CASE WHERE YOU SEE THIS");
			System.exit(0);
		}
	}

	private static void setup(int[] x, Scanner s) {

		do {
			System.out.printf("How many seconds should a North/South green light last? ");
			x[0] = s.nextInt();
		} while (x[0] < 1);

		do {
			System.out.printf("How many seconds should an East/West green light last? ");
			x[2] = s.nextInt();
		} while (x[2] < 1);

		do {
			System.out.printf("How many seconds should a yellow light last? ");
			x[1] = s.nextInt();
		} while (x[1] < 1);

		do {
			System.out.printf("How many seconds should the red lights overlap? ");
			x[3] = s.nextInt();
		} while (x[3] < 0);
		return;
	}

	private static void makeConfig(int[] settings, Scanner s) {
		System.out.printf("Save config? (y/n) ");
		String x = s.next();
		if (x.startsWith("y") || x.startsWith("Y")) {
			FileWriter fw;
			try {
				fw = new FileWriter("config.txt", true);
				fw.write(settings[0] + "." + settings[1] + "." + settings[2] + "." + settings[3]);
				fw.close();
			} catch (IOException e) {
				System.out.println("Config creation failed.");
			}
		}
	}

	private static String parseColor(int x) {
		if (x == 0 || x == 3) {
			return "green";
		} else if (x == 1) {
			return "yellow";
		} else if (x == 2) {
			return "red";
		} else {
			System.out.println("Invalid color was used. Stopping...");
			System.exit(0);
			;
		}
		System.exit(0);
		return "This message should never print, but if it does, shut it down now";
	}

	private static String msg(int light, int color, Clock clock) {
		String time = clock.instant().toString();
		String lightx = "Error";
		if (light == 1) {
			lightx = "North/South";
		} else if (light == 2) {
			lightx = "East/West";
		}
		String x = String.format(
				time + ": Lights " + lightx + " changed from " + parseColor(color) + " to " + parseColor(color + 1));
		FileWriter fw;
		try {
			fw = new FileWriter("log.txt", true);
			fw.write(x + " ");
			fw.close();
		} catch (IOException e) {
			System.out.println("Log file does not exist!");
		}
		// TODO Make it also go to a log, unless I decide to do it outside the method
		// or you know I don't do that at all, just kinda depends.
		return x;
	}

	private static void runLights(Clock clock, int[] settings, Boolean isItNS, int[] lightStatus) {
		// North-south, east-west, yellow, overlap
		int ns = settings[0] * 1000;
		int ew = settings[2] * 1000;
		int ye = settings[1] * 1000;
		int ov = settings[3] * 1000;

		try {
			if (isItNS) {
				do {
					// Red to green, sleep length of ns green
					System.out.println(msg(1, 2, clock));
					setLight(settings, true, 0);
					Thread.sleep(ns);

					// Green to yellow, sleep length of yellow
					System.out.println(msg(1, 0, clock));
					setLight(settings, true, 1);
					Thread.sleep(ye);

					// Yellow to red, sleep length of both overlaps + ew green + yellow
					System.out.println(msg(1, 1, clock));
					setLight(settings, true, 2);
					Thread.sleep(ov * 2 + ew + ye);
				} while (true);

			} else {
				// Start red
				System.out.println(msg(2, 1, clock));
				setLight(settings, false, 1);
				do {
					// Sleep ns green + yellow + 1 overlap
					Thread.sleep(ns + ye + ov);

					// Red to green, sleep length of ew
					System.out.println(msg(2, 2, clock));
					setLight(settings, false, 0);
					Thread.sleep(ew);

					// Green to yellow, sleep length of yellow
					System.out.println(msg(2, 0, clock));
					setLight(settings, false, 1);
					Thread.sleep(ye);

					// Yellow to red, sleep 1 overlap, rest is at the start of loop
					System.out.println(msg(2, 1, clock));
					setLight(settings, false, 2);
					Thread.sleep(ov);
				} while (true);
			}
		} catch (InterruptedException e) {
			System.exit(0);
		}
	}

	private static void setLight(int[] x, Boolean y, int z) {
		if (y) {
			x[0] = z;
			x[1] = z;
		} else {
			x[2] = z;
			x[3] = z;
		}
	}

	private static void checkLight(int[] x) {
		if ((x[0] == 2 && x[2] == 2) || (x[0] == 2 && x[3] == 2) || (x[1] == 2 && x[2] == 2)
				|| (x[1] == 2 && x[3] == 2)) {
			try {
				Thread.sleep(500);
				if ((x[0] == 2 && x[2] == 2) || (x[0] == 2 && x[3] == 2) || (x[1] == 2 && x[2] == 2)
						|| (x[1] == 2 && x[3] == 2)) {
					System.out.println("Lights malfunctioning. Exiting.");
					System.exit(0);
				}
			} catch (InterruptedException e) {
			}
		}
	}
}
