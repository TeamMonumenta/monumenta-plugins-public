package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SharedEmpowerment extends Ability {

	public static final int RADIUS = 10;
	public static final double SELF_BOOST_1 = 0.03;
	public static final double ALL_BOOST_2 = 0.05;

	public static final String CHARM_RADIUS = "Shared Empowerment Radius";
	public static final String CHARM_OTHER_SPEED = "Shared Empowerment Ally Speed";
	public static final String CHARM_OTHER_RESISTANCE = "Shared Empowerment Ally Damage Reduction";

	public static final AbilityInfo<SharedEmpowerment> INFO =
		new AbilityInfo<>(SharedEmpowerment.class, "Shared Empowerment", SharedEmpowerment::new)
			.linkedSpell(ClassAbility.SHARED_EMPOWERMENT)
			.scoreboardId("SharedEmpowerment")
			.shorthandName("SE")
			.descriptions(
				String.format(
					"All players within %s blocks of you receive the benefits of your passive Totemic Empowerment, and you receive a %s%% boost to the speed and damage reduction.",
					RADIUS,
					StringUtils.multiplierToPercentage(SELF_BOOST_1)
				),
				String.format(
					"The boost is increased to %s%% and affects all players in the radius.",
					StringUtils.multiplierToPercentage(ALL_BOOST_2)
				))
			.simpleDescription("Your Totemic Empowerment is now shared to those within a large radius of you.")
			.displayItem(Material.BEACON);

	public final double mRadius;
	public final double mSelfBoost;
	public final double mOtherSpeed;
	public final double mOtherResist;

	public SharedEmpowerment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mSelfBoost = isLevelOne() ? SELF_BOOST_1 : ALL_BOOST_2;
		double otherBoost = (isLevelOne() ? 0 : ALL_BOOST_2);
		mOtherSpeed = otherBoost + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_OTHER_SPEED);
		mOtherResist = otherBoost + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_OTHER_RESISTANCE);
	}

	public double getRadius() {
		return mRadius;
	}

	public double getSelfBoost() {
		return mSelfBoost;
	}

	public double getOtherSpeed() {
		return mOtherSpeed;
	}

	public double getOtherResist() {
		return mOtherResist;
	}
}
