package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import de.tr7zw.nbtapi.NBTEntity;

public class Hope implements BaseEnchantment {
	public static String PROPERTY_NAME = ChatColor.GRAY + "Hope";

	/* How much longer an item lasts per level */
	private static final int EXTRA_MINUTES_PER_LEVEL = 5;
	private static final int TICK_PERIOD = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.noneOf(ItemSlot.class);
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		item.setInvulnerable(true);
		NBTEntity nbt = new NBTEntity(item);
		nbt.setShort("Age", (short) (-1 * EXTRA_MINUTES_PER_LEVEL * Constants.TICKS_PER_MINUTE * level));

		new BukkitRunnable() {
			int mNumTicks = 0;

			@Override
			public void run() {
				item.getWorld().spawnParticle(Particle.SPELL_INSTANT, item.getLocation(), 3, 0.2, 0.2, 0.2, 0);
				if (item.isDead() || !item.isValid()) {
					this.cancel();
				}

				// Very infrequently check if the item is still actually there
				mNumTicks++;
				if (mNumTicks > 100) {
					mNumTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, TICK_PERIOD);
	}
}
