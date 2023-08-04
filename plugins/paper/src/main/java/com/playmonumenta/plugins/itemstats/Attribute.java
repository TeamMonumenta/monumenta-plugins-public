package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.itemstats.enums.AttributeType;

public interface Attribute extends ItemStat {

	/**
	 * A reference back to the associated AttributeType in ItemStatUtils.
	 *
	 * @return the associated AttributeType
	 */
	AttributeType getAttributeType();

}
