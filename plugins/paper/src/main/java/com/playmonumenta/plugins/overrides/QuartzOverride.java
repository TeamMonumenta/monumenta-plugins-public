package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class QuartzOverride extends BaseOverride {
	@Override
	public boolean inventoryClickInteraction(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		if (event.getClick() == ClickType.RIGHT
			    && "Bone Shard".equals(ItemUtils.getPlainNameIfExists(event.getCurrentItem()))) {
			DivineJustice divineJustice = plugin.mAbilityManager.getPlayerAbility(player, DivineJustice.class);
			if (divineJustice != null && divineJustice.isEnhanced()) {
				event.getCurrentItem().subtract();
				divineJustice.applyEnhancementEffect(player, true);
				player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_STEP, SoundCategory.PLAYERS, 1, 1);
				player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_STEP, SoundCategory.PLAYERS, 1, 1.5f);
				return false;
			}
		}
		return true;
	}
}
