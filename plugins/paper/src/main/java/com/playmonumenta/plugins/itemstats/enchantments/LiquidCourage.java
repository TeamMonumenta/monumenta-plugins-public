package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CourageEffect;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
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

public class LiquidCourage implements Enchantment {

	private static final int DURATION = 20 * 15;
	private static final int COOLDOWN = 20 * 30;

	private static final double AMOUNT = 0.2;
	private static final int CHARGES = 3;
	private static final String COURAGE_EFFECT_SOURCE = "LiquidCourageEffect";
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.MELEE_ENCH,
		DamageEvent.DamageType.MELEE_SKILL,
		DamageEvent.DamageType.PROJECTILE,
		DamageEvent.DamageType.PROJECTILE_SKILL,
		DamageEvent.DamageType.MAGIC
	);

	public static final Material COOLDOWN_ITEM = Material.HONEYCOMB;

	@Override
	public String getName() {
		return "Liquid Courage";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.LIQUID_COURAGE;
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.LIQUID_COURAGE) > 0) {
			ItemStack item = event.getItem();
			String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			event.setCancelled(true);

			plugin.mEffectManager.addEffect(player, COURAGE_EFFECT_SOURCE, new CourageEffect(DURATION, AMOUNT, CHARGES, AFFECTED_DAMAGE_TYPES));

			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 8));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + 8, 20)));

			// TODO: Particles and Sound Effects

			// TODO: Charm Effects and Refresh Infusion
			int cooldown = COOLDOWN;
			player.setCooldown(COOLDOWN_ITEM, cooldown);
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, plugin));
		}
	}
}

