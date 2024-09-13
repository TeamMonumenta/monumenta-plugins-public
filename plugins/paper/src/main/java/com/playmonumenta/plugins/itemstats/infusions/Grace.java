package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Grace implements Infusion {

	public static final double ATKS_BONUS = 0.015;
	private static final String MODIFIER_NAME = "GraceAttackSpeedModifier";

	@Override
	public String getName() {
		return "Grace";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.GRACE;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		double level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.GRACE);
		if (level > 0) {
			EntityUtils.replaceAttribute(player, Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(MODIFIER_NAME, level * ATKS_BONUS, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		} else {
			EntityUtils.removeAttribute(player, Attribute.GENERIC_ATTACK_SPEED, MODIFIER_NAME);
		}
	}
}
