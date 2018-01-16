package pe.project.npcs.quest.actions;

import org.bukkit.entity.Player;

import pe.project.Plugin;

public class ActionRerunComponents implements ActionBase {
	String mNpcName;
	boolean mLocked = false;

	public ActionRerunComponents(String npcName) {
		mNpcName = npcName;
	}

	@Override
	public void doAction(Plugin plugin, Player player) {
		/*
		 * Prevent infinite loops by preventing this specific action
		 * from running itself again
		 */
		if (!mLocked) {
			mLocked = true;
			plugin.mNpcManager.interactEvent(plugin, player, mNpcName);
			mLocked = false;
		} else {
			plugin.getLogger().severe("Stopped infinite loop for NPC '" + mNpcName + "'");
		}
	}
}
