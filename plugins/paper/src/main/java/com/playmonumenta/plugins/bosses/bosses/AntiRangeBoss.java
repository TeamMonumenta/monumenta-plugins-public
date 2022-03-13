package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class AntiRangeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_antirange";
	public static final int detectionRange = 40;

	private static final int ANTI_RANGE_DISTANCE = 6;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AntiRangeBoss(plugin, boss);
	}

	public AntiRangeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		Location loc = mBoss.getLocation();

		if (loc.distance(source.getLocation()) > ANTI_RANGE_DISTANCE) {
			event.setCancelled(true);

			World world = mBoss.getWorld();
			loc.add(0, 1, 0);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.3);
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.2f, 1.5f);
		}
	}

}
