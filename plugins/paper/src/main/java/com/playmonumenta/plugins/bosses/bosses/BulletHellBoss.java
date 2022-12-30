package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellBullet;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BulletHellBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_bullet_hell";

	@BossParam(help = "Bullets")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Damage")
		public int DAMAGE = 15;
		@BossParam(help = "Cast Duration (30 for Junko, 50 for sanae and 600 for border)")
		public int DURATION = 50;
		@BossParam(help = "Bullet Initial Speed (0 for sanae, 0.1 for Junko, 0.3 for border)")
		public double VELOCITY = 0;
		@BossParam(help = "Detection Range")
		public int DETECTION = 20;
		@BossParam(help = "Windup Delay (20 for sanae and junko, 60 for border)")
		public int DELAY = 20 * 1;
		@BossParam(help = "Ticks per bullet emission (1 for sanae, 5 for Junko and Border")
		public int EMISSION_TICKS = 1;
		@BossParam(help = "Charge sound")
		public Sound CHARGE_SOUND = Sound.BLOCK_NOTE_BLOCK_BIT;
		@BossParam(help = "Cooldown (20 for sanae and junko, 100 for border)")
		public int COOLDOWN = 20 * 1;
		@BossParam(help = "Bullet duration (120 for sanae, 40 for junko and border)")
		public int BULLET_DURATION = 120;
		@BossParam(help = "Bullet size")
		public double HITBOX_RADIUS = 0.3125;
		@BossParam(help = "Bullet material")
		public Material MATERIAL = Material.PURPUR_BLOCK;
		@BossParam(help = "Bullet emission sound")
		public Sound SHOOT_SOUND = Sound.BLOCK_NOTE_BLOCK_SNARE;
		@BossParam(help = "Bullet pattern")
		public String PATTERN = "SANAE";

		@BossParam(help = "Charging volume")
		public float CHARGE_VOLUME = 2;

		@BossParam(help = "Shooting volume")
		public float SHOOT_VOLUME = 0.25f;
		@BossParam(help = "Junko accel")
		// Junko Specific
		public double ACCEL = 0.2;
		@BossParam(help = "Junko begin accel")
		public int ACCEL_START = 10;
		@BossParam(help = "Junko end accel")
		public int ACCEL_END = 15;
		@BossParam(help = "Pass through walls?")
		public boolean PASS_THROUGH = false;
		@BossParam(help = "Speed of rotation (lower = faster)")
		public double ROTATION_SPEED = 480.0;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BulletHellBoss(plugin, boss);
	}

	public BulletHellBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		World world = boss.getWorld();
		BulletHellBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new BulletHellBoss.Parameters());

		super.constructBoss(new SpellManager(Arrays.asList(
			new SpellBullet(plugin, boss, new Vector(0, 0, 0), p.DURATION, p.DELAY, p.EMISSION_TICKS, p.VELOCITY, p.DETECTION, p.HITBOX_RADIUS, p.COOLDOWN, p.BULLET_DURATION, p.PATTERN,
				p.ACCEL, p.ACCEL_START, p.ACCEL_END, p.PASS_THROUGH, p.ROTATION_SPEED,
				(Entity entity, int tick) -> {
					float t = tick / 10;
					if (tick % 5 == 0) {
						world.playSound(mBoss.getLocation(), p.CHARGE_SOUND, p.CHARGE_VOLUME, t);
					}
				},
				(Entity entity) -> {
					world.playSound(mBoss.getLocation(), p.SHOOT_SOUND, p.SHOOT_VOLUME, 0);
				},
				p.MATERIAL,
				(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) -> {
					if (player != null && !blocked) {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.PROJECTILE, p.DAMAGE, prevLoc);
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0, 0, 0, 0.175).spawnAsEntityActive(boss);
				}
			))), Collections.emptyList(), p.DETECTION, null, 0);

	}

}
