package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.effects.ItemCooldown;
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

// On Consume : Refreshes 30% cooldown on all your abilities
public class TemporalBender implements Enchantment {

	private static final int COOLDOWN = 20 * 30;
	private static final double COOLDOWN_REFRESH = 0.3;
	public static final Material COOLDOWN_ITEM = Material.GLASS_BOTTLE;

	@Override
	public String getName() {
		return "Temporal Bender";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TEMPORAL_BENDER;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.TEMPORAL_BENDER) > 0) {
			ItemStack item = event.getItem();
			String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			event.setCancelled(true);

			for (Ability abil : AbilityManager.getManager().getPlayerAbilities(player).getAbilities()) {
				int cooldownReduction = (int) (abil.getModifiedCooldown() * COOLDOWN_REFRESH);
				plugin.mTimers.updateCooldown(player, abil.mInfo.mLinkedSpell, cooldownReduction);
			}

			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + 6, 20)));

			// TODO: Particles and Sound Effects

			// TODO: Charm Effects and Refresh Infusion
			int cooldown = COOLDOWN;
			player.setCooldown(COOLDOWN_ITEM, cooldown);
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, plugin));
		}
	}
}
