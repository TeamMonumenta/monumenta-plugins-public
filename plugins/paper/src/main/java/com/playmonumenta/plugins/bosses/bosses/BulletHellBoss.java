package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.spells.SpellBullet;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class BulletHellBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bullet_hell";
	public static final double DEFAULT_BULLET_RADIUS = 0.3125;

	@BossParam(help = "The launcher casts a spell that summons bullets around it that can damage and apply effects to hit players")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Amount of damage a bullet will deal")
		public int DAMAGE = 0;

		@BossParam(help = "Damage type for use with the DAMAGE parameter")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;

		@BossParam(help = "Percent health True damage a bullet will deal")
		public int DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Name of the spell used for chat messages")
		public String SPELL_NAME = "";

		@BossParam(help = "Effects to apply on hit.")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Spell duration in ticks. This value cannot be changed for certain patterns (30 for Junko, " +
			"50 for Sanae, and 600 for border)")
		public int DURATION = (int) (TICKS_PER_SECOND * 2.5);

		@BossParam(help = "Initial bullet velocity in blocks per tick (0 for sanae, 0.1 for Junko, 0.3 for border)")
		public double VELOCITY = 0;

		@BossParam(help = "Range in blocks that the launcher searches for players to target with this spell")
		public int DETECTION = 20;

		@BossParam(help = "Channeling delay in ticks. This value cannot be changed for certain patterns (20 for " +
			"Sanae and Junko, 60 for border)")
		public int DELAY = TICKS_PER_SECOND;

		@BossParam(help = "Ticks per bullet emission. This value cannot be changed for certain patterns (1 for " +
			"Sanae, 5 for Junko and Border")
		public int EMISSION_TICKS = 1;

		@BossParam(help = "Charge sound")
		public Sound CHARGE_SOUND = Sound.BLOCK_NOTE_BLOCK_BIT;

		@BossParam(help = "Cooldown in ticks. This value cannot be changed for certain patterns (20 for Sanae and " +
			"Junko, 100 for border)")
		public int COOLDOWN = TICKS_PER_SECOND;

		@BossParam(help = "Bullet lifetime duration in ticks. This value cannot be changed for certain patterns (120 " +
			"for Sanae, 40 for Junko and border)")
		public int BULLET_DURATION = TICKS_PER_SECOND * 6;

		@BossParam(help = "Bullet radius in blocks")
		public double HITBOX_RADIUS = DEFAULT_BULLET_RADIUS;

		@BossParam(help = "Block material to use for bullets")
		public Material MATERIAL = Material.PURPUR_BLOCK;

		@BossParam(help = "Bullet emission sound")
		public Sound SHOOT_SOUND = Sound.BLOCK_NOTE_BLOCK_SNARE;

		@BossParam(help = "Bullet emission pattern where each pattern is based on a Touhou character's attack pattern")
		public String PATTERN = "SANAE";

		@BossParam(help = "Charging volume")
		public float CHARGE_VOLUME = 2;

		@BossParam(help = "Shooting volume")
		public float SHOOT_VOLUME = 0.25f;

		@BossParam(help = "Added bullet acceleration in blocks per tick squared for use with Junko's pattern")
		public double ACCEL = 0.2;

		@BossParam(help = "Initial bullet acceleration in blocks per tick squared for use with Junko's pattern")
		public int ACCEL_START = 10;

		@BossParam(help = "Final bullet acceleration in blocks per tick squared for use with Junko's pattern")
		public int ACCEL_END = 15;

		@BossParam(help = "Allow bullets to pass through solid blocks")
		public boolean PASS_THROUGH = false;

		@BossParam(help = "Y offset in blocks for bullets ")
		public double OFFSET_Y = 0.0;

		@BossParam(help = "Frequency of bullet rotation. Does not change bullet trajectory and is not measured in Hz " +
			"but functions similarly")
		public double ROTATION_SPEED = 480.0;
	}

	private final HashSet<UUID> mRecentlyHitPlayers = new HashSet<>();

	public BulletHellBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final World world = mBoss.getWorld();
		final Parameters p = BossParameters.getParameters(mBoss, identityTag, new Parameters());

		super.constructBoss(
			new SpellBullet(plugin, mBoss, new Vector(0, p.OFFSET_Y, 0), p.DURATION, p.DELAY, p.EMISSION_TICKS, p.VELOCITY,
				p.DETECTION, p.HITBOX_RADIUS, p.COOLDOWN, p.BULLET_DURATION, p.PATTERN, p.ACCEL, p.ACCEL_START, p.ACCEL_END,
				p.PASS_THROUGH, p.ROTATION_SPEED,
				(Entity entity, int tick) -> {
					float t = tick / 10f;
					if (tick % 5 == 0) {
						world.playSound(mBoss.getLocation(), p.CHARGE_SOUND, SoundCategory.HOSTILE, p.CHARGE_VOLUME, t);
					}
				},
				(Entity entity) -> world.playSound(mBoss.getLocation(), p.SHOOT_SOUND, SoundCategory.HOSTILE, p.SHOOT_VOLUME, 0),
				p.MATERIAL,
				(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) -> {
					if (player != null && !blocked && !mRecentlyHitPlayers.contains(player.getUniqueId())) {
						if (p.DAMAGE_PERCENTAGE > 0) {
							DamageUtils.damage(mBoss, player, new DamageEvent.Metadata(DamageEvent.DamageType.TRUE,
									null, null, p.SPELL_NAME), p.DAMAGE_PERCENTAGE * EntityUtils.getMaxHealth(player) / 100.0,
								true, true, false);
						}
						if (p.DAMAGE > 0) {
							DamageUtils.damage(mBoss, player, new DamageEvent.Metadata(p.DAMAGE_TYPE, null,
								null, p.SPELL_NAME), p.DAMAGE, true, true, false);
						}
						p.EFFECTS.apply(player, mBoss);

						mRecentlyHitPlayers.add(player.getUniqueId());
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mRecentlyHitPlayers.remove(player.getUniqueId()), 5);
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc).count(5).extra(0.175).spawnAsEntityActive(mBoss);
				}
			),
			p.DETECTION, null, 0);
	}
}
