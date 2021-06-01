package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projectile";

	public static class Parameters {
		public int DAMAGE = 0;
		public int DISTANCE = 64;
		public double SPEED = 0.4;
		public int DETECTION = 24;
		public int DELAY = 20 * 5;
		public int COOLDOWN = 20 * 10;
		public boolean LINGERS = true;
		public double HITBOX_LENGTH = 0.5;
		public boolean SINGLE_TARGET = true;
		public double DAMAGE_PERCENTAGE = 0.0;
		public boolean LAUNCH_TRACKING = false;
		public double TURN_RADIUS = Math.PI / 30;
		public boolean COLLIDES_WITH_BLOCKS = true;

		//this effects are given to the player after he recive the damage
		public int FIRE_TICKS = 0;
		public int SILENCE_TICKS = 0;
		public float HOOK_FORCE = 0.0f;
		public int EFFECT_DURATION = 0;
		public int EFFECT_AMPLIFIER = 0;
		public PotionEffectType EFFECT = PotionEffectType.BLINDNESS;

		//particle & sound used!
		/**Sound used at the start */
		public Sound SOUND_START = Sound.ENTITY_BLAZE_AMBIENT;

		/**Particle used when launching the projectile */
		public Particle PARTICLE_LAUNCH = Particle.EXPLOSION_LARGE;
		/**Sound used when launching the projectile */
		public Sound SOUND_LAUNCH = Sound.ENTITY_BLAZE_SHOOT;

		/**Particle used for the projectile*/
		public Particle PARTICLE_PROJECTILE_MAIN = Particle.FLAME;
		/**Particle used for the projectile*/
		public Particle PARTICLE_PROJECTILE_SECOND = Particle.SMOKE_LARGE;

		/**Sound summoned every 2 sec on the projectile location */
		public Sound SOUND_PROJECTILE = Sound.ENTITY_BLAZE_BURN;

		/**Particle used when the projectile hit something */
		public Particle PARTICLE_HIT = Particle.CLOUD;
		/**Sound used when the projectile hit something */
		public Sound SOUND_HIT = Sound.ENTITY_GENERIC_DEATH;

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ProjectileBoss(plugin, boss);
	}


	public ProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		int lifetimeTicks = (int) (p.DISTANCE/p.SPEED);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
					p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.DELAY, 0));
						world.playSound(loc, p.SOUND_START, 1.5f, 1f);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(p.PARTICLE_LAUNCH, loc, 1, 0, 0, 0, 0);
						world.playSound(loc, p.SOUND_LAUNCH, 0.5f, 0.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(p.PARTICLE_PROJECTILE_MAIN, loc, 4, 0.05, 0.05, 0.05, 0.1);
						world.spawnParticle(p.PARTICLE_PROJECTILE_SECOND, loc, 3, 0.25, 0.25, 0.25, 0);
						if (ticks % 40 == 0) {
							world.playSound(loc, p.SOUND_PROJECTILE, 0.5f, 0.2f);
						}
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, p.SOUND_HIT, 0.5f, 0.5f);
						world.spawnParticle(p.PARTICLE_HIT, loc, 50, 0, 0, 0, 0.25);
						if (player != null) {
							if (p.DAMAGE > 0) {
								BossUtils.bossDamage(boss, player, p.DAMAGE);
							}

							if (p.DAMAGE_PERCENTAGE > 0.0) {
								BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE);
							}

							if (p.EFFECT_DURATION > 0) {
								player.addPotionEffect(new PotionEffect(p.EFFECT, p.EFFECT_DURATION, p.EFFECT_AMPLIFIER));
							}

							if (p.FIRE_TICKS > 0) {
								player.setFireTicks(p.FIRE_TICKS);
							}

							if (p.SILENCE_TICKS > 0) {
								AbilityUtils.silencePlayer(player, p.SILENCE_TICKS);
							}

							if (p.HOOK_FORCE > 0) {
								MovementUtils.pullTowards(boss, player, p.HOOK_FORCE);
							}

						}
					})
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);

	}
}
