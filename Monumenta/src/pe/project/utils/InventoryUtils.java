package pe.project.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import pe.project.Plugin;

public class InventoryUtils {
	private static int OFFHAND_SLOT = 40;
	private static int HELMET_SLOT = 39;
	private static int CHESTPLATE_SLOT = 38;
	private static int LEGGINGS_SLOT = 37;
	private static int BOOTS_SLOT = 36;

	public static void scheduleDelayedEquipmentCheck(Plugin plugin, Player player) {
		player.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				ItemStack offHand = player.getInventory().getItemInOffHand();

				plugin.getClass(player).PlayerItemHeldEvent(player, mainHand, offHand);
				plugin.mTrackingManager.mPlayers.updateEquipmentProperties(player);
			}
		}, 0);
	}

	public static boolean testForItemWithLore(ItemStack item, String loreText) {
		if (loreText == null || loreText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.contains(loreText)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	// TODO: This will *not* match items that don't have an NBT name (stick, stone sword, etc.)
	public static boolean testForItemWithName(ItemStack item, String nameText) {
		if (nameText == null || nameText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				String displayName = meta.getDisplayName();
				if (displayName != null && !displayName.isEmpty()) {
					if (displayName.contains(nameText)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static int getCustomEnchantLevel(ItemStack item, String nameText) {
		if (nameText == null || nameText.isEmpty()) {
			return 0;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.contains(nameText)) {
							if (loreEntry.contains(" V")) {
								return 5;
							}
							else if (loreEntry.contains("IV")) {
								return 4;
							}
							else if (loreEntry.contains("III")) {
								return 3;
							}
							else if (loreEntry.contains("II")) {
								return 2;
							}

							else
							{
								return 1;
							}
						}
					}
				}
			}
		}

		return 0;
	}

	public static double meleeEnchants(Player player, Entity damagee, double damage, DamageCause cause) {
		ItemStack weapon = player.getInventory().getItemInMainHand();
		if (testForItemWithLore(weapon, "7Chaotic")) {
			int chaoticLevel = getCustomEnchantLevel(weapon, "7Chaotic");

			Random mRandom = new Random();
			int rand = mRandom.nextInt(2 * chaoticLevel + 1) - chaoticLevel;

			if (rand > 0) {
				ParticleUtils.playParticlesInWorld(damagee.getWorld(), Particle.DAMAGE_INDICATOR, damagee.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.001);
			}

			if (cause == DamageCause.ENTITY_SWEEP_ATTACK) {
				rand = rand / 2;
			}

			damage = damage + rand;
		}

		if (testForItemWithLore(weapon, "7Ice Aspect")) {
			int iceLevel = getCustomEnchantLevel(weapon, "7Ice Aspect");
			int duration = iceLevel * 4 * 20 + 20;
			LivingEntity target = (LivingEntity)damagee;
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 2, false, true));
			ParticleUtils.playParticlesInWorld(damagee.getWorld(), Particle.SNOWBALL, damagee.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);

			if (damagee instanceof Blaze) {
				damage = damage + 1.0;
			}
		}

		if (testForItemWithLore(weapon, "7Slayer")) {
			int slayerLevel = getCustomEnchantLevel(weapon, "7Slayer");

			if (damagee instanceof Creeper || damagee instanceof Blaze || damagee instanceof Enderman || damagee instanceof Endermite) {
				damage = damage + 2.5 * slayerLevel;
			}
		}

		if (testForItemWithLore(weapon, "7Thunder")) {
			int thunderLevel = getCustomEnchantLevel(weapon, "7Thunder");

			Random mRandom = new Random();
			double rand = mRandom.nextDouble();

			if (damage >= 4 && rand < thunderLevel * 0.1) {

				LivingEntity target = (LivingEntity)damagee;
				target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 8, false, true));
				target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 50, 8, false, true));

				if (damagee instanceof Guardian) {
					damage = damage + 1.0;
				}

				player.getWorld().playSound(player.getLocation(), "entity.lightning.impact", 0.4f, 1.0f);
			}
		}

		return damage;
	}

	public static boolean isAxeItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOOD_AXE || mat == Material.STONE_AXE || mat == Material.GOLD_AXE
					|| mat == Material.IRON_AXE || mat == Material.DIAMOND_AXE;
		}

		return false;
	}

	public static boolean isBowItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.BOW;
		}

		return false;
	}

	public static boolean isSwordItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOOD_SWORD || mat == Material.STONE_SWORD || mat == Material.GOLD_SWORD
					|| mat == Material.IRON_SWORD || mat == Material.DIAMOND_SWORD;
		}

		return false;
	}

	public static boolean isPickaxeItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOOD_PICKAXE || mat == Material.STONE_PICKAXE || mat == Material.GOLD_PICKAXE
					|| mat == Material.IRON_PICKAXE || mat == Material.DIAMOND_PICKAXE;
		}

		return false;
	}

	public static boolean isScytheItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOOD_HOE || mat == Material.STONE_HOE || mat == Material.GOLD_HOE
					|| mat == Material.IRON_HOE || mat == Material.DIAMOND_HOE;
		}

		return false;
	}

	public static boolean isWandItem(ItemStack item) {
		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null && meta.hasLore()) {
				List<String> lore = meta.getLore();

				if (!lore.isEmpty()) {
					for (int i = 0; i < lore.size(); i++) {
						if (lore.get(i).contains("Magic Wand")) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public static boolean isSoulboundToPlayer(ItemStack item, Player player) {
		// TODO: Needs to handle renames
		return testForItemWithLore(item, "* Soulbound to " + player.getName() + " *");
	}

	public static boolean isPotionItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.POTION || mat == Material.LINGERING_POTION || mat == Material.SPLASH_POTION;
		}

		return false;
	}

	public static void removeRandomEquipment(Random rand, LivingEntity mob, Integer piecesToRemove) {
		int[] equipment = { 0, 1, 2, 3 };
		_shuffleArray(rand, equipment);

		EntityEquipment gear = mob.getEquipment();

		int removedCount = 0;
		for (int i = 0; i < equipment.length; i++) {
			if (removedCount == 2) {
				return;
			}

			//	Head Slot
			if (equipment[i] == 0) {
				if (gear.getHelmet().getType() != Material.AIR) {
					gear.setHelmet(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
			//	Chestplate
			else if (equipment[i] == 1) {
				if (gear.getChestplate().getType() != Material.AIR) {
					gear.setChestplate(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
			//	Legs
			else if (equipment[i] == 2) {
				if (gear.getLeggings().getType() != Material.AIR) {
					gear.setLeggings(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
			//	Boots
			else if (equipment[i] == 3) {
				if (gear.getBoots().getType() != Material.AIR) {
					gear.setBoots(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
		}
	}

	public static void removeSpecialItems(Player player) {
		//	Clear inventory
		_removeSpecialItemsFromInventory(player.getInventory());

		//	Clear Ender Chest
		_removeSpecialItemsFromInventory(player.getEnderChest());
	}

	private static void _removeSpecialItemsFromInventory(Inventory inventory) {
		for (ItemStack item : inventory.getContents()) {
			if (item != null) {
				if (_containsSpecialLore(item)) {
					inventory.removeItem(item);
				} else {
					if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
						BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
						if (meta.getBlockState() instanceof ShulkerBox) {
							ShulkerBox shulker = (ShulkerBox)meta.getBlockState();
							_removeSpecialItemsFromInventory(shulker.getInventory());

							meta.setBlockState(shulker);
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
	}

	private static boolean _containsSpecialLore(ItemStack item) {
		return	testForItemWithLore(item, "D4 Key") ||
				testForItemWithLore(item, "D5 Key");
	}

	public static String toBase64(Inventory inventory) throws IllegalStateException {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());

            // Save every element in the list
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to save item stacks.", e);
		}
	}

	public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

			//	Write the size of the inventory.
			dataOutput.writeInt(items.length);

			//	Save all the elements.
			for (int i = 0; i < items.length; i++) {
				dataOutput.writeObject(items[i]);
			}

			//	Serialize the array.
			dataOutput.close();
			return Base64Coder.encodeLines(outputStream.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to save item stacks.", e);
		}
	}

	public static Inventory fromBase64(String data) throws IOException {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

            // Read the serialized inventory
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();
            return inventory;
		} catch (ClassNotFoundException e) {
			throw new IOException("Unable to decode class type.", e);
		}
	}

	public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
    	try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
            	items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
	}

	public static ItemStack getItemFromEquipment(EntityEquipment equipment, EquipmentSlot slot) {
		switch (slot) {
		case HEAD:		return equipment.getHelmet();
		case CHEST:		return equipment.getChestplate();
		case LEGS:		return equipment.getLeggings();
		case FEET:		return equipment.getBoots();
		case HAND:		return equipment.getItemInMainHand();
		case OFF_HAND:	return equipment.getItemInOffHand();
		}

		return null;
	}

	public static boolean isArmorSlotFromId(int slotId) {
		return slotId == OFFHAND_SLOT || slotId == HELMET_SLOT || slotId == CHESTPLATE_SLOT || slotId == LEGGINGS_SLOT || slotId == BOOTS_SLOT;
	}

	static void _shuffleArray(Random rand, int[] ar)
	  {
	    for (int i = ar.length - 1; i > 0; i--)
	    {
	      int index = rand.nextInt(i + 1);
	      // Simple swap
	      int a = ar[index];
	      ar[index] = ar[i];
	      ar[i] = a;
	    }
	  }
}
