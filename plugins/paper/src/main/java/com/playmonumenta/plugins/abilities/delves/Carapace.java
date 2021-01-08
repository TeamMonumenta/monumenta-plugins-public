package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CarapaceBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;

public class Carapace extends DelveModifier {

	private static final int[] CARAPACE_HEALTH = {
			7,
			14,
			21,
			28,
			35
	};

	private static final int[] CARAPACE_SPEED_RAW_PERCENT = {
			2,
			4,
			6,
			8,
			10
	};

	public static final String DESCRIPTION = "Enemies gain a protective shell.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies block " + CARAPACE_HEALTH[0] + " Damage in any 10 second period.",
				"Enemies whose Carapace is inactive gain " + CARAPACE_SPEED_RAW_PERCENT[0] + "% Speed."
			}, {
				"Enemies block " + CARAPACE_HEALTH[1] + " Damage in any 10 second period.",
				"Enemies whose Carapace is inactive gain " + CARAPACE_SPEED_RAW_PERCENT[1] + "% Speed."
			}, {
				"Enemies block " + CARAPACE_HEALTH[2] + " Damage in any 10 second period.",
				"Enemies whose Carapace is inactive gain " + CARAPACE_SPEED_RAW_PERCENT[2] + "% Speed."
			}, {
				"Enemies block " + CARAPACE_HEALTH[3] + " Damage in any 10 second period.",
				"Enemies whose Carapace is inactive gain " + CARAPACE_SPEED_RAW_PERCENT[3] + "% Speed."
			}, {
				"Enemies block " + CARAPACE_HEALTH[4] + " Damage in any 10 second period.",
				"Enemies whose Carapace is inactive gain " + CARAPACE_SPEED_RAW_PERCENT[4] + "% Speed."
			}
	};

	private final int mCarapaceHealth;
	private final int mCarapaceSpeedRawPercent;

	public Carapace(Plugin plugin, Player player) {
		super(plugin, player, Modifier.CARAPACE);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.CARAPACE);
		mCarapaceHealth = CARAPACE_HEALTH[rank - 1];
		mCarapaceSpeedRawPercent = CARAPACE_SPEED_RAW_PERCENT[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		mob.addScoreboardTag(CarapaceBoss.identityTag);
		mob.addScoreboardTag(CarapaceBoss.identityTag + mCarapaceHealth + "," + mCarapaceSpeedRawPercent);
	}

}
