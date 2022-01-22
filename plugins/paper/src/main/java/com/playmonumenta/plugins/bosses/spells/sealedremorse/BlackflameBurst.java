package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.BeastOfTheBlackFlame;
import com.playmonumenta.plugins.bosses.bosses.Ghalkor;
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

import java.util.Collections;
import java.util.List;

public class BlackflameBurst extends Spell {

	private SpellBaseSeekingProjectile mMissile;

	private LivingEntity mBoss;
	private Plugin mPlugin;
	private BeastOfTheBlackFlame mBossClass;

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
	private static final int DAMAGE = 24;

	public BlackflameBurst(LivingEntity boss, Plugin plugin, BeastOfTheBlackFlame bossClass) {
		mBoss = boss;
		mPlugin = plugin;
		mBossClass = bossClass;

		mMissile = new SpellBaseSeekingProjectile(plugin, boss, Ghalkor.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
				SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

					if (ticks % 4 == 0) {
						world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2);
						world.spawnParticle(Particle.SMOKE_NORMAL, loc, 8, 0.5, 0.5, 0.5, 0.2);
					}
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, 2);
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 3, 1.2f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.1, 0.1, 0.1, 0.05);
					world.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.25, 0.25, 0.05);
				},
				// Hit Action
				(World world, LivingEntity player, Location loc) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 1, 0);
					loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 0.5, 0.5, 0.5);
					if (player != null) {
						BossUtils.dualTypeBlockableDamage(boss, player, DamageType.MAGIC, DamageType.FIRE, DAMAGE, 0.9, "Blackflame Burst", mBoss.getLocation());
						player.setFireTicks(4 * 20);
					}
				});
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), Ghalkor.detectionRange, true);
		Collections.shuffle(players);
		if (players.size() == 0 || mBoss.getTargetBlock(Ghalkor.detectionRange) == null) {
			return;
		}

		//Used for the launch method, does not actually target this player
		Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));

		new BukkitRunnable() {
			private int mTicks = 0;
			@Override
			public void run() {
				if (mTicks >= DELAY) {

					mMissile.launch(player, mBoss.getEyeLocation().add(0, 0, 1));
					mMissile.launch(player, mBoss.getEyeLocation().add(0, 0, -1));
					mMissile.launch(player, mBoss.getEyeLocation().add(-1, 0, 0));
					mMissile.launch(player, mBoss.getEyeLocation().add(1, 0, 0));

					mMissile.launch(player, mBoss.getEyeLocation().add(1, 0, 1));
					mMissile.launch(player, mBoss.getEyeLocation().add(-1, 0, -1));
					mMissile.launch(player, mBoss.getEyeLocation().add(-1, 0, 1));
					mMissile.launch(player, mBoss.getEyeLocation().add(1, 0, -1));

					new BukkitRunnable() {
						@Override
						public void run() {
							for (Player p : players) {
								mMissile.launch(p, p.getEyeLocation());
							}
						}
					}.runTaskLater(mPlugin, 10);

					new BukkitRunnable() {
						@Override
						public void run() {
							for (Player p : players) {
								mMissile.launch(p, p.getEyeLocation());
							}
						}
					}.runTaskLater(mPlugin, 20);

					this.cancel();
				}

				PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

				if (mTicks % 4 == 0) {
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 0.2);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 0.2);
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return (int) (6 * 20 * mBossClass.mCastSpeed);
	}
}
