package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.infusions.Refresh;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class TemporalBender implements Enchantment {
	public static final String CHARM_COOLDOWN = "Temporal Bender Cooldown";
	public static final String CHARM_COOLDOWN_REDUCTION = "Temporal Bender Cooldown Recharge Rate";
	private static final int FOOD_RESTORED = 4;
	private static final int FOOD_SATURATION_RESTORED = 4;
	private static final int COOLDOWN = TICKS_PER_SECOND * 30;
	private static final double COOLDOWN_RECHARGE_RATE = 1.0;
	private static final int COOLDOWN_RECHARGE_RATE_DURATION = TICKS_PER_SECOND * 5;
	public static final Material COOLDOWN_ITEM = Material.GLASS_BOTTLE;

	private static final Particle.DustOptions BLUE1_COLOR = new Particle.DustOptions(Color.fromRGB(0, 203, 230), 1.0f);
	private static final Particle.DustOptions BLUE2_COLOR = new Particle.DustOptions(Color.fromRGB(25, 79, 255), 1.0f);

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
	public void onConsume(final Plugin plugin, final Player player, final double level, final PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.TEMPORAL_BENDER) > 0) {
			final ItemStack item = event.getItem();
			final String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			event.setCancelled(true);
			final double cooldownRechargeRate = COOLDOWN_RECHARGE_RATE
				+ CharmManager.getLevelPercentDecimal(player, CHARM_COOLDOWN_REDUCTION);
			final int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));

			PlayerUtils.addFoodLevel(player, FOOD_RESTORED);
			PlayerUtils.addFoodSaturationLevel(player, FOOD_SATURATION_RESTORED);
			plugin.mEffectManager.addEffect(player, "TemporalBenderCooldownRechargeRate",
				new AbilityCooldownRechargeRate(COOLDOWN_RECHARGE_RATE_DURATION, cooldownRechargeRate));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()),
				new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));

			player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1, 2);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0)).count(10).delta(0.5).data(BLUE1_COLOR).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0)).count(10).delta(0.5).data(BLUE2_COLOR).spawnAsPlayerBuff(player);
		}
	}
}
