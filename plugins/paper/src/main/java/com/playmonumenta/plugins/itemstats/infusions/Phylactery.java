package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public class Phylactery implements Infusion {

	public static final double XP_KEPT = 0.1;
	public static final double DURATION_KEPT = 0.1;
	public static final String SCOREBOARD = "PhylacteryXP";

	private static HashMap<UUID, HashMap<PotionManager.PotionID, List<PotionUtils.PotionInfo>>> EFFECTS_MAP = new HashMap<>();

	@Override
	public String getName() {
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
			int previousStorage = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD).orElse(0);
			if (previousStorage <= 0) {
				previousStorage = 0;
			}
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD, previousStorage + xp);
			player.sendMessage(ChatColor.GOLD + "" + (int) (100 * value * XP_KEPT) + "% of your experience has been stored. Collect your grave to retrieve it.");
		}

		HashMap<PotionManager.PotionID, List<PotionUtils.PotionInfo>> infoMap = new HashMap<>(plugin.mPotionManager.getAllPotionInfos(player));
		infoMap.remove(PotionManager.PotionID.SAFE_ZONE);
		infoMap.remove(PotionManager.PotionID.ABILITY_SELF);

		for (List<PotionUtils.PotionInfo> infoList : infoMap.values()) {
			for (PotionUtils.PotionInfo info : infoList) {
				if (PotionUtils.hasNegativeEffects(info.mType)) {
					infoList.remove(info);
				} else {
					info.mDuration = (int) (info.mDuration * value * DURATION_KEPT);
				}
			}
		}

		EFFECTS_MAP.put(player.getUniqueId(), infoMap);
	}

	//Called when the final item in a grave is picked up or claimed
	public static void giveStoredXP(Player player) {
		int phylacteryXP = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD).orElse(0);
		if (phylacteryXP > 0) {
			ExperienceUtils.addTotalExperience(player, phylacteryXP);
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD, 0);
			player.sendMessage(ChatColor.GOLD + "You received the experience stored in the grave.");
		}
	}

	// Called by PlayerListener
	public static void applyStoredEffects(Plugin plugin, Player player) {
		HashMap<PotionManager.PotionID, List<PotionUtils.PotionInfo>> effects = EFFECTS_MAP.remove(player.getUniqueId());
		if (effects != null) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				for (PotionManager.PotionID id : effects.keySet()) {
					plugin.mPotionManager.addPotionInfos(player, id, effects.get(id));
				}
			}, 1);
		}
	}

}
