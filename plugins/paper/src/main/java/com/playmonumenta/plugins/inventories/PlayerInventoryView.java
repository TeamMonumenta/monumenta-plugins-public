package com.playmonumenta.plugins.inventories;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.point.Raycast;
import com.playmonumenta.plugins.point.RaycastData;
import com.playmonumenta.scriptedquests.utils.InventoryUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerInventoryView implements Listener {
	private static final String PERMISSION = "monumenta.peb.inventoryview";
	private static List<Player> mPlayers = new ArrayList<>(10);
	private static List<Inventory> mInventories = new ArrayList<>(10);

	@EventHandler(priority = EventPriority.LOW)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (!event.getAction().equals(Action.LEFT_CLICK_AIR) && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			return;
		}

		Player player = event.getPlayer();
		if (player == null) {
			return;
		}

		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if (mainHand != null
		    && mainHand.getType().equals(Material.WRITTEN_BOOK)
		    && InventoryUtils.testForItemWithLore(mainHand, "Skin :")
			&& InventoryUtils.testForItemWithLore(mainHand, "Soulbound to")
			&& player.hasPermission(PERMISSION)) {

			Location eyeLoc = player.getEyeLocation();
			Raycast ray = new Raycast(eyeLoc, eyeLoc.getDirection(), 3);
			ray.mThroughBlocks = false;
			ray.mTargetPlayers = true;
			ray.mThroughNonOccluding = false;
			ray.mTargetNonPlayers = false;

			RaycastData data = ray.shootRaycast();
			List<LivingEntity> entities = data.getEntities();
			if (entities != null && !entities.isEmpty()) {
				//Below if check is almost certainly not necessary, but always be careful
				if (data.getEntities().get(0) instanceof Player) {
					Player clickedPlayer = (Player)data.getEntities().get(0);
					if (!(clickedPlayer).equals(player)) {
						inventoryView(event.getPlayer(), clickedPlayer);
					}
				}

			}
		}
	}

	//This handles clicking, shift clicking, double clicking, etc.
	@EventHandler(priority = EventPriority.LOW)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (mPlayers.contains(event.getWhoClicked())
		    || mInventories.contains(event.getClickedInventory())
		    || mInventories.contains(event.getInventory())) {
			event.setCancelled(true);
		}
	}

	//Just as a precaution
	@EventHandler(priority = EventPriority.LOW)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (mPlayers.contains(event.getWhoClicked()) || mInventories.contains(event.getInventory())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		mPlayers.remove(event.getPlayer());
		mInventories.remove(event.getInventory());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerLoginEvent(PlayerLoginEvent event) {
		mPlayers.remove(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlayers.remove(event.getPlayer());
	}

	public void inventoryView(Player player, Player clickedPlayer) {
		//Make sure whoever is getting hit with a PEB doesnt have the tag that opts them out of this feature
		if (clickedPlayer.getScoreboardTags().contains("inventoryPrivacy")) {
			player.sendMessage(ChatColor.RED + "This player has opted out of inventory viewing.");
			return;
		}
		//Added for tracking to prevent them from clicking on stuff
		mPlayers.add(player);

		PlayerInventory playInv = clickedPlayer.getInventory();
		Inventory openInv = Bukkit.createInventory(null, 18, Component.text(clickedPlayer.getName() + "'s Inventory"));
		mInventories.add(openInv);

		//Set the fake inventory's top row to be the armor and offhand of the player
		openInv.setItem(0, playInv.getHelmet());
		openInv.setItem(1, playInv.getChestplate());
		openInv.setItem(2, playInv.getLeggings());
		openInv.setItem(3, playInv.getBoots());
		openInv.setItem(4, playInv.getItemInOffHand());

		//Set the fake inventory's bottom row to be the players hotbar
		openInv.setItem(9, playInv.getItem(0));
		openInv.setItem(10, playInv.getItem(1));
		openInv.setItem(11, playInv.getItem(2));
		openInv.setItem(12, playInv.getItem(3));
		openInv.setItem(13, playInv.getItem(4));
		openInv.setItem(14, playInv.getItem(5));
		openInv.setItem(15, playInv.getItem(6));
		openInv.setItem(16, playInv.getItem(7));
		openInv.setItem(17, playInv.getItem(8));

		player.openInventory(openInv);
	}
}
