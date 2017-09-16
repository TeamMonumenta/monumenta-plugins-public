package pe.project.npcs.quest.prerequisites;

import org.bukkit.entity.Player;

public interface BasePrerequisite {
	public boolean prerequisiteMet(Player player);
}
