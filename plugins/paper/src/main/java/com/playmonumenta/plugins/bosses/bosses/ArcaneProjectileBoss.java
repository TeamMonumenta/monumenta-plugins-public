package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author G3m1n1Boy
 * @deprecated use boss_projectile instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_projectile
 * /bos var Tags add boss_projectile[damage=20,distance=32,speed=0.8,delay=20,cooldown=160,turnRadius=0.042]
 * /bos var Tags add boss_projectile[SoundStart=[(BLOCK_ENCHANTMENT_TABLE_USE,2,1)],SoundLaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],SoundProjectile=[(ENTITY_BLAZE_BURN,0.5,0.2)],SoundHit=[(ENTITY_GENERIC_EXPLODE,0.5,0.5),(ENTITY_BLAZE_AMBIENT,0.5,0.5)]]
 * /bos var Tags add boss_projectile[ParticleLaunch=[(SOUL_FIRE_FLAME,0)],ParticleProjectile=[(SOUL_FIRE_FLAME,5,0.1,0.1,0.1,0.05),(SMOKE_LARGE,2,0.25,0.25,0.25)],ParticleHit=[(SOUL_FIRE_FLAME,30,0,0,0,0.25)]]
 * </pre></blockquote>
 */
@Deprecated
public final class ArcaneProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_arcaneprojectile";

	public static class Parameters extends BossParameters {
		public int DAMAGE = 20;
		public int DISTANCE = 32;
		public double SPEED = 0.8;
		public int DETECTION = 24;
		public int DELAY = 20 * 1;
		public int COOLDOWN = 20 * 8;
		public boolean LINGERS = true;
		public double HITBOX_LENGTH = 0.5;
		public boolean SINGLE_TARGET = true;
		public boolean LAUNCH_TRACKING = true;
		public double TURN_RADIUS = Math.PI / 75;
		public boolean COLLIDES_WITH_BLOCKS = true;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ArcaneProjectileBoss(plugin, boss);
	}

	public ArcaneProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		final Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		int lifetimeTicks = (int) (p.DISTANCE / p.SPEED);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
				p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						world.playSound(mBoss.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2, 1);
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.DELAY, 0));
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 5, 0.1, 0.1, 0.1, 0.05);
						world.spawnParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.25, 0.25, 0);
						if (ticks % 40 == 0) {
							world.playSound(loc, Sound.ENTITY_BLAZE_BURN, 0.5f, 0.2f);
						}
					},
					// Hit Action
					(World world, LivingEntity target, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
						world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.5f);
						world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 30, 0, 0, 0, 0.25);
						if (target != null) {
							BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE);
						}
					})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null);
	}
}
