package pe.project.managers;

import java.io.File;
import java.util.HashMap;

import org.bukkit.entity.Player;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.npcs.Npc;

public class NpcManager {
	Plugin mPlugin;
	HashMap<String, Npc> mNpcs = new HashMap<String, Npc>();

	public NpcManager(Plugin plugin) {
		mPlugin = plugin;

		if (Constants.NPCS_ENABLED) {
			String npcLocation = mPlugin.getDataFolder() + File.separator +  "npcs";
			File[] listOfFiles = new File(npcLocation).listFiles();

			if (listOfFiles != null) {
				for (File file : listOfFiles) {
					String fileName = file.getName();
					if (fileName.contains(".json")) {
						Npc npc = new Npc(mPlugin, file.getPath());
						mNpcs.put(_squashNpcName(npc.getName()), npc);
					}
				}
			}
		}
	}

	public void interactEvent(Player player, String npcName) {
		if (npcName != null && !npcName.isEmpty()) {
			Npc npc = mNpcs.get(_squashNpcName(npcName));
			if (npc != null) {
				npc.interactEvent(player, mPlugin.mRandom);
			}
		}
	}

	public void triggerEvent(Player player, String npcName, String questID, String eventName) {
		if (npcName != null && !npcName.isEmpty()) {
			Npc npc = mNpcs.get(npcName);
			if (npc != null) {
				npc.triggerEvent(player, questID, eventName);
			}
		}
	}

	private String _squashNpcName(String name) {
		return name.replaceAll("\\s+", "");
	}
}
