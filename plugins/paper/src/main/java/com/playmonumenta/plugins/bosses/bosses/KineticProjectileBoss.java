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
import org.bukkit.util.BoundingBox;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class KineticProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kineticprojectile";
	public static final int detectionRange = 24;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 4;
	private static final int DELAY = 10;
	private static final double SPEED = 0.8;
	private static final double TURN_RADIUS = 0;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int)(DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 1;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 24;
	private static final double RADIUS = 2.5;
	private static final float KNOCKBACK_SPEED = 0.5f;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new KineticProjectileBoss(plugin, boss);
	}

	public KineticProjectileBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
					SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
					// Initiate Aesthetic
					(World world, Location loc, int ticks) -> {
						PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));
					},
					// Launch Aesthetic
					(World world, Location loc, int ticks) -> {
						world.playSound(loc, Sound.ENTITY_IRON_GOLEM_STEP, 1f, 0.5f);
					},
					// Projectile Aesthetic
					(World world, Location loc, int ticks) -> {
						world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0.2, 0.2, 0.2, 0.1);
					},
					// Hit Action
					(World world, Player player, Location loc) -> {
						world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.5f);
						world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);

						BoundingBox hitbox = new BoundingBox();
						hitbox.shift(loc);
						hitbox.expand(RADIUS);

						for (Player p : PlayerUtils.playersInRange(loc, RADIUS * 3)) {
							if (hitbox.overlaps(p.getBoundingBox())) {
								BossUtils.bossDamage(boss, p, DAMAGE);

								if (!p.equals(player)) {
									MovementUtils.knockAway(loc, p, KNOCKBACK_SPEED);
								}
							}
						}
					})
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}
}
