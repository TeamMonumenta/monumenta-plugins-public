package pe.project.npcs.quest.actions;

import org.bukkit.entity.Player;

import pe.project.Main;

public interface BaseAction {
	public enum actionType {
		Dialog,
		SetScores,
		Function
	}
	
	public actionType getType();
	public void trigger(Main plugin, Player player);
}
