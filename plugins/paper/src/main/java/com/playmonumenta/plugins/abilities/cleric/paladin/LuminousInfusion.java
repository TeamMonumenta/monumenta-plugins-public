package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
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
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class LuminousInfusion extends Ability implements AbilityWithChargesOrStacks {
	public static final int R2_DAMAGE = 8;
	public static final int R3_DAMAGE = 11;
	public static final int R2_HERETIC_DAMAGE = 16;
	public static final int R3_HERETIC_DAMAGE = 22;
	public static final int CHARGES_1 = 2;
	public static final int CHARGES_2 = 3;

	private static final double RADIUS = 4;
	private static final int FIRE_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND * 18;
	private static final float KNOCKBACK_SPEED = 0.55f;

	public static final String CHARM_DAMAGE = "Luminous Infusion Damage";
	public static final String CHARM_COOLDOWN = "Luminous Infusion Cooldown";
	public static final String CHARM_RADIUS = "Luminous Infusion Radius";
	public static final String CHARM_CHARGES = "Luminous Infusion Charges";
	public static final String CHARM_FIRE_DURATION = "Luminous Infusion Fire Duration";
	public static final String CHARM_KNOCKBACK = "Luminous Infusion Knockback";

	public static final AbilityInfo<LuminousInfusion> INFO =
		new AbilityInfo<>(LuminousInfusion.class, "Luminous Infusion", LuminousInfusion::new)
			.linkedSpell(ClassAbility.LUMINOUS_INFUSION)
			.scoreboardId("LuminousInfusion")
			.shorthandName("LI")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Upon activating, your next attack on a Heretic causes an explosion.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LuminousInfusion::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)))
			.displayItem(Material.BLAZE_POWDER);

	private final double mDamage;
	private final double mHereticDamage;
	private final double mRadius;
	private final int mMaxCharges;
	private final int mFireDuration;
	private final float mKnocback;
	private final LuminousInfusionCS mCosmetic;
	private boolean mActive = false;
	private int mPrimedCharges;
	private int mRemainingCharges = 0;
	private boolean mWasOnCooldown;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_DAMAGE : R2_DAMAGE);
		mHereticDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_HERETIC_DAMAGE : R2_HERETIC_DAMAGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new LuminousInfusionCS());
		mMaxCharges = (isLevelOne() ? CHARGES_1 : CHARGES_2) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mFireDuration = CharmManager.getDuration(player, CHARM_FIRE_DURATION, FIRE_DURATION);
		mKnocback = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_KNOCKBACK, KNOCKBACK_SPEED);
	}

	public boolean cast() {
		if (mRemainingCharges <= 0) {
			return false;
		}

		mActive = true;
		int oldHits = mPrimedCharges;
		mRemainingCharges--;
		mPrimedCharges++;

		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.SIDEARM, mRemainingCharges);
		ClientModHandler.updateAbility(mPlayer, this);

		mCosmetic.infusionStartMsg(mPlayer, mPrimedCharges);
		if (oldHits == 0) {
			mCosmetic.infusionStartEffect(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedCharges);
		} else {
			mCosmetic.infusionAddStack(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedCharges);
			return true;
		}
		putOnCooldown();

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			final int EXPIRE_TICKS = getModifiedCooldown();

			@Override
			public void run() {
				mT++;
				if (mPrimedCharges > 0) {
					mCosmetic.infusionTickEffect(mPlayer, mT);
				}
				if (mT >= EXPIRE_TICKS || (!mActive && mRemainingCharges <= 0)) {
					mActive = false;
					if (mT >= EXPIRE_TICKS) {
						mCosmetic.infusionExpireMsg(mPlayer);
						ClientModHandler.updateAbility(mPlayer, LuminousInfusion.this);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1));
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		boolean enemyTriggersAbilities = Crusade.enemyTriggersAbilities(enemy);

		if (mActive && enemyTriggersAbilities) {
			execute(enemy);
		}
		return false;
	}

	public void execute(LivingEntity damagee) {
		mPrimedCharges--;
		mActive = false;

		// if there are hits remaining, turn back on after a tick to prevent looping
		if (mPrimedCharges > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					mActive = true;
					ClientModHandler.updateAbility(mPlayer, ClassAbility.LUMINOUS_INFUSION);
				}
			}.runTaskLater(Plugin.getInstance(), 1);
		}

		DamageUtils.damage(mPlayer, damagee, DamageType.MAGIC, mHereticDamage, mInfo.getLinkedSpell(), true);
		mCosmetic.infusionHitEffect(mPlayer.getWorld(), mPlayer, damagee, mRadius, 1f, 1f);
		ClientModHandler.updateAbility(mPlayer, this);

		// Exclude the damagee so that the knockaway is valid
		List<LivingEntity> affected = new Hitbox.SphereHitbox(damagee.getLocation(), mRadius).getHitMobs(damagee);
		for (LivingEntity e : affected) {
			// Reduce overall volume of noise the more mobs there are, but still make it louder for more mobs
			double volume = 0.6 / Math.sqrt(affected.size());
			mCosmetic.infusionSpreadEffect(mPlayer.getWorld(), mPlayer, damagee, e, (float) volume);

			if (isLevelTwo()) {
				EntityUtils.applyFire(Plugin.getInstance(), mFireDuration, e, mPlayer);
			}
			if (Crusade.enemyTriggersAbilities(e)) {
				DamageUtils.damage(mPlayer, e, DamageType.MAGIC, mHereticDamage, mInfo.getLinkedSpell(), true);
			} else {
				DamageUtils.damage(mPlayer, e, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
			}
			MovementUtils.knockAway(damagee.getLocation(), e, mKnocback, mKnocback / 2, true);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mWasOnCooldown && !isOnCooldown()) {
			mPrimedCharges = 0;
			mRemainingCharges = mMaxCharges;
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.LUMINOUS_INFUSION, mRemainingCharges);

			Location loc = mPlayer.getLocation();
			mCosmetic.gainMaxCharge(mPlayer, loc);

			showOffCooldownMessage();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		mWasOnCooldown = isOnCooldown();

		if (!isOnCooldown() && mRemainingCharges != mMaxCharges) {
			putOnCooldown();
		}
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}

	@Override
	public int getCharges() {
		return mRemainingCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}

	private static Description<LuminousInfusion> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to charge your hands with holy light. The next attack or ability you perform against a Heretic causes a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius explosion that deals R2: " + R2_HERETIC_DAMAGE + " / R3: ")
			.add(a -> ServerProperties.getAbilityEnhancementsEnabled(a.mPlayer) ? a.mHereticDamage : R3_HERETIC_DAMAGE, R3_HERETIC_DAMAGE)
			.add(" magic damage to it and other Heretics or R2: " + R2_DAMAGE + " / R3: ")
			.add(a -> ServerProperties.getAbilityEnhancementsEnabled(a.mPlayer) ? a.mDamage : R3_DAMAGE, R3_DAMAGE)
			.add(" damage to non-Heretics. Enemies are knocked away from the hit Heretic. ")
			.add(a -> a.mMaxCharges, CHARGES_1, false, Ability::isLevelOne)
			.add(" charges. Having multiple charges primed at once will cause one explosion at a time, and all charges are replenished when cooldown expires.")
			.addCooldown(COOLDOWN);
	}

	private static Description<LuminousInfusion> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Max charges increased to ")
			.add(a -> a.mMaxCharges, CHARGES_2, false, Ability::isLevelTwo)
			.add(". Mobs hit by an explosion are set on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_DURATION, false, Ability::isLevelTwo)
			.add(" seconds.");
	}
}
