package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.utils.AdvancementUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RushAdvancements {
	private static final String ARTIFACT_ADVANCEMENT = "monumenta:challenges/r1/rushdown/wave25";
	private static final int ARTIFACT_ROUND = 8;

	private static final String REMNANT_ADVANCEMENT = "monumenta:challenges/r1/rushdown/wave40";
	private static final int REMNANT_ROUND = 20;

	private static final String ROSE_ADVANCEMENT = "monumenta:challenges/r1/rushdown/wave75";
	private static final int ROSE_ROUND = 45;

	public static void checkWaveConquered(Player player, int round, Location loc) {
		if (!AdvancementUtils.checkAdvancement(player, ARTIFACT_ADVANCEMENT) && round > ARTIFACT_ROUND) {
			AdvancementUtils.grantAdvancement(player, ARTIFACT_ADVANCEMENT);
		}
		if (!AdvancementUtils.checkAdvancement(player, REMNANT_ADVANCEMENT) && round > REMNANT_ROUND) {
			AdvancementUtils.grantAdvancement(player, REMNANT_ADVANCEMENT);
		}
		if (!AdvancementUtils.checkAdvancement(player, ROSE_ADVANCEMENT) && round > ROSE_ROUND) {
			AdvancementUtils.grantAdvancement(player, ROSE_ADVANCEMENT);
			RushReward.roseWoolDrop(loc);
		}
	}

}
