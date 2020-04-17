package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Adrenaline implements BaseEnchantment {

	private static String ADRENALINE_METAKEY = "ActiveAdrenalineLevel";
	private static String PROPERTY_NAME = ChatColor.GRAY + "Adrenaline";

	private static final int DURATION = 20 * 3;
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	private Map<UUID, BukkitRunnable> mRunnables = new HashMap<UUID, BukkitRunnable>();

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	// Adds +0.1 movement speed per level for 60 ticks, currently walkspeed, movement speed increase broken
	// 0.2 is default for walk speed
	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {

		BukkitRunnable runnable = mRunnables.get(player.getUniqueId());
		if (runnable != null && !runnable.isCancelled()) {
			if (player.getMetadata(ADRENALINE_METAKEY).get(0).asInt() < level) {
				removeEffects(plugin, player);
				applyEffects(plugin, player, level);
			}
			runnable.cancel();
		} else {
			applyEffects(plugin, player, level);
		}

		player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR);
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				removeEffects(plugin, player);
				this.cancel();
			}
		};
		runnable.runTaskLater(plugin, DURATION);
		mRunnables.put(player.getUniqueId(), runnable);
	}

	private void applyEffects(Plugin plugin, Player player, int level) {
		if (player.hasMetadata(ADRENALINE_METAKEY)) {
			plugin.getLogger().warning("Tried to apply Adrenaline to player '" + player.getName() + "' that already has it!");
		} else {
			player.setWalkSpeed(player.getWalkSpeed() + (level * 0.02f));
			player.setMetadata(ADRENALINE_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	private void removeEffects(Plugin plugin, Player player) {
		player.setWalkSpeed(player.getWalkSpeed() - (player.getMetadata(ADRENALINE_METAKEY).get(0).asInt() * 0.02f));
		player.removeMetadata(ADRENALINE_METAKEY, plugin);
	}
}
