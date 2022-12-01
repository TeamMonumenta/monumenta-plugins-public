package com.playmonumenta.plugins.listeners;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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
			    ((clickedInventory.getHolder() == null || clickedInventory.getHolder() instanceof Villager) && clickedInventory.getType() != InventoryType.ENDER_CHEST)) {
			return;
		}

		if (ZoneUtils.hasZoneProperty(whoClicked, ZoneProperty.NO_POTIONS)) {
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
		NBTCompoundList customEffects = ItemStatUtils.getEffects(new NBTItem(item));
		if (customEffects == null || customEffects.isEmpty()) {
			return;
		}

		event.setCancelled(true);

		boolean cooldownApplies = false;
		boolean onlyGlowing = false;
		for (NBTListCompound effect : customEffects) {
			if (effect.hasKey(ItemStatUtils.EFFECT_TYPE_KEY)) {
				EffectType type = EffectType.fromType(effect.getString(ItemStatUtils.EFFECT_TYPE_KEY));
				if (type == EffectType.VANILLA_GLOW) {
					onlyGlowing = true;
				} else if (type != null) {
					onlyGlowing = false;
					if (COOLDOWN_EFFECTS.containsKey(type)) {
						double amount = COOLDOWN_EFFECTS.get(type);
						if (amount == 0 || (effect.hasKey(ItemStatUtils.EFFECT_STRENGTH_KEY) && effect.getDouble(ItemStatUtils.EFFECT_STRENGTH_KEY) >= amount)) {
							cooldownApplies = true;
							break;
						}
					}
				}
			}
		}

		if (ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY) && !onlyGlowing) {
			player.sendMessage(ChatColor.RED + "Infinite potions can not be quick drinked!");
			float pitch = ((float) FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f;
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f + pitch);
			return;
		}

		if (cooldownApplies && clickedInventory.getType() != InventoryType.CHEST) {
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
		int starvation = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.STARVATION);
		int slot = event.getSlot();
		boolean reduceStack = !ItemStatUtils.hasEnchantment(item, EnchantmentType.INFINITY);

		//If instant drink enchantment, instantly apply potion, otherwise imitate potion drinking
		if (instantDrink) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
			ItemStatUtils.applyCustomEffects(mPlugin, player, item);

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
			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					//If time to drink is finished, add effects. Otherwise, play sound of slurping every 0.5 seconds for 3.5 seconds total
					if (mTicks >= DRINK_DURATION) {
						ItemStack potion = mPotionsConsumed.remove(player.getUniqueId());
						ItemStatUtils.applyCustomEffects(mPlugin, player, potion);

						Starvation.apply(player, starvation);

						ItemStack invItem = clickedInventory.getItem(slot);
						//If Sacred Provisions check passes, do not consume, but do not enable cancel quick drink function
						//Do not run addition on infinity potions
						if (potion != null && ItemStatUtils.getEnchantmentLevel(potion, EnchantmentType.INFINITY) == 0 &&
							    NonClericProvisionsPassive.testRandomChance(player)) {
							NonClericProvisionsPassive.sacredProvisionsSound(player);

							if (invItem == null || invItem.getType().isAir() || invItem.getType().equals(Material.GLASS_BOTTLE)) {
								clickedInventory.setItem(slot, potion);
							} else {
								clickedInventory.addItem(potion);
							}
						} else {
							//Remove glass bottle from inventory once drinked
							if (invItem != null && invItem.getType().equals(Material.GLASS_BOTTLE) && !invItem.hasItemMeta()) {
								clickedInventory.setItem(slot, null);
							}
						}

						this.cancel();
						mRunnables.remove(uuid);
					}
					float pitch = ((float) FastUtils.RANDOM.nextDouble() - 0.5f) * 0.05f; //Emulate drinking variation of pitch
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f + pitch);
					mTicks += DRINK_TICK_DELAY;
				}
			};
			runnable.runTaskTimer(mPlugin, 5, DRINK_TICK_DELAY); //Delay of 5 seconds before first drink in minecraft
			mRunnables.put(uuid, runnable);
			mPotionsConsumed.put(uuid, new ItemStack(item));
		}

		if (reduceStack) {
			item.setAmount(item.getAmount() - 1);
			if (!instantDrink && item.getAmount() == 0) {
				clickedInventory.setItem(slot, new ItemStack(Material.GLASS_BOTTLE));
			}
		}
	}

	private void rightClickSplashPotion(Player player, ItemStack item) {
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
		if (prevPotion == null || !ItemStatUtils.hasEnchantment(prevPotion, EnchantmentType.INFINITY)) {
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
		mPotionsConsumed.remove(player.getUniqueId());
		mCooldowns.remove(player.getUniqueId());
		runnable.cancel();
		mRunnables.remove(player.getUniqueId());
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
}
