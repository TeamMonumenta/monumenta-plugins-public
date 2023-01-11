package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Rampage extends Ability implements AbilityWithChargesOrStacks {

	private static final int RAMPAGE_STACK_DECAY_TIME = 20 * 5;
	private static final int RAMPAGE_1_DAMAGE_PER_STACK = 50;
	private static final int RAMPAGE_2_DAMAGE_PER_STACK = 35;
	private static final double R3_DAMAGE_PER_STACK_MULTIPLIER = 1.5;
	private static final int ACTIVE_MIN_STACKS = 10;
	private static final int RAMPAGE_1_STACK_LIMIT = 15;
	private static final int RAMPAGE_2_STACK_LIMIT = 20;
	private static final double RAMPAGE_DAMAGE_RESISTANCE_PER_STACK = 0.01;
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
				("Gain a stack of rage for each %s melee damage dealt (50%% more in region 3). Stacks decay by 1 every %s seconds of not dealing melee damage and cap at %s. " +
					 "Passively gain %s%% damage resistance for each stack. " +
					 "When at %s or more stacks, right click while looking down to consume all stacks and damage mobs " +
					 "in a %s block radius by %s times the number of stacks consumed. For the next (stacks consumed / 2) seconds, " +
					 "heal %s%% of max health per second and keep your passive damage reduction.")
					.formatted(RAMPAGE_1_DAMAGE_PER_STACK, StringUtils.ticksToSeconds(RAMPAGE_STACK_DECAY_TIME), RAMPAGE_1_STACK_LIMIT,
						StringUtils.multiplierToPercentage(RAMPAGE_DAMAGE_RESISTANCE_PER_STACK), ACTIVE_MIN_STACKS, RAMPAGE_RADIUS, RAMPAGE_STACK_PERCENTAGE,
						StringUtils.multiplierToPercentage(HEAL_PERCENT)),
				"Gain a stack of rage for each %s melee damage dealt, with stacks capping at %s."
					.formatted(RAMPAGE_2_DAMAGE_PER_STACK, RAMPAGE_2_STACK_LIMIT))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Rampage::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.DOWN)
				                                                                    .keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(new ItemStack(Material.BLAZE_POWDER, 1));

	private final double mDamagePerStack;
	private final int mStackLimit;

	private int mStacks = 0;
	private double mRemainderDamage = 0;
	private int mTimeToStackDecay = 0;

	public Rampage(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePerStack = ((isLevelOne() ? RAMPAGE_1_DAMAGE_PER_STACK : RAMPAGE_2_DAMAGE_PER_STACK) + CharmManager.getLevel(mPlayer, CHARM_THRESHOLD)) * (ServerProperties.getAbilityEnhancementsEnabled() ? R3_DAMAGE_PER_STACK_MULTIPLIER : 1);
		mStackLimit = (isLevelOne() ? RAMPAGE_1_STACK_LIMIT : RAMPAGE_2_STACK_LIMIT) + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS);
	}

	public void cast() {
		if (mStacks >= ACTIVE_MIN_STACKS) {
			World world = mPlayer.getWorld();
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mStacks * RAMPAGE_STACK_PERCENTAGE);
			Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RAMPAGE_RADIUS));
			for (LivingEntity mob : hitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.getLinkedSpell());
				new PartialParticle(Particle.VILLAGER_ANGRY, mob.getLocation(), 5, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);
			}

			mPlugin.mEffectManager.addEffect(mPlayer, CUSTOM_REGENERATION_EFFECT_NAME, new CustomRegeneration(mStacks * 10, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT * EntityUtils.getMaxHealth(mPlayer)), mPlugin));
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(mStacks * 10, getDamageResistanceRatio() - 1));

			Location loc = mPlayer.getLocation();
			new PartialParticle(Particle.EXPLOSION_HUGE, loc, 3, 0.2, 0.2, 0.2, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 50, 3, 1, 3, 0).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, mStacks * 0.4f, 0.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, mStacks * 0.4f, 1.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, mStacks * 0.4f, 2);

			mStacks = 0;
			MessagingUtils.sendActionBarMessage(mPlayer, "Rage: " + mStacks);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTimeToStackDecay += 5;

			if (mTimeToStackDecay >= RAMPAGE_STACK_DECAY_TIME) {
				mTimeToStackDecay = 0;
				mStacks--;
				MessagingUtils.sendActionBarMessage(mPlayer, "Rage: " + mStacks);
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
		int newStacks = (int) (mRemainderDamage / mDamagePerStack);
		mRemainderDamage -= (newStacks * mDamagePerStack);

		if (newStacks > 0) {
			int previousStacks = mStacks;
			mStacks = Math.min(mStackLimit, mStacks + newStacks);
			MessagingUtils.sendActionBarMessage(mPlayer, "Rage: " + mStacks);
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		event.setDamage(event.getDamage() * getDamageResistanceRatio());
	}

	private double getDamageResistanceRatio() {
		return 1 - mStacks * (RAMPAGE_DAMAGE_RESISTANCE_PER_STACK + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION_PER_STACK));
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mStackLimit;
	}

}
