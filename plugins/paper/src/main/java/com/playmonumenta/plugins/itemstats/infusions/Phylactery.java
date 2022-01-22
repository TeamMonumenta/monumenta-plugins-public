package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public class Phylactery implements Infusion {

	public static final double XP_KEPT = 0.1;
	public static final String SCOREBOARD = "PhylacteryXP";

	@Override
	public @NotNull String getName() {
		return "Phylactery";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.PHYLACTERY;
	}

	@Override
	public void onDeath(Plugin plugin, Player player, double value, PlayerDeathEvent event) {
		//Subtract 100 so that low levels can't gain xp by dying
		int xp = (int) ((ExperienceUtils.getTotalExperience(player) - 100) * value * XP_KEPT);
		if (xp > 0) {
			int previousStorage = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD);
			if (previousStorage <= 0) {
				previousStorage = 0;
			}
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD, previousStorage + xp);
			player.sendMessage(ChatColor.GOLD + "" + (int) (100 * value * XP_KEPT) + "% of your experience has been stored. Collect your grave to retrieve it.");
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
