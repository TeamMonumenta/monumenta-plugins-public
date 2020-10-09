package com.playmonumenta.plugins.bosses.bosses;

import java.util.AbstractMap;
import java.util.Arrays;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class MeteorSlamBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_meteor_slam";
	public static final int detectionRange = 32;

	public static final int COOLDOWN = 20 * 8;
	private static final int MIN_RANGE = 0;
	private static final int RUN_DISTANCE = 0;
	private static final double VELOCITY_MULTIPLIER = 0.5;
	private static final double DAMAGE_RADIUS = 3;
	private static final double DAMAGE_PERCENT = 0.5;
	private static final int JUMP_HEIGHT = 1;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MeteorSlamBoss(plugin, boss);
	}

	public MeteorSlamBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager manager = new SpellManager(Arrays.asList(new SpellBaseSlam(plugin, boss, JUMP_HEIGHT, detectionRange, MIN_RANGE, RUN_DISTANCE, COOLDOWN, VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										world.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1);
										world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1);
									})
							));

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
					world.spawnParticle(Particle.FLAME, loc, 60, 0F, 0F, 0F, 0.2F);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
					world.spawnParticle(Particle.LAVA, loc, 3 * (int)(DAMAGE_RADIUS * DAMAGE_RADIUS), DAMAGE_RADIUS, 0.25f, DAMAGE_RADIUS, 0);
					if (player != null) {
						BossUtils.bossDamagePercent(mBoss, player, DAMAGE_PERCENT);
						return;
					}
					for (Player players : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS)) {
						BossUtils.bossDamagePercent(mBoss, players, DAMAGE_PERCENT);
					}
					})));
		super.constructBoss(plugin, identityTag, mBoss, manager, null, detectionRange, null);
	}
}
