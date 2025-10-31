package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.CoordinatedAttackOnDeathBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

public class Bloodlust {

	private static final double BLOODLUST_CHANCE_NORMAL = 0.1;
	private static final int BLOODLUST_COUNT_NORMAL = 1;
	private static final double BLOODLUST_CHANCE_ELITE = 0.25;
	private static final int BLOODLUST_COUNT_ELITE = 3;
	private static final double PLAYER_RADIUS = 5;
	private static final double MOB_RADIUS = 256;

	public static final String DESCRIPTION = "Enemies coordinate attacks on players when they die.";
	public static final String AVOID_BLOODTHIRSTY = "boss_bloodthirstyimmune";
	// Assuming that whatever we bothered to block from getting Bloodthirsty launched needs to also be blocked from Bloodlust
	public static final String AVOID_BLOODLUST = "boss_bloodlustimmune";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("On death of a normal enemy, there is a " + Math.round(100 * BLOODLUST_CHANCE_NORMAL * level)
				+ "% chance"),
			Component.text("for " + BLOODLUST_COUNT_NORMAL + " enemy to leap at you."),
			Component.text("On death of an elite, there is a " + Math.round(100 * BLOODLUST_CHANCE_ELITE * level)
				+ "% chance"),
			Component.text("for " + BLOODLUST_COUNT_ELITE + " enemies to leap at you."),
			Component.text("Mobs within " + Math.round(PLAYER_RADIUS) + " blocks of you will not leap.")
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		// This runs prior to BossManager parsing, so we can just add tags directly
		if (!DelvesUtils.isDelveMob(mob) && !mob.getScoreboardTags().contains(AVOID_BLOODLUST) && !mob.getScoreboardTags().contains(AVOID_BLOODTHIRSTY)) {
			if (EntityUtils.isElite(mob)) {
				if (BLOODLUST_CHANCE_ELITE * level >= 1
					|| FastUtils.RANDOM.nextDouble() < BLOODLUST_CHANCE_ELITE * level) {
					mob.addScoreboardTag(CoordinatedAttackOnDeathBoss.identityTag);
					mob.addScoreboardTag(CoordinatedAttackOnDeathBoss.identityTag
						+ "[playerradius=" + PLAYER_RADIUS
						+ ",mobradius=" + MOB_RADIUS
						+ ",detection=" + MOB_RADIUS
						+ ",affectedmobcap=" + BLOODLUST_COUNT_ELITE
						+ "]");
				}
			} else {
				if (FastUtils.RANDOM.nextDouble() < BLOODLUST_CHANCE_NORMAL * level) {
					mob.addScoreboardTag(CoordinatedAttackOnDeathBoss.identityTag);
					mob.addScoreboardTag(CoordinatedAttackOnDeathBoss.identityTag
						+ "[playerradius=" + PLAYER_RADIUS
						+ ",mobradius=" + MOB_RADIUS
						+ ",detection=" + MOB_RADIUS
						+ ",affectedmobcap=" + BLOODLUST_COUNT_NORMAL
						+ "]");
				}
			}
		}
	}
}
