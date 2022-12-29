package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
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
 * @deprecated use boss_projectile instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_projectile
 * /bos var Tags add boss_projectile[damage=20,distance=32,speed=0.8,delay=20,cooldown=160,turnRadius=0]
 * /bos var Tags add boss_projectile[SoundStart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,0.01,1)],SoundLaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],SoundProjectile=[(ENTITY_BLAZE_BURN,0)],SoundHit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]
 * /bos var Tags add boss_projectile[ParticleLaunch=[(FIREWORKS_SPARK,0)],ParticleProjectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],ParticleHit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]
 * </pre></blockquote>
 * G3m1n1Boy
 */
@Deprecated
public class MagicArrowBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_magicarrow";

	public static class Parameters extends BossParameters {
		public int DAMAGE = 20;
		public int DISTANCE = 32;
		public int DETECTION = 24;
		public int DELAY = 20 * 1;
		public double SPEED = 0.8;
		public int COOLDOWN = 20 * 8;
		public double TURN_RADIUS = 0;
		public boolean LINGERS = true;
		public double HITBOX_LENGTH = 0.5;
		public boolean SINGLE_TARGET = true;
		public boolean LAUNCH_TRACKING = true;
		public boolean COLLIDES_WITH_BLOCKS = true;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MagicArrowBoss(plugin, boss);
	}

	public MagicArrowBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		int lifeTimeTicks = (int) (p.DISTANCE / p.SPEED);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
				p.SPEED, p.TURN_RADIUS, lifeTimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.DELAY, 0));
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.5f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.05).spawnAsEntityActive(boss);
					new PartialParticle(Particle.CRIT_MAGIC, loc, 20, 0.2, 0.2, 0.2, 0.1).spawnAsEntityActive(boss);
				},
				// Hit Action
				(World world, LivingEntity target, Location loc, Location prevLoc) -> {
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1.5f);
					new PartialParticle(Particle.FIREWORKS_SPARK, loc, 30, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
					if (target != null) {
						BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, prevLoc);
					}
				})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null);
	}
}
