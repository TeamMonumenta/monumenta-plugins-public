package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.infusions.Refresh;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

// On Consume : Refreshes 30% cooldown on all your abilities
public class TemporalBender implements Enchantment {

	public static final String CHARM_COOLDOWN = "Temporal Bender Cooldown";
	public static final String CHARM_COOLDOWN_REDUCTION = "Temporal Bender Cooldown Reduction";
	private static final int COOLDOWN = 20 * 30;
	private static final int COOLDOWN_REFRESH = 5 * 20;
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

			int cooldownRefresh = COOLDOWN_REFRESH + (int) (CharmManager.getLevel(player, CHARM_COOLDOWN_REDUCTION) * 20);
			plugin.mTimers.updateCooldowns(player, cooldownRefresh);

			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + 6, 20)));

			// Sound and Particles
			player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 2);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0, BLUE1_COLOR).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0, BLUE2_COLOR).spawnAsPlayerBuff(player);

			// Cooldowns
			int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));
		}
	}
}
