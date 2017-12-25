package pe.project.items;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class MobSpawnerOverride extends OverrideItem {
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		return (player.getGameMode() == GameMode.CREATIVE) || _breakable(block);
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return _breakable(block);
	}

	private boolean _breakable(Block block) {
		Block blockUnder = block.getLocation().add(0, -1, 0).getBlock();
		if (blockUnder != null && blockUnder.getType() == Material.BEDROCK) {
			return false;
		}

		return true;
	}
}
