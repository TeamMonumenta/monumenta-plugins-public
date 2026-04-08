package com.playmonumenta.plugins.delves.abilities;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.commands.ShardSorterCommand;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.DungeonCommandMapping;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Morbid {

	public static final String DESCRIPTION = "Each death tears a crack through the realm.";
	private static final String REDIS_KEY = "MorbidDeathCounts";

	public static Component[] rankDescription(int level) {
		if (level == 1) {
			return new Component[]{
				Component.text("After 3 deaths, you are permanently"),
				Component.text("ejected from the dungeon and"),
				Component.text("cannot start another this week."),
				Component.text("You do not shatter on death.")
			};
		} else {
			return new Component[]{
				Component.text("Upon death, you are permanently"),
				Component.text("ejected from the dungeon and"),
				Component.text("cannot start another this week."),
				Component.text("You do not shatter on death.")
			};
		}
	}

	public static void applyModifiers(Player player, int level) {
		if (level == 0) {
			return;
		}

		String dungeonName = DelvesUtils.getDungeonName(player);
		JsonObject delvesData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), DelvesManager.KEY_DELVES_PLUGIN_DATA);
		// shouldnt be possible?
		if (delvesData == null) {
			MMLog.fine("Couldn't get morbid deaths for " + player.getName());
			return;
		}

		JsonObject deathCounts;
		if (delvesData.has(REDIS_KEY)) {
			deathCounts = delvesData.getAsJsonObject(REDIS_KEY);
		} else {
			deathCounts = new JsonObject();
			delvesData.add(REDIS_KEY, deathCounts);
		}

		int currentDeaths = deathCounts.has(dungeonName) ? deathCounts.get(dungeonName).getAsInt() : 0;
		currentDeaths++;
		deathCounts.addProperty(dungeonName, currentDeaths);

		int deathLimit = (level == 1) ? 3 : 1;
		if (currentDeaths >= deathLimit) {
			DungeonCommandMapping mapping = DungeonCommandMapping.getByShard(dungeonName);

			if (mapping != null) {
				player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2f, 2f);
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 1, false, false));
				player.sendMessage(Component.text("Void gashes your surroundings as you feel reality fracture around you. This morbid world is no longer stable enough for you to continue.", NamedTextColor.RED));

				if (player.getScoreboardTags().contains("DungeonRace")) {
					player.removeScoreboardTag("DungeonRace");
				}
				if (mapping.canAbandon(player)) {
					ScoreboardUtils.setScoreboardValue(player, mapping.getAccessName(), 0);
				} else {
					ScoreboardUtils.setScoreboardValue(player, mapping.getAccessName(), -1);
				}

				try {
					ShardSorterCommand.sortToShard(player, mapping.getAbandonShardName(player), null);
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
				}
			}

		}  else if (level == 1 && currentDeaths == 2) {
			player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2f, 2f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 1, false, false));
			player.sendMessage(Component.text("The crack widens; you feel that one more death will shatter it completely.", NamedTextColor.RED));
		} else {
			player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2f, 2f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 1, false, false));
			player.sendMessage(Component.text("Black as ink, the outer darkness begins to seep into this realm. Unease trembles through your body.", NamedTextColor.RED));
		}
	}


	public static void resetWeeklyDeaths(Player player) {
		JsonObject delvesData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), DelvesManager.KEY_DELVES_PLUGIN_DATA);
		if (delvesData != null && delvesData.has(REDIS_KEY)) {
			delvesData.remove(REDIS_KEY);
			MMLog.fine("Reset weekly morbid death counts for " + player.getName());
		}
	}
}
