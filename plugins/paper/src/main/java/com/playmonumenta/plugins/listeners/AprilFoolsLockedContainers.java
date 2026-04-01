package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.gui.CosmeticsGUI;
import com.playmonumenta.plugins.custominventories.ClassDisplayCustomInventory;
import com.playmonumenta.plugins.custominventories.PlayerInventoryCustomInventory;
import com.playmonumenta.plugins.guis.lib.PagedGui;
import com.playmonumenta.plugins.guis.peb.PebGui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.itemstats.gui.PlayerItemStatsGUI;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Lockable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;

public class AprilFoolsLockedContainers implements Listener {
	// April Fools
	private static final List<PagedGui.PageType> PEB_PAGES = List.of(
		PebGui.MAIN_PAGE,
		PebGui.PLAYER_INFO_PAGE,
		PebGui.GAMEPLAY_OPTIONS_PAGE,
		PebGui.TECHNICAL_OPTIONS_PAGE,
		PebGui.TRADE_GUI_PAGE,
		PebGui.INTERACTABLE_OPTIONS_PAGE,
		PebGui.SERVER_INFO_PAGE,
		PebGui.BOOK_SKINS_PAGE,
		PebGui.PICKUP_AND_DISABLE_DROP_PAGE,
		PebGui.GLOWING_PAGE,
		PebGui.PARTIAL_PARTICLES_PAGE,
		PebGui.SOUND_CONTROLS_PAGE,
		PebGui.SOUND_CATEGORIES_PAGE,
		PebGui.SOUND_OVERWORLD_PLOTS_PAGE
	);
	private static final List<Consumer<PlayerInteractEvent>> LOCKED_CONTAINER_RESULTS = List.of(
		event -> {
			Plugin plugin = Plugin.getInstance();
			Player player = event.getPlayer();
			new CosmeticsGUI(plugin, player).openInventory(player, plugin);
		},
		event -> {
			Player player = event.getPlayer();
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("sqgui show regionqg " + player.getName());
		},
		event -> {
			Player player = event.getPlayer();
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("sqgui show valleyqg " + player.getName());
		},
		event -> {
			Player player = event.getPlayer();
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("sqgui show islesqg " + player.getName());
		},
		event -> {
			Player player = event.getPlayer();
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("sqgui show ringqg " + player.getName());
		},
		event -> {
			Player player = event.getPlayer();

			// Get a list of players to be selected at random, including self for viewing your own stats
			Set<String> visibleNames = MonumentaNetworkRelayIntegration.getVisiblePlayerNames();
			List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
			targets.remove(player);
			targets.removeIf(p -> !visibleNames.contains(p.getName()));
			targets.add(player);

			new PlayerInventoryCustomInventory(player, FastUtils.getRandomElement(targets), false)
				.openInventory(player, Plugin.getInstance());
		},
		event -> {
			Player player = event.getPlayer();

			// Get a list of players to be selected at random, including self for viewing your own stats
			Set<String> visibleNames = MonumentaNetworkRelayIntegration.getVisiblePlayerNames();
			List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
			targets.remove(player);
			targets.removeIf(p -> !visibleNames.contains(p.getName()));
			targets.removeIf(p -> AbilityUtils.getPlayerClass(p) == null);
			if (AbilityUtils.getPlayerClass(player) != null) {
				targets.add(player);
			}

			if (targets.isEmpty()) {
				Block clickedBlock = event.getClickedBlock();
				if (clickedBlock == null) {
					return;
				}

				Bukkit.getScheduler().runTaskLater(
					Plugin.getInstance(),
					() -> player.playSound(clickedBlock.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1, 1),
					10L
				);
				return;
			}

			new ClassDisplayCustomInventory(player, FastUtils.getRandomElement(targets), false).open();
		},
		event -> {
			Player player = event.getPlayer();

			// Get a list of players to be selected at random, including null for viewing your own stats
			Set<String> visibleNames = MonumentaNetworkRelayIntegration.getVisiblePlayerNames();
			List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
			targets.remove(player);
			targets.removeIf(p -> !visibleNames.contains(p.getName()));
			targets.add(null);

			new PlayerItemStatsGUI(player, FastUtils.getRandomElement(targets), false)
				.openInventory(player, Plugin.getInstance());
		},
		event -> {
			Player player = event.getPlayer();

			new PebGui(player, FastUtils.getRandomElement(PEB_PAGES)).open();
		}
	);

	public static void openLockedContainerGui(Player player, Block block, PlayerInteractEvent event) {
		player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1, 1);
		FastUtils.getRandomElement(LOCKED_CONTAINER_RESULTS).accept(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		// Do nothing unless right-clicking a block
		if (!Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
			return;
		}

		// Do nothing if interaction is not allowed
		if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
			return;
		}

		// Ignore non-lockable blocks
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		if (!(block.getState() instanceof Lockable lockable)) {
			return;
		}
		if (!lockable.isLocked()) {
			return;
		}

		// Ignore sneaking players and players with the key to open the lock
		Player player = event.getPlayer();
		ItemMeta itemMeta = player.getInventory().getItemInMainHand().getItemMeta();
		if (
			player.isSneaking() ||
			!(
				itemMeta == null ||
				itemMeta.displayName() == null ||
				!lockable.getLock().equals(MessagingUtils.plainText(itemMeta.displayName()))
			)
		) {
			return;
		}

		// If the player is allowed, and the random chance is met, open the April Fools inventory
		if (handleEvent(event)) {
			event.setCancelled(true);
		}
	}

	public static boolean handleEvent(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (block == null) {
			return false;
		}
		if (!MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "AprilFoolsLock")) {
			return false;
		}

		// If the player is allowed, and the random chance is met, open the April Fools inventory
		if (
			player.hasPermission("monumenta.feature.april_fools_locked") &&
			FastUtils.randomIntInRange(1, 800) <= 1
		) {
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> openLockedContainerGui(player, block, event), 1L);
			return true;
		}

		return false;
	}
}
