package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDieIfAlone;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class DieIfAloneBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dieifalone";
	public static final int detectionRange = 24;

	public static class Parameters extends BossParameters {
		@BossParam(help = "This mob needs an enemy within this range to not die.")
		public int RADIUS = 16;
		@BossParam(help = "This mob needs a player within this range to die.")
		public int DETECTION = 40;
		@BossParam(help = "How many pulses pass before the mob qualifies as lonely.")
		public int LONELY_PULSES = 2;
		@BossParam(help = "How many ticks per pulse.")
		public int TICKS_PER_PULSE = 50;
		@BossParam(help = "How much of its max health it loses every pulse it is lonely.")
		public double DAMAGE_PERCENTAGE = 0.2;
		@BossParam(help = "Mobs with this bosstag do not count towards making the boss not lonely.")
		public List<String> DISQUALIFIED_BOSSTAGS = List.of(identityTag);
	}

	Parameters mParameters = new Parameters();

	public DieIfAloneBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		BossParameters.getParameters(boss, identityTag, mParameters);
		List<Spell> passiveSpells = List.of(new SpellDieIfAlone(mBoss, mParameters));
		super.constructBoss(SpellManager.EMPTY, passiveSpells, mParameters.DETECTION, null, 0, mParameters.TICKS_PER_PULSE);
	}

	public static boolean shouldActivate(LivingEntity boss, Parameters parameters) {
		// Check nearby entities
		Collection<LivingEntity> nearbyEntities = boss.getLocation().getNearbyLivingEntities(parameters.RADIUS);
		for (LivingEntity entity : nearbyEntities) {
			if (EntityUtils.isHostileMob(entity)) {
				// This check implicitly excludes the entity itself.
				Set<String> bosstags = new HashSet<>(entity.getScoreboardTags());
				bosstags.retainAll(parameters.DISQUALIFIED_BOSSTAGS);
				if (bosstags.isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}
}
