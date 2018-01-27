package pe.project.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.npcs.NpcQuest;
import pe.project.utils.FileUtils;
import pe.project.utils.MessagingUtils;

public class NpcManager {
	HashMap<String, NpcQuest> mNpcs = new HashMap<String, NpcQuest>();

	EnumSet<EntityType> mEntityTypes = EnumSet.noneOf(EntityType.class);

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		if (Constants.NPCS_ENABLED) {
			mNpcs = new HashMap<String, NpcQuest>();
			ArrayList<File> listOfFiles;
			ArrayList<String> listOfNpcs = new ArrayList<String>();
			int numComponents = 0;
			int numFiles = 0;

			// Attempt to load all JSON files in subdirectories of "quests"
			try {
				String questLocation = plugin.getDataFolder() + File.separator +  "quests";
				listOfFiles = FileUtils.getFilesInDirectory(questLocation, ".json");
			} catch (IOException e) {
				plugin.getLogger().severe("Caught exception trying to reload NPCs: " + e);
				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Caught exception trying to reload NPCs: " + e);
				}
				return;
			}

			for (File file : listOfFiles) {
				try {
					// Load this file into an NpcQuest object
					NpcQuest npc = new NpcQuest(file.getPath());

					// Keep track of statistics for pretty printing later
					int newComponents = npc.getComponents().size();
					numComponents += newComponents;
					numFiles++;
					listOfNpcs.add(npc.getNpcName() + ":" + Integer.toString(newComponents));

					// Track this type of entity from now on when entities are interacted with
					mEntityTypes.add(npc.getEntityType());

					// Check if an existing NPC already exists with quest components
					NpcQuest existingNpc = mNpcs.get(_squashNpcName(npc.getNpcName()));
					if (existingNpc != null) {
						// Existing NPC - add the new quest components to it
						existingNpc.addFromQuest(plugin, npc);
					} else {
						mNpcs.put(_squashNpcName(npc.getNpcName()), npc);
					}
				} catch (Exception e) {
					plugin.getLogger().severe("Caught exception: " + e);
					e.printStackTrace();

					if (sender != null) {
						sender.sendMessage(ChatColor.RED + "Failed to load quest file '" + file.getPath() + "'");
						MessagingUtils.sendStackTrace(sender, e);
					}
				}
			}

			if (sender != null) {
				sender.sendMessage(ChatColor.GOLD + "Loaded " +
				                   Integer.toString(numComponents) +
				                   " quest components from " + Integer.toString(numFiles) + " files");

				Collections.sort(listOfNpcs);
				String outMsg = "";
				for (String npc : listOfNpcs) {
					if (outMsg.isEmpty()) {
						outMsg = npc;
					} else {
						outMsg = outMsg + ", " + npc;
					}

					if (outMsg.length() > 1000) {
						sender.sendMessage(ChatColor.GOLD + outMsg);
						outMsg = "";
					}
				}

				if (!outMsg.isEmpty()) {
					sender.sendMessage(ChatColor.GOLD + outMsg);
				}
			}
		}
	}

	public NpcManager(Plugin plugin) {
		reload(plugin, null);
	}

	public boolean interactEvent(Plugin plugin, Player player, String npcName, EntityType entityType) {
		// Only search for the entity's name if we have a quest with that entity type
		if (!mEntityTypes.contains(entityType)) {
			return false;
		}

		// Only entities with custom names
		if (npcName == null || npcName.isEmpty()) {
			return false;
		}

		// Run the interaction if we have an NPC with that name
		NpcQuest npc = mNpcs.get(_squashNpcName(npcName));
		if (npc != null) {
			return npc.interactEvent(plugin, player, _squashNpcName(npcName), entityType);
		}

		return false;
	}

	private String _squashNpcName(String name) {
		return ChatColor.stripColor(name.replaceAll("\\s+", ""));
	}
}
