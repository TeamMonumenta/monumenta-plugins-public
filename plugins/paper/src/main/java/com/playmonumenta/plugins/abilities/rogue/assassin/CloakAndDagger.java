package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.assassin.CloakAndDaggerCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

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
	public static final String CHARM_STACKS_GAIN = "Cloak and Dagger Stacks Per Kill";
	public static final String CHARM_STEALTH = "Cloak and Dagger Stealth Duration";

	public static final AbilityInfo<CloakAndDagger> INFO =
		new AbilityInfo<>(CloakAndDagger.class, "Cloak and Dagger", CloakAndDagger::new)
			.linkedSpell(ClassAbility.CLOAK_AND_DAGGER)
			.scoreboardId("CloakAndDagger")
			.shorthandName("CnD")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Killing mobs grants cloak stacks, which can be consumed to enter Stealth and buff your next melee attack.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CloakAndDagger::cast, new AbilityTrigger(AbilityTrigger.Key.DROP),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.IRON_SWORD);

	private final KillTriggeredAbilityTracker mTracker;

	private final double mDamageMultiplier;
	private final int mStacksOnKill;
	private final int mStacksOnEliteKill;
	private final int mMaxStacks;
	private final int mStealthDuration;
	private final CloakAndDaggerCS mCosmetic;
	private int mCloak = 0;
	private int mCloakOnActivation = 0;
	private boolean mActive = false;

	public CloakAndDagger(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageMultiplier = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? CLOAK_1_DAMAGE_MULTIPLIER : CLOAK_2_DAMAGE_MULTIPLIER);
		mStacksOnKill = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS_GAIN);
		mStacksOnEliteKill = CLOAK_STACKS_ON_ELITE_KILL + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS_GAIN);
		mMaxStacks = (isLevelOne() ? CLOAK_1_MAX_STACKS : CLOAK_2_MAX_STACKS) + (int) CharmManager.getLevel(player, CHARM_STACKS);
		mStealthDuration = CharmManager.getDuration(mPlayer, CHARM_STEALTH, STEALTH_DURATION);
		mTracker = new KillTriggeredAbilityTracker(player, this, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CloakAndDaggerCS());
	}

	public boolean cast() {
		if (mCloak >= CLOAK_MIN_STACKS) {
			mCloakOnActivation = mCloak;
			mCloak = 0;
			mActive = true;
			AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration, mCosmetic);
			mCosmetic.castEffects(mPlayer);

			ClientModHandler.updateAbility(mPlayer, this);
			return true;
		}
		return false;
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
				mCloak = Math.min(mMaxStacks, mCloak + mStacksOnEliteKill);
			} else {
				mCloak = Math.min(mMaxStacks, mCloak + mStacksOnKill);
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

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
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

	private static Description<CloakAndDagger> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Killing a mob grants ")
			.add(a -> a.mStacksOnKill, 1)
			.add(" stack of cloak, up to ")
			.add(a -> a.mMaxStacks, CLOAK_1_MAX_STACKS, false, Ability::isLevelOne)
			.add(" stacks. Elite kills and Boss \"kills\" grant ")
			.add(a -> a.mStacksOnEliteKill, CLOAK_STACKS_ON_ELITE_KILL)
			.add(" stacks (")
			.add((a, p) -> {
				Description<CloakAndDagger> subDescription;
				if (p == null) {
					subDescription = new DescriptionBuilder<>(() -> INFO)
						.add("every ")
						.add(aa -> BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R2)
						.add(" damage to them in R2; every ")
						.add(aa -> BOSS_DAMAGE_THRESHOLD_R3, BOSS_DAMAGE_THRESHOLD_R3)
						.add(" damage to them in R3");
				} else {
					int threshold = ServerProperties.getAbilityEnhancementsEnabled(p) ? BOSS_DAMAGE_THRESHOLD_R3 : BOSS_DAMAGE_THRESHOLD_R2;
					subDescription = new DescriptionBuilder<>(() -> INFO)
						.add("every ")
						.add(aa -> threshold, threshold)
						.add(" damage to them");
				}
				return subDescription.get(a, p);
			})
			.add(" excluding damage from this skill). ")
			.addTrigger()
			.add(" to spend all cloak stacks to gain ")
			.addDuration(a -> a.mStealthDuration, STEALTH_DURATION)
			.add(" seconds of Stealth and (")
			.addPercent(a -> a.mDamageMultiplier, CLOAK_1_DAMAGE_MULTIPLIER, false, Ability::isLevelOne)
			.add(" * X) melee damage on your next melee attack while in Stealth, where X is the number of stacks you had at activation. Performing a successful melee attack or switching from your held swords ends Stealth. You must have at least ")
			.add(a -> CLOAK_MIN_STACKS, CLOAK_MIN_STACKS)
			.add(" stacks for activation.");
	}

	private static Description<CloakAndDagger> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The stack cap is increased to ")
			.add(a -> a.mMaxStacks, CLOAK_2_MAX_STACKS, false, Ability::isLevelTwo)
			.add(" and the melee damage is increased to (")
			.addPercent(a -> a.mDamageMultiplier, CLOAK_2_DAMAGE_MULTIPLIER, false, Ability::isLevelTwo)
			.add(" * X).");
	}
}
