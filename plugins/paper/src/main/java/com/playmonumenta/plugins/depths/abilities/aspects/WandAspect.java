package com.playmonumenta.plugins.depths.abilities.aspects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.enchantments.abilities.SpellPower;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class WandAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Wand";
	public static final int DAMAGE = 1;
	public static final double SPELL_MOD = 0.25;

	public WandAspect(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.STICK;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {

		if (event.getCause().equals(DamageCause.ENTITY_ATTACK) && InventoryUtils.isWandItem(mPlayer.getInventory().getItemInMainHand())) {
			event.setDamage(event.getDamage() + DAMAGE);
		}

		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {

		if (InventoryUtils.isWandItem(mPlayer.getInventory().getItemInMainHand())) {
			double initialDamage = event.getDamage();

			//Find out what the damage with full spell power would be here
			float fullDamage = SpellPower.getSpellDamage(mPlayer, (float) initialDamage);

			//Get the difference, divide it by 4 and add it to the damage
			event.setDamage(initialDamage + ((fullDamage - initialDamage) * SPELL_MOD));
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "You deal " + DAMAGE + " extra damage with wand attacks, and all abilities casted with a wand benefit from " + (int) (SPELL_MOD * 100) + "% of the wands spell power.";
	}
}

