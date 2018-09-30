package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class RoguePassive extends Ability {
	
	private static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 2.0;
	private static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.5;
	
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) { 
		//  Make sure only players trigger this.
		if (event.getDamager() instanceof Player) {
			Entity damagee = event.getEntity();

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();
			if (InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand)) {
				//  This test if the damagee is an instance of a Elite.
				if (damagee instanceof LivingEntity && EntityUtils.isElite(event.getEntity())) {
					event.setDamage(event.getDamage() * PASSIVE_DAMAGE_ELITE_MODIFIER);
				} else if (damagee instanceof LivingEntity && EntityUtils.isBoss(event.getEntity())) {
					event.setDamage(event.getDamage() * PASSIVE_DAMAGE_BOSS_MODIFIER);
				}
			}
			Bukkit.broadcastMessage("rogue passive success.");
		}
		return true; 
	}
	
	@Override
	public AbilityInfo getInfo() { 
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 4;
		info.specId = -1;
		return info; 
	}

}
