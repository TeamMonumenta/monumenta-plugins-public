package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.LuminousInfusionCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class LuminousInfusion extends MultipleChargeAbility implements AbilityWithChargesOrStacks {
	public static final int DAMAGE_UNDEAD_1 = 2;
	public static final double DIVINE_JUSTICE_DAMAGE_MULTIPLIER = 0.2;
	public static final int MAX_CHARGES_1 = 6;
	public static final int MAX_CHARGES_2 = 12;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveMeleeDamage = 0;

	private static final double RADIUS = 0.33;
	private static final int FIRE_DURATION_2 = Constants.TICKS_PER_SECOND * 3;
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 9;
	private static final int REFRESH = Constants.HALF_TICKS_PER_SECOND;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int EXPIRE_TICKS = 20 * 4;
	private static final int HIT_REQUIREMENT = 1;
	private static final int STACKS_REFRESHED = 1;
	private static final int STACKS_ON_HIT = 1;

	public static final String CHARM_DAMAGE = "Luminous Infusion Damage Per Stack";
	public static final String CHARM_COOLDOWN = "Luminous Infusion Cooldown";
	public static final String CHARM_RADIUS = "Luminous Infusion Radius Per Stack";
	public static final String CHARM_MAX_STACKS = "Luminous Infusion Max Stacks";
	public static final String CHARM_DAMAGE_MULTIPLIER = "Luminous Infusion Damage Multiplier";
	public static final String CHARM_FIRE_DURATION = "Luminous Infusion Fire Duration";
	public static final String CHARM_HIT_REQUIREMENT = "Luminous Infusion Hit Requirement";
	public static final String CHARM_STACKS_REFRESHED = "Luminous Infusion Recharge Stacks";
	public static final String CHARM_STACKS_ON_HIT = "Luminous Infusion Stacks On Hit";

	public static final AbilityInfo<LuminousInfusion> INFO =
		new AbilityInfo<>(LuminousInfusion.class, "Luminous Infusion", LuminousInfusion::new)
			.linkedSpell(ClassAbility.LUMINOUS_INFUSION)
			.scoreboardId("LuminousInfusion")
			.shorthandName("LI")
			.descriptions(
				String.format(
					"Performing a critical melee attack against an enemy grants %d stack of Luminosity capped at %s stacks. " +
					"Right click while not sneaking or holding usable items to prime one stack of Luminosity, or all stacks while looking down. All stacks unprime after %ss. " +
					"With two or more primed stacks, the next attack or ability against an undead enemy is infused with explosive power " +
					"that deals %d magic damage per stack to it and other undead enemies, or half damage against non-undead, " +
					"in a %s blocks per stack radius around it and knocking other enemies away from it. " +
					"Gain %d stack of Luminosity every %ss if you haven't triggered an explosion in %ss and have no primed stacks.",
					STACKS_ON_HIT,
					MAX_CHARGES_1,
					StringUtils.ticksToSeconds(EXPIRE_TICKS),
					DAMAGE_UNDEAD_1,
					RADIUS,
					STACKS_REFRESHED,
					StringUtils.ticksToSeconds(REFRESH),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format(
					"Luminosity stacks up to %s times. With at least one primed stack, the next critical melee attack against " +
					"an undead enemy triggers Divine Justice for %s%% of your critical attack damage. Undead enemies " +
					"hit by Luminous explosions are set on fire for %ss.",
					MAX_CHARGES_2,
					StringUtils.multiplierToPercentage(DIVINE_JUSTICE_DAMAGE_MULTIPLIER),
					StringUtils.ticksToSeconds(FIRE_DURATION_2)
				)
			)
			.simpleDescription("Upon activating, the next damage dealt to an Undead enemy causes an explosion.")
			.addTrigger(new AbilityTriggerInfo<>("castOne", "activate one stack", li -> li.cast(false), new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.LEVEL, AbilityTrigger.LookDirection.UP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addTrigger(new AbilityTriggerInfo<>("castAll", "activate all stacks", li -> li.cast(true), new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.DOWN).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLAZE_POWDER);

	private int mPrimedStacks = 0;
	private int mCooldownTicks = 0;
	private int mRechargeTicks = 0;
	private int mHitCount = 0;
	private boolean mPreventActiveStackGain = false;
	private final LuminousInfusionCS mCosmetic;
	private @Nullable Crusade mCrusade;
	private @Nullable DivineJustice mDivineJustice;
	private @Nullable BukkitRunnable mCancelRunnable;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (isLevelOne() ? MAX_CHARGES_1 : MAX_CHARGES_2) + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_STACKS);
		mCharges = getCharges();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new LuminousInfusionCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, Crusade.class);
			mDivineJustice = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, DivineJustice.class);
		});
	}

	public boolean cast(final boolean all) {
		if (!consumeCharge()) {
			return false;
		}
		mPrimedStacks = Math.min(mPrimedStacks + 1, mMaxCharges);

		if (all) {
			for (int i = mCharges; i > 0; i--) {
				consumeCharge();
				mPrimedStacks = Math.min(mPrimedStacks + 1, mMaxCharges);
			}
		}

		mCosmetic.infusionStartEffect(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedStacks);

		if (mCancelRunnable != null) {
			mCancelRunnable.cancel();
		}

		mCancelRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mCosmetic.infusionTickEffect(mPlayer, mT);
				if (mT >= EXPIRE_TICKS || mPrimedStacks <= 0) {
					for (int i = mPrimedStacks; i > 0; i--) {
						incrementCharge();
						mCosmetic.gainMaxCharge(mPlayer, mPlayer.getLocation());
					}
					mPrimedStacks = 0;
					if (mT >= EXPIRE_TICKS) {
						mCosmetic.infusionExpireMsg(mPlayer);
						ClientModHandler.updateAbility(mPlayer, LuminousInfusion.this);
					}
					this.cancel();
				}
			}
		};

		cancelOnDeath(mCancelRunnable.runTaskTimer(Plugin.getInstance(), 1, 1));
		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		mCooldownTicks += 5;
		if (mCooldownTicks >= CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, COOLDOWN) && mPrimedStacks == 0) {
			mRechargeTicks += 5;
			if (mRechargeTicks >= REFRESH) {
				mRechargeTicks = 0;

				for (int i = 0; i < STACKS_REFRESHED + CharmManager.getLevel(mPlayer, CHARM_STACKS_REFRESHED); i++) {
					if (incrementCharge() && mCharges == mMaxCharges) {
						mCosmetic.gainMaxCharge(mPlayer, mPlayer.getLocation());
					}
				}
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		final boolean triggersCrusade = Crusade.enemyTriggersAbilities(enemy, mCrusade);
		final boolean isMeleeCrit = event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer);
		int chargesToConsume = 0;

		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "LIExplosionCap") &&
			mPrimedStacks > 1 && triggersCrusade && event.getAbility() != mInfo.getLinkedSpell()) {
			execute(enemy, mPrimedStacks);
			chargesToConsume += mPrimedStacks;
		}

		if (isLevelTwo() && triggersCrusade && mDivineJustice != null && isMeleeCrit && mPrimedStacks > 0) {
			chargesToConsume++;
			mLastPassiveMeleeDamage = mDivineJustice.calculateDamage(event, DIVINE_JUSTICE_DAMAGE_MULTIPLIER, true, true);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, mLastPassiveMeleeDamage, mDivineJustice.getInfo().getLinkedSpell(), true);
		}

		/* Why are lambdas like this */
		final int finalChargesToConsume = chargesToConsume;
		Bukkit.getScheduler().runTask(mPlugin, () -> mPrimedStacks = Math.max(mPrimedStacks - finalChargesToConsume, 0));

		if (isMeleeCrit && !mPreventActiveStackGain) {
			mHitCount++;
			mPreventActiveStackGain = true;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPreventActiveStackGain = false, Constants.HALF_TICKS_PER_SECOND);

			if (mHitCount >= HIT_REQUIREMENT + CharmManager.getLevel(mPlayer, CHARM_HIT_REQUIREMENT)) {
				mHitCount = 0;

				for (int i = 0; i < STACKS_ON_HIT + CharmManager.getLevel(mPlayer, CHARM_STACKS_ON_HIT); i++) {
					if (incrementCharge() && mCharges == mMaxCharges) {
						mCosmetic.gainMaxCharge(mPlayer, mPlayer.getLocation());
					}
				}
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		return false;
	}

	private void execute(final LivingEntity damagee, final int stacks) {
		mCooldownTicks = 0;
		mRechargeTicks = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		final double ratio = (double) stacks / mMaxCharges;
		final float volumeScaling = (float) Math.sqrt(ratio);
		final Location loc = damagee.getLocation();
		final World world = mPlayer.getWorld();
		mCosmetic.infusionHitEffect(world, mPlayer, damagee, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS * stacks), ratio, volumeScaling);

		final List<LivingEntity> affected = new Hitbox.SphereHitbox(loc, stacks * (RADIUS + CharmManager.getLevel(mPlayer, CHARM_RADIUS))).getHitMobs();
		for (final LivingEntity entity : affected) {
			mCosmetic.infusionSpreadEffect(world, mPlayer, damagee, entity, volumeScaling);

			final double totalDamage = (DAMAGE_UNDEAD_1 + CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, 0)) * stacks;
			if (Crusade.enemyTriggersAbilities(entity, mCrusade)) {
				DamageUtils.damage(mPlayer, entity, DamageType.MAGIC, totalDamage, mInfo.getLinkedSpell(), true);
				if (isLevelTwo()) {
					EntityUtils.applyFire(Plugin.getInstance(), CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, FIRE_DURATION_2), entity, mPlayer);
				}
			} else {
				DamageUtils.damage(mPlayer, entity, DamageType.MAGIC, 0.5 * totalDamage, mInfo.getLinkedSpell(), true);
				Crusade.addCrusadeTag(entity, mCrusade);
			}
			if (!entity.equals(damagee)) {
				MovementUtils.knockAway(loc, entity, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2, true);
			}
		}
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	@Override
	public @Nullable String getMode() {
		return mPrimedStacks > 0 ? "active" : null;
	}
}
