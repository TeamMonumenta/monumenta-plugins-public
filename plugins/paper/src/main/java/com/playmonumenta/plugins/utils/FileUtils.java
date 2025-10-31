package com.playmonumenta.plugins.utils;

import com.google.common.base.Ascii;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.papermc.paper.datapack.Datapack;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
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
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class FileUtils {
	public static void writeFile(Path fileName, String contents) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		Files.createDirectories(fileName.getParent());
		Files.writeString(fileName, contents);
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

	public static void writeJson(String fileName, JsonElement json) throws IOException {
		writeJson(fileName, json, true);
	}

	public static void writeJson(String fileName, JsonElement json, boolean escapeHtmlCharacters) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		Path file = Path.of(fileName);

		if (!Files.exists(file)) {
			Files.createDirectories(file.getParent());
		}

		Gson gson;
		try (var writer = Files.newBufferedWriter(file)) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			if (!escapeHtmlCharacters) {
				gsonBuilder.disableHtmlEscaping();
			}
			gson = gsonBuilder.create();
			gson.toJson(json, writer);
		}
	}

	public static void writeJsonSafely(String fileName, JsonObject json, boolean escapeHtmlCharacters) throws IOException {
		String tempFileName = fileName + ".tmp";
		writeJson(tempFileName, json, escapeHtmlCharacters);

		File file = new File(fileName);
		File tempFile = new File(tempFileName);

		if (file.isFile()) {
			if (!file.delete()) {
				MMLog.warning("Failed to delete " + fileName + " before replacing it");
			}
		}

		if (!tempFile.renameTo(file)) {
			MMLog.warning("Failed to rename " + tempFileName + " to " + fileName);
		}
	}

	public static JsonObject readJson(String fileName) throws Exception {
		// Do not attempt to catch exceptions here - let them propagate to the caller

		Gson gson = new Gson();

		Reader reader = Files.newBufferedReader(Paths.get(fileName));

		return gson.fromJson(reader, JsonObject.class);

	}

	public static File getWorldMonumentaFolder(World world) {
		File worldFolder = world.getWorldFolder();
		return new File(worldFolder, "monumenta");
	}

	public static File getWorldMonumentaFile(World world, String fileName) {
		File worldMonumentaFolder = getWorldMonumentaFolder(world);
		File file = new File(worldMonumentaFolder, fileName);
		File tempFile = new File(worldMonumentaFolder, fileName + ".tmp");
		if (!file.isFile() && tempFile.isFile()) {
			// file was being replaced with tempFile, but interrupted; safe to use tempFile instead
			if (!tempFile.renameTo(file)) {
				return file;
			}
		}
		return file;
	}

	public static File getChunkMonumentaFolder(Location location) {
		File worldMonumentaFolder = getWorldMonumentaFolder(location.getWorld());
		Chunk chunk = location.getChunk();

		int cx = chunk.getX();
		int cz = chunk.getX();

		int rx = cx >> 5;
		int rz = cz >> 5;

		File monumentaRegionFolder = new File(worldMonumentaFolder, String.format("r.%d.%d", rx, rz));
		return new File(monumentaRegionFolder, String.format("c.%d.%d", cx, cz));
	}

	public static File getChunkMonumentaFile(Location location, String fileName) {
		File chunkMonumentaFolder = getChunkMonumentaFolder(location);

		File file = new File(chunkMonumentaFolder, fileName);
		File tempFile = new File(chunkMonumentaFolder, fileName + ".tmp");
		if (!file.isFile() && tempFile.isFile()) {
			// file was being replaced with tempFile, but interrupted; safe to use tempFile instead
			if (!tempFile.renameTo(file)) {
				return file;
			}
		}
		return file;
	}

	public static File getBlockMonumentaFile(Block block, String prefix, String suffix) {
		return getBlockMonumentaFile(block.getState(), prefix, suffix);
	}

	public static File getBlockMonumentaFile(BlockState block, String prefix, String suffix) {
		Location loc = block.getLocation();
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		String fileName = String.format("%s%d.%d.%d%s", prefix, x, y, z, suffix);
		return getChunkMonumentaFile(loc, fileName);
	}

	/**
	 * If the target path is a file or empty folder, delete it,
	 * recursively delete parent folders if they are now empty,
	 * and return true if anything was deleted at all, otherwise false.
	 *
	 * @param path The path to be deleted
	 * @return Returns true if a file/folder was deleted, otherwise false
	 * @throws Exception Any exceptions related to these action are to be handled by the caller.
	 */
	public static boolean deletePathAndEmptyParentFolders(File path) throws Exception {
		boolean deletedSomething = false;

		// Delete the target path
		if (path.isDirectory()) {
			// If the target path has child paths, or this cannot be determined, abort
			File[] children = path.listFiles();
			if (children == null || children.length > 0) {
				return false;
			}
			if (path.delete()) {
				deletedSomething = true;
			}
		} else if (path.isFile()) {
			if (path.delete()) {
				deletedSomething = true;
			}
		} else if (path.exists()) {
			// Some other path type? Links or block devices maybe? Not handled at any rate, abort.
			return false;
		}

		// Either we deleted something, or there was nothing to delete; recurse
		if (deletePathAndEmptyParentFolders(path.getParentFile())) {
			deletedSomething = true;
		}

		return deletedSomething;
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
