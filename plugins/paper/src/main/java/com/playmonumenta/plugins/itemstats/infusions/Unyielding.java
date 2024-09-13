package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Unyielding implements Infusion {

	public static final String MODIFIER = "UnyieldingMod";
	public static final double KB_PER_LEVEL = 0.06;

	@Override
	public String getName() {
		return "Unyielding";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.UNYIELDING;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		double level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.UNYIELDING);
		if (level > 0) {
			EntityUtils.replaceAttribute(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(MODIFIER, getKnockbackResistance(level), AttributeModifier.Operation.ADD_NUMBER));
		} else {
			EntityUtils.removeAttribute(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, MODIFIER);
		}
	}

	public static double getKnockbackResistance(double level) {
		return KB_PER_LEVEL * level;
	}

}
