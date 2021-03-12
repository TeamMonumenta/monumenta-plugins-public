package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Location;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class RageOfTheKeter implements BaseEnchantment {

	private static final String PROPERTY_NAME = ChatColor.GRAY + "Rage of the Keter";

	private static final double DAMAGE_PERCENT = 0.15;
	private static final double SPEED_PERCENT = 0.15;
	private static final int DURATION = 20 * 10;
	private static final int COOLDOWN = 20 * 25;
	private static final String ATTR_NAME = "KeterExtraSpeedAttr";
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.ENTITY_SWEEP_ATTACK
	);
	
	private static final Particle.DustOptions OLIVE_COLOR = new Particle.DustOptions(Color.fromRGB(128, 128, 0), 1.0f);
	private static final Particle.DustOptions GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(64, 128, 0), 1.0f);
	
	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event, int level) {
		if (InventoryUtils.testForItemWithLore(event.getItem(), "Rage of the Keter")) {
			player.setCooldown(event.getItem().getType(), COOLDOWN);
			event.setCancelled(true);
			World world = player.getWorld();
			plugin.mEffectManager.addEffect(player, "KeterExtraDamage", new PercentDamageDealt(DURATION, DAMAGE_PERCENT, AFFECTED_DAMAGE_CAUSES));
			plugin.mEffectManager.addEffect(player, "KeterExtraSpeed", new PercentSpeed(DURATION, SPEED_PERCENT, ATTR_NAME));
			plugin.mEffectManager.addEffect(player, "KeterParticles", new Aesthetics(DURATION,
				(entity, fourHertz, twoHertz, oneHertz) -> {
					// Tick effect
					Location loc = player.getLocation().add(0, 1, 0);
					world.spawnParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, GREEN_COLOR);
					world.spawnParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, GREEN_COLOR);
					world.spawnParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, OLIVE_COLOR);
				}, (entity) -> {
					// Lose effect
					Location loc = player.getLocation();
					world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1f, 0.65f);
					world.spawnParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, OLIVE_COLOR);
					world.spawnParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, GREEN_COLOR);
					world.spawnParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, OLIVE_COLOR);
				})
			);
			
			player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
			player.setSaturation(Math.min(player.getFoodLevel(), player.getSaturation() + 6));
			
			world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1, OLIVE_COLOR);
			world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1, GREEN_COLOR);
			world.playSound(player.getLocation(), Sound.ENTITY_STRIDER_EAT, 1, 0.85f);
			
		}
	}
}
