package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.EnergizingElixirStacks;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class EnergizingElixir extends Ability implements AbilityWithChargesOrStacks {

	private static final int COOLDOWN = 2 * 20;
	private static final int DURATION = 6 * 20;
	private static final double SPEED_AMPLIFIER_1 = 0.1;
	private static final double SPEED_AMPLIFIER_2 = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EnergizingElixirPercentSpeedEffect";
	private static final int JUMP_LEVEL = 1;
	private static final double DAMAGE_AMPLIFIER_2 = 0.1;
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "EnergizingElixirPercentDamageEffect";

	private static final String ENHANCED_STACKS_NAME = EnergizingElixirStacks.GENERIC_NAME;
	private static final double ENHANCED_BONUS = 0.03;
	private static final int ENHANCED_MAX_STACK = 4;

	public static final String CHARM_DURATION = "Energizing Elixir Effect Duration";
	public static final String CHARM_SPEED = "Energizing Elixir Speed Modifier";
	public static final String CHARM_JUMP_BOOST = "Energizing Elixir Jump Boost Modifier";
	public static final String CHARM_DAMAGE = "Energizing Elixir Damage Modifier";
	public static final String CHARM_BONUS = "Energizing Elixir Bonus Per Stack";
	public static final String CHARM_STACKS = "Energizing Elixir Max Stacks";
	public static final String CHARM_PRICE = "Energizing Elixir Potion Price";

	public static final AbilityInfo<EnergizingElixir> INFO =
		new AbilityInfo<>(EnergizingElixir.class, "Energizing Elixir", EnergizingElixir::new)
			.linkedSpell(ClassAbility.ENERGIZING_ELIXIR)
			.scoreboardId("EnergizingElixir")
			.shorthandName("EE")
			.descriptions(
				"Left click while holding an Alchemist's Bag to consume a potion to apply 10% Speed and Jump Boost 2 to yourself for 6s. Cooldown: 2s.",
				"Speed is increased to 20%; additionally, gain a 10% damage buff from all sources for the same duration.",
				"Recasting this ability while the buff is still active refreshes the duration and increases the damage bonus and speed by 3%, up to 4 stacks. Stacks decay every 6 seconds.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EnergizingElixir::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(new ItemStack(Material.RABBIT_FOOT, 1));

	private final double mSpeedAmp;
	private final int mDuration;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mStacks;
	private final int mMaxStacks;

	public EnergizingElixir(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSpeedAmp = (isLevelOne() ? SPEED_AMPLIFIER_1 : SPEED_AMPLIFIER_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mStacks = 0;
		mMaxStacks = isEnhanced() ? ENHANCED_MAX_STACK + (int) CharmManager.getLevel(mPlayer, CHARM_STACKS) : 0;
		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		if (MetadataUtils.happenedThisTick(mPlayer, AlchemistPotions.METADATA_KEY, -1)) {
			//this may be strange but sometime for some player, when they throw an Alch pot, elixir is randomly cast
			//Stopping elixir since was caused by potion
			return;
		}
		int price = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_PRICE);
		if (mAlchemistPotions == null || !mAlchemistPotions.decrementCharges(price)) {
			// If no charges, do not activate ability
			return;
		}

		if (isEnhanced()) {
			if (mPlugin.mEffectManager.hasEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME)) {
				mStacks = Math.min(mMaxStacks, mStacks + 1);
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCED_STACKS_NAME, new EnergizingElixirStacks(mDuration, mStacks));
			}
		}

		applyEffects();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		new PartialParticle(Particle.TOTEM, loc, 50, 1.5, 1, 1.5, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 1, 0);

		putOnCooldown();
	}

	private void applyEffects() {
		int duration = mDuration;
		if (mStacks > 1) {
			duration += 10; // to prevent gaps
		}
		double bonus = mStacks * (ENHANCED_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS));
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(duration, mSpeedAmp + bonus, PERCENT_SPEED_EFFECT_NAME));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, duration, JUMP_LEVEL + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST)));
		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(duration, DAMAGE_AMPLIFIER_2 + bonus + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE)));
		}
	}


	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0 && !mPlugin.mEffectManager.hasEffect(mPlayer, EnergizingElixirStacks.class)) {
			mStacks--;
			if (mStacks > 0) {
				mPlugin.mEffectManager.addEffect(mPlayer, ENHANCED_STACKS_NAME, new EnergizingElixirStacks(mDuration, mStacks));
			}
			applyEffects();
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

}
