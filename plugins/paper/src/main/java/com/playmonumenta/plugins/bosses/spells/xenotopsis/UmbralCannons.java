package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UmbralCannons extends Spell {
	// the duration and windup duration of the attack, in ticks
	private static final int DURATION = 20 * 10;
	private static final int WINDUP_DURATION = 20 * 2;

	// the base cooldown of the attack, which is modified by the cooldown ticks provided by the boss
	private static final int BASE_COOLDOWN = 20 * 10;

	// the delay between each group of cannon shots, in ticks
	private static final int DELAY_BETWEEN_SHOTS = 8;

	// the amount of cannon shots per group of cannon shots
	private static final int SHOTS_PER_GROUP = 2;

	// the amount of time between the telegraph of each shot and the time it hits, in ticks
	private static final int SHOT_WINDUP_DURATION = 20 * 2;

	// the radius of each cannon shot
	private static final int SHOT_RADIUS = 4;

	// the options for each falling block's texture in the cannon shot effect
	private static final ArrayList<Material> SHOT_BLOCK_OPTIONS = new ArrayList<>(Arrays.asList(Material.BLACK_WOOL, Material.BLACK_CONCRETE, Material.BLACK_CONCRETE_POWDER));

	// the attack and death damage of each cannon shot
	private static final int ATTACK_DAMAGE = 135;
	private static final int DEATH_DAMAGE = 24;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;
	private final int mCooldownTicks;

	private final ChargeUpManager mChargeUp;

	private final ArrayList<Location> mCurrentCannonLocations = new ArrayList<>();

	public UmbralCannons(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis, int cooldownTicks) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldownTicks = cooldownTicks;
		mXenotopsis = xenotopsis;

		mChargeUp = new ChargeUpManager(mBoss, DURATION, Component.text(""), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, 50);
	}

	@Override
	public boolean canRun() {
		return mXenotopsis.canRunSpell(this);
	}

	@Override
	public void run() {
		mChargeUp.reset();
		mChargeUp.setTitle(Component.text("Readying the ", NamedTextColor.GREEN).append(Component.text("Umbral Cannons", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));
		mChargeUp.update();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// play a sound at the start
				if (mTicks == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 4f, 0.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.5f);

					mXenotopsis.sendDialogueMessage("HOIST THE SUNKEN CANNONS FROM THE DEEP, TAINTED BY THE SCOURGE OF DEATH, AND PREPARE TO FIRE");
				}

				if (mTicks == WINDUP_DURATION / 2) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.7f, 0.7f);
				}

				if (mTicks == WINDUP_DURATION) {
					mXenotopsis.sendDialogueMessage("REND THE SKIES WITH YER BLASTS OF SHADE, LEAVE NO SOUL UNSCATHED");
				}

				// set the progress bar to the appropriate amount
				if (mTicks < WINDUP_DURATION) {
					mChargeUp.setProgress((double) mTicks / WINDUP_DURATION);
				} else if (mTicks == WINDUP_DURATION) {
					mChargeUp.setTitle(Component.text("Firing the ", NamedTextColor.GREEN).append(Component.text("Umbral Cannons", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));
				} else {
					mChargeUp.setProgress(1.0 - (double) (mTicks - WINDUP_DURATION) / (DURATION - WINDUP_DURATION + SHOT_WINDUP_DURATION + 5));
				}

				// run while the attack is active
				if (mTicks > WINDUP_DURATION && mTicks < DURATION) {
					if ((mTicks - WINDUP_DURATION) % DELAY_BETWEEN_SHOTS == 0) {
						// create a group of cannon shots
						for (int i = 0; i < SHOTS_PER_GROUP; i++) {
							createCannonShot();
						}
					}
				}

				mTicks++;
				if (mTicks == DURATION + SHOT_WINDUP_DURATION + 5 || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	// runnable for each independent cannon shot
	private void createCannonShot() {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Nullable Location mLocation = null;

			@Override
			public void run() {
				// when started
				if (mTicks == 0) {
					// try 20 times to find a valid location for the cannon shot
					boolean foundLocation = false;
					for (int i = 0; i < 20; i++) {
						Location tryLocation = mXenotopsis.mSpawnLoc.clone().add(FastUtils.randomDoubleInRange(-20, 20), 0, FastUtils.randomDoubleInRange(-20, 20)).toCenterLocation();
						tryLocation.setY(Math.floor(tryLocation.getY()));
						boolean nearOtherShots = false;
						for (Location location : mCurrentCannonLocations) {
							if (location.distance(tryLocation) < SHOT_RADIUS * 2) {
								nearOtherShots = true;
								break;
							}
						}

						if (!nearOtherShots && !tryLocation.getBlock().getType().isSolid()) {
							foundLocation = true;
							mLocation = tryLocation;
							mCurrentCannonLocations.add(mLocation);

							mWorld.playSound(mLocation, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 2f, 0.5f);

							break;
						}
					}
					if (!foundLocation) {
						this.cancel();
						return;
					}
				}

				// ring and shot telegraph particle effect
				if (mTicks < SHOT_WINDUP_DURATION && mTicks % 2 == 0 && mLocation != null) {
					new PPCircle(Particle.REDSTONE, mLocation, SHOT_RADIUS)
						.ringMode(true)
						.count(30)
						.data(new Particle.DustOptions(Color.fromRGB(19, 19, 28), 1.8f))
						.delta(0.08)
						.spawnAsBoss();
					new PPCircle(Particle.END_ROD, mLocation, SHOT_RADIUS)
						.ringMode(true)
						.count(12)
						.delta(0.08)
						.spawnAsBoss();

					double height = 10 * (1 - ((double) mTicks / SHOT_WINDUP_DURATION));
					new PartialParticle(Particle.REDSTONE, mLocation.clone().add(0, height, 0))
						.count(2)
						.extra(0.2)
						.data(new Particle.DustOptions(Color.fromRGB(19, 19, 28), 2.3f))
						.delta(0.02, 0.02, 0.02)
						.spawnAsBoss();
				}

				// damage and effects
				if (mTicks == SHOT_WINDUP_DURATION && mLocation != null) {
					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mLocation, 3, SHOT_RADIUS);
					for (Player player : hitbox.getHitPlayers(true)) {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.BLAST, mXenotopsis.scaleDamage(ATTACK_DAMAGE), "Umbral Cannons", mLocation, Xenotopsis.SHIELD_STUN_TIME * 4);
						mXenotopsis.changePlayerDeathValue(player, DEATH_DAMAGE, false);
						//MovementUtils.knockAwayRealistic(mLocation, player, 0.3f, 0.35f, true);
					}

					// sound effect
					mWorld.playSound(mLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.8f);

					// particle effect
					new PPExplosion(Particle.SQUID_INK, mLocation.clone().add(0, 0.2, 0))
						.count(80)
						.extra(0.5)
						.speed(0.5)
						.spawnAsBoss();

					// visual effect for the cannon shot
					mActiveTasks.add(DisplayEntityUtils.groundBlockQuake(mLocation, SHOT_RADIUS, SHOT_BLOCK_OPTIONS, new Display.Brightness(15, 15)));
				}

				mTicks++;
				if (mTicks > SHOT_WINDUP_DURATION || mBoss.isDead()) {
					this.cancel();
					mCurrentCannonLocations.remove(mLocation);
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return BASE_COOLDOWN + mCooldownTicks;
	}
}
