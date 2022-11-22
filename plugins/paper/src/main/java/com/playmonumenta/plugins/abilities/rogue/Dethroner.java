package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Dethroner extends Ability {

	public static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 1.3;
	public static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.15;

	public static final AbilityInfo<Dethroner> INFO =
		new AbilityInfo<>(Dethroner.class, null, Dethroner::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0) == 4);

	public Dethroner(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			//  This test if the damagee is an  Elite.
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
