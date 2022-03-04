package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;

public interface Enchantment extends ItemStat {

	/**
	 * A reference back to the associated EnchantmentType in ItemStatUtils.
	 *
	 * @return the associated EnchantmentType
	 */
	EnchantmentType getEnchantmentType();

	/**
	 * Slots that the Enchantment applies in. Defaults to all slots.
	 *
	 * @return an EnumSet of the Slots the Enchantment applies in
	 */
	default EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET);
	}

}
