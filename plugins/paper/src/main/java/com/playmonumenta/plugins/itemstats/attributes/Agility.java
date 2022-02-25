package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enchantments.Ethereal;
import com.playmonumenta.plugins.itemstats.enchantments.Evasion;
import com.playmonumenta.plugins.itemstats.enchantments.Reflexes;
import com.playmonumenta.plugins.itemstats.enchantments.Tempo;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Agility implements Attribute {

	@Override
	public String getName() {
		return "Agility";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.AGILITY;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// When there is zero (or negative) Armor, this method runs; otherwise, Armor runs.
		if (value > 0 && event.getType().isDefendable() && plugin.mItemStatManager.getAttributeAmount(player, AttributeType.ARMOR) <= 0) {
			boolean adaptability = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADAPTABILITY) > 0;
			double damageMultiplier = Armor.getDamageMultiplier(0, Armor.getSecondaryEnchantsMod(event, plugin, player),
				value, getSecondaryEnchantsMod(event, plugin, player),
				Armor.getSecondaryEnchantCap(), adaptability, 0, event.getType().isEnvironmental());
			event.setDamage(event.getDamage() * damageMultiplier);
		}
	}

	/**
	 * Gets the total modifier (0-based) of agility from secondary enchants for the given event and player.
	 */
	public static double getSecondaryEnchantsMod(DamageEvent event, Plugin plugin, Player player) {
		return Tempo.applyTempo(event, plugin, player)
			       + Reflexes.applyReflexes(event, plugin, player)
			       + Evasion.applyEvasion(event, plugin, player)
			       + Ethereal.applyEthereal(event, plugin, player);
	}

}
