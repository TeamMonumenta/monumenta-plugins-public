package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Fleetfooted extends Ability {
	private static final String SPEED_SRC = "FleetfootedSpeedModifier";

	private static final double SPEED_POTENCY = 0.1;
	private static final double FALL_DAMAGE_REDUCTION = 0.3;

	public static final String CHARM_SPEED = "Fleetfooted Speed";

	public static final AbilityInfo<Fleetfooted> INFO =
		new AbilityInfo<>(Fleetfooted.class, "Fleetfooted", Fleetfooted::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getClassNum(player) == Scout.CLASS_ID)
			.remove(Fleetfooted::removeMovementSpeed)
			.displayItem(Material.LEATHER);

	private final double mFallDamageDR;
	private double mSpeed;
	private @Nullable Swiftness mSwiftness;
	private boolean mWasInNoMobilityZone;

	public Fleetfooted(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mFallDamageDR = FALL_DAMAGE_REDUCTION;
		mSpeed = SPEED_POTENCY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mSwiftness = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Swiftness.class);
			if (mSwiftness != null) {
				mSpeed += mSwiftness.getFleetfootedBonus();
			}
			removeMovementSpeed(player);
			addMovementSpeed(player);
		});
	}

	public static Description<Fleetfooted> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Gain %p fall resistance and")
			.statValues(stat(a -> a.mFallDamageDR, FALL_DAMAGE_REDUCTION))
			.addLine("%p speed while outside of towns.")
			.statValues(stat(a -> a.mSpeed, SPEED_POTENCY));
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.FALL) {
			event.setFlatDamage(event.getDamage() * (1 - mFallDamageDR));
		}
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		final boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES);
		if (isLevelTwo()) {
			if (mWasInNoMobilityZone && !isInNoMobilityZone) {
				addMovementSpeed(mPlayer);
			} else if (!mWasInNoMobilityZone && isInNoMobilityZone) {
				removeMovementSpeed(mPlayer);
			}
		}

		mWasInNoMobilityZone = isInNoMobilityZone;
	}

	private void addMovementSpeed(final Player player) {
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
			new AttributeModifier(SPEED_SRC, mSpeed, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static void removeMovementSpeed(final Player player) {
		EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, SPEED_SRC);
	}

}
