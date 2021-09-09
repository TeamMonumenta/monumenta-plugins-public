package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class Phylactery implements BaseEnchantment {
	public static String PROPERTY_NAME = ChatColor.GRAY + "Phylactery";

	public static final double XP_KEPT = 0.1;
	public static final String SCOREBOARD = "PhylacteryXP";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public void onDeath(Plugin plugin, Player player, PlayerDeathEvent event, int level) {
		if (event.getKeepInventory()) {
			return;
		}

		//Subtract 100 so that low levels can't gain xp by dying
		int xp = (int) ((ExperienceUtils.getTotalExperience(player) - 100) * level * XP_KEPT);
		if (xp > 0) {
			int previousStorage = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD);
			if (previousStorage <= 0) {
				previousStorage = 0;
			}
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD, previousStorage + xp);
			player.sendMessage(ChatColor.GOLD + "" + (int) (100 * level * XP_KEPT) + "% of your experience has been stored. Collect your grave to retrieve it.");
		}
	}

	//Called when the final item in a grave is picked up or claimed
	public static void giveStoredXP(Player player) {
		int phylacteryXP = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD);
		if (phylacteryXP > 0) {
			ExperienceUtils.addTotalExperience(player, phylacteryXP);
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD, 0);
			player.sendMessage(ChatColor.GOLD + "You received the experience stored in the grave.");
		}
	}
}
