package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
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
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class LuminousInfusion extends MultipleChargeAbility implements KillTriggeredAbility {
	public double mLastPassiveMeleeDamage = 0; // Passive damage to share with Holy Javelin

	private static final int DAMAGE_UNDEAD_1 = 4;
	private static final double DAMAGE_UNDEAD_2 = 5.5;
	private static final double DIVINE_JUSTICE_DMG_MULT = 0.2;
	private static final int MIN_STACKS_TO_ACTIVATE = 2;
	private static final int MAX_STACKS = 6;
	private static final double RADIUS = 2.0 / 3.0;
	private static final int FIRE_DURATION_2 = TICKS_PER_SECOND * 3;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int EXPIRE_TICKS = TICKS_PER_SECOND * 4;
	private static final int STACKS_PER_KILL = 1;
	private static final int BOSS_DMG_THRESHOLD_R2 = 300;
	private static final int BOSS_DMG_THRESHOLD_R3 = 450;

	public static final String CHARM_DAMAGE = "Luminous Infusion Damage";
	public static final String CHARM_RADIUS = "Luminous Infusion Radius";
	public static final String CHARM_MIN_STACKS = "Luminous Infusion Minimum Stack Threshold";
	public static final String CHARM_MAX_STACKS = "Luminous Infusion Max Stacks";
	public static final String CHARM_DAMAGE_MULTIPLIER = "Luminous Infusion Damage Multiplier";
	public static final String CHARM_FIRE_DURATION = "Luminous Infusion Fire Duration";
	public static final String CHARM_KILL_REQUIREMENT = "Luminous Infusion Kill Requirement";
	public static final String CHARM_STACKS_PER_KILL = "Luminous Infusion Stacks Per Kill";

	public static final AbilityInfo<LuminousInfusion> INFO =
		new AbilityInfo<>(LuminousInfusion.class, "Luminous Infusion", LuminousInfusion::new)
			.linkedSpell(ClassAbility.LUMINOUS_INFUSION)
			.scoreboardId("LuminousInfusion")
			.shorthandName("LI")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Upon activating, the next damage dealt to an Undead enemy causes an explosion.")
			.addTrigger(new AbilityTriggerInfo<>("castOne", "activate one stack",
				li -> li.cast(false, false), new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK)
					.lookDirections(AbilityTrigger.LookDirection.LEVEL, AbilityTrigger.LookDirection.UP).sneaking(false)
					.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addTrigger(new AbilityTriggerInfo<>("castHalf", "activate half of current stacks",
				li -> li.cast(false, true), new AbilityTrigger(AbilityTrigger.Key.SWAP)
				.sneaking(true).enabled(false)))
			.addTrigger(new AbilityTriggerInfo<>("castAll", "activate all stacks",
				li -> li.cast(true, false), new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK)
				.lookDirections(AbilityTrigger.LookDirection.DOWN).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.sneaking(false)))
			.displayItem(Material.BLAZE_POWDER);

	private final double mDamagePerStack;
	private final double mRadiusPerStack;
	private final double mDivineJusticeDmgMult;
	private final int mFireDuration;
	private final int mKillReqForStack;
	private final int mStacksPerKill;
	private final int mMinStacksToActivate;
	private final KillTriggeredAbilityTracker mTracker;
	private final LuminousInfusionCS mCosmetic;

	private int mKillCount = 0;
	private int mPrimedStacks = 0;
	private @Nullable Crusade mCrusade;
	private @Nullable DivineJustice mDivineJustice;
	private @Nullable BukkitRunnable mCancelRunnable;

	public LuminousInfusion(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mKillReqForStack = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_KILL_REQUIREMENT);
		mMinStacksToActivate = MIN_STACKS_TO_ACTIVATE + (int) CharmManager.getLevel(mPlayer, CHARM_MIN_STACKS);
		mStacksPerKill = STACKS_PER_KILL + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS_PER_KILL);
		mMaxCharges = MAX_STACKS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_STACKS);
		mCharges = getCharges();

		mDamagePerStack = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE,
			(isLevelTwo() ? DAMAGE_UNDEAD_2 : DAMAGE_UNDEAD_1));
		mRadiusPerStack = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDivineJusticeDmgMult = DIVINE_JUSTICE_DMG_MULT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MULTIPLIER);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, FIRE_DURATION_2);
		mTracker = new KillTriggeredAbilityTracker(mPlayer, this, BOSS_DMG_THRESHOLD_R2, BOSS_DMG_THRESHOLD_R2, BOSS_DMG_THRESHOLD_R3);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new LuminousInfusionCS());

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, Crusade.class);
			mDivineJustice = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, DivineJustice.class);
		});
	}

	public boolean cast(final boolean all, final boolean half) {
		if (!consumeCharge()) {
			return false;
		}

		if (mPrimedStacks == 0) {
			mCosmetic.infusionStartEffect(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedStacks);
		} else {
			mCosmetic.infusionAddStack(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedStacks);
		}

		mPrimedStacks = Math.min(mPrimedStacks + 1, mMaxCharges);

		if (all) {
			for (int i = mCharges; i > 0; i--) {
				consumeCharge();
				mPrimedStacks = Math.min(mPrimedStacks + 1, mMaxCharges);
			}
		} else if (half) {
			for (int i = mCharges / 2; i > 0; i--) {
				consumeCharge();
				mPrimedStacks = Math.min(mPrimedStacks + 1, mMaxCharges);
			}
		}

		mCosmetic.infusionStartMsg(mPlayer, mPrimedStacks);

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
						if (mCharges == mMaxCharges) {
							mCosmetic.gainMaxCharge(mPlayer, mPlayer.getLocation());
						}
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
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		mTracker.updateDamageDealtToBosses(event);

		final boolean triggersCrusade = Crusade.enemyTriggersAbilities(enemy, mCrusade);
		final boolean isMeleeCrit = event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer);
		int chargesToConsume = 0;

		// The short circuiting on this check is very fragile. Don't reorder anything!
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "LIExplosionCap") &&
			mPrimedStacks >= mMinStacksToActivate && triggersCrusade && event.getAbility() != mInfo.getLinkedSpell()) {
			execute(enemy, mPrimedStacks);
			chargesToConsume += mPrimedStacks;
		}

		if (isLevelTwo() && triggersCrusade && mDivineJustice != null && isMeleeCrit && mPrimedStacks > 0) {
			chargesToConsume++;
			mLastPassiveMeleeDamage = mDivineJustice.calculateDamage(event, mDivineJusticeDmgMult, true, true);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, mLastPassiveMeleeDamage, mDivineJustice.getInfo().getLinkedSpell(), true);
		}

		/* Why are lambdas like this */
		final int finalChargesToConsume = chargesToConsume;
		Bukkit.getScheduler().runTask(mPlugin, () -> mPrimedStacks = Math.max(mPrimedStacks - finalChargesToConsume, 0));

		return false;
	}

	private void execute(final LivingEntity damagee, final int stacks) {
		ClientModHandler.updateAbility(mPlayer, this);

		final double ratio = (double) stacks / mMaxCharges;
		final float volumeScaling = (float) Math.sqrt(ratio);
		final Location loc = damagee.getLocation();
		loc.add(0, damagee.getHeight() / 2.0, 0);
		final World world = mPlayer.getWorld();
		mCosmetic.infusionHitEffect(world, mPlayer, damagee, stacks * mRadiusPerStack, ratio, volumeScaling);

		final double totalDamage = stacks * mDamagePerStack;
		for (final LivingEntity entity : new Hitbox.SphereHitbox(loc, stacks * mRadiusPerStack).getHitMobs()) {
			mCosmetic.infusionSpreadEffect(world, mPlayer, damagee, entity, volumeScaling);

			if (Crusade.enemyTriggersAbilities(entity, mCrusade)) {
				DamageUtils.damage(mPlayer, entity, DamageType.MAGIC, totalDamage, mInfo.getLinkedSpell(), true);
				if (isLevelTwo()) {
					EntityUtils.applyFire(Plugin.getInstance(), mFireDuration, entity, mPlayer);
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
	public void entityDeathEvent(final EntityDeathEvent event, final boolean shouldGenDrops) {
		mKillCount++;
		if (mKillCount >= mKillReqForStack) {
			triggerOnKill(event.getEntity());
			mKillCount = 0;
		}
	}

	@Override
	public void triggerOnKill(final LivingEntity mob) {
		for (int stacks = 0; stacks < mStacksPerKill; stacks++) {
			incrementCharge();
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
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public @Nullable String getMode() {
		return mPrimedStacks > 0 ? "active" : null;
	}

	private static Description<LuminousInfusion> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Killing an Undead enemy or dealing ")
			.add((a, p) -> {
				Description<LuminousInfusion> subDescription;
				if (p == null) {
					subDescription = new DescriptionBuilder<>(() -> INFO)
						.add(aa -> BOSS_DMG_THRESHOLD_R2, BOSS_DMG_THRESHOLD_R2)
						.add(" damage against a boss (")
						.add(aa -> BOSS_DMG_THRESHOLD_R3, BOSS_DMG_THRESHOLD_R3)
						.add(" in Region 3)");
				} else {
					subDescription = new DescriptionBuilder<>(() -> INFO)
						.add(aa -> aa.mTracker.getThreshold(), ServerProperties.getAbilityEnhancementsEnabled(p) ? BOSS_DMG_THRESHOLD_R3 : BOSS_DMG_THRESHOLD_R2)
						.add(" damage against a boss");
				}
				return subDescription.get(a, p);
			})
			.add(" grants ")
			.add(a -> a.mStacksPerKill, STACKS_PER_KILL)
			.add(" stack of Luminosity capped at ")
			.add(a -> a.mMaxCharges, MAX_STACKS)
			.add(" stacks. ")
			.addTrigger()
			.add(" to prime one stack of Luminosity, or all stacks while looking down. Luminosity unprimes after ")
			.addDuration(EXPIRE_TICKS)
			.add(" seconds of inactivity. With ")
			.add(a -> a.mMinStacksToActivate, MIN_STACKS_TO_ACTIVATE)
			.add(" or more primed stacks, the next attack or ability against an undead enemy is infused with explosive power that deals ")
			.add(a -> a.mDamagePerStack, DAMAGE_UNDEAD_1, false, Ability::isLevelOne)
			.add(" magic damage per stack to it and other Undead enemies, or half damage against Non-undead, in a ")
			.add(a -> a.mRadiusPerStack, RADIUS)
			.add(" blocks per stack radius around it and knocking other enemies away from it.");
	}

	private static Description<LuminousInfusion> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage per stack is increased to ")
			.add(a -> a.mDamagePerStack, DAMAGE_UNDEAD_2, false, Ability::isLevelTwo)
			.add(". With at least one primed stack, the next critical melee attack against an Undead enemy consumes one stack of Luminousity to trigger Divine Justice for ")
			.addPercent(a -> a.mDivineJusticeDmgMult, DIVINE_JUSTICE_DMG_MULT)
			.add(" of your critical attack damage. Undead enemies hit by Luminous explosions are set on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_DURATION_2)
			.add(" seconds.");
	}
}
