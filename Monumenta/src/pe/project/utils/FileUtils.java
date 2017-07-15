package pe.project.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
	public static String getCreateFile(String fileName) throws IOException {
		String content = null;
		File file = new File(fileName);
		
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
		} catch(Exception e) {
			return null;
		}
		
		FileReader reader = null;
		
		try {
			reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
	        reader.read(chars);
	        content = new String(chars);
	        reader.close();
		} catch(Exception e) {
			return null;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		
		return content;
	}
	
	public static void writeFile(String fileName, String contents) throws IOException {
		File file = new File(fileName);
		
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
		} catch(Exception e) {
		}
		
		FileWriter writer = null;

		try {
			writer = new FileWriter(file);	
			writer.write(contents);
		} catch(Exception e) {
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
