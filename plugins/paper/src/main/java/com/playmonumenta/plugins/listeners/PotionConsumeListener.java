package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.LightningBottle;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import org.bukkit.scheduler.BukkitTask;

public class PotionConsumeListener implements Listener {
	private static final int DRINK_TICK_DELAY = 4; //How many ticks between each slurp sound
	private static final int DRINK_DURATION = 24; //Ticks of total drinking
	private static final String INVENTORY_DRINK_TAG = "InventoryDrinkTag"; //Tag to enable this feature (drink from inventory right click)
	private static final String INVENTORY_DRINK_SLOW_EFFECT_NAME = "InventoryDrinkSlowEffect";

	private final Plugin mPlugin;
	/* Note that this map only contains non-cancelled tasks. Everywhere these runnables are cancelled they should be removed from this map */
	private final Map<UUID, BukkitRunnable> mRunnables = new HashMap<>();
	/* This will only contain an item if currently consuming a potion, cleared after complete */
	private final Map<UUID, ItemStack> mPotionsConsumed = new HashMap<>();

	/* Note that this map only contains non-cancelled tasks. Everywhere these runnables are cancelled they should be removed from this map */
	private final Map<UUID, BukkitTask> mCooldowns = new HashMap<>();

	public PotionConsumeListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void drinkablePotionEvent(InventoryClickEvent event) {
		HumanEntity whoClicked = event.getWhoClicked();
		Inventory clickedInventory = event.getClickedInventory();
		if (
		    // Must be a left or right click
		    !(event.getClick().equals(ClickType.LEFT) || event.getClick().equals(ClickType.RIGHT)) ||
		    // Must be a player interacting with an inventory
		    !(whoClicked instanceof Player player) ||
		    player.getGameMode() == GameMode.SPECTATOR ||
		    clickedInventory == null ||
		    ((clickedInventory.getHolder() == null || clickedInventory.getHolder() instanceof Villager) && clickedInventory.getType() != InventoryType.ENDER_CHEST) ||
		    // Must be a click on a drinkable potion or glass bottle in empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
			ZoneUtils.hasZoneProperty(whoClicked, ZoneProperty.NO_POTIONS) ||
		    !(event.getCurrentItem().getType().equals(Material.POTION) ||
		      event.getCurrentItem().getType().equals(Material.GLASS_BOTTLE))
		    ) {
			// Nope!
			return;
		}

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
			mPotionsConsumed.remove(player.getUniqueId());
			mCooldowns.remove(player.getUniqueId());
			runnable.cancel();
			mRunnables.remove(player.getUniqueId());
			event.setCancelled(true);
			return;

			//If it was a left click that did not satisfy previous conditions, return
		} else if (!event.getClick().equals(ClickType.RIGHT) ||
		           !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		           event.getCurrentItem().getType().equals(Material.GLASS_BOTTLE) ||
		           !tags.contains(INVENTORY_DRINK_TAG) ||
		           runnable != null) {

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

		if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INFINITY) > 0 && !(effects.size() == 1 && effects.get(0).getType().equals(PotionEffectType.GLOWING))) {
			player.sendMessage(ChatColor.RED + "Infinite potions can not be quick drinked!");

			float pitch = ((float)FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f;
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f + pitch);

			event.setCancelled(true);
			return;
		}

		int instantDrinkLevel = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INSTANT_DRINK);

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
			ItemStatUtils.applyCustomEffects(mPlugin, player, item);

			//Apply Starvation if applicable
			int starvation = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STARVATION);
			if (starvation > 0) {
				Starvation.apply(player, starvation);
			}
		} else {
			//Gives 80% slowness to emulate the slow walking of drinking, extra 5 ticks to match delay of drinking
			mPlugin.mEffectManager.addEffect(player, INVENTORY_DRINK_SLOW_EFFECT_NAME, new PercentSpeed(DRINK_DURATION + 5, -0.8, INVENTORY_DRINK_SLOW_EFFECT_NAME));

			runnable = new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					//If time to drink is finished, add effects. Otherwise, play sound of slurping every 0.5 seconds for 3.5 seconds total
					if (mTicks >= DRINK_DURATION) {
						ItemStatUtils.applyCustomEffects(mPlugin, player, item);

						//Apply Starvation if applicable
						int starvation = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STARVATION);
						if (starvation > 0) {
							Starvation.apply(player, starvation);
						}

						//If Sacred Provisions check passes, do not consume, but do not enable cancel quick drink function
						//Do not run addition on infinity potions
						ItemStack potion = mPotionsConsumed.remove(player.getUniqueId());
						if (potion != null && ItemStatUtils.getEnchantmentLevel(potion, EnchantmentType.INFINITY) == 0 &&
							NonClericProvisionsPassive.testRandomChance(player)) {
							NonClericProvisionsPassive.sacredProvisionsSound(player);

							Inventory inv = event.getClickedInventory();
							int slot = event.getSlot();
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
						mRunnables.remove(player.getUniqueId());
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
		if (ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.INFINITY) == 0) {
			item.setAmount(item.getAmount() - 1);
			//If not instant drink, place an empty bottle in the inventory
			if (instantDrinkLevel == 0) {
				if (item.getAmount() == 0) {
					event.getClickedInventory().setItem(event.getSlot(), new ItemStack(Material.GLASS_BOTTLE));
				}
			}
		}

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void throwablePotionEvent(InventoryClickEvent event) {
		HumanEntity whoClicked = event.getWhoClicked();
		Inventory clickedInventory = event.getClickedInventory();
		if (
		    // Must be a right click
		    !event.getClick().equals(ClickType.RIGHT) ||
		    !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		    // Must be a player interacting with their main inventory
		    !(whoClicked instanceof Player player) ||
		    player.getGameMode() == GameMode.SPECTATOR ||
		    clickedInventory == null ||
		    ((clickedInventory.getHolder() == null || clickedInventory.getHolder() instanceof Villager) && clickedInventory.getType() != InventoryType.ENDER_CHEST) ||
		    // Must be a click on a shulker box with an empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
		    !(event.getCurrentItem().getType().equals(Material.SPLASH_POTION) ||
		      event.getCurrentItem().getType().equals(Material.LINGERING_POTION))
				) {
			// Nope!
			return;
		}

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
		ProjectileLaunchEvent newEvent = new ProjectileLaunchEvent(potion);
		Bukkit.getPluginManager().callEvent(newEvent);
		//Set shooter AFTER the event is called so that Sacred Provisions does not trigger twice - this leads to a dupe exploit
		potion.setShooter(player);
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
			NonClericProvisionsPassive.sacredProvisionsSound(player);
			return;
		}

		//Remove item
		item.setAmount(item.getAmount() - 1);

		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		BukkitRunnable runnable = mRunnables.remove(event.getEntity().getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void appliedPotionEvent(EntityPotionEffectEvent event) {
		PotionEffect newEffect = event.getNewEffect();
		if (event.getModifiedType().getName().equals("WEAKNESS") && newEffect != null) {
			if (event.getAction() != EntityPotionEffectEvent.Action.REMOVED && event.getAction() != EntityPotionEffectEvent.Action.CLEARED && event.getEntity() instanceof LivingEntity le) {
				EntityUtils.applyWeaken(mPlugin, newEffect.getDuration(), (newEffect.getAmplifier() + 1) * 0.1, le, null);
				le.removePotionEffect(PotionEffectType.WEAKNESS);
				event.setCancelled(true);
			}
		}

		/*if (event.getAction() == EntityPotionEffectEvent.Action.ADDED) {
			if (EffectType.convertPotionEffect(event)) {
				event.setCancelled(true);
			}
		}*/
	}

	//Set cooldown to prevent spam
	private void setPotionCooldown(Player player) {
		BukkitTask runnable = mCooldowns.remove(player.getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}

		UUID playerUUID = player.getUniqueId();
		mCooldowns.put(playerUUID, Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			//Don't need to put anything here - method checks if BukkitRunnable is active
			mCooldowns.remove(playerUUID);
		}, 20 * 5));
	}

	//Returns true if potion cooldown is up right now
	//False if no cooldowns and the quick drink is activatable now
	private boolean checkPotionCooldown(HumanEntity player) {
		return mCooldowns.containsKey(player.getUniqueId());
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
