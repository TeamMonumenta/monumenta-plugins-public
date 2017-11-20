package pe.project.npcs.quest.actions;

import org.bukkit.entity.Player;

import pe.project.Plugin;

public interface BaseAction {
	public enum actionType {
		Dialog,
		SetScores,
		Function
	}
	
	public actionType getType();
	public void trigger(Plugin plugin, Player player);
}
