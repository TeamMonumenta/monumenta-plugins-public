package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSurgingDeath extends Spell {

	private static final String ABILITY_NAME = "Surging Death";
	private static final Integer DEATH_RING_COUNT = 3;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final int mCastTime;
	private final int mPulseCount;
	private final int mPulseCastTime;
	private final int mPulseDelayTime;
	private final int mCooldown;
	private final ChargeUpManager mChargeUp;

	public SpellSurgingDeath(Plugin plugin, LivingEntity boss, Location spawnLoc, int range, int castTime, int pulseCount, int pulseCastTime, int pulseDelayTime, int cooldown) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mCastTime = castTime;
		mPulseCount = pulseCount;
		mPulseCastTime = pulseCastTime;
		mPulseDelayTime = pulseDelayTime;
		mCooldown = cooldown;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		mChargeUp.setColor(BossBar.Color.YELLOW);
		mChargeUp.setTitle(Component.text("Channeling ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
		mChargeUp.setChargeTime(mCastTime);

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {

				if (mChargeUp.getTime() % 20 == 0) {
					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						player.playSound(player, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.2f, 2);
						player.playSound(player, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.2f, 1);
						player.playSound(player, Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.2f, 1f);
					}

					new PPCircle(Particle.BLOCK_DUST, mSpawnLoc, Ruten.arenaRadius)
						.data(Material.SOUL_SOIL.createBlockData())
						.countPerMeter(3)
						.spawnAsBoss();
				}

				if (mChargeUp.nextTick()) {

					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
					mChargeUp.setColor(BossBar.Color.RED);

					BukkitRunnable castRunnable = new BukkitRunnable() {

						int mCount = mPulseCount;
						int mT = 0;

						@Override
						public void run() {
							mChargeUp.setProgress(((double) mCount / (double) mPulseCount));
							if (mT % (mPulseCastTime + mPulseDelayTime) == 0 && mCount > 0) {
								mCount--;

								// Target arena section
								BukkitRunnable executeRunnable = new BukkitRunnable() {
									int mExTicks = 0;
									double mRingCount = DEATH_RING_COUNT;
									double mPrevRing = Ruten.arenaRadius;
									@Override
									public void run() {
										if (mBoss.isDead() || !mBoss.isValid()) {
											this.cancel();
										}

										if (mExTicks % (mPulseCastTime / DEATH_RING_COUNT) == 0 && mRingCount > 0) {
											Location loc = mSpawnLoc.clone();
											if (mRingCount > 0) {
												mRingCount--;
											}
											double maxRad = mPrevRing;
											double minRad = mPrevRing - (double) Ruten.arenaRadius / DEATH_RING_COUNT;
											if (minRad < 0) {
												minRad = 0;
											}
											mPrevRing = minRad;
											Set<Block> blocksToConvert = new HashSet<>();

											for (double deg = 0; deg < 360; deg += 0.5) {
												double cos = FastUtils.cosDeg(deg);
												double sin = FastUtils.sinDeg(deg);
												for (double radius = minRad; radius < maxRad; radius += 0.5) {
													Location l = loc.clone().add(cos * radius, 0, sin * radius);
													blocksToConvert.add(l.getBlock());
												}
											}

											World world = mSpawnLoc.getWorld();
											world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.5f, 2);
											world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.5f, 1);
											world.playSound(mSpawnLoc, Sound.BLOCK_SOUL_SOIL_BREAK, SoundCategory.HOSTILE, 0.5f, 1);

											for (double radius = minRad; radius < maxRad; radius += 1) {
												new PPCircle(Particle.BLOCK_DUST, mBoss.getLocation(), radius)
													.ringMode(true)
													.count(20)
													.data(Material.SOUL_SOIL.createBlockData())
													.spawnAsBoss();
											}

											for (Block b : blocksToConvert) {
												Ruten.modifyAnimaAtLocation(b.getLocation().clone().subtract(0, 1, 0), Ruten.AnimaTendency.DEATH);
											}

										}

										mExTicks++;
										if (mExTicks > mPulseCastTime) {
											this.cancel();
										}
									}
								};
								executeRunnable.runTaskTimer(mPlugin, 0, 1);
								mActiveRunnables.add(executeRunnable);


							}
							if (mCount <= 0 && mT > ((mPulseCastTime + mPulseDelayTime) * mPulseCount)) {
								this.cancel();
								mChargeUp.remove();
							}
							mT++;
						}
					};
					castRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(castRunnable);

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
