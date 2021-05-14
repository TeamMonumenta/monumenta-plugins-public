package com.playmonumenta.plugins.attributes;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;

/* Ability Power Attribute:
 * +X Ability Power
 * Custom scaling to increase class ability damage.
*/
public class AttributeAbilityPower implements BaseAttribute {
	private static final String PROPERTY_NAME = "Ability Power";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.CUSTOM) {
			double originalDamage = event.getDamage();
			double modifiedDamage = originalDamage + (0.1 * Math.pow(originalDamage, ((double)2/3)) * value);
			event.setDamage(modifiedDamage);
		}
	}
}
