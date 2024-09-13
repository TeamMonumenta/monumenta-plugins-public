package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellVileBloom extends Spell {

	private static final String ABILITY_NAME = "Vile Bloom";
	private static final String SPAWN_MARKER_TAG = "Ruten_VileBloom";
	private static final String RED_BLOOM_NAME = "vileBloomRed";
	private static final String BLUE_BLOOM_NAME = "vileBloomBlue";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mExecutionTime;
	private final int mBloomRange;
	private final double mDamage;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;


	public SpellVileBloom(Plugin plugin, LivingEntity boss, int castTime, int executionTime, int bloomRange, double damage, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mExecutionTime = executionTime;
		mBloomRange = bloomRange;
		mDamage = damage;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, Ruten.detectionRange * 2);
	}

	@Override
	public void run() {
		List<LivingEntity> vileBloomSpawns = new ArrayList<>();
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % 5 == 0) {
					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 1f, 2f);
					}
				}

				if (mChargeUp.nextTick()) {
					List<Location> bloomSpawnLocations = new ArrayList<>();

					for (Entity e : mBoss.getNearbyEntities(Ruten.detectionRange, Ruten.detectionRange, Ruten.detectionRange)) {
						Set<String> tags = e.getScoreboardTags();
						if (tags.contains(SPAWN_MARKER_TAG)) {
							bloomSpawnLocations.add(e.getLocation());
						}
					}

					Collections.shuffle(bloomSpawnLocations);
					for (int i = 0; i < bloomSpawnLocations.size(); i++) {
						if (i % 2 == 0) {
							vileBloomSpawns.add((LivingEntity) LibraryOfSoulsIntegration.summon(bloomSpawnLocations.get(i), RED_BLOOM_NAME));
						} else {
							vileBloomSpawns.add((LivingEntity) LibraryOfSoulsIntegration.summon(bloomSpawnLocations.get(i), BLUE_BLOOM_NAME));
						}
					}

					this.cancel();
					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.RED)));
					mChargeUp.setColor(BossBar.Color.RED);

					BukkitRunnable windDownRunnable = new BukkitRunnable() {
						int mT = 0;

						@Override
						public synchronized void cancel() {
							for (LivingEntity entity : vileBloomSpawns) {
								entity.remove();
							}
							super.cancel();
						}

						@Override
						public void run() {
							double progress = ((double) mT / (double) mExecutionTime);
							mChargeUp.setProgress(1 - progress);

							if (mT % 10 == 0) {
								for (LivingEntity entity : vileBloomSpawns) {
									if (entity.getScoreboardTags().contains(RED_BLOOM_NAME) && progress < 0.5) {
										new PPCircle(Particle.BLOCK_DUST, entity.getLocation(), mBloomRange)
											.data(Material.ROSE_BUSH.createBlockData())
											.countPerMeter(3)
											.spawnAsBoss();
									} else if (entity.getScoreboardTags().contains(BLUE_BLOOM_NAME) && progress < 1.0) {
										new PPCircle(Particle.BLOCK_DUST, entity.getLocation(), mBloomRange)
											.data(Material.BLUE_ORCHID.createBlockData())
											.countPerMeter(3)
											.spawnAsBoss();
									}
								}
								for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
									player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 1f, 1.2f);
								}
							}

							if (progress == 0.5) {
								mChargeUp.setColor(BossBar.Color.BLUE);

								for (LivingEntity entity : vileBloomSpawns) {
									if (entity.getScoreboardTags().contains(RED_BLOOM_NAME)) {
										new PPExplosion(Particle.EXPLOSION_LARGE, entity.getLocation())
											.speed(1)
											.delta(15)
											.count(120)
											.extraRange(0.15, 2)
											.spawnAsBoss();

										for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
											player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);

											if (LocationUtils.xzDistance(player.getLocation(), entity.getLocation()) <= mBloomRange) {
												DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, mDamage, null, false, true, ABILITY_NAME);
											}
										}

										entity.remove();
									}
								}
							}

							if (progress == 1.0) {
								for (LivingEntity entity : vileBloomSpawns) {
									if (entity.getScoreboardTags().contains(BLUE_BLOOM_NAME)) {
										new PPExplosion(Particle.EXPLOSION_LARGE, entity.getLocation())
											.speed(1)
											.delta(15)
											.count(120)
											.extraRange(0.15, 2)
											.spawnAsBoss();

										for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
											player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);

											if (LocationUtils.xzDistance(player.getLocation(), entity.getLocation()) <= mBloomRange) {
												DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, mDamage, null, false, true, ABILITY_NAME);
											}
										}

										entity.remove();
									}
								}
							}

							if (mT++ >= mExecutionTime) {
								this.cancel();
							}
						}
					};
					windDownRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(windDownRunnable);
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
