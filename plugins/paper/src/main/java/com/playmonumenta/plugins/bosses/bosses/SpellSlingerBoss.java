package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellSlingerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_spellslinger";
	public static final int detectionRange = 24;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = (int)(20 * 1.25);
	private static final int DELAY = 10;
	private static final double SPEED = 0.6;
	private static final double TURN_RADIUS = Math.PI / 60;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int)(DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 16;
	private static final float KNOCKBACK_SPEED = 0.5f;


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SpellSlingerBoss(plugin, boss);
	}

	public SpellSlingerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
						SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
						// Initiate Aesthetic
						(World world, Location loc, int ticks) -> { },
						// Launch Aesthetic
						(World world, Location loc, int ticks) -> {
							world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.5f);
						},
						// Projectile Aesthetic
						(World world, Location loc, int ticks) -> {
							world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 5, 0.1, 0.1, 0.1, 0.05);
							world.spawnParticle(Particle.SPELL_WITCH, loc, 10, 0, 0, 0, 0.3);
							world.spawnParticle(Particle.END_ROD, loc, 2, 0.25, 0.25, 0.25, 0);
						},
						// Hit Action
						(World world, LivingEntity player, Location loc) -> {
							world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1.5f);
							world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 30, 0, 0, 0, 0.25);
							if (player != null) {
								BossUtils.blockableDamage(boss, player, DamageType.MAGIC, DAMAGE);
								MovementUtils.knockAway(boss, player, KNOCKBACK_SPEED, false);
							}
						})
		));

		super.constructBoss(activeSpells, Collections.emptyList(), detectionRange, null);
	}

}
