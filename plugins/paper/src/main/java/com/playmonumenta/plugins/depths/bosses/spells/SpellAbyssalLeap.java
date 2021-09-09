package com.playmonumenta.plugins.depths.bosses.spells;

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

import com.playmonumenta.plugins.bosses.spells.SpellBaseSlam;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellAbyssalLeap extends SpellBaseSlam {

	public static final int DETECTION = 32;
	public static final int DELAY = 20 * 5;
	public static final int MIN_RANGE = 0;
	public static final int RUN_DISTANCE = 0;
	public static final double VELOCITY_MULTIPLIER = 0.5;
	public static final double DAMAGE_RADIUS = 3;
	public static final double DAMAGE_PERCENT = 0.5;
	public static final int JUMP_HEIGHT = 1;

	public Plugin mPlugin;

	public int mCooldownTicks;

	public SpellAbyssalLeap(Plugin plugin, LivingEntity launcher, int cooldown) {
		super(plugin, launcher, JUMP_HEIGHT, DETECTION, MIN_RANGE, RUN_DISTANCE, cooldown, VELOCITY_MULTIPLIER,
				(World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 15, 1, 0f, 1, 0);
				}, (World world, Location loc) -> {
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f));
				}, (World world, Player player, Location loc, Vector dir) -> {
					ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
							Arrays.asList(
									new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
										world.spawnParticle(Particle.SOUL, loc, 1, 0.1, 0.1, 0.1, 0.1);
										world.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1);
									})
							));

					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
					world.spawnParticle(Particle.SOUL, loc, 60, 0F, 0F, 0F, 0.2F);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F);
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3 * (int)(DAMAGE_RADIUS * DAMAGE_RADIUS), DAMAGE_RADIUS, 0.25f, DAMAGE_RADIUS, 0);
					if (player != null) {
						BossUtils.bossDamagePercent(launcher, player, DAMAGE_PERCENT, "Abyssal Leap");
						return;
					}
					for (Player players : PlayerUtils.playersInRange(loc, DAMAGE_RADIUS, true)) {
						BossUtils.bossDamagePercent(launcher, players, DAMAGE_PERCENT);
					}
					});
		mPlugin = plugin;

		mCooldownTicks = cooldown;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

}
