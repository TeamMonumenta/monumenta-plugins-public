package com.playmonumenta.plugins.itemindex;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playmonumenta.plugins.Plugin;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
			System.out.println(json);
			JsonObject root = new JsonParser().parse(json).getAsJsonObject();
			JsonElement elem = root.get("monumentaItem");
			if (elem == null) {
				return new Gson().fromJson(json, MonumentaItem.class);
			}
			return new Gson().fromJson(root.get("monumentaItem"), MonumentaItem.class);
		} catch (IllegalStateException e) {
			return null;
		} catch (NullPointerException e) {
			e.printStackTrace();
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
	public MonumentaItem getIndexMMItem(ItemStack itemStack) {
		MonumentaItem i = getIndexMMItem(itemStack.getType(), ChatColor.stripColor(itemStack.getItemMeta().getDisplayName()));
		return i;
	}

	@Nullable
	public MonumentaItem getIndexMMItem(Material material, String name) {
		if (material == null || this.mItems == null || this.mItems.size() == 0) {
			return null;
		}
		Map<String, MonumentaItem> nameMap = this.mItems.getOrDefault(material, new TreeMap<>());
		if (name == null || nameMap == null || nameMap.size() == 0) {
			return null;
		}
		return nameMap.getOrDefault(name, null);
	}

	@Nullable
	public MonumentaItem getMMItemWithEdits(ItemStack itemStack) {
		String json = new NBTItem(itemStack).getString("MonumentaItemEdits");
		MonumentaItem edits = null;
		if (json != null) {
			edits = new Gson().fromJson(json, MonumentaItem.class);
		}
		Material m = null;
		String name = null;
		if (edits != null) {
			m = edits.getOldMaterial();
			name = edits.getOldName();
			if (name != null) {
				name = ChatColor.stripColor(name.replace('&', '§'));
			}
		}
		if (m == null) {
			m = itemStack.getType();
		}
		if (name == null) {
			name = ChatColor.stripColor(itemStack.getItemMeta().getDisplayName());
		}
		MonumentaItem indexItem = getIndexMMItem(m, name);
		if (indexItem == null) {
			indexItem = new MonumentaItem();
			indexItem.setDefaultValues();
			indexItem.setMaterial(m);
		}
		MonumentaItem item = new MonumentaItem();
		item.setEdits(indexItem.clone());
		item.mergeEdits();
		item.setEdits(edits);
		return item;
	}

	public MonumentaItem[] getItemArray() {
		ArrayList<MonumentaItem> out = new ArrayList<>();
		for (Map.Entry<Material, Map<String, MonumentaItem>> entry : this.mItems.entrySet()) {
			if (entry.getValue() != null) {
				for (Map.Entry<String, MonumentaItem> entry2 : entry.getValue().entrySet()) {
					out.add(entry2.getValue());
				}
			}
		}
		return out.toArray(new MonumentaItem[0]);
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
