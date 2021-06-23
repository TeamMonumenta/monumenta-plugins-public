package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class FireBombTossBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_firebombtoss";

	public static class Parameters {
		public int LOBS = 1;
		public int DAMAGE = 64;
		public int DELAY = 100;
		public double RADIUS = 8;
		public int DETECTION = 20;
		public int FUSE_TIME = 50;
		public int COOLDOWN = 160;
		public int FIRE_DURATION = 20 * 8;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FireBombTossBoss(plugin, boss);
	}

	public FireBombTossBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, boss, p.DETECTION, p.LOBS, p.FUSE_TIME, p.COOLDOWN,
					(World world, TNTPrimed tnt, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
						world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
						world.spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.4);

						for (Player player : PlayerUtils.playersInRange(loc, p.RADIUS, true)) {
							if (player.hasLineOfSight(tnt)) {
								double multiplier = (p.RADIUS - player.getLocation().distance(loc)) / p.RADIUS;
								BossUtils.bossDamage(boss, player, p.DAMAGE * multiplier);
								player.setFireTicks((int)(p.FIRE_DURATION * multiplier));
							}
						}
					})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
