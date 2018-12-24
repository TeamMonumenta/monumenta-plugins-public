package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class MagePassive extends Ability {

	private static final double PASSIVE_DAMAGE = 1.5;

	public MagePassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = -1;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		//  Make sure only players trigger this.
		if (event.getDamager() instanceof Player) {
			Entity damagee = event.getEntity();

			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (InventoryUtils.isWandItem(mainHand)) {
				if (damagee instanceof LivingEntity) {
					event.setDamage(event.getDamage() + PASSIVE_DAMAGE);
				}
			}
		}
		return true;
	}
}
