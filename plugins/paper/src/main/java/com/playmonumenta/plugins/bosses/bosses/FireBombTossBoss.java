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
	public static final int detectionRange = 20;

	public static final int LOBS = 1;
	public static final int FUSE = 50;
	public static final double RADIUS = 8;
	public static final int POINT_BLANK_DAMAGE = 64;
	public static final int POINT_BLANK_FIRE_DURATION = 20 * 8;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new FireBombTossBoss(plugin, boss);
	}

	public FireBombTossBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, boss, detectionRange, LOBS, FUSE,
					(World world, TNTPrimed tnt, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
						world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
						world.spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.4);

						for (Player player : PlayerUtils.playersInRange(loc, RADIUS)) {
							if (player.hasLineOfSight(tnt)) {
								double multiplier = (RADIUS - player.getLocation().distance(loc)) / RADIUS;
								BossUtils.bossDamage(boss, player, POINT_BLANK_DAMAGE * multiplier);
								player.setFireTicks((int)(POINT_BLANK_FIRE_DURATION * multiplier));
							}
						}
					})
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
