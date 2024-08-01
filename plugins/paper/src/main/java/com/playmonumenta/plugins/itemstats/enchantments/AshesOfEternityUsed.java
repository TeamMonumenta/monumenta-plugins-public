package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
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

public class AshesOfEternityUsed implements Enchantment {

	@Override
	public String getName() {
		return "Ashes of Eternity - Used";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ASHES_OF_ETERNITY_USED;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {
		// Replace used AoE with original AoE if daily reset has occurred since last use
		ItemStack item = event.getItem();
		if (ScoreboardUtils.getScoreboardValue(player, AshesOfEternity.ASHES_OF_ETERNITY_DAILY_VERSION_OBJECTIVE).orElse(0)
			!= ScoreboardUtils.getScoreboardValue(player, DailyReset.DAILY_VERSION_OBJECTIVE).orElse(0)) {

			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1, 1.25f);
			ItemStatUtils.addEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY, 1);
			ItemStatUtils.removeEnchantment(item, EnchantmentType.ASHES_OF_ETERNITY_USED);
			ItemUpdateHelper.generateItemStats(item);
			Plugin.getInstance().mItemStatManager.updateStats(player);
		}
	}
}
