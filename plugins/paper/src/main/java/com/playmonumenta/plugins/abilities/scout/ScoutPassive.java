package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/*
 * Scout Passive:
 */

public class ScoutPassive extends Ability {

	private static final float DAMAGE_MULTIPLY_MELEE = 0.2f;
	private static final float DAMAGE_MULTIPLY_PROJ = 0.2f;

	public ScoutPassive(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 6;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
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
		return true;

	}
}
