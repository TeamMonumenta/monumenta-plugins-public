package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.Collections;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

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
	/bos var Tags add boss_projectile
	/bos var Tags add boss_projectile[damage=30,distance=128,speed=0.8,delay=20,cooldown=140,turnRadius=0.035,effects=[(pull,3)]]
	/bos var Tags add boss_projectile[SoundStart=[(ITEM_CROSSBOW_LOADING_MIDDLE,2,0.5)],SoundHit=[(ENTITY_ARMOR_STAND_BREAK,1,0.5)],SoundProjectile=[(ENTITY_ARROW_SHOOT,2,0.2)],SoundLaunch=[(ITEM_CROSSBOW_SHOOT,2,0.5)]]
	/bos var Tags add boss_projectile[ParticleLaunch=[(SMOKE_NORMAL,1)],ParticleProjectile=[(crit,3,0,0,0,0.1),(SPELL_INSTANT,4,0.25,0.25,0.25)],ParticleHit=[(CRIT,50,0,0,0,0.25)]]
 * </pre></blockquote>
 * @Author G3m1n1Boy
 */
@Deprecated
public class HookBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hook";

	public static class Parameters extends BossParameters {
		public int DAMAGE = 24;
		public int DETECTION = 24;
		public int DELAY = 20 * 1;
		public double SPEED = 0.8;
		public boolean LINGERS = true;
		public int COOLDOWN = 20 * 12;
		public double HITBOX_LENGTH = 0.5;
		public int LIFETIME_TICKS = 20 * 8;
		public boolean SINGLE_TARGET = true;
		public boolean LAUNCH_TRACKING = true;
		public double TURN_RADIUS = Math.PI / 90;
		public boolean COLLIDES_WITH_BLOCKS = true;
	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HookBoss(plugin, boss);
	}

	public HookBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
				p.SPEED, p.TURN_RADIUS, p.LIFETIME_TICKS, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, p.DAMAGE, 0));
						world.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 2f, 0.5f);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0, 0, 0, 0);
						world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 2f, 0.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.CRIT, loc, 3, 0, 0, 0, 0.1);
						world.spawnParticle(Particle.SMOKE_LARGE, loc, 4, 0.25, 0.25, 0.25, 0);
						if (ticks % 40 == 0) {
							world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 2f, 0.2f);
						}
					},
					// Hit Action
					(World world, LivingEntity target, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_ARMOR_STAND_BREAK, 1f, 0.5f);
						world.spawnParticle(Particle.CRIT, loc, 50, 0, 0, 0, 0.25);
						if (target != null) {
							BossUtils.dualTypeBlockableDamage(boss, target, DamageType.PROJECTILE, DamageType.MAGIC, p.DAMAGE, 0.5);
							MovementUtils.pullTowards(boss, target, 1);
						}
					}
			)));

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null);
	}
}
