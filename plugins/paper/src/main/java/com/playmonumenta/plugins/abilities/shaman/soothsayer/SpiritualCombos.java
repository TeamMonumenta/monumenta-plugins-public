package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.SpiritualCombosCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class SpiritualCombos extends Ability implements AbilityWithChargesOrStacks {
	private static final int CRYSTAL_STACK_THRESHOLD = 12;
	private static final int CRYSTAL_DAMAGE_1 = 10;
	private static final int CRYSTAL_DAMAGE_2 = 14;
	private static final int CRYSTAL_RANGE = 12;
	private static final int SHOT_COUNT_1 = 16;
	private static final int SHOT_COUNT_2 = 20;
	private static final int STACK_DECAY_TIME_1 = 8 * 20;
	private static final int STACK_DECAY_TIME_2 = 12 * 20;
	private static final int SHOT_DELAY = 12;
	private static final double SPEED_PERCENT = 0.1;
	private static final String SPEED_EFFECT_SOURCE = "Spiritual Combos Speed";

	public static String CHARM_CRYSTAL_DAMAGE = "Spiritual Combos Damage";
	public static String CHARM_CRYSTAL_RANGE = "Spiritual Combos Range";
	public static String CHARM_CRYSTAL_STACK_THRESHOLD = "Spiritual Combos Stack Threshold";
	public static String CHARM_STACK_DECAY_TIME = "Spiritual Combos Stack Decay Time";
	public static String CHARM_SHOT_COUNT = "Spiritual Combos Shot Count";
	public static String CHARM_SHOT_DELAY = "Spiritual Combos Shot Delay";
	public static String CHARM_SPEED = "Spiritual Combos Speed Amplifier";

	public static final AbilityInfo<SpiritualCombos> INFO =
		new AbilityInfo<>(SpiritualCombos.class, "Spiritual Combos", SpiritualCombos::new)
			.linkedSpell(ClassAbility.SPIRITUAL_COMBOS)
			.scoreboardId("SpiritualCombos")
			.shorthandName("SC")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Stack up spiritual crystals by killing mobs to deal damage to nearby enemies.")
			.displayItem(Material.AMETHYST_CLUSTER);

	private final double mCrystalDamage;
	private final double mCrystalRange;
	private final int mCrystalStackThreshold;
	private final int mStackDecayTime;
	private final int mTotalShots;
	private final int mShotDelay;
	private final double mSpeed;
	private int mDecayTimer = 0;
	private final SpiritualCombosCS mCosmetic;

	private int mCrystalStacks = 0;
	private int mCrystalShots = 0; // Shots == visual stacks
	private boolean mSpendingStacks = false;
	protected @Nullable BukkitTask mSystemTask = null;

	public SpiritualCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCrystalDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_CRYSTAL_DAMAGE, isLevelTwo() ? CRYSTAL_DAMAGE_2 : CRYSTAL_DAMAGE_1);
		mCrystalRange = CharmManager.getRadius(player, CHARM_CRYSTAL_RANGE, CRYSTAL_RANGE);
		mCrystalStackThreshold = CRYSTAL_STACK_THRESHOLD + (int) CharmManager.getLevel(player, CHARM_CRYSTAL_STACK_THRESHOLD);
		mStackDecayTime = CharmManager.getDuration(mPlayer, CHARM_STACK_DECAY_TIME, isLevelTwo() ? STACK_DECAY_TIME_2 : STACK_DECAY_TIME_1);
		mTotalShots = (isLevelTwo() ? SHOT_COUNT_2 : SHOT_COUNT_1) + (int) CharmManager.getLevel(player, CHARM_SHOT_COUNT);
		mShotDelay = CharmManager.getDuration(player, CHARM_SHOT_DELAY, SHOT_DELAY);
		mSpeed = SPEED_PERCENT + CharmManager.getLevelPercentDecimal(player, CHARM_SPEED);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SpiritualCombosCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (enemy.isDead() && event.getAbility() != mInfo.getLinkedSpell() && MetadataUtils.checkOnceThisTick(mPlugin, enemy, "SpiritualCombosStack")) {
					mCrystalStacks++;
					mDecayTimer = 0;
					updateNotify();
				}
			}
		}.runTaskLater(mPlugin, 1);
		return false;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mSystemTask == null) {
			cancelOnDeath(mSystemTask = new BukkitRunnable() {
				double mRotationAngle = 0;
				final List<PPPeriodic> mParticles = new ArrayList<>();

				{
					for (int i = 0; i < 3; i++) {
						mCosmetic.spiritualCombosSwirl(mParticles, mPlayer);
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
						mDecayTimer = 0;
						mCrystalStacks = 0;
						mCrystalShots = mTotalShots;
						mSpendingStacks = true;
						updateNotify();
						fireStack();
						mCosmetic.spiritualCombosTrigger(mPlayer);
					}

					if (mT % 20 == 0 && mCrystalStacks > 0) {
						mT = 0;
						if (!mSpendingStacks) {
							mDecayTimer += 20;
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
					if (mCrystalShots > 0) {
						for (int i = 0; i < 3; i++) {
							mCosmetic.spiritualCombosActiveSwirl(mParticles, mPlayer, mRotationAngle, mSpendingStacks, i);
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

	private void fireStack() {
		boolean hitSomething = false;
		List<LivingEntity> targets = new Hitbox.SphereHitbox(mPlayer.getLocation(), mCrystalRange).getHitMobs();
		targets.removeIf(target -> !LocationUtils.hasLineOfSight(mPlayer, target));
		Collections.shuffle(targets);
		for (LivingEntity target : targets) {
			if (!DamageUtils.isImmuneToDamage(target, DamageEvent.DamageType.MAGIC)) {
				hitSomething = true;
				mCosmetic.spiritualCombosHit(mPlayer, target);
				DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC, mCrystalDamage, ClassAbility.SPIRITUAL_COMBOS, true, false);
				updateNotify();
				break;
			}
		}
		if (hitSomething || mCrystalShots != mTotalShots) {
			if (mCrystalShots == mTotalShots) {
				// must be first shot, grant speed effect
				mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_SOURCE, new PercentSpeed(mTotalShots * mShotDelay, mSpeed, SPEED_EFFECT_SOURCE).displaysTime(false).deleteOnAbilityUpdate(true));
			}
			mCrystalShots--;
		}
		if (mCrystalShots <= 0) {
			mCosmetic.spiritualCombosExpire(mPlayer, mPlugin);
			updateNotify();
			mSpendingStacks = false;
		}
	}

	public void updateNotify() {
		showChargesMessage();
		ClientModHandler.updateAbility(mPlayer, ClassAbility.SPIRITUAL_COMBOS);
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

	private static Description<SpiritualCombos> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Gain a stack of spiritual crystals each time you kill a mob not with this ability. When you get ")
			.add(a -> a.mCrystalStackThreshold, CRYSTAL_STACK_THRESHOLD, true)
			.add(" stacks, they reset, and the crystals lash out at mobs within ")
			.add(a -> a.mCrystalRange, CRYSTAL_RANGE)
			.add(" blocks. Every ")
			.addDuration(a -> a.mShotDelay, SHOT_DELAY, true)
			.add(" seconds, a random mob is shot, dealing ")
			.add(a -> a.mCrystalDamage, CRYSTAL_DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage, up to a total of ")
			.add(a -> a.mTotalShots, SHOT_COUNT_1, false, Ability::isLevelOne)
			.add(" shots. The first shot will wait to be fired if there are no mobs in range, but subsequent shots will do nothing if there are no mobs to shoot. Stacks decay at a rate of 1 stack per ")
			.addDuration(a -> a.mStackDecayTime, STACK_DECAY_TIME_1, false, Ability::isLevelOne)
			.add(" seconds. Additionally, gain ")
			.addPercent(a -> a.mSpeed, SPEED_PERCENT)
			.add(" speed while spiritual crystals are firing.");
	}

	private static Description<SpiritualCombos> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mCrystalDamage, CRYSTAL_DAMAGE_2, false, Ability::isLevelTwo)
			.add(". The number of shots is increased to ")
			.add(a -> a.mTotalShots, SHOT_COUNT_2, false, Ability::isLevelTwo)
			.add(". Stack decay rate is reduced to 1 stack per ")
			.addDuration(a -> a.mStackDecayTime, STACK_DECAY_TIME_2, false, Ability::isLevelTwo)
			.add(" seconds.");
	}
}
