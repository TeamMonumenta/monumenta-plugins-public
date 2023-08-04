package com.playmonumenta.plugins.listeners;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class PotionConsumeListener implements Listener {
	private static final int DRINK_TICK_DELAY = 4; //How many ticks between each slurp sound
	private static final int DRINK_DURATION = 24; //Ticks of total drinking
	private static final String INVENTORY_DRINK_TAG = "InventoryDrinkTag"; //Tag to enable this feature (drink from inventory right click)
	private static final String INVENTORY_DRINK_SLOW_EFFECT_NAME = "InventoryDrinkSlowEffect";
	private static final ImmutableMap<EffectType, Double> COOLDOWN_EFFECTS = ImmutableMap.<EffectType, Double>builder()
		.put(EffectType.INSTANT_HEALTH, 0d)
		.put(EffectType.ABSORPTION, 0d)
		.put(EffectType.SATURATION, 0d)
		.put(EffectType.VANILLA_REGEN, 2d)
		.put(EffectType.RESISTANCE, 0.2d)
		.build();

	private final Plugin mPlugin;
	/* Note that this map only contains non-cancelled tasks. Everywhere these runnables are cancelled they should be removed from this map */
	private final Map<UUID, BukkitRunnable> mRunnables = new HashMap<>();
	/* This will only contain an item if currently consuming a potion, cleared after complete */
	private final Map<UUID, ItemStack> mPotionsConsumed = new HashMap<>();
	/* This will only contain an slot number if currently consuming a potion, cleared after complete */
	private final Map<UUID, Integer> mPotionsSlot = new HashMap<>();

	/* Note that this map only contains non-cancelled tasks. Everywhere these runnables are cancelled they should be removed from this map */
	private final Map<UUID, BukkitTask> mCooldowns = new HashMap<>();

	public PotionConsumeListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null) {
			return;
		}

		HumanEntity whoClicked = event.getWhoClicked();
		if (!(whoClicked instanceof Player player) || player.getGameMode() == GameMode.SPECTATOR || !ScoreboardUtils.checkTag(player, INVENTORY_DRINK_TAG)) {
			return;
		}

		if (!ItemUtils.isNullOrAir(event.getCursor())) {
			return;
		}

		Inventory clickedInventory = event.getClickedInventory();
		if (clickedInventory == null ||
			    ((clickedInventory.getHolder() == null || clickedInventory.getHolder() instanceof Villager) &&
						clickedInventory.getType() != InventoryType.ENDER_CHEST)) {
			return;
		}

		if (ZoneUtils.hasZoneProperty(whoClicked, ZoneProperty.NO_POTIONS)) {
			return;
		}

		if (ItemStatUtils.getTier(item) == Tier.LEGACY) {
			return;
		}

		ClickType click = event.getClick();
		Material mat = item.getType();
		if (click == ClickType.LEFT && mat == Material.GLASS_BOTTLE) {
			leftClickBottle(event, player, item, clickedInventory);
			return;
		}

		if (click == ClickType.RIGHT && (
			mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION)) {
			rightClickPotion(event, player, item, clickedInventory);
		}
	}

	private void rightClickPotion(InventoryClickEvent event, Player player, ItemStack item, Inventory clickedInventory) {
		ReadableNBTList<ReadWriteNBT> customEffects = NBT.get(item, ItemStatUtils::getEffects);
		if (customEffects == null || customEffects.isEmpty()) {
			return;
		}

		event.setCancelled(true);

		if (ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY) && !isOnlyGlowing(customEffects)) {
			player.sendMessage(ChatColor.RED + "Infinite potions can not be quick drinked!");
			float pitch = ((float) FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f;
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f + pitch);
			return;
		}

		if (cooldownApplies(customEffects) && clickedInventory.getType() != InventoryType.CHEST) {
			if (checkPotionCooldown(player)) {
				player.sendMessage(ChatColor.RED + "Quick drink is still on cooldown!");
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				return;
			} else {
				setPotionCooldown(player);
			}
		}

		Material mat = item.getType();
		if (mat == Material.POTION) {
			rightClickDrinkablePotion(event, player, item, clickedInventory);
		} else {
			// We already checked that it is a splash or lingering potion
			rightClickSplashPotion(player, item);
		}

		CoreProtectIntegration.logContainerTransaction(player, clickedInventory.getLocation());
	}

	private void rightClickDrinkablePotion(InventoryClickEvent event, Player player, ItemStack item, Inventory clickedInventory) {
		boolean instantDrink = ItemStatUtils.hasEnchantment(item, EnchantmentType.INSTANT_DRINK);

		if (!instantDrink && mRunnables.get(player.getUniqueId()) != null) {
			return;
		}

		int starvation = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STARVATION);
		int slot = event.getSlot();
		boolean reduceStack = !ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY);

		//If instant drink enchantment, instantly apply potion, otherwise imitate potion drinking
		if (instantDrink) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			ItemStatUtils.applyCustomEffects(mPlugin, player, item);
			StatTrackManager.getInstance().incrementStatImmediately(item, player, InfusionType.STAT_TRACK_CONSUMED, 1);

			//Apply Starvation if applicable
			Starvation.apply(player, starvation);

			if (reduceStack && NonClericProvisionsPassive.testRandomChance(player)) {
				NonClericProvisionsPassive.sacredProvisionsSound(player);
				reduceStack = false;
			}
		} else {
			//Gives 80% slowness to emulate the slow walking of drinking, extra 5 ticks to match delay of drinking
			mPlugin.mEffectManager.addEffect(player, INVENTORY_DRINK_SLOW_EFFECT_NAME, new PercentSpeed(DRINK_DURATION + 5, -0.8, INVENTORY_DRINK_SLOW_EFFECT_NAME).displays(false));

			UUID uuid = player.getUniqueId();
			mPotionsConsumed.put(uuid, new ItemStack(item));
			mPotionsSlot.put(uuid, slot);
			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					//If time to drink is finished, add effects. Otherwise, play sound of slurping every 0.5 seconds for 3.5 seconds total
					if (mTicks >= DRINK_DURATION) {
						ItemStack potion = mPotionsConsumed.remove(player.getUniqueId());
						ItemStatUtils.applyCustomEffects(mPlugin, player, potion);
						StatTrackManager.getInstance().incrementStatImmediately(item, player, InfusionType.STAT_TRACK_CONSUMED, 1);

						Starvation.apply(player, starvation);

						//If Sacred Provisions check passes, do not consume, but do not enable cancel quick drink function
						//Do not run addition on infinity potions
						if (potion != null && ItemStatUtils.getEnchantmentLevel(potion, EnchantmentType.INFINITY) == 0 &&
							    NonClericProvisionsPassive.testRandomChance(player)) {
							NonClericProvisionsPassive.sacredProvisionsSound(player);
							postPotionDrink(player, clickedInventory, potion, false);
						} else {
							//Remove glass bottle from inventory once drinked
							postPotionDrink(player, clickedInventory, null, false);
						}

						this.cancel();
						mRunnables.remove(uuid);
					}
					float pitch = ((float) FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f; //Emulate drinking variation of pitch
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f + pitch);
					mTicks += DRINK_TICK_DELAY;
				}
			};
			mRunnables.put(uuid, runnable);
			runnable.runTaskTimer(mPlugin, 5, DRINK_TICK_DELAY); //Delay of 5 seconds before first drink in minecraft
		}

		if (reduceStack) {
			item.setAmount(item.getAmount() - 1);
			if (!instantDrink && item.getAmount() == 0) {
				clickedInventory.setItem(slot, new ItemStack(Material.GLASS_BOTTLE));
			}
		}
	}

	/**
	 * This is called after the potion drink runnable has finished and when InventoryCloseEvent is called
	 * @param player - player that drank the potion
	 * @param inventory - inventory to check and refund potion
	 * @param potion - potion to refund
	 * @param inventoryClose - if this is an InventoryClose event
	 */
	private void postPotionDrink(Player player, Inventory inventory, @Nullable ItemStack potion, Boolean inventoryClose) {
		UUID uuid = player.getUniqueId();
		BukkitRunnable runnable = mRunnables.get(uuid);
		// return if there is no drink in progress
		if (runnable == null) {
			return;
		}

		Integer slot = mPotionsSlot.get(uuid);
		if (slot == null) {
			return;
		}
		ItemStack invItem = inventory.getItem(slot);
		if (potion != null) { // if potion is not null, then assume that provisions is triggered
			// only run if player still has container open
			if (inventory.equals(player.getOpenInventory().getTopInventory()) || inventory.equals(player.getOpenInventory().getBottomInventory())) {
				if (invItem == null || invItem.getType().isAir() || invItem.getType().equals(Material.GLASS_BOTTLE)) {
					inventory.setItem(slot, potion);
				} else {
					InventoryUtils.giveItem(player, potion, inventory);
				}
			} else {
				// otherwise put the potion anywhere in player inventory
				InventoryUtils.giveItem(player, potion);
			}
			mPotionsSlot.remove(uuid); // provisions should NEVER be triggered by InventoryCloseEvent
		} else if (invItem != null && invItem.getType().equals(Material.GLASS_BOTTLE) && !invItem.hasItemMeta()) {
			inventory.setItem(slot, null);
			if (!inventoryClose) {
				mPotionsSlot.remove(uuid);
			}
		}
	}

	private void rightClickSplashPotion(Player player, ItemStack item) {
		//Create item as type splash or lingering and set entity item to the inv potion
		ThrownPotion potion = EntityUtils.spawnSplashPotion(player, item, true);
		// mimic splash range of a regular splash potion
		// this only exists because mojank code doesn't allow me to have potions collide with the player
		if (potion.getItem().getType() == Material.SPLASH_POTION) {
			PotionUtils.mimicSplashPotionEffect(player, potion);
			PotionUtils.splashPotionParticlesAndSound(player, potion.getPotionMeta().getColor());
		}

		//If Sacred Provisions check passes, do not consume
		if (NonClericProvisionsPassive.testRandomChance(player)) {
			NonClericProvisionsPassive.sacredProvisionsSound(player);
			return;
		}

		//Remove item
		item.setAmount(item.getAmount() - 1);

	}

	private void leftClickBottle(InventoryClickEvent event, Player player, ItemStack item, Inventory clickedInventory) {
		if (item.hasItemMeta()) {
			return;
		}

		UUID uuid = player.getUniqueId();
		BukkitRunnable runnable = mRunnables.get(uuid);
		if (runnable == null) {
			return;
		}

		ItemStack prevPotion = mPotionsConsumed.get(uuid);
		if (prevPotion == null || ItemStatUtils.hasEnchantment(prevPotion, EnchantmentType.INFINITY)) {
			return;
		}

		//Sets previous drink in and stops drink runnable
		//Replaces glass bottle if there's one, if more, removes one and places the potion somewhere in the inventory
		if (item.getAmount() == 1) {
			clickedInventory.setItem(event.getSlot(), prevPotion);
		} else {
			item.subtract();
			prevPotion.setAmount(1);
			clickedInventory.addItem(prevPotion);
		}
		mPotionsConsumed.remove(uuid);
		mPotionsSlot.remove(uuid);
		if (cooldownApplies(prevPotion)) {
			mCooldowns.remove(uuid);
		}
		runnable.cancel();
		mRunnables.remove(uuid);
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		BukkitRunnable runnable = mRunnables.remove(event.getEntity().getUniqueId());
		if (runnable != null) {
			runnable.cancel();
		}
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

	private boolean cooldownApplies(ItemStack item) {
		return NBT.get(item, nbt -> {
			ReadableNBTList<ReadWriteNBT> customEffects = ItemStatUtils.getEffects(nbt);
			return cooldownApplies(customEffects);
		});
	}

	private boolean cooldownApplies(@Nullable ReadableNBTList<ReadWriteNBT> customEffects) {
		if (customEffects == null) {
			return false;
		}

		for (ReadWriteNBT effect : customEffects) {
			if (effect.hasTag(ItemStatUtils.EFFECT_TYPE_KEY)) {
				EffectType type = EffectType.fromType(effect.getString(ItemStatUtils.EFFECT_TYPE_KEY));
				if (COOLDOWN_EFFECTS.containsKey(type)) {
					double amount = COOLDOWN_EFFECTS.get(type);
					if (amount == 0 || (effect.hasTag(ItemStatUtils.EFFECT_STRENGTH_KEY) && effect.getDouble(ItemStatUtils.EFFECT_STRENGTH_KEY) >= amount)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean isOnlyGlowing(ReadableNBTList<ReadWriteNBT> customEffects) {
		for (ReadWriteNBT effect : customEffects) {
			if (effect.hasTag(ItemStatUtils.EFFECT_TYPE_KEY)) {
				EffectType type = EffectType.fromType(effect.getString(ItemStatUtils.EFFECT_TYPE_KEY));
				if (type != EffectType.VANILLA_GLOW) {
					return false;
				}
			}
		}
		return true;
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
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		if (inventory instanceof PlayerInventory) {
			return;
		}
		postPotionDrink((Player) event.getPlayer(), inventory, null, true);
	}
}
