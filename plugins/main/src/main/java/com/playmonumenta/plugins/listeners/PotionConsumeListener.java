package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.InstantDrink;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class PotionConsumeListener implements Listener {
	private static final int DRINK_TICK_DELAY = 4; //How many ticks between each slurp sound
	private static final int DRINK_DURATION = 24; //Ticks of total drinking
	private static final String INVENTORY_DRINK_TAG = "InventoryDrinkTag"; //Tag to enable this feature (drink from inventory right click)

	private final Plugin mPlugin;
	private final Map<UUID, BukkitRunnable> mRunnables = new HashMap<UUID, BukkitRunnable>();

	public PotionConsumeListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void InventoryClickEvent(InventoryClickEvent event) {

		if (
		    // Must not be cancelled
		    event.isCancelled() ||
		    // Must be a right click
		    event.getClick() == null ||
		    !event.getClick().equals(ClickType.RIGHT) ||
		    // Must be placing a single block
		    event.getAction() == null ||
		    !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		    // Must be a player interacting with an inventory
		    event.getWhoClicked() == null ||
		    !(event.getWhoClicked() instanceof Player) ||
		    event.getClickedInventory() == null ||
		    // Must be a click on a drinkable potion in empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
		    !event.getCurrentItem().getType().equals(Material.POTION)
		) {
			// Nope!
			return;
		}

		Player player = (Player)event.getWhoClicked();
		PotionMeta meta = (PotionMeta)event.getCurrentItem().getItemMeta();
		ItemStack item = event.getCurrentItem();

		Set<String> tags = player.getScoreboardTags();
		BukkitRunnable runnable = mRunnables.get(player.getUniqueId());
		if (!tags.contains(INVENTORY_DRINK_TAG) || runnable != null && !runnable.isCancelled()) {
			return;
		}//Cannot drink another potion if one is already being drinked, or player does not have this feature enabled

		//If instant drink enchantment, instantly apply potion, otherwise imitate potion drinking
		if (InventoryUtils.getCustomEnchantLevel(item, InstantDrink.PROPERTY_NAME, false) != 0) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
			PotionUtils.applyPotion(mPlugin, player, meta);
		} else {
			//Gives slowness IV to emulate the slow walking of drinking, extra 5 ticks to match delay of drinking
			mPlugin.mPotionManager.addPotion(player, PotionID.ITEM, new PotionEffect(PotionEffectType.SLOW, DRINK_DURATION + 5, 3, true, false));

			runnable = new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					//If time to drink is finished, add effects. Otherwise, play sound of slurping every 0.5 seconds for 3.5 seconds total
					if (mTicks >= DRINK_DURATION) {
						PotionUtils.applyPotion(mPlugin, player, meta);
						this.cancel();
					}
					float pitch = ((float)Math.random() - 0.5f) * 0.05f; //Emulate drinking variation of pitch
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f + pitch);
					mTicks += DRINK_TICK_DELAY;
				}
			};
			runnable.runTaskTimer(mPlugin, 5, DRINK_TICK_DELAY); //Delay of 5 seconds before first drink in minecraft
			mRunnables.put(player.getUniqueId(), runnable);
		}

		if (!item.containsEnchantment(Enchantment.ARROW_INFINITE)) {
			item.setAmount(item.getAmount() - 1);
			if (item.getAmount() == 0) {
				event.getClickedInventory().setItem(event.getSlot(), new ItemStack(Material.GLASS_BOTTLE));
			} else {
				player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
			}
		}

		event.setCancelled(true);
	}
}
