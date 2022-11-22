package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Versatile extends Ability {

	public static final float DAMAGE_MULTIPLY_MELEE = 0.50f;
	public static final float DAMAGE_MULTIPLY_PROJ = 0.40f;

	public static final AbilityInfo<Versatile> INFO =
		new AbilityInfo<>(Versatile.class, null, Versatile::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 6);

	public Versatile(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// Hunting Companion uses both melee and projectile scaling already
		if (event.getAbility() == ClassAbility.HUNTING_COMPANION) {
			return false;
		}

		if (event.getType() == DamageEvent.DamageType.MELEE || event.getType() == DamageEvent.DamageType.MELEE_SKILL || event.getType() == DamageEvent.DamageType.MELEE_ENCH) {
			double currentdamage = event.getDamage();
			double percentproj = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
			if (percentproj > 0) {
				event.setDamage(currentdamage * (1 + (percentproj - 1) * DAMAGE_MULTIPLY_MELEE));
			}
		} else if (event.getType() == DamageEvent.DamageType.PROJECTILE || event.getType() == DamageEvent.DamageType.PROJECTILE_SKILL) {
			double currentdamage = event.getDamage();
			double percentatk = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, ItemStatUtils.AttributeType.ATTACK_DAMAGE_MULTIPLY);
			if (percentatk > 0) {
				event.setDamage(currentdamage * (1 + (percentatk - 1) * DAMAGE_MULTIPLY_PROJ));
			}
		}
		return false; // no recursion possible as we only change the damage amount

	}
}
