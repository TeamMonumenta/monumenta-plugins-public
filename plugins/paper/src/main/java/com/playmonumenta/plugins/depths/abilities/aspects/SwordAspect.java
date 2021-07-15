package com.playmonumenta.plugins.depths.abilities.aspects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class SwordAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Sword";
	public static final double DAMAGE = 1.5;

	public SwordAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.IRON_SWORD;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {

		if (event.getCause().equals(DamageCause.ENTITY_ATTACK) && InventoryUtils.isSwordItem(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + DAMAGE);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "You deal 1.5 extra damage with sword attacks.";
	}
}

