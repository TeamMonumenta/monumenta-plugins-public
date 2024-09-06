package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellVerdantGrasp extends Spell {

	private static final String ABILITY_NAME = "Verdant Grasp";
	private static final double PULL_STRENGTH = 0.15;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mCooldown;
	private final int mDamage;
	private final int mChannelTime;
	private final int mCastTime;
	private final double mRadius;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;


	public SpellVerdantGrasp(Plugin plugin, LivingEntity boss, int range, int damage, int cooldown, int channelTime, int castTime, double radius, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mDamage = damage;
		mCooldown = cooldown;
		mChannelTime = channelTime;
		mCastTime = castTime;
		mRadius = radius;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, mCastTime, Component.text("Casting ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);

	}

	@Override
	public void run() {
		mChargeUp.reset();
		mChargeUp.setColor(BossBar.Color.YELLOW);
		mChargeUp.setTitle(Component.text("Channeling ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
		mChargeUp.setChargeTime(mChannelTime);

		BukkitRunnable runnable = new BukkitRunnable() {

			int mChargeTicks = 0;

			@Override
			public void run() {

				if (mChargeUp.getTime() % 5 == 0) {
					for (Player p : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						p.playSound(p.getLocation(), Sound.BLOCK_FUNGUS_PLACE, SoundCategory.HOSTILE, 1f, 1f);
					}
					new PPCircle(Particle.BLOCK_DUST, mBoss.getLocation(), mRadius * (double) mChargeTicks / mChannelTime)
						.data(Material.CRIMSON_HYPHAE.createBlockData())
						.count(60)
						.spawnAsBoss();
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
					mChargeUp.setColor(BossBar.Color.RED);
					this.cancel();

					List<Player> players = HexfallUtils.getPlayersInRuten(mSpawnLoc);
					Collections.shuffle(players);
					List<Player> findTargets = new ArrayList<>();
					List<Player> findBreakers = new ArrayList<>();

					switch (players.size()) {
						case 1 -> findTargets.add(players.get(0));
						case 2 -> {
							findTargets.add(players.get(0));
							findBreakers.add(players.get(1));
						}
						case 3 -> {
							findTargets.add(players.get(0));
							findTargets.add(players.get(1));
							findBreakers.add(players.get(2));
						}
						case 4 -> {
							findTargets.add(players.get(0));
							findTargets.add(players.get(1));
							findBreakers.add(players.get(2));
							findBreakers.add(players.get(3));
						}
						default -> findTargets.addAll(players);
					}

					BukkitRunnable castRunnable = new BukkitRunnable() {

						int mT = 0;
						final Location mBossLocOffset = mBoss.getLocation().clone().add(0, 1, 0);
						List<Player> mTargets = findTargets;
						List<Player> mBreakers = findBreakers;

						@Override
						public void run() {
							mTargets = mTargets.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());
							mBreakers = mBreakers.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());

							double progress = 1 - ((double) mT / (double) mCastTime);
							mChargeUp.setProgress(progress);
							EntityUtils.selfRoot(mBoss, mCastTime);

							if (mTargets.isEmpty()) {
								mChargeUp.remove();
								this.cancel();
							}

							if (mT % 10 == 0) {
								for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
									player.playSound(player.getLocation(), Sound.BLOCK_FUNGUS_BREAK, SoundCategory.HOSTILE, 1f, 1.5f);
								}
							}

							List<Player> brokenTargets = new ArrayList<>();
							for (Player target : mTargets) {
								if (mT % 5 == 0) {
									Vector dir = target.getLocation().subtract(mBossLocOffset.toVector().setY(0)).toVector().multiply(-(PULL_STRENGTH * mT / mCastTime));
									target.setVelocity(dir);

									new PPLine(Particle.BLOCK_DUST, mBossLocOffset, target.getLocation().add(0, 1, 0))
										.data(Material.WARPED_HYPHAE.createBlockData())
										.countPerMeter(7)
										.spawnAsBoss();
									new PPCircle(Particle.BLOCK_DUST, mBossLocOffset.clone().add(0, -1, 0), mRadius)
										.data(Material.CRIMSON_HYPHAE.createBlockData())
										.count(60)
										.spawnAsBoss();
								}

								BoundingBox box = BoundingBox.of(mBossLocOffset, 0.3f, 0.3f, 0.3f);
								Vector vec = LocationUtils.getDirectionTo(target.getLocation(), mBossLocOffset);
								double distance = target.getLocation().distance(mBossLocOffset);

								for (int i = 0; i < 100; i++) {
									box.shift(vec.clone().multiply(0.01 * distance));
									for (Player breaker : mBreakers) {
										if (box.overlaps(breaker.getBoundingBox())) {
											brokenTargets.add(target);
											break;
										}
									}
									if (brokenTargets.contains(target)) {
										break;
									}
								}
							}

							for (Player brokenTarget : brokenTargets) {
								mTargets.remove(brokenTarget);
							}

							if (mT >= mCastTime) {
								mChargeUp.remove();
								this.cancel();

								new PPExplosion(Particle.BLOCK_DUST, mBossLocOffset)
									.data(Material.CRIMSON_HYPHAE.createBlockData())
									.count(150)
									.delta(mRadius / 2)
									.spawnAsBoss();

								for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
									player.playSound(mBossLocOffset, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 2f, 2f);
									player.playSound(mBossLocOffset, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5f, 1f);
									if (LocationUtils.xzDistance(player.getLocation(), mBossLocOffset.clone().add(0, -1, 0)) <= mRadius) {
										DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, mDamage, null, true, false, ABILITY_NAME);
									}
								}
							}
							mT++;
						}
					};
					castRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(castRunnable);

				}
				mChargeTicks++;
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
