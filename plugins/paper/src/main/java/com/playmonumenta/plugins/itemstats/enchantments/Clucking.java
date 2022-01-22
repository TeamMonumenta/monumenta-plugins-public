package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;

public class Clucking implements Enchantment {

	private static final int DROPPED_TICK_PERIOD = 60;
	private boolean mRun = false;

	@Override
	public String getName() {
		return "Clucking";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CLUCKING;
	}

	@Override
	public EnumSet<ItemStatUtils.Slot> getSlots() {
		return EnumSet.of(ItemStatUtils.Slot.MAINHAND);
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (PremiumVanishIntegration.isInvisible(player)) {
			return;
		}

		if (oneHz && mRun) {
			mRun = false;
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
		} else if (oneHz) {
			mRun = true;
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!item.getLocation().isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				item.getWorld().playSound(item.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);

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
