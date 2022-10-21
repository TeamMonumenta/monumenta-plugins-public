package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PairedUnnaturalForce extends Spell {

	private static final String ABILITY_NAME = "Unnatural Force";
	private static final int CAST_TIME = 20 * 3;
	private static final int EXECUTION_TIME = 20 * 3;
	private static final int DISPLAY_TIME = 20 * 2;
	private static final int COOLDOWN = 20 * 5;
	private static final int DAMAGE = 90;
	private static final int RANGE = 50;
	private static final int MAX_HEIGHT = 1;
	private static final int P_INCREMENT_RATE = 6;
	private static final int P_COUNT = 2;
	private static final Particle.DustOptions TEAL = new Particle.DustOptions(Color.fromRGB(24, 199, 203), 1.0f);
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	private final int mOuterRadius;
	private final int mMidRadius;
	private final int mMinRadius;


	public PairedUnnaturalForce(Plugin plugin, LivingEntity boss, Location spawnLoc, int minRadius, int midRadius, int outerRadius) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mMinRadius = minRadius;
		mMidRadius = midRadius;
		mOuterRadius = outerRadius;

		mChargeUp = new ChargeUpManager(mBoss, CAST_TIME, ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, RANGE);
	}

	@Override
	public void run() {
		World world = mSpawnLoc.getWorld();
		mChargeUp.setTime(0);
		mChargeUp.setColor(BarColor.YELLOW);
		List<String> options = new ArrayList<>();
		options.add("Inner ");
		options.add("Outer ");
		Collections.shuffle(options);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mMaxRad1 = 0;
			int mMaxRad2 = 0;
			int mMinRad1 = 0;
			int mMinRad2 = 0;
			int mYOffset1 = 0;
			int mYOffset2 = 0;

			@Override
			public void run() {
				if (options.get(0).equals("Inner ")) {
					mMaxRad1 = mMidRadius;
					mMinRad1 = mMinRadius;
					mMaxRad2 = mOuterRadius;
					mMinRad2 = mMidRadius;
					mYOffset1 = 0;
					mYOffset2 = 1;
				}
				if (options.get(0).equals("Outer ")) {
					mMaxRad1 = mOuterRadius;
					mMinRad1 = mMidRadius;
					mMaxRad2 = mMidRadius;
					mMinRad2 = mMinRadius;
					mYOffset1 = 1;
					mYOffset2 = 0;
				}

				mChargeUp.setTitle(ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + options.get(0) + ABILITY_NAME);
				if (mChargeUp.getTime() % 5 == 0 && mChargeUp.getTime() <= DISPLAY_TIME) {
					if (mChargeUp.getTime() % 10 == 0) {
						world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 10.5f, 2f);
					}
					for (double deg = 0; deg < 360; deg += P_INCREMENT_RATE) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = mMinRad1; x < mMaxRad1; x++) {
							for (int i = 0; i < MAX_HEIGHT; i++) {
								world.spawnParticle(Particle.REDSTONE, mSpawnLoc.clone().add(cos * x, mYOffset1 + i, sin * x), P_COUNT, 0.1, 1, 0.1, 0, TEAL);
							}
						}
					}
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.setTitle(ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + options.get(1) + ABILITY_NAME);
					world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 10.5f, 2);
					world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 10.5f, 1);

					for (double deg = 0; deg < 360; deg += P_INCREMENT_RATE * 2) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = mMinRad1; x < mMaxRad1; x += P_INCREMENT_RATE) {
							for (int y = 0; y < MAX_HEIGHT; y++) {
								Location loc = mSpawnLoc.clone().add(cos * x, y, sin * x);
								world.spawnParticle(Particle.SMOKE_NORMAL, loc, 5, 0.15, 0.15, 0.15, 0);
								if (deg % 4 == 0) {
									world.spawnParticle(Particle.BLOCK_DUST, loc, 5, 0.15, 0.1, 0.15, 0.75, Material.DEEPSLATE_TILES.createBlockData());
								} else {
									world.spawnParticle(Particle.BLOCK_DUST, loc, 5, 0.15, 0.1, 0.15, 0.75, Material.POLISHED_DEEPSLATE.createBlockData());
								}
								if (deg % 30 == 0) {
									world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 5, 0.15, 0.1, 0.15, 0.25);
								}
							}
						}
					}
					for (Player p : PlayerUtils.playersInRange(mSpawnLoc, mMaxRad1, true)) {
						if (!PlayerUtils.playersInRange(mSpawnLoc, mMinRad1, true).contains(p)) {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, "Unnatrual Force");
							MovementUtils.knockAway(mSpawnLoc, p, 0, .75f, false);
						}
					}

					BukkitRunnable runnable = new BukkitRunnable() {
						int mT = 0;
						@Override
						public void run() {
							double progress = 1 - ((double) mT / (double) EXECUTION_TIME);
							mChargeUp.setProgress(progress);
							mChargeUp.setColor(BarColor.RED);

							if (progress % 0.1 == 0) {
								if (progress % 0.2 == 0) {
									world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 10.5f, 2f);
								}
								for (double deg = 0; deg < 360; deg += P_INCREMENT_RATE) {
									double cos = FastUtils.cosDeg(deg);
									double sin = FastUtils.sinDeg(deg);

									for (int x = mMinRad2; x < mMaxRad2; x++) {
										for (int i = 0; i < MAX_HEIGHT; i++) {
											world.spawnParticle(Particle.REDSTONE, mSpawnLoc.clone().add(cos * x, mYOffset2 + i, sin * x), P_COUNT, 0.1, 1, 0.1, 0, TEAL);
										}
									}
								}
							}

							if (mT > EXECUTION_TIME) {

								world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 10.5f, 2);
								world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 10.5f, 1);
								for (double deg = 0; deg < 360; deg += P_INCREMENT_RATE * 2) {
									double cos = FastUtils.cosDeg(deg);
									double sin = FastUtils.sinDeg(deg);
									for (int x = mMinRad2; x < mMaxRad2; x += P_INCREMENT_RATE) {
										for (int y = 0; y < MAX_HEIGHT; y++) {
											Location loc = mSpawnLoc.clone().add(cos * x, y, sin * x);
											world.spawnParticle(Particle.SMOKE_NORMAL, loc, 5, 0.15, 0.15, 0.15, 0);
											if (deg % 4 == 0) {
												world.spawnParticle(Particle.BLOCK_DUST, loc, 5, 0.15, 0.1, 0.15, 0.75, Material.DEEPSLATE_TILES.createBlockData());
											} else {
												world.spawnParticle(Particle.BLOCK_DUST, loc, 5, 0.15, 0.1, 0.15, 0.75, Material.POLISHED_DEEPSLATE.createBlockData());
											}
											if (deg % 30 == 0) {
												world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 5, 0.15, 0.1, 0.15, 0.25);
											}
										}
									}
								}
								for (Player p : PlayerUtils.playersInRange(mSpawnLoc, mMaxRad2, true)) {
									if (!PlayerUtils.playersInRange(mSpawnLoc, mMinRad2, true).contains(p)) {
										DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, "Unnatrual Force");
										MovementUtils.knockAway(mSpawnLoc, p, 0, .75f, false);
									}
								}
								this.cancel();
								mActiveRunnables.remove(this);
							}
							mT += 1;
						}
					};
					runnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable);
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN + EXECUTION_TIME + CAST_TIME;
	}
}
