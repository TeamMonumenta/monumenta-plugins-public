package com.playmonumenta.plugins.utils;

import com.google.common.base.Ascii;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import io.papermc.paper.datapack.Datapack;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

public class FileUtils {
	public static String readFile(String fileName) throws Exception, FileNotFoundException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		File file;

		if (fileName == null || fileName.isEmpty()) {
			throw new Exception("Filename is null or empty");
		}

		file = new File(fileName);
		if (!file.exists()) {
			throw new FileNotFoundException("File '" + fileName + "' does not exist");
		}

		InputStreamReader reader = null;
		final int bufferSize = 1024;
		final char[] buffer = new char[bufferSize];
		final StringBuilder content = new StringBuilder();

		try {
			reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);
			while (true) {
				int rsz = reader.read(buffer, 0, buffer.length);
				if (rsz < 0) {
					break;
				}
				content.append(buffer, 0, rsz);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		return content.toString();
	}

	public static void writeFile(String fileName, String contents) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		File file = new File(fileName);

		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8)) {
			writer.write(contents);
		}
	}

	public static void moveFile(String fromFile, String toFile) throws Exception,
		FileNotFoundException {
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

	/**
	 * Returns a list of all files in the directory that are both regular files
	 * AND end with the specified string
	 */
	public static List<Path> getFilesInDirectory(Path folderPath,
	                                             String endsWith) throws IOException {
		ArrayList<Path> matchedFiles = new ArrayList<>();

		try (Stream<Path> stream = Files.walk(folderPath, 100, FileVisitOption.FOLLOW_LINKS)) {
			stream.forEach(path -> {
				if (Ascii.toLowerCase(path.toString()).endsWith(endsWith) && !path.toFile().isDirectory()) {
					// Note - this will pass directories that end with .json back to the caller too
					matchedFiles.add(path);
				}
			});
		}

		return matchedFiles;
	}

	public static void writeJson(String fileName, JsonObject json) throws IOException {
		writeJson(fileName, json, true);
	}

	public static void writeJson(String fileName, JsonObject json, boolean escapeHtmlCharacters) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		File file = new File(fileName);

		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		OutputStreamWriter writer = null;
		JsonWriter jsonWriter = null;
		Gson gson;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8);
			GsonBuilder gsonBuilder = new GsonBuilder();
			if (!escapeHtmlCharacters) {
				gsonBuilder.disableHtmlEscaping();
			}
			gson = gsonBuilder.create();
			jsonWriter = gson.newJsonWriter(writer);
			jsonWriter.setIndent("    ");
			gson.toJson(json, jsonWriter);
		} finally {
			if (jsonWriter != null) {
				jsonWriter.close();
			}
			if (writer != null) {
				writer.close();
			}
		}
	}

	public static JsonObject readJson(String fileName) throws Exception {
		// Do not attempt to catch exceptions here - let them propagate to the caller

		Gson gson = new Gson();

		Reader reader = Files.newBufferedReader(Paths.get(fileName));

		return gson.fromJson(reader, JsonObject.class);

	}

	/**
	 * Returns a list of all files matching the specified parameters in enabled datapacks.
	 *
	 * @param subfolder Should be the name of the subfolder the files of interest live in (i.e. loot_tables, advancements, functions)
	 * @param extension The expected file extension, including the "." (i.e. ".json", ".mcfunction")
	 */
	public static Map<NamespacedKey, Path> getEnabledDatapackFiles(String subfolder, String extension) {
		// Join the server root directory + primary world name + "datapacks" to get the root datapacks directory
		Path datapacksRoot = Paths.get(Bukkit.getServer().getWorldContainer().getAbsolutePath(), Bukkit.getServer().getWorlds().get(0).getName(), "datapacks");

		Map<NamespacedKey, Path> results = new LinkedHashMap<>();

		MMLog.fine("Searching for enabled datapack files matching subfolder '" + subfolder + "' and extension '" + extension + "'");
		for (Datapack pack : Bukkit.getDatapackManager().getEnabledPacks()) {
			if (pack.getName().startsWith("file/")) {
				String packName = pack.getName().substring("file/".length());
				Path datapackPath = Paths.get(datapacksRoot.toString(), packName);
				MMLog.finer("Searching datapack '" + packName + "': " + datapackPath);
				try {
					for (Path path : getFilesInDirectory(datapackPath, extension)) {
						// Get just the path under the datapack (e.g. data/monumenta/loot_tables/whatever)
						String subpath = datapackPath.relativize(path).toString();
						// Remove the file extension
						subpath = subpath.substring(0, subpath.length() - extension.length());

						// Split by file separator, max 4 parts ("data", "namespace", "loot_tables", "path")
						String[] split = subpath.split(Pattern.quote(File.separator), 4);
						if (split.length == 4) {
							if (split[2].equals(subfolder)) {
								// Matching file!
								try {
									NamespacedKey key = NamespacedKeyUtils.fromString(split[1] + ":" + split[3].replace(File.separator, "/"));
									results.put(key, path);
									MMLog.finer("Matched datapack file " + key + " -> " + path);
								} catch (IllegalArgumentException ex) {
									MMLog.warning("Datapack file name can't be parsed to NamespacedKey: " + path);
								}
							} else {
								MMLog.finest("Datapack file matches extension '" + extension + "' but not subfolder '" + subfolder + "': " + path);
							}
						} else {
							MMLog.warning("Datapack file is nested in an invalid folder. Likely should be moved or deleted: " + path);
						}
					}
				} catch (Exception ex) {
					MMLog.severe("Failed to load loot tables from datapack '" + datapackPath + "': " + ex.getMessage());
					ex.printStackTrace();
				}
			} else {
				MMLog.warning("Datapack '" + pack.getName() + "' isn't a file/ pack, so its contents won't be properly returned by getEnabledDatapackFiles()");
			}
		}
		MMLog.fine("Identified " + results.size() + " datapack files matching subfolder '" + subfolder + "' and extension '" + extension + "'");

		return results;
	}

	public static class SimpleImage {

		private BufferedImage mImage;
		private int mWidth;
		private int mHeight;

		public SimpleImage(Path imagePath) throws IllegalArgumentException, IOException {
			mImage = ImageIO.read(new File(imagePath.toUri()));
			mWidth = mImage.getWidth();
			mHeight = mImage.getHeight();
		}

		public void resize(int width, int height) {
			BufferedImage scaled = new BufferedImage(width, height, mImage.getType());
			AffineTransform at = new AffineTransform();
			at.scale((double) width / (double) mWidth, (double) height / (double) mHeight);
			AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			mImage = scaleOp.filter(mImage, scaled);
			mWidth = mImage.getWidth();
			mHeight = mImage.getHeight();
		}

		public int[][] getARGBMatrix() {
			byte[] pixels = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
			boolean mHasAlphaChannel = mImage.getAlphaRaster() != null;
			int mPixelLength = mHasAlphaChannel ? 4 : 3;
			int[][] argbMatrix = new int[mWidth][mHeight];

			for (int x = 0; x < mWidth; x++) {
				for (int y = 0; y < mHeight; y++) {
					int pos = (y * mPixelLength * mWidth) + (x * mPixelLength);

					int argb = -16777216; // 255 alpha
					if (mHasAlphaChannel) {
						argb = (((int) pixels[pos++] & 0xff) << 24); // alpha
					}

					argb += ((int) pixels[pos++] & 0xff); // blue
					argb += (((int) pixels[pos++] & 0xff) << 8); // green
					argb += (((int) pixels[pos++] & 0xff) << 16); // red
					argbMatrix[x][y] = argb;
				}
			}

			return argbMatrix;
		}

		public int getWidth() {
			return mWidth;
		}

		public int getHeight() {
			return mHeight;
		}
	}
}
