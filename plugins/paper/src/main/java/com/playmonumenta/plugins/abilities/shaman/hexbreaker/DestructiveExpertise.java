package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DestructiveExpertise extends Ability {
	public static final double DAMAGE_BOOST = 0.05;

	public static final AbilityInfo<DestructiveExpertise> INFO =
		new AbilityInfo<>(DestructiveExpertise.class, null, DestructiveExpertise::new)
			.canUse(player -> AbilityUtils.getSpecNum(player) == Shaman.HEXBREAKER_ID);

	public DestructiveExpertise(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability != null && !ability.isFake() && event.getType() == DamageEvent.DamageType.MAGIC) {
			event.setDamage(event.getFlatDamage() * (1 + AbilityUtils.getEffectiveTotalSpecPoints(mPlayer) * DAMAGE_BOOST));
		}
		return false;
	}
}
