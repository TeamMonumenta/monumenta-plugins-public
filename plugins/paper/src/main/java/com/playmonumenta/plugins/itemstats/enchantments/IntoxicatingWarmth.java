package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.effects.WarmthEffect;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class IntoxicatingWarmth implements Enchantment {

	private static final String WARMTH_EFFECT_SOURCE = "IntoxicatingWarmthEffect";
	private static final int DURATION = 20 * 15;
	private static final int COOLDOWN = 20 * 25;
	public static final Material COOLDOWN_ITEM = Material.CLAY_BALL;

	@Override
	public String getName() {
		return "Intoxicating Warmth";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INTOXICATING_WARMTH;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.INTOXICATING_WARMTH) > 0) {
			ItemStack item = event.getItem();
			String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			plugin.mEffectManager.addEffect(player, WARMTH_EFFECT_SOURCE, new WarmthEffect(DURATION));
			event.setCancelled(true);

			// TODO: Particles and Sound Effects

			// TODO: Charm Effects and Refresh Infusion
			int cooldown = COOLDOWN;
			player.setCooldown(COOLDOWN_ITEM, cooldown);
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, plugin));
		}
	}
}
