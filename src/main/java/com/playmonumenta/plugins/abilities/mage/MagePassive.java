package com.playmonumenta.plugins.abilities.mage;

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

public class MagePassive extends Ability {

	private static final double PASSIVE_DAMAGE = 1.5;

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		//  Make sure only players trigger this.
		if (event.getDamager() instanceof Player) {
			Entity damagee = event.getEntity();

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (InventoryUtils.isWandItem(mainHand)) {
				if (damagee instanceof LivingEntity) {
					EntityUtils.damageEntity(mPlugin, (LivingEntity)damagee, PASSIVE_DAMAGE, player);
				}
			}
			Bukkit.broadcastMessage("DEBUG: mage passive success.");
		}
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		return info;
	}
}
