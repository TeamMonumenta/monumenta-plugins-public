package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;

import java.util.Random;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;

public class MagePassive extends Ability {

	private static final double PASSIVE_DAMAGE = 1.5;

	public MagePassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = -1;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		//  Make sure only players trigger this.
		if (event.getDamager() instanceof Player) {
			Entity damagee = event.getEntity();

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (InventoryUtils.isWandItem(mainHand)) {
				if (damagee instanceof LivingEntity) {
					event.setDamage(event.getDamage() + PASSIVE_DAMAGE);
				}
			}
		}
		return true;
	}
}
