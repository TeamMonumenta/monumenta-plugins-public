package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.DeathVulnerability;
import com.playmonumenta.plugins.effects.hexfall.LifeVulnerability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellCascadingHex extends Spell {

	private static final String ABILITY_NAME = "Cascading Hex";
	private static final String CAST_ABILITY_NAME = "Hex Explosion";
	private static final int MAX_FALL_HEIGHT = 10;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mRadius;
	private final int mDamage;
	private final int mVulnLength;
	private final int mFallTime;
	private final int mMaximumDuration;
	private final int mChannelTime;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final ChargeUpManager mChargeUp;

	public SpellCascadingHex(Plugin plugin, LivingEntity boss, int range, int radius, int damage, int vulnLength, int fallTime, int maximumDuration, int channelTime, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mRadius = radius;
		mDamage = damage;
		mVulnLength = vulnLength;
		mFallTime = fallTime;
		mMaximumDuration = maximumDuration;
		mChannelTime = channelTime;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, channelTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {
		mChargeUp.reset();

		Map<Location, Boolean> locationTypes = new HashMap<>();

		boolean isLife = false;
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if (e.getScoreboardTags().contains("Hycenea_Cascades")) {
				locationTypes.put(e.getLocation(), isLife);
				isLife = !isLife;
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % 20 == 0) {
					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1f, (float) mChargeUp.getTime() / mChannelTime);
						player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 1f, (float) mChargeUp.getTime() / mChannelTime);
					}
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.remove();
					this.cancel();

					for (Location loc : locationTypes.keySet()) {
						Material mat = locationTypes.get(loc) ? Material.MOSS_BLOCK : Material.SOUL_SAND;
						FallingBlock fallingBlock = mBoss.getWorld().spawn(mBoss.getEyeLocation(), FallingBlock.class, b -> b.setBlockData(mat.createBlockData()));
						fallingBlock.setGravity(false);
						fallingBlock.setDropItem(false);
						EntityUtils.disableBlockPlacement(fallingBlock);
						Location blockLoc = fallingBlock.getLocation();
						Location endLoc = loc.clone().add(0, MAX_FALL_HEIGHT, 0);
						Vector vec = LocationUtils.getVectorTo(endLoc, blockLoc);
						vec.normalize().multiply(endLoc.distance(blockLoc) / 20);
						fallingBlock.setVelocity(vec);
						fallingBlock.addScoreboardTag("DisableBlockPlacement");

						BukkitRunnable fbRunnable = new BukkitRunnable() {
							int mT = 0;

							@Override
							public void run() {
								if (fallingBlock.getLocation().distance(endLoc) <= 0.25 || mT++ >= 20) {
									fallingBlock.remove();
									spawnCascade(loc, locationTypes.get(loc));
									this.cancel();
								}
							}
						};
						mActiveRunnables.add(fbRunnable);
						fbRunnable.runTaskTimer(mPlugin, 0, 1);
					}
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

	private void spawnCascade(Location loc, boolean isLife) {

		BukkitRunnable runnable = new BukkitRunnable() {
			final BlockDisplay mCascadeDisplay = loc.getWorld().spawn(loc.clone().add(-0.5, 0, -0.5), BlockDisplay.class);
			final TextDisplay mTextDisplay = loc.getWorld().spawn(loc.clone().add(0, 5.5, 0), TextDisplay.class);
			int mT = 0;
			int mCurrentFallTime = mFallTime;
			final int mMaxTickMarks = 60;

			@Override
			public void run() {
				if (mT == 0) {
					mCascadeDisplay.setBlock(isLife ? Material.MOSS_BLOCK.createBlockData() : Material.SOUL_SAND.createBlockData());
					mCascadeDisplay.setBrightness(new Display.Brightness(mCascadeDisplay.getLocation().getBlock().getLightLevel(), mCascadeDisplay.getLocation().getBlock().getLightFromSky()));
					mCascadeDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0f), new Quaternionf()));
					mCascadeDisplay.addScoreboardTag("HexfallDisplay");

					mTextDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
					mTextDisplay.setBillboard(Display.Billboard.CENTER);
					mTextDisplay.addScoreboardTag("HexfallDisplay");
				}

				float progress = (float) mCurrentFallTime / mFallTime;

				Component text = Component.empty().append(Component.text(ABILITY_NAME + "\n", Style.style(NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)))
					.append(Component.text("[", NamedTextColor.WHITE))
					.append(Component.text("|".repeat((int) (mMaxTickMarks * (1 - progress))), NamedTextColor.GREEN))
					.append(Component.text("|".repeat((int) (mMaxTickMarks * progress)), NamedTextColor.RED))
					.append(Component.text("]\n", NamedTextColor.WHITE))
					.append(Component.text(CAST_ABILITY_NAME + " ", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)))
					.append(Component.text("[ ", NamedTextColor.WHITE))
					.append(Component.text(String.format("%.02f", (float) (mMaximumDuration - mT) / 20) + "s", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)))
					.append(Component.text(" ]", NamedTextColor.WHITE));
				mTextDisplay.text(text);

				double currentHeight = MAX_FALL_HEIGHT * ((double) mCurrentFallTime / (double) mFallTime);
				mCascadeDisplay.setTransformation(new Transformation(new Vector3f(0, (float) currentHeight, 0), mCascadeDisplay.getTransformation().getLeftRotation(), mCascadeDisplay.getTransformation().getScale(), mCascadeDisplay.getTransformation().getRightRotation()));
				mCascadeDisplay.setInterpolationDelay(-1);

				if (!HexfallUtils.playersInBossInXZRange(loc, mRadius, true).isEmpty()) {
					if (mT % 2 == 0) {
						new PPPillar(Particle.REDSTONE, loc, 5)
							.data(new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.65f))
							.count(15)
							.spawnAsBoss();
					}
				} else {
					mCurrentFallTime--;
					if (mT % 2 == 0) {
						new PPPillar(Particle.REDSTONE, loc, 5)
							.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.65f))
							.count(5)
							.spawnAsBoss();
					}
				}

				if (mT % 4 == 0) {
					new PPCircle(Particle.REDSTONE, loc, mRadius)
						.count(25)
						.data(new Particle.DustOptions(isLife ? Color.fromRGB(0, 204, 0) : Color.fromRGB(153, 76, 37), 1.65f))
						.ringMode(true)
						.spawnAsBoss();
				}

				if (mCurrentFallTime <= 0) {
					this.cancel();

					for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						p.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.35f, 1.25f);

						if (isLife) {
							if (mPlugin.mEffectManager.getActiveEffect(p, LifeVulnerability.class) != null) {
								PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME);
							} else {
								mPlugin.mEffectManager.clearEffects(p, DeathVulnerability.GENERIC_NAME);
								mPlugin.mEffectManager.addEffect(p, LifeVulnerability.GENERIC_NAME, new LifeVulnerability(mVulnLength));
							}
						} else {
							if (mPlugin.mEffectManager.getActiveEffect(p, DeathVulnerability.class) != null) {
								PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME);
							} else {
								mPlugin.mEffectManager.clearEffects(p, LifeVulnerability.GENERIC_NAME);
								mPlugin.mEffectManager.addEffect(p, DeathVulnerability.GENERIC_NAME, new DeathVulnerability(mVulnLength));
							}
						}

						BukkitRunnable resolveRunnable = new BukkitRunnable() {
							int mTT = 0;
							final int mAnimationTime = 30;
							final double mMaxOuterRadius = 15;
							double mCurrentRad = 0;

							@Override
							public void run() {
								if (mTT % 2 == 0) {
									new PPCircle(Particle.REDSTONE, loc, mCurrentRad)
										.data(new Particle.DustOptions(isLife ? Color.fromRGB(0, 204, 0) : Color.fromRGB(153, 76, 37), 1.65f))
										.countPerMeter(3)
										.spawnAsBoss();
								}
								mCurrentRad++;
								if (mTT++ >= mAnimationTime || mCurrentRad > mMaxOuterRadius) {
									this.cancel();
								}
							}
						};
						resolveRunnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(resolveRunnable);
					}
				} else if (mT >= mMaximumDuration) {
					this.cancel();

					new PPExplosion(Particle.EXPLOSION_HUGE, loc.clone().add(0, 2, 0))
						.delta(2)
						.count(50)
						.spawnAsBoss();

					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, mDamage, null, false, true, CAST_ABILITY_NAME);
					}
				}
				mT++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mCascadeDisplay.remove();
				mTextDisplay.remove();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}
