package com.playmonumenta.plugins.plots;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class PlotAccessManager {
	private static class PlotAccessEntry {
		final String mName;
		final long mExpiration;

		private PlotAccessEntry(String name, long expiration) {
			mName = name;
			mExpiration = expiration;
		}

		private @Nonnull JsonObject toJson() {
			JsonObject obj = new JsonObject();
			obj.addProperty("name", mName);
			obj.addProperty("expiration", mExpiration);
			return obj;
		}

		private boolean isExpired() {
			if (mExpiration <= 0) {
				return false;
			} else {
				return mExpiration < java.time.Instant.now().getEpochSecond();
			}
		}

		private static @Nonnull PlotAccessEntry fromJson(JsonObject obj) {
			return new PlotAccessEntry(obj.get("name").getAsString(), obj.get("expiration").getAsLong());
		}
	}

	private static class PlotEntry {
		/* This is the exact same key that is used by the hash table to index this entry.
		 * It's stored in the hash table, not via JSON here, to minimize that duplication as much as possible
		 */
		final String mCoordsKey;
		final String mOwnerName;
		final UUID mOwnerUUID;
		final Map<UUID, PlotAccessEntry> mAccess;

		private PlotEntry(String coordsKey, String ownerName, UUID ownerUUID, Map<UUID, PlotAccessEntry> access) {
			mCoordsKey = coordsKey;
			mOwnerName = ownerName;
			mOwnerUUID = ownerUUID;
			mAccess = access;
		}

		private boolean hasAccess(UUID tested) {
			PlotAccessEntry entry = mAccess.get(tested);
			return entry != null && !entry.isExpired();
		}

		private boolean hasAnyNonExpired() {
			for (PlotAccessEntry entry : mAccess.values()) {
				if (!entry.isExpired()) {
					return true;
				}
			}
			return false;
		}

		private void addAccess(Player player, long expiration) {
			mAccess.put(player.getUniqueId(), new PlotAccessEntry(player.getName(), expiration));
		}

		private String[] getSuggestions() {
			String[] ret = new String[mAccess.size()];

			int i = 0;
			for (PlotAccessEntry entry : mAccess.values()) {
				ret[i] = entry.mName;
			}

			return ret;
		}

		/* Returns null if there is no access left (all removed or it was expired) */
		private @Nullable JsonObject toJson() {
			if (!hasAnyNonExpired()) {
				return null;
			}

			JsonObject obj = new JsonObject();
			obj.addProperty("owner_name", mOwnerName);
			obj.addProperty("owner_uuid", mOwnerUUID.toString());

			JsonObject access = new JsonObject();
			for (Map.Entry<UUID, PlotAccessEntry> entry : mAccess.entrySet()) {
				if (!entry.getValue().isExpired()) {
					access.add(entry.getKey().toString(), entry.getValue().toJson());
				}
			}
			obj.add("access", access);
			return obj;
		}

		private static @Nonnull PlotEntry fromJson(String coordsKey, JsonObject obj) {
			Map<UUID, PlotAccessEntry> access = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : obj.get("access").getAsJsonObject().entrySet()) {
				access.put(UUID.fromString(entry.getKey()), PlotAccessEntry.fromJson(entry.getValue().getAsJsonObject()));
			}

			return new PlotEntry(coordsKey, obj.get("owner_name").getAsString(), UUID.fromString(obj.get("owner_uuid").getAsString()), access);
		}
	}

	private final Logger mLogger;
	private final String mConfigPath;
	private final Map<String, PlotEntry> mPlots = new HashMap<>();
	/* A read-only map that is useful for a player to see what plots they have access to */
	private final Map<UUID, List<PlotEntry>> mReverseIndex = new HashMap<>();

	public PlotAccessManager(Logger logger, String configPath) {
		mLogger = logger;
		mConfigPath = configPath;

		registerCommands();
		load();
	}

	private void registerCommands() {
		/********************* help *********************/
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.NONE)
			.executes((sender, args) -> {
				help(sender);
			})
			.register();
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.NONE)
			.withArguments(new MultiLiteralArgument("help"))
			.executes((sender, args) -> {
				help(sender);
			})
			.register();

		/********************* LIST *********************/
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.NONE)
			.withArguments(new MultiLiteralArgument("list"))
			.executes((sender, args) -> {
				list(getSenderPlayer(sender));
			})
			.register();

		/********************* ADD *********************/
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.NONE)
			.withArguments(new MultiLiteralArgument("add"))
			.withArguments(new StringArgument("name").overrideSuggestions((sender) -> {
				return Bukkit.getOnlinePlayers().stream().map((player) -> player.getName()).toArray(String[]::new);
			}))
			.executes((sender, args) -> {
				add(getSenderPlayer(sender), (String)args[1], null);
			})
			.register();
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.NONE)
			.withArguments(new MultiLiteralArgument("add"))
			.withArguments(new StringArgument("name").overrideSuggestions((sender) -> {
				return Bukkit.getOnlinePlayers().stream().map((player) -> player.getName()).toArray(String[]::new);
			}))
			.withArguments(new StringArgument("duration"))
			.executes((sender, args) -> {
				add(getSenderPlayer(sender), (String)args[1], (String)args[2]);
			})
			.register();

		/********************* REMOVE *********************/
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.NONE)
			.withArguments(new MultiLiteralArgument("remove"))
			.withArguments(new StringArgument("name").overrideSuggestions((sender) -> getRemoveSuggestions(sender)))
			.executes((sender, args) -> {
				remove(getSenderPlayer(sender), (String)args[1]);
			})
			.register();

		/********************* TEST *********************/
		new CommandAPICommand("plotaccess")
			.withPermission(CommandPermission.fromString("monumenta.plotaccess.test"))
			.withArguments(new MultiLiteralArgument("test"))
			.withArguments(new LocationArgument("location"))
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				PlotEntry plot = mPlots.get(getPlotCoordsKey((Location)args[1]));
				if (plot == null) {
					// No plot at these coordinates with an access list
					return 0;
				}

				/* Returns 0 if no access, 1 if on the approved access list */
				return plot.hasAccess(((Player)args[2]).getUniqueId()) ? 1 : 0;
			})
			.register();
	}

	private void help(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "");
		sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "/plotaccess help");
		sender.sendMessage(ChatColor.GREEN + "This command is used to give other people access to your plot");
		sender.sendMessage(ChatColor.GREEN + "/plotaccess " + ChatColor.AQUA + "list");
		sender.sendMessage(ChatColor.GREEN + "  Lists access to your plot and other plots you can access");
		sender.sendMessage(ChatColor.GREEN + "/plotaccess " + ChatColor.AQUA + "add playerName optionalDuration");
		sender.sendMessage(ChatColor.GREEN + "  Lets " + ChatColor.AQUA + "playerName" + ChatColor.GREEN + " access your plot");
		sender.sendMessage(ChatColor.GREEN + "  Player must be online and connected to this plots shard");
		sender.sendMessage(ChatColor.GREEN + "  Use " + ChatColor.AQUA + "optionalDuration" + ChatColor.GREEN + " to indicate when access expires");
		sender.sendMessage(ChatColor.GREEN + "  For example 5d3h for 5 days 3 hours; max 365 days");
		sender.sendMessage(ChatColor.GREEN + "/plotaccess " + ChatColor.AQUA + "remove playerName");
		sender.sendMessage(ChatColor.GREEN + "  Removes " + ChatColor.AQUA + "playerName" + ChatColor.GREEN + " from access your plot");
		sender.sendMessage(ChatColor.GREEN + "  Will *not* teleport them out if they are already there!");
		sender.sendMessage(ChatColor.GREEN + "  (or if they logged out there)");
	}

	private void list(Player owner) {
		String plotCoordsKey = getPlotCoordsKey(owner);
		PlotEntry plot = mPlots.get(plotCoordsKey);
		if (plot == null || !plot.hasAnyNonExpired()) {
			owner.sendMessage(ChatColor.GREEN + "There are no players with access to your plot");
		} else {
			owner.sendMessage(ChatColor.GREEN + "These players have access to your plot at " + plotCoordsKey + ":");

			for (PlotAccessEntry access : plot.mAccess.values()) {
				if (!access.isExpired()) {
					if (access.mExpiration <= 0) {
						owner.sendMessage(ChatColor.GREEN + "  " + ChatColor.AQUA + access.mName);
					} else {
						owner.sendMessage(ChatColor.GREEN + "  " + ChatColor.AQUA + access.mName + ChatColor.GREEN + " Expires: " + MessagingUtils.getTimeDifferencePretty(access.mExpiration));
					}
				}
			}
		}

		List<PlotEntry> otherAccess = mReverseIndex.get(owner.getUniqueId());
		boolean anyNonExpired = false;
		if (otherAccess != null) {
			for (PlotEntry other : otherAccess) {
				PlotAccessEntry access = other.mAccess.get(owner.getUniqueId());
				if (access != null && !access.isExpired()) {
					anyNonExpired = true;
				}
			}
		}
		if (otherAccess == null || !anyNonExpired) {
			owner.sendMessage(ChatColor.GREEN + "You don't have access to any other player's plot");
		} else {
			owner.sendMessage(ChatColor.GREEN + "You have access to these other plots:");
			for (PlotEntry other : otherAccess) {
				PlotAccessEntry access = other.mAccess.get(owner.getUniqueId());
				if (access != null && !access.isExpired()) {
					if (access.mExpiration <= 0) {
						owner.sendMessage(ChatColor.GREEN + "  " + other.mCoordsKey + " " + ChatColor.AQUA + other.mOwnerName);
					} else {
						owner.sendMessage(ChatColor.GREEN + "  " + other.mCoordsKey + " " + ChatColor.AQUA + other.mOwnerName + ChatColor.GREEN + " Expires: " + MessagingUtils.getTimeDifferencePretty(access.mExpiration));
					}
				}
			}
		}
	}

	private void add(Player owner, String addedName, String duration) throws WrapperCommandSyntaxException {
		Player added = Bukkit.getPlayer(addedName);
		if (added == null) {
			CommandAPI.fail("Player '" + addedName + "' not found. They must be online and currently on this plots shard");
		}

		String plotCoordsKey = getPlotCoordsKey(owner);
		PlotEntry plot = mPlots.get(plotCoordsKey);
		if (plot == null) {
			/* Not currently in the map because no access - create it */
			plot = new PlotEntry(plotCoordsKey, owner.getName(), owner.getUniqueId(), new HashMap<>());
			mPlots.put(plotCoordsKey, plot);
		}

		long expiration = -1;
		if (duration != null) {
			duration = duration.toUpperCase();
			if (duration.contains("D")) {
				duration = "P" + (duration.endsWith("D") ? duration : duration.replaceAll("D", "DT"));
			} else {
				duration = "PT" + duration;
			}
			try {
				expiration = Duration.parse(duration).getSeconds();
				if (expiration > 365 * 24 * 60 * 60) {
					CommandAPI.fail("Duration must be at most 1 year");
				} else if (expiration < 60) {
					CommandAPI.fail("Duration must be at least 1 minute");
				}

				// Add the current time, so we store the time when it expires, not the delta
				expiration += java.time.Instant.now().getEpochSecond();
			} catch (Exception ex) {
				CommandAPI.fail(ex.getMessage());
			}
		}

		if (added.getUniqueId().equals(owner.getUniqueId())) {
			CommandAPI.fail("You can not add yourself to your own plot");
		}

		plot.addAccess(added, expiration);
		owner.sendMessage(ChatColor.GREEN + "Successfully added " + ChatColor.AQUA + added.getName() + ChatColor.GREEN + " to access your plot");
		added.sendMessage(ChatColor.GREEN + "You now have access to " + ChatColor.AQUA + owner.getName() + ChatColor.GREEN + "'s plot");

		save();
	}

	private void remove(Player owner, String removedName) {
		PlotEntry plot = mPlots.get(getPlotCoordsKey(owner));
		if (plot == null) {
			owner.sendMessage(ChatColor.GREEN + "There are no players with access to your plot");
			return;
		}

		Iterator<Map.Entry<UUID, PlotAccessEntry>> iter = plot.mAccess.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<UUID, PlotAccessEntry> entry = iter.next();
			/* Check both key and value in case the user types in the player's UUID */
			if (entry.getKey().toString().equalsIgnoreCase(removedName) || entry.getValue().mName.equalsIgnoreCase(removedName)) {
				iter.remove();

				owner.sendMessage(ChatColor.GREEN + "Player '" + removedName + "' no longer has access to your plot");
				save();
				return;
			}
		}

		owner.sendMessage(ChatColor.GREEN + "Player '" + removedName + "' did not have access to your plot");
	}

	private String[] getRemoveSuggestions(CommandSender sender) {
		if (!(sender instanceof Player)) {
			/* This should never happen... */
			return new String[0];
		}
		Player player = (Player)sender;

		PlotEntry plot = mPlots.get(getPlotCoordsKey(player));
		if (plot == null) {
			// No plot at these coordinates with an access list
			return new String[0];
		}

		/* Returns 0 if no access, 1 if on the approved access list */
		return plot.getSuggestions();
	}

	private Player getSenderPlayer(CommandSender sender) throws WrapperCommandSyntaxException {
		if (!(sender instanceof Player)) {
			CommandAPI.fail("This command can only be run by players");
		}
		return (Player)sender;
	}

	/* Gets the player's plot coords based on scores. If no plot, return null */
	private @Nullable String getPlotCoordsKey(Player player) {
		int x = ScoreboardUtils.getScoreboardValue(player, "plotx");
		int y = ScoreboardUtils.getScoreboardValue(player, "ploty");
		int z = ScoreboardUtils.getScoreboardValue(player, "plotz");

		if (y <= 0) {
			return null;
		}

		return String.format("%d,%d,%d", x, y, z);
	}

	/* Gets a Location's coordinates to a compatible string key */
	private @Nullable String getPlotCoordsKey(Location loc) {
		return String.format("%d,%d,%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	/* Called once at startup */
	private void load() {
		mPlots.clear();

		try {
			String content = FileUtils.readFile(mConfigPath);
			if (content == null || content.isEmpty()) {
				mLogger.warning("Plots access file '" + mConfigPath + "' is empty - defaulting to no plot access");
			} else {
				Gson gson = new Gson();
				JsonObject obj = gson.fromJson(content, JsonObject.class);
				for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
					try {
						mPlots.put(entry.getKey(), PlotEntry.fromJson(entry.getKey(), entry.getValue().getAsJsonObject()));
					} catch (Exception e) {
						mLogger.severe("Failed to load plot entry: " + e);
						e.printStackTrace();
					}
				}
			}
		} catch (FileNotFoundException e) {
			mLogger.warning("Plots access file '" + mConfigPath + "' does not exist - defaulting to no plot access");
		} catch (Exception e) {
			mLogger.severe("Caught exception: " + e);
			e.printStackTrace();
		}

		updateReverseIndex();
	}

	/* Called each time a change is made to plot access. Does not need to be called during plugin shutdown */
	private void save() {
		updateReverseIndex();

		JsonObject obj = new JsonObject();

		for (Map.Entry<String, PlotEntry> entry : mPlots.entrySet()) {
			JsonObject serialized = entry.getValue().toJson();
			if (serialized != null) {
				/* serialized plot will be null if all its entries expired / were removed */
				obj.add(entry.getKey(), serialized);
			}
		}

		/* Do the actual file IO on an async thread */
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			String tempFilePath = mConfigPath + ".tmp";
			try {
				FileUtils.writeFile(tempFilePath, new GsonBuilder().setPrettyPrinting().create().toJson(obj));
			} catch (Exception e) {
				mLogger.severe("Caught exception saving file '" + tempFilePath + "': " + e);
				e.printStackTrace();
			}
			try {
				FileUtils.moveFile(tempFilePath, mConfigPath);
			} catch (Exception e) {
				mLogger.severe("Caught exception renaming file '" + tempFilePath + "' to '" + mConfigPath + "': " + e);
				e.printStackTrace();
			}
		});
	}

	private void updateReverseIndex() {
		mReverseIndex.clear();
		for (Map.Entry<String, PlotEntry> plot : mPlots.entrySet()) {
			for (UUID uuid : plot.getValue().mAccess.keySet()) {
				List<PlotEntry> accessList = mReverseIndex.get(uuid);
				if (accessList == null) {
					accessList = new LinkedList<>();
					mReverseIndex.put(uuid, accessList);
				}
				accessList.add(plot.getValue());
			}
		}
	}
}
