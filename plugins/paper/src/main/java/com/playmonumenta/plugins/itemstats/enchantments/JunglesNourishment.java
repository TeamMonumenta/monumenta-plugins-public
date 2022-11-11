package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.infusions.Quench;
import com.playmonumenta.plugins.itemstats.infusions.Refresh;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class JunglesNourishment implements Enchantment {

	private static final double HEAL_PERCENT = 0.4;
	private static final int DURATION = 20 * 5;
	private static final double PERCENT_DAMAGE_RECEIVED = -0.2;
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "JunglesNourishmentResistance";
	private static final int COOLDOWN = 20 * 25;
	public static final Material COOLDOWN_ITEM = Material.MELON_SEEDS;

	public static final String CHARM_COOLDOWN = "Jungle's Nourishment Cooldown";
	public static final String CHARM_HEALTH = "Jungle's Nourishment Health";
	public static final String CHARM_RESISTANCE = "Jungle's Nourishment Resistance";
	public static final String CHARM_DURATION = "Jungle's Nourishment Duration";

	@Override
	public String getName() {
		return "Jungles Nourishment";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.JUNGLES_NOURISHMENT;
	}

	@Override public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND, ItemStatUtils.Slot.OFFHAND);
	}

	@Override
	public void onConsume(Plugin plugin, Player player, double level, PlayerItemConsumeEvent event) {
		if (ItemStatUtils.getEnchantmentLevel(event.getItem(), EnchantmentType.JUNGLES_NOURISHMENT) > 0) {
			ItemStack item = event.getItem();
			String source = ItemCooldown.toSource(getEnchantmentType());
			if (plugin.mEffectManager.hasEffect(player, source)) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(item) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
				return;
			}

			double healPercent = HEAL_PERCENT + CharmManager.getLevelPercentDecimal(player, CHARM_HEALTH);
			PlayerUtils.healPlayer(plugin, player, healPercent * EntityUtils.getMaxHealth(player), player);

			plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived((int)((DURATION + CharmManager.getExtraDuration(player, CHARM_DURATION)) * Quench.getDurationScaling(plugin, player)), PERCENT_DAMAGE_RECEIVED - CharmManager.getLevelPercentDecimal(player, CHARM_RESISTANCE)));

			int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));
			player.setFoodLevel(24);
			World world = player.getWorld();
			world.spawnParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1);
			world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1);
			world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1.25f);
		}
	}
}
