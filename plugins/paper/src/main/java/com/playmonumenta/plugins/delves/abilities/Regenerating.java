package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.RegenerationPercentBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class Regenerating {

	private static final int PERCENT_HEALING_FLAT = 3;
	private static final int PERCENT_HEALING_PER_LEVEL = 3;
	private static final int INTERRUPTION_TICKS = 40;
	private static final int TICKS_PER_HEAL = 10;
	private static final int DETECTION = 32;
	public static final String AVOID_REGENERATING = "boss_regeneratingimmune";

	public static final String DESCRIPTION = "Nearby enemies regain health after not being hit.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Enemies heal "
				+ (PERCENT_HEALING_FLAT + PERCENT_HEALING_PER_LEVEL * level)
				+ "% of their max health every "
				+ TICKS_PER_HEAL / (double) TICKS_PER_SECOND
				+ " seconds."),
			Component.text("Dealing non-DoT damage to the mob cancels the healing for "
				+ Math.round(INTERRUPTION_TICKS / (double) TICKS_PER_SECOND)
				+ " seconds."),
			Component.text("Enemies only heal when within "
				+ DETECTION
				+ " blocks of a player.")
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!EntityUtils.isBoss(mob)
			&& !DelvesUtils.isDelveMob(mob)
			&& !mob.getScoreboardTags().contains(AVOID_REGENERATING)) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(RegenerationPercentBoss.identityTag);
			mob.addScoreboardTag(RegenerationPercentBoss.identityTag + "[detection=" + DETECTION
				+ ",healpercentage=" + (PERCENT_HEALING_FLAT + PERCENT_HEALING_PER_LEVEL * level)
				+ ",ticksperheal=" + TICKS_PER_HEAL
				+ ",interruptionticks=" + INTERRUPTION_TICKS
				+ ",dotinterrupts=" + false
				+ "]");
		}
	}
}
