package com.playmonumenta.plugins.managers.travelanchor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public class WorldAnchorGroups {
	// If other special always-present groups are required, they need fixed UUIDs. To do this,
	// implement a custom UUIDv8 specification; the version and variant values are fixed, the rest are custom.
	public static final UUID DEFAULT_GROUP_UUID = new UUID(0L, 0L);
	public static final Material DEFAULT_GROUP_MATERIAL = Material.ENDER_PEARL;
	public static final String DEFAULT_GROUP_NAME = "Default Group";

	private final UUID mWorldId;
	private final AnchorGroup mDefaultGroup;
	private final Map<UUID, AnchorGroup> mGroupById = new HashMap<>();
	private final Map<String, AnchorGroup> mGroupByName = new TreeMap<>();

	public WorldAnchorGroups(World world) {
		mWorldId = world.getUID();

		AnchorGroup defaultGroup = null;
		File anchorGroupsFile = getAnchorGroupsFile();
		if (anchorGroupsFile != null && anchorGroupsFile.isFile()) {
			String anchorGroupsPath = anchorGroupsFile.getPath();
			try {
				JsonObject json = FileUtils.readJson(anchorGroupsPath);

				try {
					defaultGroup = new AnchorGroup(DEFAULT_GROUP_UUID, json.get("mDefaultGroup"), false);
				} catch (Exception groupEx) {
					String errorMessage = "Failed to load default anchor group at " + anchorGroupsPath + ": " + groupEx;
					MMLog.warning(errorMessage, groupEx);
					MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
				}

				if (!(json.get("mGroups") instanceof JsonArray groupsJson)) {
					throw new Exception("Expected 'groups' in " + anchorGroupsPath + " to be type JsonArray");
				}

				int index = 0;
				for (JsonElement groupJson : groupsJson) {
					try {
						AnchorGroup anchorGroup = new AnchorGroup(groupJson);
						mGroupById.put(anchorGroup.id(), anchorGroup);
						mGroupByName.put(anchorGroup.name(), anchorGroup);
					} catch (Exception groupEx) {
						String errorMessage = "Failed to load anchor group at " + anchorGroupsPath + " (group index " + index + "): " + groupEx;
						MMLog.warning(errorMessage, groupEx);
						MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
					}
					index++;
				}
			} catch (Exception ex) {
				String errorMessage = "Failed to load anchor group data at " + anchorGroupsPath + ": " + ex;
				MMLog.warning(errorMessage, ex);
				MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
			}
		}

		if (defaultGroup == null) {
			defaultGroup = new AnchorGroup(DEFAULT_GROUP_UUID, DEFAULT_GROUP_NAME, DEFAULT_GROUP_MATERIAL, false);
		}
		mDefaultGroup = defaultGroup;
	}

	public void save() {
		File anchorGroupsFile = getAnchorGroupsFile();
		if (anchorGroupsFile == null) {
			return;
		}

		String anchorGroupsPath = anchorGroupsFile.getPath();

		if (isDefault()) {
			if (anchorGroupsFile.isFile()) {
				try {
					FileUtils.deletePathAndEmptyParentFolders(anchorGroupsFile);
				} catch (Exception ex) {
					String errorMessage = "Failed to delete anchor group data at " + anchorGroupsFile.getPath() + ": " + ex;
					MMLog.warning(errorMessage, ex);
					MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
				}
			}
			return;
		}

		JsonArray groupsJsonArray = new JsonArray();
		for (AnchorGroup anchorGroup : mGroupById.values()) {
			groupsJsonArray.add(anchorGroup.toJson());
		}

		JsonObject json = new JsonObject();
		json.add("mDefaultGroup", mDefaultGroup.toJson());
		json.add("mGroups", groupsJsonArray);

		try {
			FileUtils.writeJsonSafely(anchorGroupsPath, json, false);
		} catch (IOException ex) {
			String errorMessage = "Failed to write anchor group data at " + anchorGroupsPath + ": " + ex;
			MMLog.severe(errorMessage, ex);
			MonumentaNetworkRelayIntegration.sendAdminMessage(errorMessage);
		}
	}

	public boolean isDefault() {
		if (!DEFAULT_GROUP_MATERIAL.equals(mDefaultGroup.itemMat())) {
			return false;
		}

		if (!DEFAULT_GROUP_NAME.equals(mDefaultGroup.name())) {
			return false;
		}

		return mGroupById.isEmpty();
	}

	public @Nullable World getWorld() {
		return Bukkit.getWorld(mWorldId);
	}

	public @Nullable File getAnchorGroupsFile() {
		World world = getWorld();
		if (world == null) {
			return null;
		}
		return FileUtils.getWorldMonumentaFile(world, "anchor_groups.json");
	}

	public @Nullable AnchorGroup anchorGroup(UUID id) {
		if (id.equals(mDefaultGroup.id())) {
			return mDefaultGroup;
		}

		return mGroupById.get(id);
	}

	public @Nullable AnchorGroup anchorGroup(String name) {
		if (name.equals(mDefaultGroup.name())) {
			return mDefaultGroup;
		}
		return mGroupByName.get(name);
	}

	public List<AnchorGroup> anchorGroups() {
		List<AnchorGroup> results = new ArrayList<>(mGroupByName.values());
		results.add(0, mDefaultGroup);
		return results;
	}

	public boolean addGroup(AnchorGroup anchorGroup) {
		String newName = anchorGroup.name();
		if (mDefaultGroup.name().equals(newName) || mGroupByName.containsKey(newName)) {
			return false;
		}

		mGroupById.put(anchorGroup.id(), anchorGroup);
		mGroupByName.put(anchorGroup.name(), anchorGroup);

		return true;
	}

	public boolean renameGroup(AnchorGroup anchorGroup, String newName) {
		if (mDefaultGroup.name().equals(newName) || mGroupByName.containsKey(newName)) {
			return false;
		}

		String oldName = anchorGroup.name();
		// Don't add special groups into the main groups! Not an error, though, you may rename them.
		if (mGroupByName.containsKey(oldName)) {
			mGroupByName.put(newName, anchorGroup);
		}
		anchorGroup.name(newName);
		mGroupByName.remove(oldName);

		return true;
	}

	public void deleteGroup(AnchorGroup anchorGroup) {
		mGroupById.remove(anchorGroup.id());
		mGroupByName.remove(anchorGroup.name());
	}
}
