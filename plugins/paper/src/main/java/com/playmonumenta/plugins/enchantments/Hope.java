package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

import de.tr7zw.nbtapi.NBTEntity;

public class Hope implements BaseSpawnableItemEnchantment {
	public static String PROPERTY_NAME = ChatColor.GRAY + "Hope";

	/* How much longer an item lasts per level */
	private static final int EXTRA_MINUTES_PER_LEVEL = 5;
	private static final int TICK_PERIOD = 6;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.noneOf(ItemSlot.class);
	}

	@Override
	public boolean isMultiLevel() {
		return false;
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
				Location loc = item.getLocation();

				if (!loc.isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				item.getWorld().spawnParticle(Particle.SPELL_INSTANT, loc, 3, 0.2, 0.2, 0.2, 0);

				//Attempt to move the item upwards if in lava
				Material currentBlock = loc.getBlock().getType();
				Material upwardsBlock = loc.clone().add(0, 1, 0).getBlock().getType();
				if (currentBlock == Material.LAVA && (upwardsBlock == Material.LAVA || upwardsBlock == Material.AIR)) {
					item.teleport(loc.clone().add(0, 0.25, 0));
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
