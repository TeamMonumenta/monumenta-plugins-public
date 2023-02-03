package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils;

public class PotionDamage implements Attribute {
	@Override
	public String getName() {
		return "Potion Damage";
	}

	@Override
	public ItemStatUtils.AttributeType getAttributeType() {
		return ItemStatUtils.AttributeType.POTION_DAMAGE;
	}

	@Override
	public double getPriorityAmount() {
		return 2;
	}
}
