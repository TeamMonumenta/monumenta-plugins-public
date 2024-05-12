package com.playmonumenta.plugins.nodeplanner;

import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.jetbrains.annotations.Nullable;

public class NodePlanner implements Listener {

	public static final String COMMAND = "nodeplanner";
	public static final String TAG = "node_planner";
	private static @Nullable NodePlanner INSTANCE = null;

	private final Map<UUID, NodePlan> mPlanByEntityUuid = new HashMap<>();
	private final Set<NodePlan> mDespawningPlans = new HashSet<>();

	private NodePlanner() {
		INSTANCE = this;
	}

	// Must first be called onLoad, may then be used onEnable
	public static NodePlanner getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new NodePlanner();
		}

		return INSTANCE;
	}

	public static void registerCommands() {
		CommandPermission perms = CommandPermission.fromString("monumenta.nodeplanner");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("summon_plan"));
		arguments.add(new LocationArgument("Location"));
		arguments.add(new FloatArgument("GB Per Block", 1.0f, 100.0f));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Location loc = args.getUnchecked("Location");
				float gigabytesPerBlock = args.getUnchecked("GB Per Block");
				runSummonPlan(sender, loc, gigabytesPerBlock);
			})
			.register();
	}

	public static void runSummonPlan(CommandSender sender, Location location, float gigabytesPerBlock) throws WrapperCommandSyntaxException {
		String fullPath = Plugin.getInstance().getDataFolder() + File.separator + "node_memory_info.json";
		JsonObject planObject;
		try {
			planObject = FileUtils.readJson(fullPath);
		} catch (Exception ex) {
			sender.sendMessage(Component.text("Failed to load node_memory_info.json"));
			MessagingUtils.sendStackTrace(sender, ex);
			throw CommandAPI.failWithString("Unable to load node_memory_info.json");
		}

		NodePlanner nodePlanner = getInstance();
		NodePlan nodePlan = new NodePlan(location, gigabytesPerBlock, planObject);
		for (UUID entityUuid : nodePlan.getEntityUuids()) {
			nodePlanner.mPlanByEntityUuid.put(entityUuid, nodePlan);
		}
	}

	// Clean up entities after shard restart (crash or shutdown, doesn't matter)
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void interactionLoadEvent(EntitiesLoadEvent event) {
		for (Entity entity : event.getEntities()) {
			if (entity.getScoreboardTags().contains(TAG)) {
				entity.remove();
			}
		}
	}

	// Clean up unloaded entities
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void interactionDespawnEvent(EntityRemoveEvent event) {
		Entity gone = event.getEntity();
		NodePlan victimPlan = mPlanByEntityUuid.get(gone.getUniqueId());
		if (victimPlan == null) {
			return;
		}
		if (mDespawningPlans.add(victimPlan)) {
			victimPlan.killEntities();
			mPlanByEntityUuid.values().removeIf(victimPlan::equals);
			Bukkit.getScheduler()
				.runTaskLater(Plugin.getInstance(), () -> mDespawningPlans.remove(victimPlan), 2);
		}
	}

	// Interaction events (attack key, typically left click)
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void interactionDamageEvent(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player player) {
			if (event.getEntity() instanceof Interaction interaction) {
				onInteract(player, interaction);
			}
		}
	}

	// Interaction events (use key, typically right click)
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void interactionInteractEvent(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Interaction interaction) {
			onInteract(event.getPlayer(), interaction);
		}
	}

	private void onInteract(Player player, Interaction interaction) {
		NodePlan plan = mPlanByEntityUuid.get(interaction.getUniqueId());
		if (plan == null) {
			if (interaction.getScoreboardTags().contains(TAG)) {
				player.sendMessage(Component.text("This isn't part of an active plan! " + interaction.getName()));
			}
			return;
		}
		plan.onInteract(player, interaction);
	}
}
