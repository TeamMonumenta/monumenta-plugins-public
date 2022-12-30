package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DivineAura implements Enchantment {

	private static final String TAG_TO_DISABLE = "NoDivineAura";
	private static final Set<UUID> DISABLE_TAG = new HashSet<>();

	@Override
	public String getName() {
		return "Divine Aura";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DIVINE_AURA;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		if (player.getScoreboardTags().contains(TAG_TO_DISABLE)) {
			DISABLE_TAG.add(player.getUniqueId());
		} else {
			DISABLE_TAG.remove(player.getUniqueId());
		}
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double value, PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (InventoryUtils.testForItemWithLore(item, "To my friends,") && player.getCooldown(item.getType()) <= 0) {
				World world = player.getWorld();

				if (!DISABLE_TAG.contains(player.getUniqueId())) {
					new PartialParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1).spawnAsPlayerActive(player);
					world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 0.25f);
					player.sendMessage(ChatColor.AQUA + "You feel the Divine Aura around you fall dormant...");
					player.addScoreboardTag(TAG_TO_DISABLE);
				} else {
					new PartialParticle(Particle.SPELL, player.getLocation().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 1).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 1, 0), 25, 0.5, 0.45, 0.25, 1).spawnAsPlayerActive(player);
					world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1.25f);
					player.sendMessage(ChatColor.AQUA + "You feel a Divine Aura envelop you.");
					player.removeScoreboardTag(TAG_TO_DISABLE);
				}
				player.setCooldown(item.getType(), 20);
				InventoryUtils.scheduleDelayedEquipmentCheck(plugin, player, null);
			}
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
			return;
		}

		if (!DISABLE_TAG.contains(player.getUniqueId()) && twoHz) {
			final Location loc = player.getLocation().add(0, 1, 0);
			new PartialParticle(Particle.SPELL_INSTANT, loc, 5, 0.4, 0.4, 0.4, 0).spawnAsPlayerPassive(player);
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (!DISABLE_TAG.contains(player.getUniqueId()) && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)) {
			new PartialParticle(Particle.SPELL_INSTANT, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 6, enemy.getWidth(), enemy.getHeight() / 2, enemy.getWidth(), 1).spawnAsPlayerActive(player);
			new PartialParticle(Particle.FIREWORKS_SPARK, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 4, 0, 0, 0, 0.15).spawnAsPlayerActive(player);
		}
	}
}
