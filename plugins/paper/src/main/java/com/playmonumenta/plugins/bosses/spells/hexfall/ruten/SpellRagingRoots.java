package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellRagingRoots extends Spell {

	private static final String ABILITY_NAME = "Raging Roots (☠)";
	private static final double ANTI_HEAL_MODIFIER = -0.5;
	private static final double SLOW_AMPLIFIER = -0.95;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final double mDamage;
	private final int mInterval;
	private final int mDebuffDuration;
	private final float mSpeed;
	private final float mBoltDuration;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellRagingRoots(Plugin plugin, LivingEntity boss, int range, int castTime, double damage, int interval, int mDebuffDuration, float mSpeed, float mBoltDuration, Location spawnLoc, int coolown) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mDamage = damage;
		mInterval = interval;
		mCooldown = coolown;
		this.mDebuffDuration = mDebuffDuration;
		this.mSpeed = mSpeed;
		this.mBoltDuration = mBoltDuration;
		this.mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, net.kyori.adventure.text.Component.text("Channeling ", NamedTextColor.GOLD).append(net.kyori.adventure.text.Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, mRange * 2);
	}

	@Override
	public void run() {

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % mInterval == 0) {
					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						player.playSound(player, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
						player.playSound(player, Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);

						BukkitRunnable trackingBoltRunnable = new BukkitRunnable() {
							int mTicks = 0;
							final Location mBoltSpawnLoc = mSpawnLoc.clone().add(LocationUtils.getDirectionTo(player.getLocation(), mSpawnLoc).multiply(Ruten.arenaRadius));
							final BoundingBox mHitbox = BoundingBox.of(mBoltSpawnLoc, 0.2, 0.2, 0.2);

							@Override
							public void run() {
								if (mBoss.isDead() || !mBoss.isValid()) {
									this.cancel();
								}
								Vector dir = LocationUtils.getDirectionTo(player.getLocation(), mBoltSpawnLoc).setY(0);
								for (int i = 0; i < 2; i++) {
									Location loc = mHitbox.getCenter().toLocation(mBoss.getWorld());
									loc.setY(Ruten.arenaHeightY + 1);
									mHitbox.shift(dir.clone().multiply(mSpeed * 0.5));

									// If hits player
									for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
										if (player.getBoundingBox().overlaps(mHitbox)) {
											BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, mDamage, SpellStranglingRoot.ABILITY_NAME, mBoss.getLocation());
											loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1, 0);
											new PartialParticle(Particle.TOTEM, loc)
												.count(1)
												.spawnAsBoss();
											new PartialParticle(Particle.BLOCK_DUST, loc)
												.count(1)
												.data(Material.VERDANT_FROGLIGHT.createBlockData())
												.spawnAsBoss();
											new PartialParticle(Particle.BLOCK_DUST, loc)
												.count(1)
												.data(Material.MOSS_BLOCK.createBlockData())
												.spawnAsBoss();
											Plugin plugin = Plugin.getInstance();
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
										new PartialParticle(Particle.BLOCK_DUST, loc)
											.count(1)
											.data(Material.VERDANT_FROGLIGHT.createBlockData())
											.spawnAsBoss();
										new PartialParticle(Particle.BLOCK_DUST, loc)
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
										new PartialParticle(Particle.BLOCK_DUST, loc)
											.count(1)
											.data(Material.VERDANT_FROGLIGHT.createBlockData())
											.spawnAsBoss();
										new PartialParticle(Particle.BLOCK_DUST, loc)
											.count(1)
											.data(Material.MOSS_BLOCK.createBlockData())
											.spawnAsBoss();
										this.cancel();
									}
								}

								Location loc = mHitbox.getCenter().toLocation(mBoss.getWorld());
								loc.setY(Ruten.arenaHeightY + 1);
								if (mTicks % 5 == 0) {
									player.playSound(loc, Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 0.5f, 2);
									player.playSound(loc, Sound.BLOCK_GRASS_STEP, SoundCategory.HOSTILE, 0.75f, 0);
								}

								new PPExplosion(Particle.BLOCK_DUST, loc)
									.speed(1)
									.count(7)
									.extraRange(0.15, 0.2)
									.data(Material.SLIME_BLOCK.createBlockData())
									.spawnAsBoss();
								new PPExplosion(Particle.BLOCK_DUST, loc)
									.speed(1)
									.count(7)
									.extraRange(0.15, 0.2)
									.data(Material.VERDANT_FROGLIGHT.createBlockData())
									.spawnAsBoss();
								new PPExplosion(Particle.BLOCK_DUST, loc)
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
					}
				}

				if (mChargeUp.nextTick()) {
					this.cancel();

					for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
						PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME, true, true, true);
					}

					new PPSpiral(Particle.SLIME, mBoss.getLocation(), 23)
						.distanceFalloff(50)
						.count(120)
						.spawnAsBoss();
					new PPSpiral(Particle.BLOCK_DUST, mBoss.getLocation(), 23)
						.distanceFalloff(50)
						.count(120)
						.data(Material.OAK_LEAVES.createBlockData())
						.spawnAsBoss();

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
