package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.assassin.CloakAndDaggerCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class CloakAndDagger extends Ability implements KillTriggeredAbility, AbilityWithChargesOrStacks {

	private static final double CLOAK_1_DAMAGE_MULTIPLIER = 2;
	private static final double CLOAK_2_DAMAGE_MULTIPLIER = 3;
	private static final int CLOAK_1_MAX_STACKS = 8;
	private static final int CLOAK_2_MAX_STACKS = 12;
	private static final int CLOAK_MIN_STACKS = 4;
	private static final int CLOAK_STACKS_ON_ELITE_KILL = 5;
	private static final int STEALTH_DURATION = (int) (2.5 * 20);
	private static final int BOSS_DAMAGE_THRESHOLD_R2 = 300;
	private static final int BOSS_DAMAGE_THRESHOLD_R3 = 450;

	public static final String CHARM_DAMAGE = "Cloak and Dagger Damage";
	public static final String CHARM_STACKS = "Cloak and Dagger Max Stacks";
	public static final String CHARM_STEALTH = "Cloak and Dagger Stealth Duration";

	public static final AbilityInfo<CloakAndDagger> INFO =
		new AbilityInfo<>(CloakAndDagger.class, "Cloak and Dagger", CloakAndDagger::new)
			.linkedSpell(ClassAbility.CLOAK_AND_DAGGER)
			.scoreboardId("CloakAndDagger")
			.shorthandName("CnD")
			.descriptions(
				String.format("When you kill an enemy you gain a stack of cloak. Elite kills and Boss \"kills\" give you %s stacks (every %s damage to them in R2; every %s damage to them in R3). Stacks are capped at %s. " +
					              "When you press the drop key with dual wielded swords, you lose your cloak stacks and gain %s seconds of Stealth " +
					              "and (%s * X) extra damage on your next stealth attack, where X is the number of stacks you had at activation. You must have at least %s stacks to activate this.",
					CLOAK_STACKS_ON_ELITE_KILL,
					BOSS_DAMAGE_THRESHOLD_R2,
					BOSS_DAMAGE_THRESHOLD_R3,
					CLOAK_1_MAX_STACKS,
					STEALTH_DURATION / 20.0,
					(int) CLOAK_1_DAMAGE_MULTIPLIER,
					CLOAK_MIN_STACKS),
				String.format("Cloak stacks are now capped at %s and bonus damage is increased to (%s * X) where X is the number of stacks you have upon activating this skill.",
					CLOAK_2_MAX_STACKS,
					(int) CLOAK_2_DAMAGE_MULTIPLIER))
			.simpleDescription("Killing mobs gains cloak stacks, which can be consumed to enter stealth mode and buff the next melee attack.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CloakAndDagger::cast, new AbilityTrigger(AbilityTrigger.Key.DROP),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.IRON_SWORD);

	private final KillTriggeredAbilityTracker mTracker;

	private final double mDamageMultiplier;
	private final int mMaxStacks;
	private final CloakAndDaggerCS mCosmetic;
	private int mCloak = 0;
	private int mCloakOnActivation = 0;
	private boolean mActive = false;

	public CloakAndDagger(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageMultiplier = (isLevelOne() ? CLOAK_1_DAMAGE_MULTIPLIER : CLOAK_2_DAMAGE_MULTIPLIER) + CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE);
		mMaxStacks = (isLevelOne() ? CLOAK_1_MAX_STACKS : CLOAK_2_MAX_STACKS) + (int) CharmManager.getLevel(player, CHARM_STACKS);
		mTracker = new KillTriggeredAbilityTracker(player, this, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CloakAndDaggerCS());
	}

	public void cast() {
		if (mCloak >= CLOAK_MIN_STACKS) {
			mCloakOnActivation = mCloak;
			mCloak = 0;
			mActive = true;
			AbilityUtils.applyStealth(mPlugin, mPlayer, CharmManager.getDuration(mPlayer, CHARM_STEALTH, STEALTH_DURATION), mCosmetic);
			mCosmetic.castEffects(mPlayer);

			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		mTracker.updateDamageDealtToBosses(event);
		if (AbilityUtils.isStealthed(mPlayer) && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH) && mActive) {
			AbilityUtils.removeStealth(mPlugin, mPlayer, false, mCosmetic);
			if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
				DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, mCloakOnActivation * mDamageMultiplier, mInfo.getLinkedSpell(), true);

				mCosmetic.activationEffects(mPlayer, enemy);
			}

			mActive = false;
		}
		return false; // only tallies damage done
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mActive && !AbilityUtils.isStealthed(mPlayer)) {
			mActive = false;
		}
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (mCloak < mMaxStacks) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				mCloak = Math.min(mMaxStacks, mCloak + CLOAK_STACKS_ON_ELITE_KILL);
			} else {
				mCloak++;
			}
			ClientModHandler.updateAbility(mPlayer, this);
		}

		showChargesMessage();
	}

	@Override
	public int getCharges() {
		return mCloak;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

}
