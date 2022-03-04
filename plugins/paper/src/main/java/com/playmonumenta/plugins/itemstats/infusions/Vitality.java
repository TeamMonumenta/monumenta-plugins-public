package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import java.util.Collection;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class Vitality implements Infusion {

	public static final String MODIFIER = "VitalityMod";
	private static final double HP_PCT_PER_LEVEL = 0.01;

	@Override
	public String getName() {
		return "Vitality";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.VITALITY;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		AttributeInstance ai = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (ai != null) {
			Collection<AttributeModifier> modTemp = ai.getModifiers();
			for (AttributeModifier mod : modTemp) {
				if (mod != null && mod.getName().equals(MODIFIER)) {
					ai.removeModifier(mod);
				}
			}
		}
		if (plugin.mItemStatManager.getInfusionLevel(player, InfusionType.VITALITY) > 0) {
			if (player != null) {
				double healthBoostPct = HP_PCT_PER_LEVEL * plugin.mItemStatManager.getInfusionLevel(player, InfusionType.VITALITY);
				AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (maxHealth != null) {
					AttributeModifier mod = new AttributeModifier(MODIFIER, healthBoostPct,
							AttributeModifier.Operation.MULTIPLY_SCALAR_1);
					maxHealth.addModifier(mod);
				}
			}
		}

		double maxHealth = EntityUtils.getMaxHealth(player);
		if (player.getHealth() > maxHealth) {
			player.setHealth(maxHealth);
		}
	}

}
