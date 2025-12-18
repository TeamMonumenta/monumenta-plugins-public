package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.classes.Rogue.STEALTH_COLOR;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Killing a mob grants you %d stack of *Cloak and Dagger*.").styles(UNDERLINED)
				.statValues(stat(a -> a.mStacksOnKill, 1))
			.addLine("Killing an Elite or dealing %d damage to Bosses grants")
				.statValues(perRegion(BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3))
			.addLine("you %d stacks instead.").styles(UNDERLINED)
				.statValues(stat(a -> a.mStacksOnEliteKill, CLOAK_STACKS_ON_ELITE_KILL))
			.addLine()
			.addLine("Activate with at least %d stacks to spend all of them,")
				.statValues(stat(CLOAK_MIN_STACKS))
			.addLine("granting yourself *Stealth* until your next attack, which").styles(STEALTH_COLOR)
			.addLine("deals bonus damage.")
			.addLine()
			.addStat("Bonus Damage: +%d1 (m) per stack")
				.statValues(stat(a -> a.mDamageMultiplier, CLOAK_1_DAMAGE_MULTIPLIER))
			.addStat("Effect: Stealth for %t")
				.statValues(stat(a -> a.mStealthDuration, STEALTH_DURATION))
			.addStat("Max Daggers: %d1")
				.statValues(stat(a -> a.mMaxStacks, CLOAK_1_MAX_STACKS))
			.addDashedLine();
	}

	private static Description<CloakAndDagger> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Cloak and Dagger*'s").styles(UNDERLINED)
			.addLine("bonus damage and maximum stacks.")
			.addLine()
			.addStatComparison("Bonus Damage: +%d1 -> +%d2 (m) per stack")
				.statValues(stat(CLOAK_1_DAMAGE_MULTIPLIER), stat(a -> a.mDamageMultiplier, CLOAK_2_DAMAGE_MULTIPLIER))
			.addStatComparison("Max Stacks: %d1 -> %d2")
				.statValues(stat(CLOAK_1_MAX_STACKS), stat(a -> a.mMaxStacks, CLOAK_2_MAX_STACKS))
			.addDashedLine();
	}
}
