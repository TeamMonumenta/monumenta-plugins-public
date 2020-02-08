package com.playmonumenta.plugins.enchantments.evasions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.other.EvasionEnchant;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class MeleeEvasion implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Melee Evasion";
	private static final int EVASION_MELEE_THRESHOLD = 2;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR, ItemSlot.OFFHAND, ItemSlot.MAINHAND);
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, int level, EntityDamageByEntityEvent event) {
		//Add the extra location distance check because mob ability count as ENTITY_ATTACK for some reason.
		if (event.getCause() == DamageCause.ENTITY_ATTACK && EntityUtils.getRealFinalDamage(event) > 0
			&& event.getDamager().getBoundingBox().expand(EVASION_MELEE_THRESHOLD).contains(event.getEntity().getLocation().toVector())) {
			EvasionEnchant evasion = (EvasionEnchant) AbilityManager.getManager().getPlayerAbility(player, EvasionEnchant.class);
			evasion.mChance += (16 * level);
		}
	}

}
