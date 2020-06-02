package com.playmonumenta.plugins.items;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ItemManager {
	private Map<Material, Map<String, MonumentaItem>> mItems;
	private File mItemFolderFile;

	public ItemManager() {
		this.mItemFolderFile = new File(Plugin.getInstance().getDataFolder().getPath() + File.separator + "ItemIndex");
		this.mItems = new TreeMap<>();
		this.load();
	}

	public void load() {
		File[] files = this.mItemFolderFile.listFiles();
		if (files == null || files.length == 0) {
			return;
		}
		for (File directory : files) {
			if (directory.isDirectory()) {
				this.mItems.put(Material.valueOf(directory.getName().toUpperCase()), this.loadMaterialDirectory(directory));
			}
		}
	}

	private Map<String, MonumentaItem> loadMaterialDirectory(File directory) {
		Map<String, MonumentaItem> out = new TreeMap<>();
		File[] files = directory.listFiles();
		if (files == null || files.length == 0) {
			return null;
		}
		for (File file : files) {
			try {
				String content = new String(Files.readAllBytes(file.toPath()));
				MonumentaItem item = jsonToItem(content);
				if (item == null) {
					continue;
				}
				String name = item.getNameColorless();
				out.put(name, item);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return out;
	}

	@Nullable
	public static MonumentaItem jsonToItem(String json) {
		if (json == null || json.length() == 0) {
			return null;
		}
		try {
			JsonObject root = new JsonParser().parse(json).getAsJsonObject();
			JsonElement elem = root.get("monumentaItem");
			if (elem == null) {
				return new Gson().fromJson(json, MonumentaItem.class);
			}
			return new Gson().fromJson(root.get("monumentaItem"), MonumentaItem.class);
		} catch (IllegalStateException e) {
			return null;
		}
	}

	public void addToItemMap(MonumentaItem item) {
		Map<String, MonumentaItem> submap = this.mItems.getOrDefault(item.getMaterial(), null);
		if (submap == null) {
			submap = new TreeMap<>();
		}
		String name = item.getNameColorless();
		submap.put(name, item);
		this.mItems.put(item.getMaterial(), submap);
	}

	public void updateFile(MonumentaItem item) {
		String material = item.getMaterial().toString().toLowerCase();
		String name = item.getNameColorless().replaceAll("[^A-Za-z0-9 ]", "").replace(' ', '_').toLowerCase();
		File dir = new File(this.mItemFolderFile.getPath() + File.separator + material);
		File file = new File(dir.getPath() + File.separator + name + ".json");
		try {
			dir.mkdirs();
			String json = item.toLootTablePrettyJson();
			System.out.println(json);
			com.google.common.io.Files.write(json, file, Charsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addToManager(MonumentaItem item) {
		this.addToItemMap(item);
		this.updateFile(item);
	}

	@Nullable
	public MonumentaItem getMMItem(ItemStack itemStack) {
		return getMMItem(itemStack.getType(), ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()));
	}

	@Nullable
	public MonumentaItem getMMItem(Material material, String name) {
		return this.mItems.getOrDefault(material, new TreeMap<>()).getOrDefault(name, null);
	}

	public MonumentaItem[] getItemArray() {
		ArrayList<MonumentaItem> out = new ArrayList<>();
		for (Map.Entry<Material, Map<String, MonumentaItem>> entry : this.mItems.entrySet()) {
			for (Map.Entry<String, MonumentaItem> entry2 : entry.getValue().entrySet()) {
				out.add(entry2.getValue());
			}
		}
		return out.toArray(new MonumentaItem[0]);
	}

	@Nullable
	public String extractJsonFromEditable(ItemStack item) {
		List<String> loreLines = item.getLore();
		if (loreLines == null || loreLines.size() < 1) {
			return null;
		}
		String lore = loreLines.get(0);
		String[] split = lore.split("§¬§¬§¬", -1);
		if (split[0].length() == lore.length()) {
			return null;
		}
		return StringUtils.convertToVisibleLoreLine(split[0]);
	}


	public MonumentaItem getMMItemFromEditable(ItemStack itemInMainHand) {
		String json = extractJsonFromEditable(itemInMainHand);
		return jsonToItem(json);
	}

	public void remove(MonumentaItem item) {
		Map<String, MonumentaItem> submap = this.mItems.get(item.getMaterial());
		String name = item.getNameColorless();
		submap.remove(name);
		String material = item.getMaterial().toString().toLowerCase();
		name = item.getNameColorless().replaceAll("[^A-Za-z0-9 ]", "").replace(' ', '_');
		File dir = new File(this.mItemFolderFile.getPath() + File.separator + material);
		File file = new File(dir.getPath() + File.separator + name + ".json");
		file.delete();
	}
}
