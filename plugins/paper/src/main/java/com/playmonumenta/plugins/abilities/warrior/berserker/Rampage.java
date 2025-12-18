package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.RampageCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public final class Rampage extends Ability implements AbilityWithChargesOrStacks {

	private static final double DAMAGE_L1 = 6;
	private static final double DAMAGE_L2 = 10;
	private static final double RADIUS = 4;
	private static final int BLOODLUST_COST = 4;
	private static final int MAX_BLOODLUST_GAIN_L1 = 3;
	private static final int MAX_BLOODLUST_GAIN_L2 = 5;
	private static final double HEAL_PERCENT = 0.01;
	private static final double DAMAGE_PERCENT_L1 = 0.10;
	private static final double DAMAGE_PERCENT_L2 = 0.15;
	private static final double MELEE_RESISTANCE_PERCENT = 0.1;
	private static final double KNOCKBACK = 0.45;
	private static final int DURATION_PER_STACK = Constants.TICKS_PER_SECOND;
	private static final int INITIAL_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final String DAMAGE_EFFECT_NAME = "RampagePercentDamageEffect";
	private static final String REGENERATION_EFFECT_NAME = "RampageCustomRegenerationEffect";
	private static final String RESISTANCE_EFFECT_NAME = "RampageMeleeResistanceEffect";
	private static final String AESTHETICS_EFFECT_NAME = "RampageAestheticEffect";

	public static final String CHARM_DAMAGE = "Rampage Damage";
	public static final String CHARM_MAX_BLOODLUST_GAIN = "Rampage Max Bloodlust Gain";

	public static final String CHARM_DAMAGE_BUFF = "Rampage Damage Buff";
	public static final String CHARM_MELEE_RESISTANCE = "Rampage Melee Resistance";
	public static final String CHARM_HEALING = "Rampage Healing";

	public static final String CHARM_BLOODLUST_COST = "Rampage Bloodlust Cost";
	public static final String CHARM_RADIUS = "Rampage Range";
	public static final String CHARM_KNOCKBACK = "Rampage Knockback";
	public static final String CHARM_INITIAL_DURATION = "Rampage Initial Duration";
	public static final String CHARM_DURATION_PER_STACK = "Rampage Duration Per Stack";
	public static final String CHARM_MAX_DURATION = "Rampage Max Duration";

	public static final AbilityInfo<Rampage> INFO =
		new AbilityInfo<>(Rampage.class, "Rampage", Rampage::new)
			.linkedSpell(ClassAbility.RAMPAGE)
			.scoreboardId("Rampage")
			.shorthandName("Rmp")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Consume Bloodlust stacks to knock enemies back, and gain health regeneration and melee damage on Bloodlust gain.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Rampage::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.DOWN)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLAZE_POWDER);

	private final double mDamage;
	private final double mRadius;
	private final double mHealing;
	private final double mDamageBuff;
	private final double mMeleeResistance;
	private final double mKnockback;
	private final int mBloodlustCost;
	private final int mMaxDuration;
	private final int mMaxBloodlustGain;
	private final int mDurationPerStack;
	private final int mInitialDuration;

	private int mBloodlustExtension = 0;

	private @Nullable Bloodlust mBloodlust;
	private int mLastCastTicks = 0;

	private final RampageCS mCosmetic;

	public Rampage(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_L1 : DAMAGE_L2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDurationPerStack = CharmManager.getDuration(mPlayer, CHARM_DURATION_PER_STACK, DURATION_PER_STACK);
		mMaxBloodlustGain = (isLevelOne() ? MAX_BLOODLUST_GAIN_L1 : MAX_BLOODLUST_GAIN_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_BLOODLUST_GAIN);
		mInitialDuration = CharmManager.getDuration(mPlayer, CHARM_INITIAL_DURATION, INITIAL_DURATION);

		mHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT);
		mDamageBuff = (isLevelOne() ? DAMAGE_PERCENT_L1 : DAMAGE_PERCENT_L2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_BUFF);
		mKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mMeleeResistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MELEE_RESISTANCE, MELEE_RESISTANCE_PERCENT);

		mBloodlustCost = BLOODLUST_COST + (int) CharmManager.getLevel(mPlayer, CHARM_BLOODLUST_COST);
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_MAX_DURATION, mDurationPerStack * mMaxBloodlustGain + mInitialDuration);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new RampageCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mBloodlust = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Bloodlust.class));
	}

	public boolean cast() {
		if (mBloodlust == null || isRecast()) {
			return false;
		}

		int stacks = mBloodlust.getStacks();

		if (stacks < mBloodlustCost) {
			return false;
		}
		mBloodlustExtension = 0;

		World world = mPlayer.getWorld();

		mBloodlust.useStacks(mBloodlustCost);
		EffectManager effectManager = mPlugin.mEffectManager;

		Effect rampage = effectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);
		boolean keepBuff = rampage != null && rampage.getDuration() > mInitialDuration;

		if (!keepBuff) {
			effectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(mInitialDuration,
				(entity, fourHertz, twoHertz, oneHertz) -> rampageTick(fourHertz, twoHertz, oneHertz),
				(entity) -> rampageEnd()).deleteOnAbilityUpdate(true));

			effectManager.addEffect(mPlayer, DAMAGE_EFFECT_NAME,
				new PercentDamageDealt(mInitialDuration, mDamageBuff).damageTypes(EnumSet.of(DamageType.MELEE, DamageType.MELEE_SKILL)).deleteOnAbilityUpdate(true));

			effectManager.addEffect(mPlayer, REGENERATION_EFFECT_NAME,
				new CustomRegeneration(mInitialDuration, mHealing * EntityUtils.getMaxHealth(mPlayer), 5, null, false, mPlugin));

			if (isLevelTwo()) {
				effectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME,
					new PercentDamageReceived(mInitialDuration, -mMeleeResistance, EnumSet.of(DamageType.MELEE)).deleteOnAbilityUpdate(true));
			}
		}

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(mPlayer, mob, (float) mKnockback, true);
			mCosmetic.onHitMob(mPlayer, mob);
		}

		ClientModHandler.updateAbility(mPlayer, this);
		Location loc = mPlayer.getLocation();
		mCosmetic.onCast(mPlayer, loc, world, mRadius);
		return true;
	}

	public void triggerOnBloodlust(int stacks) {
		EffectManager effectManager = mPlugin.mEffectManager;

		if (!effectManager.hasEffect(mPlayer, DAMAGE_EFFECT_NAME)) {
			return;
		}

		Effect rampage = effectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);
		Effect regen = effectManager.getActiveEffect(mPlayer, REGENERATION_EFFECT_NAME);
		Effect dmg = effectManager.getActiveEffect(mPlayer, DAMAGE_EFFECT_NAME);
		Effect res = isLevelTwo() ? effectManager.getActiveEffect(mPlayer, RESISTANCE_EFFECT_NAME) : null;

		if (rampage == null) {
			return;
		}

		for (int i = 0; i < stacks && mBloodlustExtension < mMaxBloodlustGain; i++) {
			rampage.setDuration(Math.min(rampage.getDuration() + mDurationPerStack, mMaxDuration));

			if (regen != null) {
				regen.setDuration(Math.min(regen.getDuration() + mDurationPerStack, mMaxDuration));
			}

			if (dmg != null) {
				dmg.setDuration(Math.min(dmg.getDuration() + mDurationPerStack, mMaxDuration));
			}

			if (res != null) {
				res.setDuration(Math.min(res.getDuration() + mDurationPerStack, mMaxDuration));
			}
			ClientModHandler.updateAbility(mPlayer, this);
			mBloodlustExtension++;
			mCosmetic.onStackGain(mPlayer.getWorld(), mPlayer.getLocation());
		}
	}

	private void rampageTick(boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.tick(mPlayer, fourHertz, twoHertz, oneHertz);
		if (oneHertz) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	private void rampageEnd() {
		mCosmetic.loseEffect(mPlayer);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private boolean isRecast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		if (ticks - mLastCastTicks <= 5) {
			return true;
		}
		mLastCastTicks = ticks;
		return false;
	}

	// Using this for UMM
	@Override
	public @NotNull Component getHotbarMessage() {
		final TextColor color = INFO.getActionBarColor();
		final String name = INFO.getHotbarName();
		int charges = getCharges();

		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (charges == 0 && mBloodlust != null) {
			output = output.append(mBloodlust.getStacks() >= mBloodlustCost ?
				Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD) :
				Component.text("x", NamedTextColor.RED, TextDecoration.BOLD)
			);
		} else {
			output = output.append(Component.text(charges + "s ",
				charges >= getMaxCharges() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
		}

		return output;
	}

	@Override
	public int getMaxCharges() {
		return mMaxDuration / 20;
	}

	@Override
	public int getCharges() {
		Effect rampage = mPlugin.mEffectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);
		return rampage != null ? rampage.getDuration() / 20 : 0;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	private static Description<Rampage> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Spend %d stacks of *Bloodlust* to deal damage to all").styles(Bloodlust.BLOODLUST_COLOR)
				.statValues(stat(a -> a.mBloodlustCost, BLOODLUST_COST))
			.addLine("nearby mobs and knock them back and enter a")
			.addLine("rampage for the next %t.")
				.statValues(stat(a -> a.mInitialDuration, INITIAL_DURATION))
			.addLine("(Rampage's damage doesn't contribute to Bloodlust)")
			.addLine()
			.addStat("Damage: %d1 (m)")
				.statValues(stat(a -> a.mDamage, DAMAGE_L1))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addLine()
			.addLine("While *Rampage* is active, you deal more melee").styles(UNDERLINED)
			.addLine("damage and are continuously healed, and gaining")
			.addLine("*Bloodlust* stacks extends its duration.").styles(Bloodlust.BLOODLUST_COLOR, UNDERLINED)
			.addLine()
			.addStat("Effect: +%p1 Melee Damage")
				.statValues(stat(a -> a.mDamageBuff, DAMAGE_PERCENT_L1))
			.addStat("Healing: %p HP every %t")
			.statValues(stat(a -> a.mHealing, HEAL_PERCENT), stat(5))
			.addStat("Duration Increase: +%t per stack (max +%t1)")
				.statValues(stat(a -> a.mDurationPerStack, DURATION_PER_STACK), stat(a -> a.mMaxBloodlustGain * a.mDurationPerStack, MAX_BLOODLUST_GAIN_L1 * DURATION_PER_STACK))
			.addDashedLine();
	}

	private static Description<Rampage> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Rampage*'s damage, damage boost,").styles(UNDERLINED)
			.addLine("and the number of times its duration can")
			.addLine("be increased.")
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (m)")
				.statValues(stat(DAMAGE_L1), stat(a -> a.mDamage, DAMAGE_L2))
			.addStatComparison("Effect: +%p1 -> +%p2 Melee Damage")
				.statValues(stat(DAMAGE_PERCENT_L1), stat(a -> a.mDamageBuff, DAMAGE_PERCENT_L2))
			.addStatComparison("Max Duration Increase: +%t1 -> +%t2")
				.statValues(stat(MAX_BLOODLUST_GAIN_L1 * DURATION_PER_STACK), stat(a -> a.mMaxBloodlustGain * a.mDurationPerStack, MAX_BLOODLUST_GAIN_L2 * DURATION_PER_STACK))
			.addLine()
			.addLine("Gain melee resistance while *Rampage* is active.").styles(UNDERLINED)
			.addLine()
			.addStat("Effect: +%p Melee Resistance")
				.statValues(stat(a -> a.mMeleeResistance, MELEE_RESISTANCE_PERCENT))
			.addDashedLine();
	}
}
