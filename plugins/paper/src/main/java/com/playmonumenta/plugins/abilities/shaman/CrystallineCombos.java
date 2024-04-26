package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class CrystallineCombos extends Ability implements AbilityWithChargesOrStacks {
	public static int CRYSTAL_STACK_THRESHOLD = 12;
	public static int CRYSTAL_DAMAGE_1 = 7;
	public static int CRYSTAL_DAMAGE_2 = 10;
	public static int CRYSTAL_RANGE = 12;
	public static int SHOT_COUNT_1 = 16;
	public static int SHOT_COUNT_2 = 20;
	public static double SPEED_PER_STACK = 0.04;
	public static double MAX_SPEED = .9;
	public static int SPEED_DURATION = 6 * 20;
	public static int COOLDOWN = 4 * 20;
	public static int STACK_DECAY_TIME_1 = 8;
	public static int STACK_DECAY_TIME_2 = 12;
	public static int STACK_DECAY_TIME_ENHANCE = 16;
	public static int SHOT_DELAY = 12;

	public static String CHARM_CRYSTAL_DAMAGE = "Crystalline Combos Damage";
	public static String CHARM_CRYSTAL_RANGE = "Crystalline Combos Range";
	public static String CHARM_CRYSTAL_STACK_THRESHOLD = "Crystalline Combos Stack Threshold";
	public static String CHARM_CRYSTAL_TOTEM_COUNT_PERCENTAGE = "Crystalline Combos Totem Count Percentage";
	public static String CHARM_SPEED_PER_STACK = "Crystalline Combos Speed per Stack";
	public static String CHARM_MAX_SPEED = "Crystalline Combos Max Speed";
	public static String CHARM_SPEED_DURATION = "Crystalline Combos Speed Duration";
	public static String CHARM_STACK_DECAY_TIME = "Crystalline Combos Stack Decay Time";
	public static String CHARM_SHOT_COUNT = "Crystalline Combos Shot Count";
	public static String CHARM_SHOT_DELAY = "Crystalline Combos Shot Delay";

	public static final AbilityInfo<CrystallineCombos> INFO =
		new AbilityInfo<>(CrystallineCombos.class, "Crystalline Combos", CrystallineCombos::new)
			.linkedSpell(ClassAbility.CRYSTALLINE_COMBOS)
			.scoreboardId("CrystalCombos")
			.shorthandName("CC")
			.descriptions(
				String.format("Every kill not caused by this skill grants you 1 stack of crystals, and after %s stacks " +
					"the crystals lash out at any mobs within %s blocks of you at a rate of 1 shot per %ss, " +
					"dealing %s damage to each for a total " +
					"of %s shots. Stacks decay at a rate of 1 stack per %ss.",
					CRYSTAL_STACK_THRESHOLD, CRYSTAL_RANGE,
					StringUtils.ticksToSeconds(SHOT_DELAY), CRYSTAL_DAMAGE_1,
					SHOT_COUNT_1, STACK_DECAY_TIME_1),
				String.format("Damage is increased to %s, number of shots increased to %s, " +
					"stack decay rate is reduced to 1 stack per %ss.",
					CRYSTAL_DAMAGE_2, SHOT_COUNT_2, STACK_DECAY_TIME_2),
				String.format("Swap hands while sneaking and holding a projectile weapon " +
					"will use all existing crystal stacks to provide %s%% speed per stack (max %s%%) to the Shaman " +
					"for %ss. Decay rate reduced to 1 stack per %ss. Can now target outside of line of sight. Cooldown %ss.",
					StringUtils.multiplierToPercentage(SPEED_PER_STACK),
					StringUtils.multiplierToPercentage(MAX_SPEED),
					StringUtils.ticksToSeconds(SPEED_DURATION),
					STACK_DECAY_TIME_ENHANCE,
					StringUtils.ticksToSeconds(COOLDOWN)))
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CrystallineCombos::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.simpleDescription("Stack up crystals by killing mobs to deal damage to nearby enemies.")
			.displayItem(Material.AMETHYST_CLUSTER);

	private final double mCrystalDamage;
	private final double mCrystalRange;
	private final int mCrystalStackThreshold;
	private final double mSpeedPerStack;
	private final double mMaxSpeed;
	private final int mSpeedDuration;
	private final int mStackDecayTime;
	private final int mTotalShots;
	private final int mShotDelay;
	private int mDecayTimer = 0;

	private int mCrystalStacks = 0;
	private int mCrystalShots = 0; //Shots == visual stacks
	private boolean mSpendingStacks = false;
	protected @Nullable BukkitTask mSystemTask = null;
	private static final Particle.DustOptions PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 200), 0.8f);
	private static final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES =
		List.of(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.4,
			(Location loc) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0.1, 0.1, 0.1, PARTICLE_COLOR)
				.spawnAsOtherPlayerActive()));
	private static final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES_BREAK =
		List.of(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.4,
			(Location loc) -> new PartialParticle(Particle.CRIT_MAGIC, loc, 1)
				.spawnAsOtherPlayerActive()));

	public CrystallineCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mCrystalDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_CRYSTAL_DAMAGE,
			isLevelTwo() ? CRYSTAL_DAMAGE_2 : CRYSTAL_DAMAGE_1);
		mCrystalRange = CharmManager.getRadius(player, CHARM_CRYSTAL_RANGE, CRYSTAL_RANGE);
		mCrystalStackThreshold = (int) (CRYSTAL_STACK_THRESHOLD
			+ CharmManager.getLevel(player, CHARM_CRYSTAL_STACK_THRESHOLD));
		mSpeedPerStack = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPEED_PER_STACK, SPEED_PER_STACK);
		mMaxSpeed = CharmManager.calculateFlatAndPercentValue(player, CHARM_MAX_SPEED, MAX_SPEED);
		mSpeedDuration = CharmManager.getDuration(player, CHARM_SPEED_DURATION, SPEED_DURATION);
		mStackDecayTime = (int) ((isEnhanced() ? STACK_DECAY_TIME_ENHANCE :
			isLevelTwo() ? STACK_DECAY_TIME_2 : STACK_DECAY_TIME_1) +
					CharmManager.getLevel(player, CHARM_STACK_DECAY_TIME));
		mTotalShots = (int) ((isLevelTwo() ? SHOT_COUNT_2 : SHOT_COUNT_1) + CharmManager.getLevel(player, CHARM_SHOT_COUNT));
		mShotDelay = CharmManager.getCooldown(player, CHARM_SHOT_DELAY, SHOT_DELAY);
	}

	public boolean cast() {
		if (!isEnhanced() || mSpendingStacks
			|| mCrystalStacks == 0 || isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		int savedStacks = mCrystalStacks;
		mCrystalStacks = 0;
		updateNotify();
		mPlugin.mEffectManager.addEffect(mPlayer, "CrystalCombosSpeed", new PercentSpeed(mSpeedDuration,
			Math.min(mMaxSpeed, savedStacks * mSpeedPerStack), "CrystalCombosSpeed"));
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (event.getSource() != null
					&& event.getSource().getUniqueId().equals(mPlayer.getUniqueId())
					&& enemy.isDead()
					&& event.getAbility() != ClassAbility.CRYSTALLINE_COMBOS) {

					mCrystalStacks++;
					mDecayTimer = 0;
					updateNotify();
				}
			}
		}.runTaskLater(mPlugin, 3);
		return false;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mSystemTask == null) {
			cancelOnDeath(mSystemTask = new BukkitRunnable() {
				final int mGapBetweenParticleColors = 200 / mCrystalStackThreshold;
				double mRotationAngle = 0;
				final List<PPPeriodic> mParticles = new ArrayList<>();

				{
					for (int i = 0; i < 3; i++) {
						mParticles.add(new PPPeriodic(Particle.REDSTONE,
							mPlayer.getLocation())
							.extra(0.1)
							.data(new Particle.DustOptions(Color.fromRGB(100, 100, 255), 0.8f)));
					}
				}

				int mT = 0;
				int mTicksForShooter = 0;

				@Override
				public void run() {
					mT++;

					if (mSpendingStacks) {
						mTicksForShooter++;
						if (mTicksForShooter >= mShotDelay) {
							fireStack();
							mTicksForShooter = 0;
						}
					} else {
						mCrystalShots = mCrystalStacks;
					}
					if (mCrystalStacks >= mCrystalStackThreshold) {
						mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,
							 SoundCategory.PLAYERS, 2, 1.5f);
						mDecayTimer = 0;
						mCrystalStacks = 0;
						mCrystalShots = mTotalShots;
						mSpendingStacks = true;
						updateNotify();
						fireStack();
					}

					if (mT % 20 == 0 && mCrystalStacks > 0) {
						mT = 0;
						if (!mSpendingStacks) {
							mDecayTimer++;
							if (mDecayTimer >= mStackDecayTime) {
								mCrystalStacks--;
								mDecayTimer = 0;
								updateNotify();
							}
						}
					}


					// Particles runner
					if (!mPlayer.isOnline()
					    || mPlayer.isDead()
						|| PremiumVanishIntegration.isInvisibleOrSpectator(mPlayer)) {
						this.cancel();
						mSystemTask = null;
					}
					mRotationAngle += 10;
					mRotationAngle %= 360;
					int colorAdjust = mCrystalShots * mGapBetweenParticleColors;
					if (colorAdjust < 0 || colorAdjust > 200) {
						colorAdjust = 0;
					}
					if (mCrystalShots > 0) {
						for (int i = 0; i < 3; i++) {
							PPPeriodic particle = mParticles.get(i);
							particle.location(
									LocationUtils.getHalfHeightLocation(mPlayer)
										.add(
											FastUtils.cos(Math.toRadians(mRotationAngle + (i * 120))),
											-0.1,
											FastUtils.sin(Math.toRadians(mRotationAngle + (i * 120)))
										)
								).data(new Particle.DustOptions(
									Color.fromRGB(mSpendingStacks ? 255 : colorAdjust,
										mSpendingStacks ? 150 : colorAdjust,
										mSpendingStacks ? 150 : 255), 0.8f))
								.spawnAsPlayerPassive(mPlayer);
						}
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}
	}

	@Override
	public int getCharges() {
		return mCrystalStacks;
	}

	@Override
	public int getMaxCharges() {
		return mCrystalStackThreshold;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	private void fireStack() {
		boolean hitSomething = false;
		List<LivingEntity> targets = EntityUtils.getNearbyMobsInSphere(mPlayer.getLocation(),
			mCrystalRange, null);
		if (!isEnhanced()) {
			targets.removeIf(target -> !LocationUtils.hasLineOfSight(mPlayer, target));
		}
		Collections.shuffle(targets);
		for (LivingEntity target : targets) {
			if (!DamageUtils.isImmuneToDamage(target, DamageEvent.DamageType.MAGIC)) {
				hitSomething = true;
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_HIT,
					SoundCategory.PLAYERS, 1.2f, 1.0f);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK,
					SoundCategory.PLAYERS, 2f, 1.4f);
				DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC, mCrystalDamage,
					ClassAbility.CRYSTALLINE_COMBOS, true, false);
				new PPLine(Particle.REDSTONE,
					LocationUtils.getHalfHeightLocation(mPlayer).add(0, -0.1, 0),
					LocationUtils.getHalfHeightLocation(target)).data(PARTICLE_COLOR)
					.delta(0.1).extra(0.1).countPerMeter(10)
					.spawnAsPlayerActive(mPlayer);
				updateNotify();
				break;
			}
		}
		if (hitSomething || mCrystalShots != mTotalShots) {
			mCrystalShots--;
		}
		if (mCrystalShots <= 0) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.0f);
			updateNotify();
			mSpendingStacks = false;
			ParticleUtils.explodingRingEffect(mPlugin, LocationUtils.getHalfHeightLocation(mPlayer), 2.5,
				1, 5, PARTICLES);
			ParticleUtils.explodingRingEffect(mPlugin, LocationUtils.getHalfHeightLocation(mPlayer), 2.5,
				1, 5, PARTICLES_BREAK);
		}
	}

	public void updateNotify() {
		showChargesMessage();
		ClientModHandler.updateAbility(mPlayer, ClassAbility.CRYSTALLINE_COMBOS);
	}

	@Override
	public void invalidate() {
		if (mSystemTask != null) {
			mSystemTask.cancel();
			mSystemTask = null;
		}
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}
}
