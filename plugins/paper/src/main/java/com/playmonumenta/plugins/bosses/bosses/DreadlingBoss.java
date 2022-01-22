package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDreadlingParticle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public final class DreadlingBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_dreadling";
	public static final int detectionRange = 24;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DreadlingBoss(plugin, boss);
	}

	public DreadlingBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellDreadlingParticle(boss)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		Location loc = damagee.getLocation();

		LivingEntity dreadnaught = null;
		double dreadnaughtDistance = Double.POSITIVE_INFINITY;
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, 16)) {
			if (mob.getScoreboardTags().contains(DreadnaughtParticleBoss.identityTag)) {
				double distance = loc.distance(mob.getLocation());
				if (distance < dreadnaughtDistance) {
					dreadnaughtDistance = distance;
					dreadnaught = mob;
				}
			}
		}

		if (dreadnaught != null) {
			MovementUtils.pullTowards(dreadnaught, damagee, 0.5f);
		}
	}
}
