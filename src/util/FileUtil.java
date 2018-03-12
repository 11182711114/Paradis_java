package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileUtil {
	public static void writeToFile(String toWrite, File toWriteIn) throws IOException {
		if (!toWriteIn.exists()) {
			new File(toWriteIn.getAbsolutePath().substring(0, toWriteIn.getAbsolutePath().lastIndexOf(File.separator)))
					.mkdirs();
		}

		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(toWriteIn, true)));
		pw.println(toWrite);
		pw.flush();
		pw.close();
	}

	public static String[] readFromFile(File f) throws FileNotFoundException {
		List<String> output = new ArrayList<>();
		Scanner sc;

		sc = new Scanner(new BufferedReader(new FileReader(f)));

		while (sc.hasNextLine())
			output.add(sc.nextLine());

		sc.close();

		String[] tmpOut = shiftArray(output.toArray(new String[0]));
		tmpOut[0] = f.getName();

		return tmpOut;
	}

	private static String[] shiftArray(String[] s) {
		String[] tmp = new String[s.length + 1];
		for (int i = (s.length - 1); i > -1; i--) {
			tmp[i + 1] = s[i];
		}
		return tmp;
	}

	public static void writeToFile(String[] rt, File toWriteIn) throws IOException {
		if (!toWriteIn.exists()) {
			new File(toWriteIn.getAbsolutePath().substring(0, toWriteIn.getAbsolutePath().lastIndexOf(File.separator)))
					.mkdirs();
		}

		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(toWriteIn, true)));
		for (String s : rt) {
			pw.println(s);
		}
		pw.flush();
		pw.close();
	}
}