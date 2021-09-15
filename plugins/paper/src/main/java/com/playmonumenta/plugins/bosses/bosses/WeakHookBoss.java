package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/**
 * @deprecated use boss_projectile instead, like this:
 * <blockquote><pre>
	/bos var Tags add boss_projectile
	/bos var Tags add boss_projectile[damage=30,distance=128,speed=0.8,delay=20,cooldown=140,turnRadius=0.035,effects=[(pull,3)]]
	/bos var Tags add boss_projectile[SoundStart=[(ITEM_CROSSBOW_LOADING_MIDDLE,2,0.5)],SoundHit=[(ENTITY_ARMOR_STAND_BREAK,1,0.5)],SoundProjectile=[(ENTITY_ARROW_SHOOT,2,0.2)],SoundLaunch=[(ITEM_CROSSBOW_SHOOT,2,0.5)]]
	/bos var Tags add boss_projectile[ParticleLaunch=[(CRIT,1)],ParticleProjectile=[(crit,3,0,0,0,0.1),(SPELL_INSTANT,4,0.25,0.25,0.25)],ParticleHit=[(CRIT,50,0,0,0,0.25)]]
 * </pre></blockquote>
 * @Author G3m1n1Boy
 */
public class WeakHookBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_weakhook";
	public static final int detectionRange = 24;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = true;
	private static final int COOLDOWN = 20 * 12;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.8;
	private static final double TURN_RADIUS = Math.PI / 90;
	private static final int LIFETIME_TICKS = 20 * 8;
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 30;

	public static class Parameters {
		public float MULTIPLIER = 3;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WeakHookBoss(plugin, boss);
	}

	public WeakHookBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
					SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));
						world.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 2f, 0.5f);
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0);
						world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 2f, 0.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.CRIT, loc, 3, 0, 0, 0, 0.1);
						world.spawnParticle(Particle.SPELL_INSTANT, loc, 4, 0.25, 0.25, 0.25, 0);
						if (ticks % 40 == 0) {
							world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 2f, 0.2f);
						}
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_ARMOR_STAND_BREAK, 1f, 0.5f);
						world.spawnParticle(Particle.CRIT, loc, 50, 0, 0, 0, 0.25);
						if (player != null) {
							BossUtils.bossDamage(boss, player, DAMAGE);
							MovementUtils.pullTowardsByUnit((Entity)boss, (LivingEntity)player, p.MULTIPLIER);
						}
					}
			)));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
