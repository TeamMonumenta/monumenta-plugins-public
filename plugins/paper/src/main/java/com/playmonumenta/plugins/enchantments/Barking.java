package com.playmonumenta.plugins.enchantments;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

//Barking: At level one, has a 1/5 chance to make a dog noise when breaking stripped wood.
//At level two, functions similar to clucking/baaing/oinking but with dog noises.
//If the player has Debarking, cancel barking sounds.
public class Barking implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Barking";
	private static final int DROPPED_TICK_PERIOD = 60;
	public static boolean hasDebarking = false;
	//Holy crap there's a lot of stripped wood in minecraft.
	public static List<Material> listOfStrippedWood = Arrays.asList(Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD, Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_OAK_LOG, Material.STRIPPED_OAK_WOOD, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_SPRUCE_WOOD);

	/* This is shared by all instances */
	private static int staticTicks = 0;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return true;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.INVENTORY);
	}

	@Override
	public boolean hasOnSpawn() {
		return true;
	}

	@Override
	public void applyProperty(Plugin plugin, Player player, int level) {
		//When equipment is updated, check if the player has debarking.
		boolean hasBarking = false;
		List<String> lore = player.getInventory().getItemInMainHand().getItemMeta().getLore();
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Barking")) {
				hasBarking = true;
				break;
			}
			hasBarking = false;
		}

		if (hasBarking == true) {
			for (String loreEntry : lore) {
				if (loreEntry.contains(ChatColor.GRAY + "Debarking")) {
					hasDebarking = true;
					return;
				}
			}
			hasDebarking = false;
		}
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item, int level) {
		if ((level == 1) && (hasDebarking == false)) {
			if (listOfStrippedWood.contains(event.getBlock().getType())) {
				int num = FastUtils.RANDOM.nextInt(5);
				if (num == 0) {
					//Congratulations, you're our lucky winner!
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
				}
			}
		}
	}

	@Override
	public void tick(Plugin plugin, World world, Player player, int level) {
		if ((level > 1) && (hasDebarking == false)) {

			staticTicks++;

			/*
			 * Max - 60 items or more is every 3s (60 ticks)
			 * Min - 1 item is every 33s (650 ticks)
			 */
			if (level > 60) {
				level = 60;
			}

			int modulo = 60 + (600 - (level * 10));

			// Since this is only called once per second
			modulo = modulo / 20;

			if (staticTicks % modulo == 0) {
				world.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);
			}
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, int level) {
		if ((level > 1) && (hasDebarking == false)) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (item == null || item.isDead()) {
						this.cancel();
					}

					item.getWorld().playSound(item.getLocation(), Sound.ENTITY_WOLF_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.8f);

					// Very infrequently check if the item is still actually there
					mTicks++;
					if (mTicks > 200) {
						mTicks = 0;
						if (!EntityUtils.isStillLoaded(item)) {
							this.cancel();
						}
					}
				}
			}.runTaskTimer(plugin, 10, DROPPED_TICK_PERIOD);

		}
	}
}
