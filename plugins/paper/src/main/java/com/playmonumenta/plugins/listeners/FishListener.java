package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackFishCaught;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class FishListener implements Listener {

	@EventHandler(ignoreCancelled = false)
	public void onFish(PlayerFishEvent event) {
		Player player = event.getPlayer();
		//r3 fishing needs its own check as minigames and combat give fish.
		if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && !ServerProperties.getAbilityEnhancementsEnabled(player)) {
			Item caughtItem = (Item) event.getCaught();
			if (caughtItem == null) {
				return;
			}
			ItemStack caughtItemStack = caughtItem.getItemStack();
			Material caughtItemType = caughtItemStack.getType();
			if (Constants.Materials.FISH.contains(caughtItemType)) {
				StatTrackFishCaught.fishCaught(player);
			}
		}

	}
}
