package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.HashSet;
import java.util.Set;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.abilities.SpellPower;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class ElementalSpiritIce extends Ability {
	@NotNull public static final Spells SPELL = Spells.ELEMENTAL_SPIRIT_ICE;

	public static final int DAMAGE_1 = 4;
	public static final int DAMAGE_2 = 6;
	public static final int SIZE = 3;
	public static final double BOW_MULTIPLIER_1 = 0.1;
	public static final int BOW_PERCENTAGE_1 = (int)(BOW_MULTIPLIER_1 * 100);
	public static final double BOW_MULTIPLIER_2 = 0.15;
	public static final int BOW_PERCENTAGE_2 = (int)(BOW_MULTIPLIER_2 * 100);
	public static final int PULSE_INTERVAL = Constants.TICKS_PER_SECOND;
	public static final int PULSES = 3;
	public static final int COOLDOWN_TICKS = ElementalSpiritFire.COOLDOWN_TICKS;

	private final int mLevelDamage;
	private final double mLevelBowMultiplier;
	private final @NotNull Set<LivingEntity> mEnemiesAffected = new HashSet<>();

	private @Nullable ElementalArrows mElementalArrows;
	private @Nullable BukkitTask mPlayerParticlesGenerator;
	private @Nullable BukkitTask mEnemiesAffectedProcessor;
	private @Nullable BukkitTask mSpiritPulser;

	public ElementalSpiritIce(Plugin plugin, Player player) {
		/* NOTE
		 * Display name is null so this variant will be ignored by the tesseract.
		 * This variant also does not have a description
		 */
		super(plugin, player, null);
		mInfo.mLinkedSpell = SPELL;

		mInfo.mScoreboardId = "ElementalSpirit";
		mInfo.mCooldown = COOLDOWN_TICKS;

		boolean isUpgraded = getAbilityScore() == 2;
		mLevelDamage = isUpgraded ? DAMAGE_2 : DAMAGE_1;
		mLevelBowMultiplier = isUpgraded ? BOW_MULTIPLIER_2 : BOW_MULTIPLIER_1;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mElementalArrows = AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalArrows.class);
			});
		}
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (
			event.getMagicType() == MagicType.ICE
			&& event.getSpell() != null && !event.getSpell().equals(SPELL)
		) {
			mEnemiesAffected.add(event.getDamaged());
			if (mEnemiesAffectedProcessor == null) {
				mEnemiesAffectedProcessor = new BukkitRunnable() {
					@Override
					public void run() {
						mEnemiesAffectedProcessor = null;

						@NotNull Location playerLocation = mPlayer.getLocation();
						@Nullable LivingEntity closestEnemy = null;
						double closestDistanceSquared = 7050;

						for (LivingEntity enemy : mEnemiesAffected) {
							if (enemy.isValid()) {
								double distanceSquared = playerLocation.distanceSquared(enemy.getLocation());
								if (distanceSquared < closestDistanceSquared) {
									closestDistanceSquared = distanceSquared;
									closestEnemy = enemy;
								}
							}
						}
						mEnemiesAffected.clear();

						if (closestEnemy != null) {
							putOnCooldown();

							@NotNull Location centre = LocationUtils.getHalfHeightLocation(closestEnemy);
							float spellDamage = SpellPower.getSpellDamage(mPlayer, mLevelDamage);
							@NotNull World world = mPlayer.getWorld();
							mSpiritPulser = new BukkitRunnable() {
								int mPulses = 1; // The current pulse for this run

								@Override
								public void run() {
									// Damage actions
									for (LivingEntity mob : EntityUtils.getNearbyMobs(centre, SIZE)) {
										float finalDamage = spellDamage;
										if (
											event.getSpell().equals(Spells.ELEMENTAL_ARROWS)
											&& mElementalArrows != null
										) {
											finalDamage += mElementalArrows.getLastDamage() * mLevelBowMultiplier;
										}

										EntityUtils.damageEntity(mPlugin, mob, finalDamage, mPlayer, MagicType.ICE, true, SPELL, true, true, true);
										mob.setVelocity(new Vector()); // Wipe velocity, extreme local climate
									}

									// Ice spirit effects
									@NotNull PartialParticle partialParticle = new PartialParticle(
										Particle.SNOWBALL,
										centre,
										150,
										PartialParticle.getWidthDelta(SIZE),
										0.1
									).spawnAsPlayer(mPlayer);
									//TODO falling dust
									partialParticle.mParticle = Particle.FIREWORKS_SPARK;
									partialParticle.mCount = 30;
									partialParticle.spawnAsPlayer(mPlayer);
									world.playSound(centre, Sound.ENTITY_TURTLE_HURT_BABY, 1, 0.2f);
									world.playSound(centre, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.05f);

									if (mPulses >= PULSES) {
										this.cancel();
									} else {
										mPulses++;
									}
								}
							}.runTaskTimer(mPlugin, 0, PULSE_INTERVAL);
						}
					}
				}.runTask(mPlugin);
			}
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayerParticlesGenerator == null) {
			mPlayerParticlesGenerator = new BukkitRunnable() {
				double mVerticalAngle = 0;
				double mRotationAngle = 180;

				@Override
				public void run() {
					if (
						isTimerActive(SPELL)
						|| !mPlayer.isValid() // Ensure player is not dead, is still online?
					) {
						this.cancel();
						mPlayerParticlesGenerator = null;
					}

					mVerticalAngle -= 5.5;
					mRotationAngle -= 10;
					mVerticalAngle %= -360;
					mRotationAngle %= -360;

					new PartialParticle(
						Particle.SNOWBALL,
						LocationUtils
							.getHalfHeightLocation(mPlayer)
							.add(
								FastUtils.cos(Math.toRadians(mRotationAngle)),
								FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.5,
								FastUtils.sin(Math.toRadians(mRotationAngle))
							),
						3,
						0,
						0
					).spawnAsPlayer(mPlayer, true);
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public void invalidate() {
		if (mPlayerParticlesGenerator != null) {
			mPlayerParticlesGenerator.cancel();
		}

		if (mEnemiesAffectedProcessor != null) {
			mEnemiesAffectedProcessor.cancel();
		}
		if (mSpiritPulser != null) {
			mSpiritPulser.cancel();
		}
	}
}