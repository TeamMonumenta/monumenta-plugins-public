package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SupportExpertise extends Ability {
	public static final double DAMAGE_BOOST = 0.04;

	public static final String CHARM_RADIUS = "Support Expertise Radius";

	public static final int RADIUS = 12;
	public static final double SELF_BOOST = 0.02;

	public static final AbilityInfo<SupportExpertise> INFO =
		new AbilityInfo<>(SupportExpertise.class, "Support Expertise", SupportExpertise::new)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Shaman.SOOTHSAYER_ID);

	public final double mRadius;

	public SupportExpertise(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability != null && !ability.isFake() && event.getType() == DamageEvent.DamageType.MAGIC) {
			event.setFlatDamage(event.getFlatDamage() * (1 + AbilityUtils.getEffectiveTotalSpecPoints(mPlayer) * DAMAGE_BOOST));
		}
		return false;
	}

	private static Description<SupportExpertise> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your abilities deal ")
			.addPercent(DAMAGE_BOOST)
			.add(" more damage per specialization point. Additionally, your Totemic Empowerment buffs are applied to other players within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of any of your totems and are ")
			.addPercent(SELF_BOOST)
			.add(" stronger for yourself.");
	}
}
