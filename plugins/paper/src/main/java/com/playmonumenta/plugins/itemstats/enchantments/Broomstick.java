package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.RiptideDisable;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.EnumSet;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;

public class Broomstick implements Enchantment {

	private static final String RIPTIDE_DISABLE_EFFECT = "BroomstickDisable";
	private static final int DISABLE_DURATION = 1000 * 20;

	@Override
	public String getName() {
		return "Broomstick";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.BROOMSTICK;
	}

	@Override
	public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND, ItemStatUtils.Slot.OFFHAND);
	}

	@Override
	public void onRiptide(Plugin plugin, Player player, double level, PlayerRiptideEvent event) {
		plugin.mEffectManager.addEffect(player, RIPTIDE_DISABLE_EFFECT, new RiptideDisable(DISABLE_DURATION));
	}
}
