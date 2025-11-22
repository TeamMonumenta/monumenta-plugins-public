package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.ScorchedEarthCS;
import com.playmonumenta.plugins.effects.ScorchedEarthDamage;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

public class ScorchedEarth extends MultipleChargeAbility implements PotionAbility, AbilityWithDuration {
	private static final String SCORCHED_EARTH_POTION_METAKEY = "ScorchedEarthPotion";

	private static final int SCORCHED_EARTH_1_COOLDOWN = 20 * 30;
	private static final int SCORCHED_EARTH_2_COOLDOWN = 20 * 25;
	private static final int SCORCHED_EARTH_1_CHARGES = 1;
	private static final int SCORCHED_EARTH_2_CHARGES = 2;
	private static final int SCORCHED_EARTH_DURATION = 20 * 15;
	private static final int SCORCHED_EARTH_FIRE_DURATION = 20 * 4;
	public static final double SCORCHED_EARTH_DAMAGE_FRACTION = 0.25;
	private static final double SCORCHED_EARTH_RADIUS = 5;
	private static final String SCORCHED_EARTH_EFFECT_NAME = "ScorchedEarthDamageEffect";

	public static final String CHARM_COOLDOWN = "Scorched Earth Cooldown";
	public static final String CHARM_CHARGES = "Scorched Earth Charge";
	public static final String CHARM_DURATION = "Scorched Earth Duration";
	public static final String CHARM_RADIUS = "Scorched Earth Radius";
	public static final String CHARM_DAMAGE = "Scorched Earth Damage";
	public static final String CHARM_FIRE_DURATION = "Scorched Earth Fire Duration";

	public static final AbilityInfo<ScorchedEarth> INFO =
		new AbilityInfo<>(ScorchedEarth.class, "Scorched Earth", ScorchedEarth::new)
			.linkedSpell(ClassAbility.SCORCHED_EARTH)
			.scoreboardId("ScorchedEarth")
			.shorthandName("SE")
			.actionBarColor(TextColor.color(230, 134, 0))
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deploy a circular zone in which enemies take extra damage based on your potion damage.")
			.cooldown(SCORCHED_EARTH_1_COOLDOWN, SCORCHED_EARTH_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.BROWN_DYE);

	private final List<Instance> mActiveInstances = new ArrayList<>();

	private record Instance(Location mLocation, int mEndTick, PlayerItemStats mStats, ScorchedEarthCS mCosmetic) {
	}

	private final int mDuration;
	private final double mRadius;
	private final int mFireDuration;
	private final double mDamageMult;
	private int mLastCastTicks = 0;
	private final ScorchedEarthCS mCosmetic;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mCurrDuration = -1;

	public ScorchedEarth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (isLevelOne() ? SCORCHED_EARTH_1_CHARGES : SCORCHED_EARTH_2_CHARGES) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SCORCHED_EARTH_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, SCORCHED_EARTH_RADIUS);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, SCORCHED_EARTH_FIRE_DURATION);
		mDamageMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, SCORCHED_EARTH_DAMAGE_FRACTION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ScorchedEarthCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mAlchemistPotions = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class));
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mAlchemistPotions == null) {
			mActiveInstances.clear();
			return;
		}
		manageChargeCooldowns();

		for (Iterator<Instance> iterator = mActiveInstances.iterator(); iterator.hasNext(); ) {
			final Instance instance = iterator.next();
			final int timeRemaining = instance.mEndTick() - Bukkit.getCurrentTick();

			if (timeRemaining <= 0) {
				iterator.remove();
			} else {
				instance.mCosmetic().activeEffects(mPlayer, instance.mLocation(), mRadius, timeRemaining, mDuration);
				final double damage = mDamageMult * mAlchemistPotions.getDamage(instance.mStats());
				final Hitbox hitbox = new Hitbox.SphereHitbox(instance.mLocation(), mRadius);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					mPlugin.mEffectManager.addEffect(mob, SCORCHED_EARTH_EFFECT_NAME, new ScorchedEarthDamage(10, damage, mPlayer, instance.mStats(), mFireDuration, mCosmetic));
				}
			}
		}

		if (mCurrDuration >= mDuration) {
			mCurrDuration = -1;
			ClientModHandler.updateAbility(mPlayer, this);
		}

		if (mCurrDuration >= 0) {
			mCurrDuration += 5;
		}
	}

	@Override
	public void alchemistPotionThrown(ThrownPotion potion) {
		if (!mPlayer.isSneaking()) {
			return;
		}
		final int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;
		potion.setMetadata(SCORCHED_EARTH_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, PlayerItemStats playerItemStats) {
		if (mAlchemistPotions != null && potion.hasMetadata(SCORCHED_EARTH_POTION_METAKEY)) {
			loc.setDirection(loc.toVector().subtract(mPlayer.getLocation().toVector()));
			mCosmetic.landEffects(mPlayer, loc, mRadius, mDuration);
			final ScorchedEarthCS activeCosmetic = mCosmetic.copyForActiveInstance();
			// immediately do periodic effects too (the next ordinary execution may be delayed by up to 5 ticks)
			activeCosmetic.activeEffects(mPlayer, loc, mRadius, mDuration, mDuration);
			mActiveInstances.add(new Instance(loc, Bukkit.getCurrentTick() + mDuration, playerItemStats, activeCosmetic));
			return true;
		}
		return false;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<ScorchedEarth> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Throw an Alchemist's Potion while sneaking to deploy a ")
			.add(a -> a.mRadius, SCORCHED_EARTH_RADIUS)
			.add(" block radius zone that lasts ")
			.addDuration(a -> a.mDuration, SCORCHED_EARTH_DURATION)
			.add(" seconds where the potion lands. Mobs in this zone are dealt ")
			.addPercent(a -> a.mDamageMult, SCORCHED_EARTH_DAMAGE_FRACTION)
			.add(" of your potion's damage and set on fire for ")
			.addDuration(a -> a.mFireDuration, SCORCHED_EARTH_FIRE_DURATION)
			.add(" seconds whenever taking damage of types other than ailment or fire.")
			.addCooldown(SCORCHED_EARTH_1_COOLDOWN, Ability::isLevelOne);
	}

	private static Description<ScorchedEarth> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Charges: ")
			.add(a -> a.mMaxCharges, SCORCHED_EARTH_2_CHARGES, false, Ability::isLevelTwo)
			.add(".")
			.addCooldown(SCORCHED_EARTH_2_COOLDOWN, Ability::isLevelTwo);
	}
}
