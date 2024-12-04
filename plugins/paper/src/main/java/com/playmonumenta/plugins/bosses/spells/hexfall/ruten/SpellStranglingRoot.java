package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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

public class SpellStranglingRoot extends Spell {

	public static final String ABILITY_NAME = "Strangling Root";
	private static final double ANTI_HEAL_MODIFIER = -0.5;
	private static final double SLOW_AMPLIFIER = -0.95;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mDamage;
	private final int mDebuffDuration;
	private final float mSpeed;
	private final int mCastCount;
	private final int mInterval;
	private final int mCooldown;
	private final int mBoltDuration;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellStranglingRoot(Plugin plugin, LivingEntity boss, int range, int damage, int debuffDuration, float speed, int castCount, int interval, int cooldown, int boltDuration, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mDamage = damage;
		mDebuffDuration = debuffDuration;
		mSpeed = speed;
		mCastCount = castCount;
		mInterval = interval;
		mCooldown = cooldown;
		mBoltDuration = boltDuration;
		mChargeUp = new ChargeUpManager(boss, mCastCount * mInterval, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange * 2);
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		mChargeUp.reset();

		BukkitRunnable runnable = new BukkitRunnable() {
			List<Player> mPlayers = HexfallUtils.getPlayersInRuten(mSpawnLoc);

			@Override
			public void run() {
				mPlayers = mPlayers.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());

				if (mChargeUp.getTime() % 4 == 0) {
					for (Player player : mPlayers) {
						player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1f, 0f);
					}
				}

				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.setTitle(Component.text("Casting ", NamedTextColor.YELLOW).append(Component.text(ABILITY_NAME, NamedTextColor.GOLD)));
					mChargeUp.setColor(BossBar.Color.RED);

					BukkitRunnable multiCastRunnable = new BukkitRunnable() {
						int mCount = mCastCount;
						int mTicksMultiCast = 0;
						int mTChargeUp = 0;
						@Override
						public void run() {
							mPlayers = mPlayers.stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());
							mChargeUp.setProgress(((double) mCount / (double) mCastCount));

							if (mTicksMultiCast % mInterval == 0 && mCount > 0) {
								mCount--;

								mPlayers.forEach((p) -> {
									p.playSound(p, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
									p.playSound(p, Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
								});

								mPlayers.forEach(p -> {
									BoundingBox mHitbox = BoundingBox.of(mBoss.getLocation(), 0.2, 0.2, 0.2);
									BukkitRunnable trackingBoltRunnable = new BukkitRunnable() {
										int mTicks = 0;

										@Override
										public void run() {
											if (mBoss.isDead() || !mBoss.isValid()) {
												this.cancel();
											}
											Vector dir = LocationUtils.getDirectionTo(p.getLocation(), mBoss.getLocation()).setY(0);
											for (int i = 0; i < 2; i++) {
												Location loc = mHitbox.getCenter().toLocation(mBoss.getWorld());
												loc.setY(Ruten.arenaHeightY + 1);
												mHitbox.shift(dir.clone().multiply(mSpeed * 0.5));

												// If hits player
												for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
													if (player.getBoundingBox().overlaps(mHitbox)) {
														BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, mDamage, ABILITY_NAME, mBoss.getLocation());
														loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1, 0);
														new PartialParticle(Particle.TOTEM, loc)
															.count(1)
															.spawnAsBoss();
														new PartialParticle(Particle.BLOCK_CRACK, loc)
															.count(1)
															.data(Material.VERDANT_FROGLIGHT.createBlockData())
															.spawnAsBoss();
														new PartialParticle(Particle.BLOCK_CRACK, loc)
															.count(1)
															.data(Material.MOSS_BLOCK.createBlockData())
															.spawnAsBoss();
														com.playmonumenta.plugins.Plugin plugin = com.playmonumenta.plugins.Plugin.getInstance();
														plugin.mEffectManager.addEffect(player, "StranglingRootAntiHeal", new PercentHeal(mDebuffDuration, ANTI_HEAL_MODIFIER));
														plugin.mEffectManager.addEffect(player, "StranglingRootSlowness", new PercentSpeed(mDebuffDuration, SLOW_AMPLIFIER, "StranglingRootSlowness"));
														this.cancel();
													}
												}
												// If hits block
												if (loc.getBlock().getType().isSolid()) {
													loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1, 0);
													new PartialParticle(Particle.TOTEM, loc)
														.count(1)
														.spawnAsBoss();
													new PartialParticle(Particle.BLOCK_CRACK, loc)
														.count(1)
														.data(Material.VERDANT_FROGLIGHT.createBlockData())
														.spawnAsBoss();
													new PartialParticle(Particle.BLOCK_CRACK, loc)
														.count(1)
														.data(Material.MOSS_BLOCK.createBlockData())
														.spawnAsBoss();
													this.cancel();
												}

												// If above air
												if (loc.add(0, -1, 0).getBlock().getType() == Material.AIR || loc.add(0, -1, 0).getBlock().getType() == Material.VOID_AIR || loc.add(0, -1, 0).getBlock().getType() == Material.CAVE_AIR) {
													loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1, 0);
													new PartialParticle(Particle.TOTEM, loc)
														.count(1)
														.spawnAsBoss();
													new PartialParticle(Particle.BLOCK_CRACK, loc)
														.count(1)
														.data(Material.VERDANT_FROGLIGHT.createBlockData())
														.spawnAsBoss();
													new PartialParticle(Particle.BLOCK_CRACK, loc)
														.count(1)
														.data(Material.MOSS_BLOCK.createBlockData())
														.spawnAsBoss();
													this.cancel();
												}
											}
											Location loc = mHitbox.getCenter().toLocation(mBoss.getWorld());
											loc.setY(Ruten.arenaHeightY + 1);
											if (mTicks % 5 == 0) {
												p.playSound(loc, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 0.5f, 2);
												p.playSound(loc, Sound.BLOCK_GRASS_STEP, SoundCategory.HOSTILE, 0.75f, 0);
											}

											new PPExplosion(Particle.BLOCK_CRACK, loc)
												.speed(1)
												.count(7)
												.extraRange(0.15, 0.2)
												.data(Material.SLIME_BLOCK.createBlockData())
												.spawnAsBoss();
											new PPExplosion(Particle.BLOCK_CRACK, loc)
												.speed(1)
												.count(7)
												.extraRange(0.15, 0.2)
												.data(Material.VERDANT_FROGLIGHT.createBlockData())
												.spawnAsBoss();
											new PPExplosion(Particle.BLOCK_CRACK, loc)
												.speed(1)
												.count(7)
												.extraRange(0.15, 0.2)
												.data(Material.MOSS_BLOCK.createBlockData())
												.spawnAsBoss();

											mTicks++;
											if (mTicks > mBoltDuration || loc.distance(mBoss.getLocation()) > mRange) {
												this.cancel();
											}
										}
									};
									trackingBoltRunnable.runTaskTimer(mPlugin, 0, 1);
									mActiveRunnables.add(trackingBoltRunnable);
								});
							}
							if (mCount <= 0 && mTicksMultiCast > (mInterval * mCastCount)) {
								this.cancel();
								mChargeUp.remove();
							}
							mTicksMultiCast++;
							mTChargeUp++;
						}
					};
					multiCastRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(multiCastRunnable);
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown + (mInterval * mCastCount);
	}
}
