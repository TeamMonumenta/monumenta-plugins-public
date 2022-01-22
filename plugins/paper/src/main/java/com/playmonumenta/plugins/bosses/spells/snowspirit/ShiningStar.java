package com.playmonumenta.plugins.bosses.spells.snowspirit;

import com.playmonumenta.plugins.bosses.bosses.SnowSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ShiningStar extends Spell {
	public static final int DURATION = 20 * 8;

	private SpellBaseSeekingProjectile mMissile;

	private LivingEntity mBoss;
	private Plugin mPlugin;

	//How fast the bullets shoot
	private int mDelay = 2;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 8;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.8;
	private static final double TURN_RADIUS = 0;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int)(DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = false;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 13;

	public ShiningStar(LivingEntity boss, Plugin plugin) {
		mBoss = boss;
		mPlugin = plugin;

		mMissile = new SpellBaseSeekingProjectile(plugin, boss, SnowSpirit.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
				SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.5f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 0f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 1, 0.05, 0.05, 0.05, 0.025);
					world.spawnParticle(Particle.CRIT_MAGIC, loc, 5, 0.1, 0.1, 0.1, 0.05);
					world.spawnParticle(Particle.DOLPHIN, loc, 3, 0, 0, 0, 0.3);
				},
				// Hit Action
				(World world, LivingEntity player, Location loc) -> {
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1.5f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.5f);
					world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 10, 0, 0, 0, 0.25);
					if (player != null) {
						BossUtils.blockableDamage(boss, player, DamageType.MAGIC, DAMAGE, "Shining Star", mBoss.getLocation());
					}
				});
	}

	@Override
	public void run() {
		mBoss.teleport(mBoss.getLocation().add(0, 10, 0));
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 5, 0.5f);

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), SnowSpirit.detectionRange, true);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks >= DURATION) {
					this.cancel();
				}
				if (mTicks % (mDelay * 5) == 0) {
					for (Player player : players) {
						mMissile.launch(player, player.getLocation());
					}
				}
				if (!players.isEmpty()) {
					Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
					mMissile.launch(player, player.getLocation().add(FastUtils.randomDoubleInRange(-30, 30), FastUtils.randomDoubleInRange(-30, 30), FastUtils.randomDoubleInRange(-30, 30)));
				}
				mTicks += mDelay;
			}
		}.runTaskTimer(mPlugin, 20, mDelay);
	}

	@Override
	public int cooldownTicks() {
		return 12 * 20;
	}

	public void setSpeed(int speed) {
		mDelay = speed;
	}
}
