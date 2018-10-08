package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class BowMastery extends Ability {

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	@Override
	public boolean LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int bowMastery = getAbilityScore(player);
		if (arrow.isCritical()) {
			int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
			EntityUtils.damageEntity(mPlugin, damagee, bonusDamage, player);
		}
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 6;
		info.specId = -1;
		info.scoreboardId = "BowMastery";
		return info;
	}

}
