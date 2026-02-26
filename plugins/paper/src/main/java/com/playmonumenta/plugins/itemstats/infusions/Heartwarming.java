package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class Heartwarming implements Infusion {
	@Override
	public InfusionType getInfusionType() {
		return InfusionType.HEARTWARMING;
	}

	@Override
	public String getName() {
		return "Heartwarming";
	}

	@Override
	public void onBlockDropItem(Plugin plugin, Player player, double value, BlockDropItemEvent event) {
		if (player.isSneaking()) {
			return;
		}

		// This method gets called with the combined infusion value of all slots,
		// however Heartwarming should only affect items in your main hand
		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager
			.getPlayerItemStatsMappings()
			.get(player.getUniqueId());
		if (playerItemStats == null || playerItemStats.getMainhandAddStats().get(this) <= 0) {
			return;
		}

		for (Item itemEntity : event.getItems()) {
			ItemStack drop = itemEntity.getItemStack();

			ItemStack furnaceResult = ItemUtils.getFurnaceResult(drop);
			if (furnaceResult == null) {
				continue;
			}

			itemEntity.setItemStack(furnaceResult);
		}
	}
}
