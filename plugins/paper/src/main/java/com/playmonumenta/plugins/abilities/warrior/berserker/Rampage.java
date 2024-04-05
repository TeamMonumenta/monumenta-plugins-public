package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.RampageCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Rampage extends Ability implements AbilityWithChargesOrStacks {

	private static final int RAMPAGE_STACK_DECAY_TIME = 20 * 5;
	private static final int RAMPAGE_1_DAMAGE_PER_STACK = 50;
	private static final int RAMPAGE_2_DAMAGE_PER_STACK = 35;
	private static final double R3_DAMAGE_PER_STACK_MULTIPLIER = 2;
	private static final int ACTIVE_MIN_STACKS = 10;
	private static final int RAMPAGE_1_STACK_LIMIT = 15;
	private static final int RAMPAGE_2_STACK_LIMIT = 20;
	private static final double RAMPAGE_DAMAGE_RESISTANCE_PER_STACK = 0.0075;
	private static final double RAMPAGE_RADIUS = 4;
	private static final double HEAL_PERCENT = 0.025;
	private static final double RAMPAGE_STACK_PERCENTAGE = 1.5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "RampagePercentDamageResistEffect";
	private static final String CUSTOM_REGENERATION_EFFECT_NAME = "RampageCustomRegenerationEffect";

	public static final String CHARM_THRESHOLD = "Rampage Damage Threshold";
	public static final String CHARM_DAMAGE = "Rampage Damage";
	public static final String CHARM_STACKS = "Rampage Max Stacks";
	public static final String CHARM_RADIUS = "Rampage Range";
	public static final String CHARM_REDUCTION_PER_STACK = "Rampage Resistance Per Stack";
	public static final String CHARM_HEALING = "Rampage Healing";

	public static final AbilityInfo<Rampage> INFO =
		new AbilityInfo<>(Rampage.class, "Rampage", Rampage::new)
			.linkedSpell(ClassAbility.RAMPAGE)
			.scoreboardId("Rampage")
			.shorthandName("Rmp")
			.descriptions(
				("Gain a stack of rage for each %s melee damage dealt (%s%% more in Region 3). Stacks decay by 1 every %s seconds of not dealing melee damage and cap at %s. " +
					 "Passively gain %s%% damage resistance for each stack. " +
					 "When at %s or more stacks, right click while looking down to consume all stacks and deal %s times the number of stacks consumed melee damage to " +
					 "mobs within %s blocks. For the next (stacks consumed / 2) seconds, " +
					 "heal %s%% of max health per second and keep your passive damage reduction.")
					.formatted(
						RAMPAGE_1_DAMAGE_PER_STACK,
						StringUtils.multiplierToPercentage(R3_DAMAGE_PER_STACK_MULTIPLIER - 1),
						StringUtils.ticksToSeconds(RAMPAGE_STACK_DECAY_TIME),
						RAMPAGE_1_STACK_LIMIT,
						StringUtils.multiplierToPercentage(RAMPAGE_DAMAGE_RESISTANCE_PER_STACK),
						ACTIVE_MIN_STACKS,
						StringUtils.to2DP(RAMPAGE_RADIUS),
						RAMPAGE_STACK_PERCENTAGE,
						StringUtils.multiplierToPercentage(HEAL_PERCENT)
					),
				"Gain a stack of rage for each %s melee damage dealt, with stacks capping at %s."
					.formatted(
						RAMPAGE_2_DAMAGE_PER_STACK,
						RAMPAGE_2_STACK_LIMIT
					))
			.simpleDescription("Dealing damage grants rage stacks that give resistance, which can be consume to deal area damage and heal yourself.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Rampage::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.DOWN)
				                                                                    .keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLAZE_POWDER);

	private final double mDamagePerStack;
	private final double mResistancePerStack;
	private final int mStackLimit;

	private int mStacks = 0;
	private double mRemainderDamage = 0;
	private int mTimeToStackDecay = 0;

	private final RampageCS mCosmetic;

	public Rampage(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePerStack = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_THRESHOLD, isLevelOne() ? RAMPAGE_1_DAMAGE_PER_STACK : RAMPAGE_2_DAMAGE_PER_STACK) * (ServerProperties.getAbilityEnhancementsEnabled(mPlayer) ? R3_DAMAGE_PER_STACK_MULTIPLIER : 1);
		mResistancePerStack = -(RAMPAGE_DAMAGE_RESISTANCE_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION_PER_STACK));
		mStackLimit = (isLevelOne() ? RAMPAGE_1_STACK_LIMIT : RAMPAGE_2_STACK_LIMIT) + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new RampageCS());
	}

	public boolean cast() {
		if (mStacks >= ACTIVE_MIN_STACKS) {
			World world = mPlayer.getWorld();
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mStacks * RAMPAGE_STACK_PERCENTAGE);
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RAMPAGE_RADIUS));
			for (LivingEntity mob : hitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.getLinkedSpell(), true);
				mCosmetic.onHitMob(mPlayer, mob);
			}

			mPlugin.mEffectManager.addEffect(mPlayer, CUSTOM_REGENERATION_EFFECT_NAME, new CustomRegeneration(mStacks * 10, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT * EntityUtils.getMaxHealth(mPlayer)), mPlugin));
			addDamageReductionEffect(false);

			Location loc = mPlayer.getLocation();
			mCosmetic.onCast(mPlayer, loc, world);

			mStacks = 0;
			showChargesMessage();
			ClientModHandler.updateAbility(mPlayer, this);
			return true;
		}
		return false;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTimeToStackDecay += 5;

			if (mTimeToStackDecay >= RAMPAGE_STACK_DECAY_TIME) {
				mTimeToStackDecay = 0;
				mStacks--;
				if (mStacks > 0) {
					addDamageReductionEffect(true);
				}
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if ((event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH)
				&& event.getAbility() != ClassAbility.RAMPAGE) {
			damageDealt(event.getFinalDamage(false));
		}
		return false; // does not deal damage, just tallies the damage dealt
	}

	private void damageDealt(double damage) {
		mTimeToStackDecay = 0;

		mRemainderDamage += damage;
		int newStacks = (int) Math.floor(mRemainderDamage / mDamagePerStack);
		mRemainderDamage -= (newStacks * mDamagePerStack);

		if (newStacks > 0) {
			int previousStacks = mStacks;
			mStacks = Math.min(mStackLimit, mStacks + newStacks);
			showChargesMessage();
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		if (mStacks > 0) {
			addDamageReductionEffect(true);
		}
	}

	private void addDamageReductionEffect(boolean passive) {
		int duration = passive ? RAMPAGE_STACK_DECAY_TIME : mStacks * 10;
		double resistance = mStacks * mResistancePerStack;
		if (!passive) {
			// Clear old effects so the .displaysTime(false) isn't carried over
			mPlugin.mEffectManager.clearEffects(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME);
		}
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(duration, resistance).displaysTime(!passive));
	}

	@Override
	public void showChargesMessage() {
		sendActionBarMessage("Rage: " + mStacks);
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mStackLimit;
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
}
