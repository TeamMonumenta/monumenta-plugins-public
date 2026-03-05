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
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
	private int mRemainingCharges;
	private boolean mWasOnCooldown;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_DAMAGE : R2_DAMAGE);
		mHereticDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? R3_HERETIC_DAMAGE : R2_HERETIC_DAMAGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new LuminousInfusionCS());
		mMaxCharges = (isLevelOne() ? CHARGES_1 : CHARGES_2) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mRemainingCharges = Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, ClassAbility.LUMINOUS_INFUSION), mMaxCharges);
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
		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.LUMINOUS_INFUSION, mRemainingCharges);

		ClientModHandler.updateAbility(mPlayer, this);

		mCosmetic.infusionStartMsg(mPlayer, mPrimedCharges);
		if (oldHits == 0) {
			mCosmetic.infusionStartEffect(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedCharges);
		} else {
			mCosmetic.infusionAddStack(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mPrimedCharges);
			return true;
		}
		if (!isOnCooldown()) {
			putOnCooldown();
		}

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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Activate to prime your next attack or ability")
			.addLine("on a *Heretic* to explode and deal damage").styles(Cleric.HERETIC_COLOR)
			.addLine("to nearby mobs, knocking them away.")
			.addLine("*Heretics* take increased damage.").styles(Cleric.HERETIC_COLOR)
			.addLine()
			.addStat("Damage: %d (s) (to non-Heretics)")
				.statValues(perRegion(a -> a.mDamage, R2_DAMAGE, R3_DAMAGE))
			.addStat("Damage: %d (s) (to Heretics)")
				.statValues(perRegion(a -> a.mHereticDamage, R2_HERETIC_DAMAGE, R3_HERETIC_DAMAGE))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Charges: %d1")
				.statValues(stat(a -> a.mMaxCharges, CHARGES_1))
			.addStat("Cooldown: %t (refreshes all charges at once)")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<LuminousInfusion> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Luminous Infusion*'s max charges.").styles(UNDERLINED)
			.addLine()
			.addLine("*Luminous Infusion* now ignites mobs hit.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Charges: %d1 -> %d2")
			.statValues(
				stat(CHARGES_1),
				stat(a -> a.mMaxCharges, CHARGES_2))
			.addStat("Effect: Fire for %t")
				.statValues(stat(a -> a.mFireDuration, FIRE_DURATION))
			.addDashedLine();
	}
}
