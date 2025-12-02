package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;

public class PotionRechargeRate implements Attribute {
	@Override
	public String getName() {
		return "Potion Recharge Rate";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.POTION_RECHARGE_RATE;
	}

	@Override
	public double getPriorityAmount() {
		return 1998;
	}
}
