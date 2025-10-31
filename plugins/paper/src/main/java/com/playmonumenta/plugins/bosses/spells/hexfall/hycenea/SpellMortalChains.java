package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.MortalVulnerability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellMortalChains extends Spell {

	private static final String ABILITY_NAME = "Mortal Chains";
	private static final String CAST_ABILITY_NAME = "Chain Explosion";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final double mRadius;
	private final int mDamage;
	private final int mMaxTetherDuration;
	private final int mTetherBreakTime;
	private final int mTetherBreakThreshold;
	private final int mDebuffDuration;
	private final int mCooldown;
	private final ChargeUpManager mChargeUp;
	private final Location mSpawnLoc;

	public SpellMortalChains(Plugin plugin, LivingEntity boss, int range, double radius, int damage, int castTime, int maxTetherDuration, int tetherBreakTime, int tetherBreakThreshold, int debuffDuration, int cooldown, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mRadius = radius;
		mDamage = damage;
		mMaxTetherDuration = maxTetherDuration;
		mTetherBreakTime = tetherBreakTime;
		mTetherBreakThreshold = tetherBreakThreshold;
		mDebuffDuration = debuffDuration;
		mCooldown = cooldown;
		mSpawnLoc = spawnLoc;
		mChargeUp = new ChargeUpManager(boss, castTime, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(ABILITY_NAME, NamedTextColor.YELLOW)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, range * 2);
	}

	@Override
	public void run() {

		List<Location> totemLoc = new ArrayList<>();
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if (e.getScoreboardTags().contains("Hycenea_Cardinal")) {
				totemLoc.add(e.getLocation());
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {

				if (mChargeUp.getTime() % 5 == 0) {
					for (Location loc : totemLoc) {
						new PPCircle(Particle.BLOCK_CRACK, loc, mRadius).data(Material.OAK_WOOD.createBlockData()).count(25).spawnAsBoss();
						new PPCircle(Particle.BLOCK_CRACK, loc, mRadius).data(Material.JUNGLE_LEAVES.createBlockData()).count(25).spawnAsBoss();

						if (!HexfallUtils.playersInBossInXZRange(loc, mRadius, true).isEmpty()) {
							new PPPillar(Particle.REDSTONE, loc, 5).data(new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.65f)).count(15).spawnAsBoss();
						} else {
							new PPPillar(Particle.REDSTONE, loc, 5).data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.65f)).count(5).spawnAsBoss();
						}
					}
				}

				if (mChargeUp.getTime() % 8 == 0) {
					for (Player viewer : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						viewer.playSound(viewer.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.25f, 0.5f + (mChargeUp.getTime() / 100f) * 1.5f);
					}
				}

				if (mChargeUp.nextTick()) {
					mChargeUp.remove();
					this.cancel();

					List<Location> unsoakedLocs = new ArrayList<>();
					List<Player> playersWithTethers = new ArrayList<>();

					boolean soaked = true;
					for (Location loc : totemLoc) {

						BukkitRunnable soakResolveRunnable = new BukkitRunnable() {
							int mT = 0;
							final int mAnimationTime = 20 * 2;
							double mCircleRadius = mRadius;

							@Override
							public void run() {
								mCircleRadius = (mRadius * (1d - ((double) mT / (double) mAnimationTime)));
								if (mT % 2 == 0) {
									new PPCircle(Particle.BLOCK_CRACK, loc, mCircleRadius).data(Material.OAK_WOOD.createBlockData()).count(35).spawnAsBoss();
									new PPCircle(Particle.BLOCK_CRACK, loc, mCircleRadius).data(Material.JUNGLE_LEAVES.createBlockData()).count(35).spawnAsBoss();
								}
								if (mT >= mAnimationTime) {
									new PPExplosion(Particle.SOUL_FIRE_FLAME, loc)
										.count(80)
										.extra(0.5)
										.speed(0.5)
										.spawnAsBoss();
									this.cancel();
								}
								mT++;
							}
						};
						soakResolveRunnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(soakResolveRunnable);

						List<Player> playersInSoak = HexfallUtils.playersInBossInXZRange(loc, mRadius, true);

						if (!playersInSoak.isEmpty()) {
							attachTether(loc.clone().add(0, 1.5, 0), playersInSoak.get(0));
							playersInSoak.get(0).sendMessage(Component.text("A chain latches onto your very soul...").decorate(TextDecoration.ITALIC).color(TextColor.color(160, 160, 160)));
							playersWithTethers.add(playersInSoak.get(0));
						} else {
							unsoakedLocs.add(loc);

							soaked = false;
							mBoss.getWorld().strikeLightningEffect(loc);

							BukkitRunnable failRaidwideRunnable = new BukkitRunnable() {
								int mT = 0;
								final int mAnimationTime = 30;
								final double mMaxOuterRadius = 25;
								final double mMaxInnerRadius = 20;
								double mOuterRad = mRadius;
								double mInnerRad = 0;

								@Override
								public void run() {
									if (mT % 2 == 0) {
										new PPCircle(Particle.BLOCK_CRACK, loc, mInnerRad).data(Material.OAK_WOOD.createBlockData()).count(15).spawnAsBoss();
										new PPCircle(Particle.BLOCK_CRACK, loc, mInnerRad).data(Material.JUNGLE_LEAVES.createBlockData()).count(15).spawnAsBoss();
										for (double rad = mInnerRad; rad < mOuterRad; rad += 0.5) {
											new PPCircle(Particle.BLOCK_CRACK, loc, rad).data(Material.POPPY.createBlockData()).count(15).spawnAsBoss();
										}
									}

									double ratio = ((double) mT) / mAnimationTime;
									mInnerRad = mMaxInnerRadius * ratio;
									mOuterRad = mMaxOuterRadius * ratio;

									if (mT >= mAnimationTime) {
										this.cancel();
									}
									mT++;
								}
							};
							failRaidwideRunnable.runTaskTimer(mPlugin, 0, 1);
							mActiveRunnables.add(failRaidwideRunnable);

						}
					}

					for (Location location : unsoakedLocs) {
						for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
							if (!playersWithTethers.contains(player)) {
								attachTether(location.clone().add(0, 1.5, 0), player);
								player.sendMessage(Component.text("A chain latches onto your very soul...").decorate(TextDecoration.ITALIC).color(TextColor.color(160, 160, 160)));
								playersWithTethers.add(player);
								break;
							}
						}
					}

					if (!soaked) {
						for (Player p : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
							PlayerUtils.killPlayer(p, mBoss, ABILITY_NAME);
						}
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

	private void attachTether(Location loc, Player p) {

		p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.75f, 1f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			int mTetherTimer = mTetherBreakTime;
			final TextDisplay mTextDisplay = loc.getWorld().spawn(loc.clone().add(0, 2.5, 0), TextDisplay.class);
			final int mMaxTickMarks = 60;

			@Override
			public void run() {

				if (!HexfallUtils.playerInBoss(p)) {
					this.cancel();
					return;
				}

				if (mT == 0) {
					mTextDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
					mTextDisplay.setBillboard(Display.Billboard.CENTER);
					mTextDisplay.addScoreboardTag("HexfallDisplay");
				}

				Location yAdjust = p.getLocation().clone();
				yAdjust.setY(loc.getY());
				double tetherLength = loc.distance(yAdjust);
				if (tetherLength > (mTetherBreakThreshold + 3)) {
					mTetherTimer -= 10;
				} else if (tetherLength > mTetherBreakThreshold) {
					mTetherTimer--;
				}

				float progress = Math.min(1, Math.max(0, (float) tetherLength / mTetherBreakThreshold));

				Component indicatorName = Component.empty().append(Component.text(ABILITY_NAME + "\n", Style.style(NamedTextColor.DARK_RED, TextDecoration.BOLD)))
					.append(Component.text("[", NamedTextColor.WHITE))
					.append(Component.text("|".repeat((int) (mMaxTickMarks * (1 - progress))), NamedTextColor.GREEN))
					.append(Component.text("|".repeat((int) (mMaxTickMarks * progress)), NamedTextColor.RED))
					.append(Component.text("]\n", NamedTextColor.WHITE))
					.append(Component.text(CAST_ABILITY_NAME + " ", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)))
					.append(Component.text("[ ", NamedTextColor.WHITE))
					.append(Component.text(String.format("%.02f", (float) (mMaxTetherDuration - mT) / 20) + "s", Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)))
					.append(Component.text(" ]", NamedTextColor.WHITE));
				mTextDisplay.text(indicatorName);

				if (mT % 2 == 0) {
					if (tetherLength > (mTetherBreakThreshold + 3)) {
						new PPLine(Particle.REDSTONE, loc, p.getEyeLocation().add(0, -1, 0)).data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f)).countPerMeter(2).spawnAsBoss();
					} else if (tetherLength > mTetherBreakThreshold) {
						new PPLine(Particle.BLOCK_CRACK, loc, p.getEyeLocation().add(0, -1, 0)).data(Material.RED_WOOL.createBlockData()).countPerMeter(2).spawnAsBoss();
					} else {
						new PPLine(Particle.SPELL_WITCH, loc, p.getEyeLocation().add(0, -1, 0)).countPerMeter(2).spawnAsBoss();
					}
				}

				if (mTetherTimer <= 0) {
					this.cancel();

					BukkitRunnable tetherResolveRunnable = new BukkitRunnable() {
						int mT = 0;
						final int mAnimationTime = 30;
						final double mMaxOuterRadius = 25;
						final double mMaxInnerRadius = 20;
						double mOuterRad = mRadius;
						double mInnerRad = 0;

						@Override
						public void run() {
							if (mT % 2 == 0) {
								new PPCircle(Particle.BLOCK_CRACK, p.getLocation(), mInnerRad).data(Material.CHORUS_PLANT.createBlockData()).count(20).spawnAsBoss();
								for (double rad = mInnerRad; rad < mOuterRad; rad += 0.5) {
									new PPCircle(Particle.BLOCK_CRACK, p.getLocation(), rad).data(Material.POPPY.createBlockData()).count(20).spawnAsBoss();
								}
							}
							double ratio = ((double) mT) / mAnimationTime;
							mInnerRad = mMaxInnerRadius * ratio;
							mOuterRad = mMaxOuterRadius * ratio;
							if (mT >= mAnimationTime) {
								this.cancel();
							}
							mT++;
						}
					};
					tetherResolveRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(tetherResolveRunnable);

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						if (mPlugin.mEffectManager.getActiveEffect(player, MortalVulnerability.class) != null) {
							PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME);
						}
					}

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 2f);
						mPlugin.mEffectManager.addEffect(player, MortalVulnerability.GENERIC_NAME, new MortalVulnerability(mDebuffDuration));
					}
				} else if (mT >= mMaxTetherDuration) {
					this.cancel();

					new PPExplosion(Particle.EXPLOSION_HUGE, loc.clone().add(0, 2, 0))
						.delta(2)
						.count(50)
						.spawnAsBoss();

					p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

					for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, mDamage, null, false, true, CAST_ABILITY_NAME);
					}
				}
				mT++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				mTextDisplay.remove();
				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}
}
