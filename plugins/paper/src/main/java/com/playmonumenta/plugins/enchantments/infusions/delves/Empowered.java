package com.playmonumenta.plugins.enchantments.infusions.delves;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.NotNull;

public class Empowered implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Empowered";
	private static final int XP_NEEDED = 2250;
	private static final int XP_MOLT_PER_LEVEL = 250;
	private static final double HP_PCT_HEALED = 0.1;
	private static final String SCOREBOARD_EMPOWERED = "EmpoweredXP";

	@Override
	public @NotNull String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent playerExpChangeEvent, int level) {
		int expAmount = playerExpChangeEvent.getAmount() + ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_EMPOWERED);

		int xpNeeded = (XP_NEEDED - (XP_MOLT_PER_LEVEL * (int) DelveInfusionUtils.getModifiedLevel(plugin, player, level)));
		if (expAmount > xpNeeded) {
			expAmount -= xpNeeded;
			PlayerUtils.healPlayer(player, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * HP_PCT_HEALED);
		}
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_EMPOWERED, expAmount);
	}
}
