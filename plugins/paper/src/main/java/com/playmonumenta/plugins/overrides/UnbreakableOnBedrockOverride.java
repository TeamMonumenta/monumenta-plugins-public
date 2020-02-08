package com.playmonumenta.plugins.overrides;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class UnbreakableOnBedrockOverride extends BaseOverride {
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		if ((player.getGameMode() == GameMode.CREATIVE) || breakable(block)) {
			// Breaking was allowed - remove the metadata associated with the spawner
			if (block.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				block.removeMetadata(Constants.SPAWNER_COUNT_METAKEY, plugin);
			}
			return true;
		} else {
			MessagingUtils.sendActionBarMessage(plugin, player, "This block can not be broken!");
		}
		return false;
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return breakable(block);
	}

	private boolean breakable(Block block) {
		Block blockUnder = block.getLocation().add(0, -1, 0).getBlock();
		if (blockUnder != null && (
		        blockUnder.getType() == Material.BEDROCK ||
		        blockUnder.getType() == Material.BARRIER
		    )) {
			return false;
		}

		return true;
	}
}
