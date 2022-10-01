package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.effects.WarmthEffect;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class IntoxicatingWarmth implements Enchantment {

	private static final String WARMTH_EFFECT_SOURCE = "IntoxicatingWarmthEffect";
	public static final String CHARM_COOLDOWN = "Intoxicating Warmth Cooldown";
	public static final String CHARM_DURATION = "Intoxicating Warmth Duration";
	public static final String CHARM_SATURATION = "Intoxicating Warmth Food And Saturation";
	private static final int DURATION = 20 * 15;
	private static final int COOLDOWN = 20 * 30;
	public static final Material COOLDOWN_ITEM = Material.CLAY_BALL;

	private static final Particle.DustOptions PEACH_COLOR = new Particle.DustOptions(Color.fromRGB(255, 158, 97), 1.0f);
	private static final Particle.DustOptions YELLOW_COLOR = new Particle.DustOptions(Color.fromRGB(204, 197, 63), 1.0f);

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

			int duration = DURATION + CharmManager.getExtraDuration(player, CHARM_DURATION);
			double amount = 1 + CharmManager.getLevelPercent(player, CHARM_SATURATION);

			plugin.mEffectManager.addEffect(player, WARMTH_EFFECT_SOURCE, new WarmthEffect(duration, (float) amount));
			event.setCancelled(true);

			// Initial Eating Sounds
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					switch (mT) {
						case 0:
						case 2:
							player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0F, 2);
							break;
						case 1:
							player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0F, 1.5F);
							break;
						default:
							this.cancel();
					}
					mT++;
				}
			}.runTaskTimer(plugin, 0, 5);

			// Particles
			plugin.mEffectManager.addEffect(player, "IntoxicatingWarmthParticles", new Aesthetics(DURATION,
					(entity, fourHertz, twoHertz, oneHertz) -> {
						// Tick effect
						Location loc = player.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, YELLOW_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, YELLOW_COLOR).spawnAsPlayerBuff(player);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, PEACH_COLOR).spawnAsPlayerBuff(player);
					}, (entity) -> {
					// Lose effect
					Location loc = player.getLocation();
					player.playSound(loc, Sound.ENTITY_LLAMA_EAT, 1f, 0.5f);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, PEACH_COLOR).spawnAsPlayerBuff(player);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, YELLOW_COLOR).spawnAsPlayerBuff(player);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, PEACH_COLOR).spawnAsPlayerBuff(player);
				})
			);

			// Set Cooldown
			int cooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, Refresh.reduceCooldown(plugin, player, COOLDOWN));
			plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(cooldown, item, COOLDOWN_ITEM, plugin));
		}
	}
}
