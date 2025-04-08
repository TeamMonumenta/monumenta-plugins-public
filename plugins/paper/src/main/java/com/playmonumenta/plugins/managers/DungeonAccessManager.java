package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.DungeonCommandMapping;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

public class DungeonAccessManager {
	public static final Pattern RE_SHARD_INSTANCE_PART = Pattern.compile("-?\\d+$");

	public static void updateVisitTime(Player player) {
		String shardName = ServerProperties.getShardName();
		shardName = RE_SHARD_INSTANCE_PART.matcher(shardName).replaceAll("");

		DungeonCommandMapping dungeonCommandMapping = DungeonCommandMapping.getByShard(shardName);
		if (dungeonCommandMapping == null) {
			return;
		}

		String lastVisitObjective = dungeonCommandMapping.getLastVisitName();
		if (lastVisitObjective == null) {
			return;
		}

		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = scoreboard.getObjective(lastVisitObjective);
		if (objective == null) {
			scoreboard.registerNewObjective(
				lastVisitObjective,
				Criteria.DUMMY,
				Component.text(lastVisitObjective),
				RenderType.INTEGER
			);
		}

		ScoreboardUtils.setScoreboardValue(player, lastVisitObjective, (int) DateUtils.getDaysSinceEpoch());

		for (Component line : dungeonCommandMapping.getDungeonAccessTimeInfo(player)) {
			player.sendMessage(line);
		}
	}

	public static void checkPlayerAccess(Player player) {
		updateVisitTime(player);
		for (DungeonCommandMapping dungeonMapping : DungeonCommandMapping.values()) {
			dungeonMapping.checkPlayerAccess(player);
		}

		DungeonCommandMapping delveMapping = DungeonCommandMapping.getByDelveBounty(player);
		if (delveMapping != null) {
			delveMapping.delveBountyAbandonCheck(player);
		}
	}

}
