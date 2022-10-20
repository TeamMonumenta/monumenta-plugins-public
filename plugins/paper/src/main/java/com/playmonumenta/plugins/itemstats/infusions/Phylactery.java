package com.playmonumenta.plugins.itemstats.infusions;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.listeners.GraveListener;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
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
	public static final double DURATION_KEPT = 0.05;
	public static final int BASE_POTION_KEEP_LEVEL = 10;
	public static final String GRAVE_XP_SCOREBOARD = "PhylacteryXP";

	private static final HashMap<UUID, HashMap<PotionManager.PotionID, List<PotionUtils.PotionInfo>>> POTION_EFFECTS_MAP = new HashMap<>();
	private static final HashMap<UUID, List<EffectPair>> CUSTOM_EFFECTS_MAP = new HashMap<>();

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
		value -= BASE_POTION_KEEP_LEVEL;
		if (!event.getKeepLevel()) {
			int playerXp = ExperienceUtils.getTotalExperience(player);
			int newTotalXp = ExperienceUtils.getTotalExperience(event.getNewLevel()) + event.getNewExp();
			int xpLoss = playerXp - newTotalXp;
			int savedXp = (int) (xpLoss * value * XP_KEPT);
			if (savedXp > 0) {
				event.setDroppedExp((int) (event.getDroppedExp() * (1 - value * XP_KEPT)));
				if (!event.getKeepInventory() && GraveListener.gravesEnabled(player)) {
					int previousStorage = Math.max(0, ScoreboardUtils.getScoreboardValue(player, GRAVE_XP_SCOREBOARD).orElse(0));
					ScoreboardUtils.setScoreboardValue(player, GRAVE_XP_SCOREBOARD, previousStorage + savedXp);
					player.sendMessage(ChatColor.GOLD + "" + (int) (100 * value * XP_KEPT) + "% of your experience has been stored. Collect your grave to retrieve it.");
				} else {
					newTotalXp += savedXp;
					int newLevel = ExperienceUtils.getLevel(newTotalXp);
					int newXp = newTotalXp - ExperienceUtils.getTotalExperience(newLevel);
					event.setNewLevel(newLevel);
					event.setNewExp(newXp);
				}
			}
		}

		value += BASE_POTION_KEEP_LEVEL;

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

		POTION_EFFECTS_MAP.put(player.getUniqueId(), infoMap);

		// Store Effects into CUSTOM_EFFECTS_MAP
		List<Effect> effects = plugin.mEffectManager.getAllEffects(player);

		if (effects != null) {
			List<EffectPair> resultEffects = new ArrayList<>();
			for (Effect effect : effects) {
				if (effect.isBuff()) {
					try {
						String source = plugin.mEffectManager.getSource(player, effect);
						JsonObject effectObject = effect.serialize();
						Effect newEffect = EffectManager.getEffectFromJson(effectObject, plugin);
						if (newEffect != null) {
							newEffect.setDuration((int) (effect.getDuration() * value * DURATION_KEPT));
							resultEffects.add(new EffectPair(source, newEffect));
						}
					} catch (Exception e) {
						// cry
						e.printStackTrace();
					}
				}
			}
			plugin.mEffectManager.clearEffects(player);
			CUSTOM_EFFECTS_MAP.put(player.getUniqueId(), resultEffects);
		}
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
		HashMap<PotionManager.PotionID, List<PotionUtils.PotionInfo>> potionEffects = POTION_EFFECTS_MAP.remove(player.getUniqueId());
		if (potionEffects != null) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				for (PotionManager.PotionID id : potionEffects.keySet()) {
					plugin.mPotionManager.addPotionInfos(player, id, potionEffects.get(id));
				}
			}, 1);
		}

		List<EffectPair> customEffects = CUSTOM_EFFECTS_MAP.remove(player.getUniqueId());
		if (customEffects != null) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				for (EffectPair pair : customEffects) {
					String source = pair.mSource;
					Effect effect = pair.mEffect;

					// Skip Patreon Effects
					if (!source.startsWith("PatronShrine")) {
						plugin.mEffectManager.addEffect(player, source, effect);
					}
				}
			}, 1);
		}
	}

	public static class EffectPair {
		public final String mSource;
		public final Effect mEffect;

		public EffectPair(String source, Effect effect) {
			mSource = source;
			mEffect = effect;
		}
	}
}
