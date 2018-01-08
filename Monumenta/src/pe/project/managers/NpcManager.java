package pe.project.managers;

import java.io.File;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.npcs.NpcQuest;
import pe.project.utils.MessagingUtils;

public class NpcManager {
	HashMap<String, NpcQuest> mNpcs = new HashMap<String, NpcQuest>();

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		if (Constants.NPCS_ENABLED) {
			mNpcs = new HashMap<String, NpcQuest>();

			String questLocation = plugin.getDataFolder() + File.separator +  "quests";
			File[] listOfFiles = new File(questLocation).listFiles();

			// TODO: Would be nice if this supported an arbitrary directory structure
			// Perhaps walk all .json files?

			if (listOfFiles != null) {
				for (File file : listOfFiles) {
					String fileName = file.getName();
					if (fileName.endsWith(".json")) {
						try {
							// Load this file into an NpcQuest object
							NpcQuest npc = new NpcQuest(file.getPath());

							if (sender != null) {
								sender.sendMessage(ChatColor.GOLD + "Loaded " +
														Integer.toString(npc.getComponents().size()) +
														" quest components for NPC '" + npc.getNpcName() + "'");
							}

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
				}
			}
		}
	}

	public NpcManager(Plugin plugin) {
		reload(plugin, null);
	}

	public void interactEvent(Plugin plugin, Player player, String npcName) {
		if (npcName != null && !npcName.isEmpty()) {
			NpcQuest npc = mNpcs.get(_squashNpcName(npcName));
			if (npc != null) {
				npc.interactEvent(plugin, player, npcName);
			}
		}
	}

	private String _squashNpcName(String name) {
		return name.replaceAll("\\s+", "");
	}
}
