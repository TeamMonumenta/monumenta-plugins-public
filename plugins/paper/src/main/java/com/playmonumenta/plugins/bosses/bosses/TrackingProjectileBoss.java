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

/**
 * @deprecated use boss_projectile instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_projectile
 * /bos var Tags add boss_projectile[damage=24,speed=0.2,delay=20,cooldown=320,launchTraking=true,turnRadius=3.141]
 * /bos var Tags add boss_projectile[SoundStart=BLOCK_BEACON_POWER_SELECT,ParticleLaunch=SPELL_WITCH,SoundLaunch=ENTITY_EVOKER_CAST_SPELL,ParticleProjectileMain=SPELL_WITCH]
 * /bos var Tags add boss_projectile[ParticleProjectileSecond=SMOKE_LARGE,SoundProjectile=BLOCK_BEACON_POWER_SELECT,SoundHit=BLOCK_BEACON_DEACTIVATE,ParticleHit=SPELL_WITCH]
 * </pre></blockquote>
 * @G3m1n1Boy
 */
public class TrackingProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_trackingprojectile";
	public static final int detectionRange = 24;

	private static final int DAMAGE = 24;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.2;
	private static final int COOLDOWN = 20 * 16;
	private static final boolean LINGERS = true;
	private static final double HITBOX_LENGTH = 0.5;
	private static final int LIFETIME_TICKS = 20 * 16;
	private static final double TURN_RADIUS = Math.PI;
	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = true;
	private static final boolean COLLIDES_WITH_BLOCKS = true;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TrackingProjectileBoss(plugin, boss);
	}

	public TrackingProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
					SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));
						world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.5f);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.SPELL_WITCH, loc, 40, 0, 0, 0, 0.3);
						world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 0.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.SPELL_WITCH, loc, 6, 0, 0, 0, 0.3);
						world.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.4, 0.4, 0.4, 0);
						if (ticks % 40 == 0) {
							world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 0.5f);
						}
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 2f, 0.5f);
						world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.25);
						if (player != null) {
							BossUtils.bossDamage(boss, player, DAMAGE);
						}
					})
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
