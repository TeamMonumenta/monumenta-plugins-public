package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SupportExpertise extends Ability {
	public static final double DAMAGE_BOOST = 0.04;

	public static final String CHARM_RADIUS = "Support Expertise Radius";

	public static final int RADIUS = 12;
	public static final double SELF_BOOST = 0.02;

	public static final AbilityInfo<SupportExpertise> INFO =
		new AbilityInfo<>(SupportExpertise.class, null, SupportExpertise::new)
			.canUse(player -> AbilityUtils.getSpecNum(player) == Shaman.SOOTHSAYER_ID);

	public SupportExpertise(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability != null && !ability.isFake() && event.getType() == DamageEvent.DamageType.MAGIC) {
			event.setFlatDamage(event.getFlatDamage() * (1 + AbilityUtils.getEffectiveTotalSpecPoints(mPlayer) * DAMAGE_BOOST));
		}
		return false;
	}
}
