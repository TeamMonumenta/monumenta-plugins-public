package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.LightningBottle;
import com.playmonumenta.plugins.enchantments.InstantDrink;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class PotionConsumeListener implements Listener {
	private static final int DRINK_TICK_DELAY = 4; //How many ticks between each slurp sound
	private static final int DRINK_DURATION = 24; //Ticks of total drinking
	private static final String INVENTORY_DRINK_TAG = "InventoryDrinkTag"; //Tag to enable this feature (drink from inventory right click)

	private final Plugin mPlugin;
	private final Map<UUID, BukkitRunnable> mRunnables = new HashMap<UUID, BukkitRunnable>();
	private Map<UUID, ItemStack> mPotionsConsumed = new HashMap<UUID, ItemStack>();

	private Map<UUID, BukkitRunnable> mCooldowns = new HashMap<UUID, BukkitRunnable>();

	public PotionConsumeListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void drinkablePotionEvent(InventoryClickEvent event) {
		HumanEntity whoClicked = event.getWhoClicked();
		if (
		    // Must not be cancelled
		    event.isCancelled() ||
		    // Must be a left or right click
		    event.getClick() == null ||
		    !(event.getClick().equals(ClickType.LEFT) || event.getClick().equals(ClickType.RIGHT)) ||
		    // Must be placing a single block
		    event.getAction() == null ||
		    // Must be a player interacting with an inventory
		    whoClicked == null ||
		    !(whoClicked instanceof Player) ||
		    ((Player)whoClicked).getGameMode() == GameMode.SPECTATOR ||
		    event.getClickedInventory() == null ||
		    // Must be a click on a drinkable potion or glass bottle in empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
		    ItemUtils.isItemShattered(event.getCurrentItem()) ||
		    !(event.getCurrentItem().getType().equals(Material.POTION) ||
		      event.getCurrentItem().getType().equals(Material.GLASS_BOTTLE))
		    ) {
			// Nope!
			return;
		}

		Player player = (Player) whoClicked;
		ItemStack item = event.getCurrentItem();

		Set<String> tags = player.getScoreboardTags();
		BukkitRunnable runnable = mRunnables.get(player.getUniqueId());
		ItemStack prevPotion = mPotionsConsumed.get(player.getUniqueId());

		//If empty bottle is left clicked, stops potion buff and reverts the potion back
		//Otherwise, cannot drink another potion if one is already being drinked, or player does not have this feature enabled
		if (event.getClick().equals(ClickType.LEFT) &&
		    item != null &&
		    item.getType().equals(Material.GLASS_BOTTLE) &&
		    !item.hasItemMeta() &&
		    runnable != null &&
		    !runnable.isCancelled() &&
		    prevPotion != null &&
		    !prevPotion.containsEnchantment(Enchantment.ARROW_INFINITE)) {

			//Sets previous drink in and stops drink runnable
			//Replaces glass bottle if there's one, if more, removes one and places the potion somewhere in the inventory
			if (item.getAmount() == 1) {
				event.getClickedInventory().setItem(event.getSlot(), prevPotion);
			} else {
				item.subtract();
				prevPotion.setAmount(1);
				event.getClickedInventory().addItem(prevPotion);
			}
			mPotionsConsumed.put(player.getUniqueId(), null);
			mCooldowns.remove(player.getUniqueId());
			runnable.cancel();
			event.setCancelled(true);
			return;

			//If it was a left click that did not satisfy previous conditions, return
		} else if (!event.getClick().equals(ClickType.RIGHT) ||
		           !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		           event.getCurrentItem().getType().equals(Material.GLASS_BOTTLE) ||
		           !tags.contains(INVENTORY_DRINK_TAG) ||
		           runnable != null && !runnable.isCancelled()) {

			//Return time
			return;
		}

		PotionMeta meta = (PotionMeta) item.getItemMeta();
		List<PotionEffect> effects = PotionUtils.getEffects(meta);

		if (checkPotionCooldown(player) && event.getClickedInventory().getType() != InventoryType.CHEST && isCooldownApplicable(effects)) {
			player.sendMessage(ChatColor.RED + "Quick drink is still on cooldown!");
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

			event.setCancelled(true);
			return;
		}

		if (item.containsEnchantment(Enchantment.ARROW_INFINITE) && !(effects.size() == 1 && effects.get(0).getType().equals(PotionEffectType.GLOWING))) {
			player.sendMessage(ChatColor.RED + "Infinite potions can not be quick drinked!");

			float pitch = ((float)FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f;
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f + pitch);

			event.setCancelled(true);
			return;
		}

		int instantDrinkLevel = InventoryUtils.getCustomEnchantLevel(item, InstantDrink.PROPERTY_NAME, false);

		if (PotionUtils.isLuckPotion(meta)) {
			Location loc = player.getLocation();
			loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Luck potions can no longer be consumed");
			event.setCancelled(true);
			return;
		}

		InventoryType invType = event.getClickedInventory().getType();
		if (invType == InventoryType.PLAYER || invType == InventoryType.ENDER_CHEST || invType == InventoryType.SHULKER_BOX || invType == InventoryType.CRAFTING) {
			if (isCooldownApplicable(effects)) {
				setPotionCooldown(player);
			}
		}

		//If instant drink enchantment, instantly apply potion, otherwise imitate potion drinking
		if (instantDrinkLevel != 0) {
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
					if (mTicks >= DRINK_DURATION && !this.isCancelled()) {
						PotionUtils.applyPotion(mPlugin, player, meta);
						//If Sacred Provisions check passes, do not consume, but do not enable cancel quick drink function
						//Do not run addition on infinity potions
						if (mPotionsConsumed.get(player.getUniqueId()) != null &&
							!mPotionsConsumed.get(player.getUniqueId()).containsEnchantment(Enchantment.ARROW_INFINITE) &&
							NonClericProvisionsPassive.testRandomChance(player)) {

							Inventory inv = event.getClickedInventory();
							int slot = event.getSlot();
							ItemStack potion = mPotionsConsumed.get(player.getUniqueId());
							ItemStack invItem = inv.getItem(slot);
							if (invItem == null || invItem.getType().isAir() || invItem.getType().equals(Material.GLASS_BOTTLE)) {
								inv.setItem(slot, potion);
							} else {
								inv.addItem(potion);
							}
						} else {
							//Remove glass bottle from inventory once drinked
							Inventory inv = event.getClickedInventory();
							int slot = event.getSlot();
							ItemStack invItem = inv.getItem(slot);
							if (invItem != null && invItem.getType().equals(Material.GLASS_BOTTLE) && !invItem.hasItemMeta()) {
								inv.setItem(slot, null);
							}
						}

						this.cancel();
					}
					float pitch = ((float)FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f; //Emulate drinking variation of pitch
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f + pitch);
					mTicks += DRINK_TICK_DELAY;
				}
			};
			runnable.runTaskTimer(mPlugin, 5, DRINK_TICK_DELAY); //Delay of 5 seconds before first drink in minecraft
			mRunnables.put(player.getUniqueId(), runnable);
			mPotionsConsumed.put(player.getUniqueId(), new ItemStack(item));
		}

		CoreProtectIntegration.logContainerTransaction(player, event.getClickedInventory().getLocation());

		//Do not reduce potions or place glass bottles if the potion is infinite
		if (!item.containsEnchantment(Enchantment.ARROW_INFINITE)) {
			item.setAmount(item.getAmount() - 1);
			//If not instant drink, place an empty bottle in the inventory
			if (instantDrinkLevel == 0) {
				if (item.getAmount() == 0) {
					event.getClickedInventory().setItem(event.getSlot(), new ItemStack(Material.GLASS_BOTTLE));
				} else {
					//Re-enable if we ever want stacked non-instant drink potions to be cancellable (can't remove the bottle though)
//					player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
				}
			}
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void throwablePotionEvent(InventoryClickEvent event) {
		HumanEntity whoClicked = event.getWhoClicked();
		if (
		    // Must not be cancelled
		    event.isCancelled() ||
		    // Must be a right click
		    event.getClick() == null ||
		    !event.getClick().equals(ClickType.RIGHT) ||
		    // Must be placing a single block
		    event.getAction() == null ||
		    !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		    // Must be a player interacting with their main inventory
		    whoClicked == null ||
		    !(whoClicked instanceof Player) ||
		    ((Player)whoClicked).getGameMode() == GameMode.SPECTATOR ||
		    event.getClickedInventory() == null ||
		    // Must be a click on a shulker box with an empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
			ItemUtils.isItemShattered(event.getCurrentItem()) ||
		    !(event.getCurrentItem().getType().equals(Material.SPLASH_POTION) ||
		      event.getCurrentItem().getType().equals(Material.LINGERING_POTION))
				) {
			// Nope!
			return;
		}

		Player player = (Player) whoClicked;
		ItemStack item = event.getCurrentItem();

		Set<String> tags = player.getScoreboardTags();

		if (!tags.contains(INVENTORY_DRINK_TAG) || InventoryUtils.testForItemWithName(item, "Alchemist's Potion") || InventoryUtils.testForItemWithName(item, LightningBottle.POTION_NAME) || ItemUtils.isAlchemistItem(item)) {
			//Needs this tag to work and cannot be an Alchemist Potion or Lightning Bottle
			return;
		}

		PotionMeta meta = (PotionMeta) item.getItemMeta();
		List<PotionEffect> effects = PotionUtils.getEffects(meta);

		if (checkPotionCooldown(player) && event.getClickedInventory().getType() != InventoryType.CHEST && isCooldownApplicable(effects)) {
			player.sendMessage(ChatColor.RED + "Quick drink is still on cooldown!");
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

			event.setCancelled(true);
			return;
		}

		//Create item as type splash or lingering and set entity item to the inv potion
		ThrownPotion potion = (ThrownPotion) player.getWorld().spawnEntity(player.getLocation(), EntityType.SPLASH_POTION);
		potion.setItem(item);
		potion.setShooter(player);
		ProjectileLaunchEvent newEvent = new ProjectileLaunchEvent(potion);
		Bukkit.getPluginManager().callEvent(newEvent);
		if (newEvent.isCancelled()) {
			potion.remove();
			return;
		}

		CoreProtectIntegration.logContainerTransaction(player, event.getClickedInventory().getLocation());

		InventoryType invType = event.getClickedInventory().getType();
		if (invType == InventoryType.PLAYER || invType == InventoryType.ENDER_CHEST || invType == InventoryType.SHULKER_BOX || invType == InventoryType.CRAFTING) {
			if (isCooldownApplicable(effects)) {
				setPotionCooldown(player);
			}
		}

		//If Sacred Provisions check passes, do not consume
		if (NonClericProvisionsPassive.testRandomChance(player)) {
			event.setCancelled(true);
			return;
		}

		//Remove item
		item.setAmount(item.getAmount() - 1);

		event.setCancelled(true);
	}

	//Set cooldown to prevent spam
	private void setPotionCooldown(Player player) {
		BukkitRunnable runnable = mCooldowns.remove(player.getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
		runnable = new BukkitRunnable() {
			@Override
			public void run() {
				//Don't need to put anything here - method checks if BukkitRunnable is active
				mCooldowns.remove(player.getUniqueId());
				this.cancel();
			}
		};
		runnable.runTaskLater(mPlugin, 20 * 5);
		mCooldowns.put(player.getUniqueId(), runnable);
	}

	//Returns true if potion cooldown is up right now
	//False if no cooldowns and the quick drink is activatable now
	private boolean checkPotionCooldown(HumanEntity player) {
		return mCooldowns.containsKey(player.getUniqueId()) && !mCooldowns.get(player.getUniqueId()).isCancelled();
	}

	private boolean isCooldownApplicable(List<PotionEffect> effects) {
		for (PotionEffect effect : effects) {
			PotionEffectType type = effect.getType();
			int amp = effect.getAmplifier();
			if (type.equals(PotionEffectType.HEAL) || (type.equals(PotionEffectType.REGENERATION) && amp >= 2) || (type.equals(PotionEffectType.DAMAGE_RESISTANCE) && amp >= 2) || type.equals(PotionEffectType.ABSORPTION) || type.equals(PotionEffectType.SATURATION)) {
				return true;
			}
		}
		return false;
	}
}
