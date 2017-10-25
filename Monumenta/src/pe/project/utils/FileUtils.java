package pe.project.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

public class FileUtils {
	public static String readFile(String fileName) throws Exception, FileNotFoundException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		String content = null;
		File file;

		if (fileName == null || fileName.isEmpty()) {
			throw new Exception("Filename is null or empty");
		}

		file = new File(fileName);
		if (!file.exists()) {
			throw new FileNotFoundException("File '" + fileName + "' does not exist");
		}

		FileReader reader = null;
		try {
			reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			content = new String(chars);
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

	public static void moveFile(String fromFile, String toFile) throws Exception, FileNotFoundException {
		if (fromFile == null || fromFile.isEmpty()) {
			throw new Exception("fromFile is null or empty");
		}
		if (toFile == null || toFile.isEmpty()) {
			throw new Exception("toFile is null or empty");
		}

		// Open the new file if it exists
		File sourceFile = new File(fromFile);
		if (!sourceFile.exists()) {
			throw new FileNotFoundException("sourceFile '" + fromFile + "' does not exist");
		}

		// Make sure the target directory exists
		File destFile = new File(toFile);
		destFile.getParentFile().mkdirs();

		// Rename the file and overwrite anything there
		sourceFile.renameTo(destFile);
	}
}
