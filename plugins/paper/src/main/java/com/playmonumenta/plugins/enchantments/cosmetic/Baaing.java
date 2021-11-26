package com.playmonumenta.plugins.enchantments.cosmetic;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseSpawnableItemEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Plays sheep baaing sounds when in inventory or dropped
 */
public class Baaing implements BaseSpawnableItemEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Baaing";
	private static final int DROPPED_TICK_PERIOD = 60;

	/* This is shared by all instances */
	private static int staticTicks = 0;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public void tick(Plugin plugin, Player player, int level) {
		if (PremiumVanishIntegration.isInvisible(player)) {
			return;
		}

		staticTicks++;

		/*
		 * Max - 60 items or more is every 3s (60 ticks)
		 * Min - 1 item is every 33s (650 ticks)
		 */
		if (level > 60) {
			level = 60;
		}

		int modulo = 60 + (600 - (level * 10));

		// Since this is only called once per second
		modulo = modulo / 20;

		if (staticTicks % modulo == 0) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!item.getLocation().isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				item.getWorld().playSound(item.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);

				// Very infrequently check if the item is still actually there
				mTicks++;
				if (mTicks > 200) {
					mTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, DROPPED_TICK_PERIOD);
	}
}
