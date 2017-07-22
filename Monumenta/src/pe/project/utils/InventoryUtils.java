package pe.project.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public class InventoryUtils {
	public static boolean testForItemWithLore(ItemStack item, String loreText) {
		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (int i = 0; i < lore.size(); i++) {
						if (lore.get(i).contains(loreText)) {
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	public static boolean isAxeItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOOD_AXE || mat == Material.STONE_AXE || mat == Material.GOLD_AXE
					|| mat == Material.IRON_AXE || mat == Material.DIAMOND_AXE;
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
