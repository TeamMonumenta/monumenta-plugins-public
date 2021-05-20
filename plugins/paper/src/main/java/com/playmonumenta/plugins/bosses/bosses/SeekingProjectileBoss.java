package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class SeekingProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_seekingprojectile";

	public static class Parameters {
		public int DAMAGE = 20;
		public int DISTANCE = 64;
		public double SPEED = 0.4;
		public int DETECTION = 24;
		public int DELAY = 20 * 1;
		public int COOLDOWN = 20 * 12;
		public boolean LINGERS = true;
		public double HITBOX_LENGTH = 0.5;
		public int FIRE_DURATION = 20 * 5;
		public boolean SINGLE_TARGET = true;
		public boolean LAUNCH_TRACKING = false;
		public double TURN_RADIUS = Math.PI / 30;
		public boolean COLLIDES_WITH_BLOCKS = true;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SeekingProjectileBoss(plugin, boss);
	}

	public SeekingProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		int lifetimeTicks = (int) (p.DISTANCE/p.SPEED);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
					p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.DELAY, 0));
						world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.5f);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
						world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.1);
						world.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.25, 0.25, 0);
						if (ticks % 40 == 0) {
							world.playSound(loc, Sound.ENTITY_BLAZE_BURN, 0.5f, 0.2f);
						}
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
						world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.25);
						if (player != null) {
							BossUtils.bossDamage(boss, player, p.DAMAGE);
							player.setFireTicks(p.FIRE_DURATION);
						}
					})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null);
	}
}
