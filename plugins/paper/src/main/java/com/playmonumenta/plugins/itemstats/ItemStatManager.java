package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStat.ItemStatPrioritySort;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class ItemStatManager implements Listener {

	public static class PlayerItemStats {

		public static class ItemStatsMap implements Iterable<Entry<ItemStat, Double>> {
			private final Map<ItemStat, Double> mMap = new LinkedHashMap<>();

			public void add(ItemStat stat, double value) {
				if (value != 0) {
					Double old = mMap.get(stat);
					if (old == null) {
						mMap.put(stat, value);
					} else {
						if (old + value == 0) {
							mMap.remove(stat);
						} else {
							mMap.put(stat, old + value);
						}
					}
				}
			}

			public double get(ItemStat stat) {
				Double value = mMap.get(stat);
				return value == null ? 0 : value;
			}

			@Override
			public Iterator<Entry<ItemStat, Double>> iterator() {
				return mMap.entrySet().iterator();
			}
		}

		private final Player mPlayer;
		private ItemStatsMap mArmorAddStats = new ItemStatsMap();
		private ItemStatsMap mArmorMultiplyStats = new ItemStatsMap();
		private ItemStatsMap mStats = new ItemStatsMap();

		public PlayerItemStats(Player player) {
			mPlayer = player;
			updateStats(true);
		}

		public PlayerItemStats(PlayerItemStats playerItemStats) {
			mPlayer = playerItemStats.getPlayer();
			mStats = playerItemStats.getItemStats();
		}

		public Player getPlayer() {
			return mPlayer;
		}

		public ItemStatsMap getItemStats() {
			return mStats;
		}

		public void updateStats(boolean updateAll) {
			PlayerInventory inventory = mPlayer.getInventory();
			updateStats(inventory.getItemInMainHand(), inventory.getItemInOffHand(), inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots(), updateAll);
		}

		public void updateStats(@Nullable ItemStack mainhand, @Nullable ItemStack offhand, @Nullable ItemStack head, @Nullable ItemStack chest, @Nullable ItemStack legs, @Nullable ItemStack feet, boolean updateAll) {
			ItemStatsMap newArmorAddStats;
			ItemStatsMap newMainhandAddStats = new ItemStatsMap();
			ItemStatsMap newArmorMultiplyStats;
			ItemStatsMap newMainhandMultiplyStats = new ItemStatsMap();
			ItemStatsMap newStats = new ItemStatsMap();

			if (updateAll) {
				newArmorAddStats = new ItemStatsMap();
				newArmorMultiplyStats = new ItemStatsMap();

				EnumMap<Slot, ItemStack> items = new EnumMap<>(Slot.class);
				items.put(Slot.OFFHAND, offhand);
				items.put(Slot.HEAD, head);
				items.put(Slot.CHEST, chest);
				items.put(Slot.LEGS, legs);
				items.put(Slot.FEET, feet);

				for (Slot slot : Slot.values()) {
					ItemStack item = items.get(slot);
					if (item == null || item.getType() == Material.AIR) {
						continue;
					}

					if (slot.equals(Slot.OFFHAND) && ItemUtils.isArmorOrWearable(item) && !ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)) {
						continue;
					}

					NBTItem nbt = new NBTItem(item);
					NBTCompound enchantments = ItemStatUtils.getEnchantments(nbt);
					NBTCompound infusions = ItemStatUtils.getInfusions(nbt);
					NBTCompoundList attributes = ItemStatUtils.getAttributes(nbt);

					for (ItemStat stat : ITEM_STATS) {
						if (stat instanceof Attribute attribute) {
							newArmorAddStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.ADD, slot));
							newArmorMultiplyStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.MULTIPLY, slot));
						} else if (stat instanceof Enchantment enchantment) {
							if (enchantment.getName().equals("MainhandOffhandDisable") && ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()) > 0) {
								break;
							}
							if (enchantment.getSlots().contains(slot)) {
								newArmorAddStats.add(stat, ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()));
							}
							if (enchantment.getName().equals("RegionScalingDamageTaken") && (ItemStatUtils.getRegion(item) == ItemStatUtils.Region.ISLES || ItemStatUtils.getRegion(item) == ItemStatUtils.Region.RING)) {
								newArmorAddStats.add(stat, 1);
							}
						} else if (stat instanceof Infusion infusion) {
							newArmorAddStats.add(stat, ItemStatUtils.getInfusionLevel(infusions, infusion.getInfusionType()));
						}
					}
				}
			} else {
				newArmorAddStats = mArmorAddStats;
				newArmorMultiplyStats = mArmorMultiplyStats;
			}

			if (mainhand != null && mainhand.getType() != Material.AIR && !ItemUtils.isArmorOrWearable(mainhand)) {
				NBTItem nbt = new NBTItem(mainhand);
				NBTCompound enchantments = ItemStatUtils.getEnchantments(nbt);
				NBTCompound infusions = ItemStatUtils.getInfusions(nbt);
				NBTCompoundList attributes = ItemStatUtils.getAttributes(nbt);

				for (ItemStat stat : ITEM_STATS) {
					if (stat instanceof Attribute attribute) {
						newMainhandAddStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.ADD, Slot.MAINHAND));
						newMainhandMultiplyStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.MULTIPLY, Slot.MAINHAND));
					} else if (stat instanceof Enchantment enchantment) {
						if (enchantment.getName().equals("OffhandMainhandDisable") && ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()) > 0) {
							break;
						}
						if (enchantment.getSlots().contains(Slot.MAINHAND)) {
							newMainhandAddStats.add(stat, ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()));
						}
					} else if (stat instanceof Infusion infusion) {
						newMainhandAddStats.add(stat, ItemStatUtils.getInfusionLevel(infusions, infusion.getInfusionType()));
					}
				}
			}

			for (ItemStat stat : ITEM_STATS) {
				if (newArmorAddStats.get(stat) + newMainhandAddStats.get(stat) == 0 && newArmorMultiplyStats.get(stat) + newMainhandMultiplyStats.get(stat) != 0 && stat instanceof Attribute) {
					newStats.add(stat, 1 + newArmorMultiplyStats.get(stat) + newMainhandMultiplyStats.get(stat));
				} else {
					newStats.add(stat, (newArmorAddStats.get(stat) + newMainhandAddStats.get(stat)) * (1 + newArmorMultiplyStats.get(stat) + newMainhandMultiplyStats.get(stat)));
				}
				if (stat.getName().equals("CritScaling") || stat.getName().equals("AntiCritScaling") ||
					stat.getName().equals("WeaknessApply") || stat.getName().equals("StrengthApply") ||
					stat.getName().equals("WeaknessCancel") || stat.getName().equals("StrengthCancel")) {
					newStats.add(stat, 1);
				}
				if (stat.getName().equals("RegionScalingDamageDealt") && (ItemStatUtils.getRegion(mainhand) == ItemStatUtils.Region.ISLES || ItemStatUtils.getRegion(mainhand) == ItemStatUtils.Region.RING)) {
					newStats.add(stat, 1);
				}
			}

			mArmorAddStats = newArmorAddStats;
			mArmorMultiplyStats = newArmorMultiplyStats;
			mStats = newStats;

			//Tell the ItemStats that there has been an update\
			Plugin plugin = Plugin.getInstance();
			for (ItemStat stat : ITEM_STATS) {
				stat.onEquipmentUpdate(plugin, mPlayer);
			}
		}

		public void print() {
			MessagingUtils.sendRawMessage(mPlayer, "");
			for (Entry<ItemStat, Double> entry : mStats) {
				MessagingUtils.sendRawMessage(mPlayer, String.format("%s: %f", entry.getKey().getName(), entry.getValue()));
			}
		}
	}

	private static final List<ItemStat> ITEM_STATS = new ArrayList<>();

	static {
		for (AttributeType type : AttributeType.values()) {
			if (type.getItemStat() != null) {
				ITEM_STATS.add(type.getItemStat());
			}
		}

		for (EnchantmentType type : EnchantmentType.values()) {
			if (type.getItemStat() != null) {
				ITEM_STATS.add(type.getItemStat());
			}
		}

		for (InfusionType type : InfusionType.values()) {
			if (type.getItemStat() != null) {
				ITEM_STATS.add(type.getItemStat());
			}
		}
		Collections.sort(ITEM_STATS, new ItemStatPrioritySort());
	}

	private final Plugin mPlugin;
	private final BukkitRunnable mTimer;
	private final Map<UUID, PlayerItemStats> mPlayerItemStatsMappings = new HashMap<>();
	private static final int PERIOD = 5;

	public ItemStatManager(Plugin plugin) {
		mPlugin = plugin;

		mTimer = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += PERIOD;
				boolean twoHertz = mTicks % 10 == 0;
				boolean oneHertz = mTicks % 20 == 0;

				try {
					try {
						for (Entry<UUID, PlayerItemStats> entry : mPlayerItemStatsMappings.entrySet()) {
							Player player = entry.getValue().getPlayer();
							tick(mPlugin, player, twoHertz, oneHertz);
						}
				} catch (Exception ex) {
						Plugin.getInstance().getLogger().severe("Error in item stat manager tick: " + ex.getMessage());
						ex.printStackTrace();
					}
				} catch (Exception ex) {
					Plugin.getInstance().getLogger().severe("SEVERE error in item stat manager ticking task that caused many pieces to be skipped: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		};

		mTimer.runTaskTimer(plugin, 0, PERIOD);
	}

	public Map<UUID, PlayerItemStats> getPlayerItemStatsMappings() {
		return mPlayerItemStatsMappings;
	}

	private void updateStatsDelayed(Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
					mPlayerItemStatsMappings.get(player.getUniqueId()).updateStats(true);
				}
			}
		}.runTaskLater(mPlugin, 0);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		mPlayerItemStatsMappings.put(event.getPlayer().getUniqueId(), new PlayerItemStats(event.getPlayer()));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlayerItemStatsMappings.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useItemInHand() == Result.DENY) {
			return;
		}

		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && ItemUtils.isArmor(event.getItem())) {
			updateStatsDelayed(event.getPlayer());
		}
	}

	// It is possible to switch items fast enough that we need an exact stat update
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerItemHeldEvent(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			mPlayerItemStatsMappings.get(player.getUniqueId()).updateStats(player.getInventory().getItem(event.getNewSlot()), null, null, null, null, null, false);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		Entity entity = event.getEntity();
		if (!event.isCancelled() && entity instanceof Player player) {
			updateStatsDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void inventoryClickEvent(InventoryClickEvent event) {
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player player) {
			updateStatsDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void inventoryDragEvent(InventoryDragEvent event) {
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player player) {
			updateStatsDelayed(player);
		}
	}

	public @Nullable PlayerItemStats getPlayerItemStats(Player player) {
		return mPlayerItemStatsMappings.get(player.getUniqueId());
	}

	public double getEnchantmentLevel(Player player, EnchantmentType type) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			return mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats().get(type.getItemStat());
		} else {
			return 0;
		}
	}

	public double getInfusionLevel(Player player, InfusionType type) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			return mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats().get(type.getItemStat());
		} else {
			return 0;
		}
	}

	public double getAttributeAmount(Player player, AttributeType type) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			return mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats().get(type.getItemStat());
		} else {
			return 0;
		}
	}

	public void tick(Plugin plugin, Player player, boolean twoHz, boolean oneHz) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().tick(plugin, player, entry.getValue(), twoHz, oneHz);
			}
		}
	}

	public void onDamage(Plugin plugin, Player player, DamageEvent event, LivingEntity enemy) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			onDamage(plugin, player, mPlayerItemStatsMappings.get(player.getUniqueId()), event, enemy);
		}
	}

	public void onDamage(Plugin plugin, Player player, PlayerItemStats stats, DamageEvent event, LivingEntity enemy) {
		for (Entry<ItemStat, Double> entry : stats.getItemStats()) {
			entry.getKey().onDamage(plugin, player, entry.getValue(), event, enemy);
		}
	}

	public void onHurt(Plugin plugin, Player player, DamageEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onHurt(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onHurtByEntity(Plugin plugin, Player player, DamageEvent event, Entity damager) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onHurtByEntity(plugin, player, entry.getValue(), event, damager);
			}
		}
	}

	public void onHurtByEntityWithSource(Plugin plugin, Player player, DamageEvent event, Entity damager, LivingEntity source) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onHurtByEntityWithSource(plugin, player, entry.getValue(), event, damager, source);
			}
		}
	}

	public void onHurtFatal(Plugin plugin, Player player, DamageEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onHurtFatal(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onLaunchProjectile(Plugin plugin, Player player, ProjectileLaunchEvent event, Projectile projectile) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onLaunchProjectile(plugin, player, entry.getValue(), event, projectile);
			}
		}
	}

	public void onKill(Plugin plugin, Player player, EntityDeathEvent event, LivingEntity enemy) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onKill(plugin, player, entry.getValue(), event, enemy);
			}
		}
	}

	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onBlockBreak(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onPlayerInteract(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onConsume(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onRegain(Plugin plugin, Player player, EntityRegainHealthEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onRegain(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onItemDamage(Plugin plugin, Player player, PlayerItemDamageEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onItemDamage(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onExpChange(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onDeath(Plugin plugin, Player player, PlayerDeathEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onDeath(plugin, player, entry.getValue(), event);
			}
		}
	}

	/*
	 * Watch for spawned items
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void itemSpawnEvent(ItemSpawnEvent event) {
		checkSpawnedItem(event.getEntity());
	}

	/*
	 * Chunk loading an item entity also counts as spawning
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof Item) {
				checkSpawnedItem((Item)entity);
			}
		}
	}

	private void checkSpawnedItem(Item item) {
		if (item != null) {
			ItemStack stack = item.getItemStack();
			if (stack != null) {
				NBTItem nbt = new NBTItem(stack);
				NBTCompound enchantments = ItemStatUtils.getEnchantments(nbt);
				Map<String, ItemStatUtils.EnchantmentType> revMap = ItemStatUtils.EnchantmentType.REVERSE_MAPPINGS;

				for (String ench : ItemStatUtils.EnchantmentType.SPAWNABLE_ENCHANTMENTS) {
					int level = ItemStatUtils.getEnchantmentLevel(enchantments, revMap.get(ench));
					if (level > 0) {
						revMap.get(ench).getItemStat().onSpawn(mPlugin, item, level);
					}
				}

				NBTCompound infusions = ItemStatUtils.getInfusions(nbt);
				Map<String, ItemStatUtils.InfusionType> revMapInfusions = ItemStatUtils.InfusionType.REVERSE_MAPPINGS;

				for (String infusion : ItemStatUtils.InfusionType.SPAWNABLE_INFUSIONS) {
					int level = ItemStatUtils.getInfusionLevel(infusions, revMapInfusions.get(infusion));
					if (level > 0) {
						revMapInfusions.get(infusion).getItemStat().onSpawn(mPlugin, item, level);
					}
				}
			}
		}
	}

	public FixedMetadataValue getPlayerItemStatsMetadata(Player player) {
		return new FixedMetadataValue(mPlugin, new PlayerItemStats(getPlayerItemStats(player)));
	}
}
