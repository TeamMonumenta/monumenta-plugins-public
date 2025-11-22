package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.ChoirBellsCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public class ChoirBells extends Ability implements AbilityWithDuration {

	private static final int DURATION = 8 * 20;
	private static final double WEAKEN_EFFECT = 0.2;
	private static final double VULNERABILITY_EFFECT = 0.3;
	private static final double VULNERABILITY_EFFECT_2 = 0.2;
	private static final double SLOWNESS_AMPLIFIER = 0.1;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.1;
	private static final int COOLDOWN = 16 * 20;
	private static final int CHOIR_BELLS_RANGE = 10;
	private static final int DAMAGE = 8;
	private static final int DAMAGE_2 = 3;
	private static final int EXTRA_TOLLS = 2;
	private static final int TOLL_DELAY = 3 * 20;

	public static final String CHARM_DAMAGE = "Choir Bells Damage";
	public static final String CHARM_COOLDOWN = "Choir Bells Cooldown";
	public static final String CHARM_SLOW = "Choir Bells Slowness Amplifier";
	public static final String CHARM_VULN = "Choir Bells Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Choir Bells Weakness Amplifier";
	public static final String CHARM_RANGE = "Choir Bells Range";
	public static final String CHARM_DURATION = "Choir Bells Debuff Duration";
	public static final String CHARM_TOLLS = "Choir Bells Tolls";
	public static final String CHARM_TOLL_DELAY = "Choir Bells Toll Delay";

	public static final AbilityInfo<ChoirBells> INFO =
		new AbilityInfo<>(ChoirBells.class, "Choir Bells", ChoirBells::new)
			.linkedSpell(ClassAbility.CHOIR_BELLS)
			.scoreboardId("ChoirBells")
			.shorthandName("CB")
			.hotbarName("\uD83D\uDD14")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Taunt, slow, and apply vulnerability to nearby mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChoirBells::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.BELL);

	private final double mSlownessAmount;
	private final double mSlownessAmount2;
	private final double mWeakenEffect;
	private final double mVulnerabilityEffect;
	private final double mVulnerabilityEffect2;
	private final int mDuration;
	private final double mRange;
	private final double mDamage;
	private final double mTollDamage;
	private final int mExtraTolls;
	private final int mTollDelay;
	private final int mTollsDuration;

	private @Nullable BukkitRunnable mTollRunnable;
	private int mCurrentDuration = -1;

	private final ChoirBellsCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public ChoirBells(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlownessAmount = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + SLOWNESS_AMPLIFIER;
		mSlownessAmount2 = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + SLOWNESS_AMPLIFIER_2;
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + WEAKEN_EFFECT;
		mVulnerabilityEffect = CharmManager.getLevelPercentDecimal(player, CHARM_VULN) + VULNERABILITY_EFFECT;
		mVulnerabilityEffect2 = CharmManager.getLevelPercentDecimal(player, CHARM_VULN) + VULNERABILITY_EFFECT_2;
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, DURATION);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, CHOIR_BELLS_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
		mExtraTolls = EXTRA_TOLLS + (int) CharmManager.getLevel(player, CHARM_TOLLS);
		mTollDelay = CharmManager.getDuration(player, CHARM_TOLL_DELAY, TOLL_DELAY);
		mTollsDuration = mExtraTolls * mTollDelay;
		mTollDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChoirBellsCS());

		Bukkit.getScheduler().runTask(plugin, () ->
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location cosmeticLoc = mPlayer.getLocation();
		mCosmetic.bellsCastEffect(mPlayer, mRange, cosmeticLoc);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRange);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			mCosmetic.bellsApplyEffect(mPlayer, mob);
			EntityUtils.applySlow(mPlugin, mDuration, mSlownessAmount, mob);

			if (Crusade.enemyTriggersAbilities(mob)) {
				// Infusion
				EntityUtils.applyTaunt(mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, true);
				EntityUtils.applyVulnerability(mPlugin, mDuration, mVulnerabilityEffect, mob);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeakenEffect, mob);
			}
			Crusade.addCrusadeTag(mob, mCrusade);
		}
		putOnCooldown();

		if (isLevelTwo()) {
			if (mTollRunnable != null) {
				mTollRunnable.cancel();
			}
			mCurrentDuration = 1;
			ClientModHandler.updateAbility(mPlayer, this);
			mTollRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (!mPlayer.isOnline() || mPlayer.isDead()) {
						this.cancel();
						return;
					}

					if (mCurrentDuration % mTollDelay == 0) {
						mCosmetic.bellsCastEffect(mPlayer, mRange, cosmeticLoc);
						for (LivingEntity mob : hitbox.getHitMobs()) {
							mCosmetic.bellsApplyEffect(mPlayer, mob);
							// Tolls debuff always last the length of a pulse
							EntityUtils.applySlow(mPlugin, mTollDelay, mSlownessAmount2, mob);
							if (Crusade.enemyTriggersAbilities(mob)) {
								EntityUtils.applyTaunt(mob, mPlayer);
								DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mTollDamage, mInfo.getLinkedSpell(), true, true);
								EntityUtils.applyVulnerability(mPlugin, mTollDelay, mVulnerabilityEffect2, mob);
							}
							Crusade.addCrusadeTag(mob, mCrusade);
						}
					}

					if (mCurrentDuration >= 0) {
						mCurrentDuration++;
					}
					if (mCurrentDuration > getInitialAbilityDuration()) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrentDuration = -1;
					ClientModHandler.updateAbility(mPlayer, ChoirBells.this);
				}
			};
			cancelOnDeath(mTollRunnable.runTaskTimer(mPlugin, 1, 1));
		}
		return true;
	}

	private static Description<ChoirBells> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to afflict all enemies within ")
			.add(a -> a.mRange, CHOIR_BELLS_RANGE)
			.add(" blocks with ")
			.addPercent(a -> a.mSlownessAmount, SLOWNESS_AMPLIFIER)
			.add(" slowness for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. Heretics are taunted, dealt ")
			.add(a -> a.mDamage, DAMAGE)
			.add(" magic damage, and are afflicted with ")
			.addPercent(a -> a.mVulnerabilityEffect, VULNERABILITY_EFFECT)
			.add(" vulnerability and ")
			.addPercent(a -> a.mWeakenEffect, WEAKEN_EFFECT)
			.add(" weakness for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<ChoirBells> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("After casting, the bells will toll ")
			.add(a -> a.mExtraTolls, EXTRA_TOLLS)
			.add(" more times at the casting location, at a rate of every ")
			.addDuration(a -> a.mTollDelay, TOLL_DELAY, true)
			.add(" seconds. The extra tolls apply ")
			.addPercent(a -> a.mSlownessAmount2, SLOWNESS_AMPLIFIER_2, false, Ability::isLevelTwo)
			.add(" slowness to all mobs and ")
			.addPercent(a -> a.mVulnerabilityEffect2, VULNERABILITY_EFFECT_2, false, Ability::isLevelTwo)
			.add(" vulnerability and ")
			.add(a -> a.mTollDamage, DAMAGE_2)
			.add(" magic damage to Heretics in its ")
			.add(a -> a.mRange, CHOIR_BELLS_RANGE)
			.add(" block radius for ")
			.addDuration(a -> a.mTollDelay, TOLL_DELAY)
			.add(" seconds, taunting them as well.");
	}

	@Override
	public void invalidate() {
		if (mTollRunnable != null && !mTollRunnable.isCancelled()) {
			mTollRunnable.cancel();
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mTollsDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrentDuration >= 0 ? getInitialAbilityDuration() - this.mCurrentDuration : 0;
	}
}
