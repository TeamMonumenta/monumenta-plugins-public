package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLeapAttack;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class LeapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_leap";
	public static final int detectionRange = 32;

	private static final int COOLDOWN = 20 * 4;
	private static final int MIN_RANGE = 6;
	private static final int RUN_DISTANCE = 3;
	private static final double VELOCITY_MULTIPLIER = 1.3;
	private static final double DAMAGE_RADIUS = 3;
	private static final int DAMAGE = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LeapBoss(plugin, boss);
	}

	public LeapBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseLeapAttack(plugin, boss, detectionRange, MIN_RANGE, RUN_DISTANCE, COOLDOWN, VELOCITY_MULTIPLIER,
					// Initiate Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.VILLAGER_ANGRY, loc, 15, 0.5, 0.5, 0.5, 0);
						world.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.5f);
					},
					// Leap Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.CLOUD, loc, 30, 0.1, 0.1, 0.1, 0.1);
						world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
					},
					// Leaping Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.CLOUD, loc, 1, 0.3, 0.3, 0.3, 0.1);
					},
					// Hit Action
					(World world, Player player, Location loc, Vector dir) -> {
						ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
								Arrays.asList(
										new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
											world.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1);
											world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1);
										})
								)
						);
						world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
						for (Player p : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS, true)) {
							BossUtils.blockableDamage(boss, p, DamageType.MELEE, DAMAGE);
						}
					}, null, null)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}

}
