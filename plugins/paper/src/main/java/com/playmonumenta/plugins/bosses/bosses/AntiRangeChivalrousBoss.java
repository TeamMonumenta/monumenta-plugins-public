package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.abilities.delves.Chivalrous;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class AntiRangeChivalrousBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_antirangechivalrous";
	public static final int detectionRange = 40;
	public static final String ignoreTag = "antirange_ignore";

	private static final int ANTI_RANGE_DISTANCE = 6;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AntiRangeChivalrousBoss(plugin, boss);
	}

	public AntiRangeChivalrousBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		Entity root = mBoss;
		while (root.isInsideVehicle()) {
			root = root.getVehicle();
		}

		String name = root.getCustomName();
		if (!Chivalrous.MOUNT_NAMES[0].equals(name) && !Chivalrous.MOUNT_NAMES[1].equals(name)) {
			return;
		}

		Location loc = mBoss.getLocation();

		//Arrows are given ignoreTag from a Depths ability to ignore the boss ability
		Set<String> tags = damager.getScoreboardTags();
		if (tags.contains(ignoreTag)) {
			return;
		}

		if (loc.distance(source.getLocation()) > ANTI_RANGE_DISTANCE) {
			event.setCancelled(true);

			World world = mBoss.getWorld();
			loc.add(0, 1, 0);
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0, 0, 0, 0.3);
			world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.2f, 1.5f);
		}
	}

}
