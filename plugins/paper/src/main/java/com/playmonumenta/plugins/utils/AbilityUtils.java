package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class AbilityUtils {

	private static final String ARROW_REFUNDED_METAKEY = "ArrowRefunded";
	private static final String POTION_REFUNDED_METAKEY = "PotionRefunded";

	private static final Map<Player, Integer> INVISIBLE_PLAYERS = new HashMap<Player, Integer>();
	private static @Nullable BukkitRunnable invisTracker = null;

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
			int currentDuration = INVISIBLE_PLAYERS.getOrDefault(player, 0);
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

	public static void increaseHealingPlayer(Player player, int duration, double healBoost, String cause) {
		Plugin.getInstance().mEffectManager.addEffect(player, cause, new PercentHeal(duration, healBoost));
		if (healBoost < 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, duration, -1));
			player.sendActionBar(Component.text("You have reduced healing for " + duration / 20 + "s", NamedTextColor.DARK_RED));
		}
	}

	// the unluck potion effect does not increase nor decrease luck attribute
	public static void increaseDamageRecievedPlayer(Player player, int duration, double damageBoost, String cause) {
		Plugin.getInstance().mEffectManager.addEffect(player, cause, new PercentDamageReceived(duration, damageBoost));
		if (damageBoost > 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, duration, -1));
		} else {
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, -1));
		}
	}

	// the weakness potion effect does not increase nor decrease melee damage
	public static void increaseDamageDealtPlayer(Player player, int duration, double damageBoost, String cause) {
		Plugin.getInstance().mEffectManager.addEffect(player, cause, new PercentDamageDealt(duration, damageBoost));
		if (damageBoost < 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, -1));
		} else {
			player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, -1));
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
			if (item != null && InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
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


	public static boolean updateAlchemistItem(ItemStack item, int count) {
		ItemMeta meta = item.getItemMeta();

		if (item.getType() == Material.SPLASH_POTION) {
			PotionMeta potionMeta = (PotionMeta) meta;

			if (count >= 4) {
				potionMeta.setColor(Color.GREEN);
			} else if (count == 3) {
				potionMeta.setColor(Color.YELLOW);
			} else if (count == 2) {
				potionMeta.setColor(Color.ORANGE);
			} else if (count == 1) {
				potionMeta.setColor(Color.RED);
			} else {
				potionMeta.setColor(Color.BLACK);
			}
			item.setItemMeta(potionMeta);
		}

		if (meta.hasDisplayName() && meta.getDisplayName().contains("Alchemist's Bag")) {
			meta.setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Alchemist's Bag (" + count + ")");
			item.setItemMeta(meta);
			ItemUtils.setPlainTag(item);
			return true;
		}
		return false;
	}

	/**
	 * Refunds the shot arrow if possible
	 *
	 * @param player
	 * @param arrow
	 * @return Whether the arrow was refunded or not
	 */
	public static boolean refundArrow(Player player, AbstractArrow arrow) {
		// Do not refund extra arrows shot my multishot crossbows (or bows with infinity, though that is checked later on again)
		if (arrow.getPickupStatus() != AbstractArrow.PickupStatus.ALLOWED) {
			return false;
		}
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		//Only refund arrow once
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, ARROW_REFUNDED_METAKEY)) {
			if (ItemUtils.isSomeBow(mainHand)) {
				if (!mainHand.containsEnchantment(Enchantment.ARROW_INFINITE)) {

					arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
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
						// No arrow left - player must have shot their last arrow. Grab the arrow from the event and give it back to the player, then abort
						InventoryUtils.giveItem(player, arrow.getItemStack());
						return true;
					}

					//arrowStack has the count from before the arrow is shot
					//so if from bow we just keep the same amount
					//and if from crossbow we add one arrow
					ItemStack arrowStack = playerInv.getItem(arrowSlot);
					if (arrow.isShotFromCrossbow()) {
						int stackSize = arrowStack.getAmount();
						if (stackSize < arrowStack.getMaxStackSize()) {
							arrowStack.setAmount(stackSize + 1);
						} else {
							ItemStack clone = arrowStack.clone();
							clone.setAmount(1);
							InventoryUtils.giveItem(player, clone);
						}
					} else {
						playerInv.setItem(arrowSlot, arrowStack);
					}
					return true;
				}
			}
		}
		return false;
	}

	public static boolean refundPotion(Player player, ThrownPotion potion) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, POTION_REFUNDED_METAKEY)) {
			ItemStack item = potion.getItem();
			if (mainHand != null && mainHand.isSimilar(item) && !mainHand.containsEnchantment(Enchantment.ARROW_INFINITE)) {
				mainHand.setAmount(mainHand.getAmount() + 1);
				return true;
			}
		}
		return false;
	}

	public static String getClass(Player player) {
		int classVal = ScoreboardUtils.getScoreboardValue(player, "Class").orElse(0);
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
			return "No Class";
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
		int classVal = ScoreboardUtils.getScoreboardValue(player, "Specialization").orElse(0);
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
