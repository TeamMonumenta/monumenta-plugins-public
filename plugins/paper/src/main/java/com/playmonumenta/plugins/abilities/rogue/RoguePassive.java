package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class RoguePassive extends Ability {

	public static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 1.3;
	public static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.15;

	public RoguePassive(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 4;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			//  This test if the damagee is an instance of a Elite.
			if (enemy != null) {
				if (EntityUtils.isElite(enemy)) {
					event.setDamage(event.getDamage() * PASSIVE_DAMAGE_ELITE_MODIFIER);
				} else if (EntityUtils.isBoss(enemy)) {
					event.setDamage(event.getDamage() * PASSIVE_DAMAGE_BOSS_MODIFIER);
				}
			}
		}
		return false; // increases event damage and does not cause another damage instance, so no recursion
	}
}
