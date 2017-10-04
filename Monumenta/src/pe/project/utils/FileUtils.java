package pe.project.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {
	public static String readFile(String fileName) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		String content = null;
		File file;

		if (fileName == null || fileName.isEmpty()) {
			throw new IOException("Filename is null or empty");
		}

		file = new File(fileName);
		if (!file.exists()) {
			throw new IOException("File '" + fileName + "' does not exist");
		}

		FileReader reader = null;
		try {
			reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			content = new String(chars);
			reader.close();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		return content;
	}

	public static void writeFile(String fileName, String contents) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		File file = new File(fileName);

		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			writer.write(contents);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
