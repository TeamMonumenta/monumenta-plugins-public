package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class AshesOfEternity implements Enchantment {

	@Override
	public String getName() {
		return "Ashes of Eternity";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ASHES_OF_ETERNITY;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 9980; // before Resurrection
	}

	public static final String ASHES_OF_ETERNITY_DAILY_VERSION_OBJECTIVE = "AshesofEternityDailyVersion";

	@Override
	public void onHurtFatal(Plugin plugin, Player player, double value, DamageEvent event) {
		// Checks that the player has not activated AoE earlier in the same day. Necessary in order to prevent:
		// 1. Item replacements giving players 2 uses on thursdays
		// 2. Players using multiple AoE items in the same day
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ScoreboardUtils.getScoreboardValue(player, ASHES_OF_ETERNITY_DAILY_VERSION_OBJECTIVE).orElse(0)
			!= ScoreboardUtils.getScoreboardValue(player, DailyReset.DAILY_VERSION_OBJECTIVE).orElse(0)) {

			if (Resurrection.execute(plugin, player, event, null)) {

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 2, 2);
				ItemStatUtils.addEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY_USED, 1);
				ItemStatUtils.removeEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY);
				ItemUpdateHelper.generateItemStats(item);
				Plugin.getInstance().mItemStatManager.updateStats(player);

				ScoreboardUtils.setScoreboardValue(player, ASHES_OF_ETERNITY_DAILY_VERSION_OBJECTIVE,
					ScoreboardUtils.getScoreboardValue(player, DailyReset.DAILY_VERSION_OBJECTIVE).orElse(0));
			}
		}
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {
		// Replaces AoE with its used variant if the player has already used AoE earlier in the same day. Solves the same
		// issues as listed above, but is necessary in order to signal to the player that it cannot be used more than once.
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ScoreboardUtils.getScoreboardValue(player, ASHES_OF_ETERNITY_DAILY_VERSION_OBJECTIVE).orElse(0)
			== ScoreboardUtils.getScoreboardValue(player, DailyReset.DAILY_VERSION_OBJECTIVE).orElse(0)
			&& ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ASHES_OF_ETERNITY) == 1) {

			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, SoundCategory.PLAYERS, 1, 1);
			ItemStatUtils.addEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY_USED, 1);
			ItemStatUtils.removeEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY);
			ItemUpdateHelper.generateItemStats(item);
			Plugin.getInstance().mItemStatManager.updateStats(player);
		}
	}
}
