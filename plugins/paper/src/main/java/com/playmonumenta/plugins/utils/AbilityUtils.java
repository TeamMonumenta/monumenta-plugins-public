package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;



public class AbilityUtils {

	private static final String ARROW_REFUNDED_METAKEY = "ArrowRefunded";
	private static final String POTION_REFUNDED_METAKEY = "PotionRefunded";

	private static final Map<Player, Integer> INVISIBLE_PLAYERS = new HashMap<Player, Integer>();
	private static BukkitRunnable invisTracker = null;

	public static final String IGNORE_TAG = "summon_ignore";

	private static void startInvisTracker(Plugin plugin) {
		invisTracker = new BukkitRunnable() {
			@Override
			public void run() {
				if (INVISIBLE_PLAYERS.isEmpty()) {
					this.cancel();
				} else {
					Iterator<Entry<Player, Integer>> iter = INVISIBLE_PLAYERS.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<Player, Integer> entry = iter.next();

						Player player = entry.getKey();
						if (!player.isValid() || player.isDead() || !player.isOnline()) {
							iter.remove();
							continue;
						}

						ItemStack item = player.getInventory().getItemInMainHand();
						if (entry.getValue() <= 0) {
							// Run after this loop is complete to avoid concurrent modification
							Bukkit.getScheduler().runTask(plugin, () -> removeStealth(plugin, player, false));
						} else if (item == null || item.getType().isAir() || (!ItemUtils.isAxe(item) && !ItemUtils.isSword(item) && !ItemUtils.isHoe(item))) {
							// Run after this loop is complete to avoid concurrent modification
							Bukkit.getScheduler().runTask(plugin, () -> removeStealth(plugin, player, true));
						} else {
							player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().clone().add(0, 0.5, 0), 1, 0.35, 0.25, 0.35, 0.05f);
							entry.setValue(entry.getValue() - 1);
						}
					}
				}
			}
		};
		invisTracker.runTaskTimer(plugin, 0, 1);
	}

	public static boolean isStealthed(Player player) {
		return INVISIBLE_PLAYERS.containsKey(player);
	}

	public static void removeStealth(Plugin plugin, Player player, boolean inflictPenalty) {
		Location loc = player.getLocation();
		World world = player.getWorld();

		world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f);
		world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_HURT, 0.6f, 0.5f);

		plugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);

		INVISIBLE_PLAYERS.remove(player);

		if (inflictPenalty) {
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 3, 1));
		}
	}

	public static void applyStealth(Plugin plugin, Player player, int duration) {
		Location loc = player.getLocation();
		World world = player.getWorld();

		world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f);
		world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, 1f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 0.5f, 2f);

		if (!isStealthed(player)) {
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
			INVISIBLE_PLAYERS.put(player, duration);
		} else {
			int currentDuration = INVISIBLE_PLAYERS.get(player);
			plugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INVISIBILITY, duration + currentDuration, 0));
			INVISIBLE_PLAYERS.put(player, duration + currentDuration);
		}

		if (invisTracker == null || invisTracker.isCancelled()) {
			startInvisTracker(plugin);
		}

		for (LivingEntity entity : EntityUtils.getNearbyMobs(player.getLocation(), 64)) {
			if (entity instanceof Mob) {
				Mob mob = (Mob) entity;
				if (mob.getTarget() != null && mob.getTarget().getUniqueId().equals(player.getUniqueId())) {
					mob.setTarget(null);
				}
			}
		}
	}

	private static final String ABILITY_SILENCE_EFFECT_NAME = "AbilitySilence";

	public static void silencePlayer(Player player, int tickDuration) {
		Plugin.getInstance().mEffectManager.addEffect(player, ABILITY_SILENCE_EFFECT_NAME, new AbilitySilence(tickDuration));
	}

	public static void unsilencePlayer(Player player) {
		Plugin.getInstance().mEffectManager.clearEffects(player, ABILITY_SILENCE_EFFECT_NAME);
	}

	public static ItemStack getAlchemistPotion() {
		//TODO get from loot tables instead
		ItemStack itemStack = new ItemStack(Material.SPLASH_POTION, 1);
		PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

		potionMeta.setBasePotionData(new PotionData(PotionType.MUNDANE));
		potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS); // Hide "No Effects" vanilla potion effect lore
		potionMeta.setColor(Color.WHITE);
		String plainName = "Alchemist's Potion";
		potionMeta.setDisplayName(ChatColor.AQUA + plainName); // OG Alchemist's Potion item name colour of &b

		List<String> loreList = Arrays.asList(
			ChatColor.DARK_GRAY + "A unique potion for Alchemists." // Standard Monumenta lore text colour of &8
		);
		potionMeta.setLore(loreList);

		itemStack.setItemMeta(potionMeta);
		ItemUtils.setPlainTag(itemStack); // Support for resource pack textures like with other items & mechanisms
		return itemStack;
	}

	public static void addAlchemistPotions(Player player, int numAddedPotions) {
		if (numAddedPotions == 0) {
			return;
		}

		Inventory inv = player.getInventory();
		ItemStack firstFoundPotStack = null;
		int potCount = 0;

		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				if (firstFoundPotStack == null) {
					firstFoundPotStack = item;
				}
				potCount += item.getAmount();
			}
		}

		if (potCount < 32) {
			if (firstFoundPotStack != null) {
				firstFoundPotStack.setAmount(firstFoundPotStack.getAmount() + numAddedPotions);
			} else {
				ItemStack newPotions = getAlchemistPotion();
				newPotions.setAmount(numAddedPotions);
				inv.addItem(newPotions);
			}
		}
	}

	// You can't just use a negative value with the add method if the potions to be remove are distributed across multiple stacks
	// Returns false if the player doesn't have enough potions in their inventory
	public static boolean removeAlchemistPotions(Player player, int numPotionsToRemove) {
		Inventory inv = player.getInventory();
		List<ItemStack> potionStacks = new ArrayList<ItemStack>();
		int potionCount = 0;

		// Make sure the player has enough potions
		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				potionCount += item.getAmount();
				potionStacks.add(item);
				if (potionCount >= numPotionsToRemove) {
					break;
				}
			}
		}

		if (potionCount >= numPotionsToRemove) {
			for (ItemStack potionStack : potionStacks) {
				if (potionStack.getAmount() >= numPotionsToRemove) {
					potionStack.setAmount(potionStack.getAmount() - numPotionsToRemove);
					break;
				} else {
					numPotionsToRemove -= potionStack.getAmount();
					potionStack.setAmount(0);
					if (numPotionsToRemove == 0) {
						break;
					}
				}
			}

			return true;
		}

		return false;
	}

	public static boolean updateAlchemistItem(@NotNull ItemStack item, int count) {
		ItemMeta meta = item.getItemMeta();

		if (meta.getDisplayName() != null) {
			if (meta.getDisplayName().contains("Alchemist's Bag")) {
				meta.setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Alchemist's Bag (" + count + ")");
				item.setItemMeta(meta);
				return true;
			}
		}
		return false;
	}

	public static void refundArrow(Player player, AbstractArrow arrow) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		//Only refund arrow once
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, ARROW_REFUNDED_METAKEY)) {
			if (ItemUtils.isSomeBow(mainHand) || ItemUtils.isSomeBow(offHand)) {
				int infLevel = Math.max(mainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE), offHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE));
				if (infLevel == 0) {
					arrow.setPickupStatus(Arrow.PickupStatus.ALLOWED);
					Inventory playerInv = player.getInventory();
					int firstArrow = playerInv.first(Material.ARROW);
					int firstTippedArrow = playerInv.first(Material.TIPPED_ARROW);
					int firstSpectralArrow = playerInv.first(Material.SPECTRAL_ARROW);
					final int arrowSlot;
					if (firstArrow == -1 && firstTippedArrow > -1 && firstSpectralArrow == -1) {
						arrowSlot = firstTippedArrow;
					} else if (firstArrow > - 1 && firstTippedArrow == -1 && firstSpectralArrow == -1) {
						arrowSlot = firstArrow;
					} else if (firstArrow == -1 && firstTippedArrow == -1 && firstSpectralArrow > -1) {
						arrowSlot = firstSpectralArrow;
					} else if (firstArrow > - 1 && firstTippedArrow > -1) {
						arrowSlot = Math.min(firstArrow, firstTippedArrow);
					} else if (firstArrow > -1 && firstSpectralArrow > -1) {
						arrowSlot = Math.min(firstArrow, firstSpectralArrow);
					} else if (firstTippedArrow > -1 && firstSpectralArrow > -1) {
						arrowSlot = Math.min(firstSpectralArrow, firstTippedArrow);
					} else if (firstTippedArrow > -1 && firstSpectralArrow > -1 && firstArrow > -1) {
						arrowSlot = Math.min(firstTippedArrow, Math.min(firstSpectralArrow, firstArrow));
					} else {
						return;
					}

					// Make sure the duplicate arrow can't be picked up
					arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
					// I'm not sure why this works, but it does.
					if (arrow.isShotFromCrossbow()) {
						playerInv.getItem(arrowSlot).setAmount(playerInv.getItem(arrowSlot).getAmount() + 1);
					} else {
						playerInv.setItem(arrowSlot, playerInv.getItem(arrowSlot));
					}
				}
			}
		}
	}

	public static void refundPotion(Player player, ThrownPotion potion) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, POTION_REFUNDED_METAKEY)) {
			ItemStack item = potion.getItem();
			if (mainHand != null && mainHand.isSimilar(item)) {
				mainHand.setAmount(mainHand.getAmount() + 1);
			} else if (offHand != null && offHand.isSimilar(item)) {
				offHand.setAmount(offHand.getAmount() + 1);
			}
		}
	}

	public static String getClass(Player player) {
		int classVal = ScoreboardUtils.getScoreboardValue(player, "Class");
		switch (classVal) {
		case 1:
			return "Mage";
		case 2:
			return "Warrior";
		case 3:
			return "Cleric";
		case 4:
			return "Rogue";
		case 5:
			return "Alchemist";
		case 6:
			return "Scout";
		case 7:
			return "Warlock";
		default:
			return "???";
		}
	}

	public static int getClass(String str) {
		switch (str) {
		case "Mage":
			return 1;
		case "Warrior":
			return 2;
		case "Cleric":
			return 3;
		case "Rogue":
			return 4;
		case "Alchemist":
			return 5;
		case "Scout":
			return 6;
		case "Warlock":
			return 7;
		default:
			return 0;
		}
	}

	public static String getSpec(Player player) {
		int classVal = ScoreboardUtils.getScoreboardValue(player, "Specialization");
		switch (classVal) {
		case 1:
			return "Arcanist";
		case 2:
			return "Elementalist";
		case 3:
			return "Berserker";
		case 4:
			return "Guardian";
		case 5:
			return "Paladin";
		case 6:
			return "Hierophant";
		case 7:
			return "Swordsage";
		case 8:
			return "Assassin";
		case 9:
			return "Harbinger";
		case 10:
			return "Apothecary";
		case 11:
			return "Ranger";
		case 12:
			return "Hunter";
		case 13:
			return "Reaper";
		case 14:
			return "Tenebrist";
		default:
			return "No Spec";
		}
	}

	public static int getSpec(String str) {
		switch (str) {
		case "Arcanist":
			return 1;
		case "Elementalist":
			return 2;
		case "Berserker":
			return 3;
		case "Guardian":
			return 4;
		case "Paladin":
			return 5;
		case "Hierophant":
			return 6;
		case "Swordsage":
			return 7;
		case "Assassin":
			return 8;
		case "Harbinger":
			return 9;
		case "Apothecary":
			return 10;
		case "Ranger":
			return 11;
		case "Hunter":
			return 12;
		case "Reaper":
			return 13;
		case "Tenebrist":
			return 14;
		default:
			return 0;
		}
	}

	public static boolean isBlocked(EntityDamageEvent event) {
		return event.getFinalDamage() <= 0;
	}
}