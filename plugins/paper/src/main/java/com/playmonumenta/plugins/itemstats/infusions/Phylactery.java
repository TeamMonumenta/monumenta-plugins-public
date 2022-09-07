package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.listeners.GraveListener;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public class Phylactery implements Infusion {

	public static final double XP_KEPT = 0.1;
	public static final double DURATION_KEPT = 0.1;
	public static final String GRAVE_XP_SCOREBOARD = "PhylacteryXP";

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
		if (!event.getKeepLevel()) {
			int playerXp = ExperienceUtils.getTotalExperience(player);
			if (playerXp > 0) {
				event.setDroppedExp((int) (event.getDroppedExp() * (1 - value * XP_KEPT)));
				if (!event.getKeepInventory() && GraveListener.gravesEnabled(player)) {
					int previousStorage = Math.max(0, ScoreboardUtils.getScoreboardValue(player, GRAVE_XP_SCOREBOARD).orElse(0));
					ScoreboardUtils.setScoreboardValue(player, GRAVE_XP_SCOREBOARD, previousStorage + (int) (playerXp * value * XP_KEPT));
					player.sendMessage(ChatColor.GOLD + "" + (int) (100 * value * XP_KEPT) + "% of your experience has been stored. Collect your grave to retrieve it.");
				} else {
					int newTotalXp = ExperienceUtils.getTotalExperience(event.getNewLevel()) + event.getNewExp();
					int xpLoss = playerXp - newTotalXp;
					if (xpLoss > 0) {
						newTotalXp += (int) (xpLoss * value * XP_KEPT);
						int newLevel = ExperienceUtils.getLevel(newTotalXp);
						int newXp = newTotalXp - ExperienceUtils.getTotalExperience(newLevel);
						event.setNewLevel(newLevel);
						event.setNewExp(newXp);
					}
				}
			}
		}

		HashMap<PotionManager.PotionID, List<PotionUtils.PotionInfo>> infoMap = new HashMap<>(plugin.mPotionManager.getAllPotionInfos(player));
		infoMap.remove(PotionManager.PotionID.SAFE_ZONE);
		infoMap.remove(PotionManager.PotionID.ABILITY_SELF);

		Iterator<List<PotionUtils.PotionInfo>> iter = infoMap.values().iterator();
		while (iter.hasNext()) {
			List<PotionUtils.PotionInfo> infoList = iter.next();
			for (PotionUtils.PotionInfo info : infoList) {
				if (info.mType != null && PotionUtils.hasNegativeEffects(info.mType)) {
					iter.remove();
				} else {
					info.mDuration = (int) (info.mDuration * value * DURATION_KEPT);
				}
			}
		}

		EFFECTS_MAP.put(player.getUniqueId(), infoMap);
	}

	//Called when the final item in a grave is picked up or claimed
	public static void giveStoredXP(Player player) {
		int phylacteryXP = ScoreboardUtils.getScoreboardValue(player, GRAVE_XP_SCOREBOARD).orElse(0);
		if (phylacteryXP > 0) {
			ExperienceUtils.addTotalExperience(player, phylacteryXP);
			ScoreboardUtils.setScoreboardValue(player, GRAVE_XP_SCOREBOARD, 0);
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
