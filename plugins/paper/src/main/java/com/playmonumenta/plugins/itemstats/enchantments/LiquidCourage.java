package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CourageEffect;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.infusions.Quench;
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
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class LiquidCourage implements Enchantment {

	public static final String CHARM_COOLDOWN = "Liquid Courage Cooldown";
	public static final String CHARM_DURATION = "Liquid Courage Duration";
	public static final String CHARM_RESISTANCE = "Liquid Courage Resistance";
	public static final String CHARM_CHARGES = "Liquid Courage Charges";
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

	private static final Particle.DustOptions ORANGE1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 128, 0), 1.0f);
	private static final Particle.DustOptions ORANGE2_COLOR = new Particle.DustOptions(Color.fromRGB(214, 94, 47), 1.0f);

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

			int duration = (int) (CharmManager.getDuration(player, CHARM_DURATION, DURATION) * Quench.getDurationScaling(plugin, player));
			plugin.mEffectManager.addEffect(player, COURAGE_EFFECT_SOURCE, new CourageEffect(duration, CharmManager.calculateFlatAndPercentValue(player, CHARM_RESISTANCE, AMOUNT), (int) (CHARGES + CharmManager.getLevel(player, CHARM_CHARGES)), AFFECTED_DAMAGE_TYPES));

			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 8));
			player.setSaturation(Math.min(player.getFoodLevel(), Math.min(player.getSaturation() + 8, 20)));

			// Particles + Sound
			player.playSound(player.getLocation(), Sound.ENTITY_VINDICATOR_CELEBRATE, SoundCategory.PLAYERS, 1, 1);
			new PartialParticle(Particle.REDSTONE, player.getLocation(), 10, 0.25, 0.25, 0.25, 0.1, ORANGE1_COLOR).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.REDSTONE, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1, ORANGE2_COLOR).spawnAsPlayerBuff(player);

			// Set Cooldown
			int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));
		}
	}
}

