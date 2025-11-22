package com.playmonumenta.plugins.integrations.monumentanetworkrelay;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.networkrelay.DestOfflineEvent;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public class BroadcastedEvents implements Listener {
	public static final String BASE_BROADCAST_CHANNEL = "monumenta.eventbroadcast";
	public static final String UPDATE_BROADCAST_CHANNEL = BASE_BROADCAST_CHANNEL + ".update";
	public static final String PROXIED_TASK_BROADCAST_CHANNEL = BASE_BROADCAST_CHANNEL + ".proxied";

	public static final int BROADCAST_DELAY = 1200; // 1 minute

	private static final String SEPARATOR = "@";
	private static final int CHILD_CLEANING_DELAY = 3600; // 3 minutes

	private static final Argument<String> SHARD_ARGUMENT = new StringArgument("shard");
	private static final Argument<String> EVENT_NAME_ARGUMENT = new StringArgument("Event Name")
		.replaceSuggestions(ArgumentSuggestions.strings(KnownEvent.names()));

	@MonotonicNonNull
	private static BukkitTask mPeriodicUpdateTask;
	private static final ConcurrentMap<String, Event> mEventMap = new ConcurrentHashMap<>();

	public static ConcurrentMap<String, Event> getCurrentEvents() {
		return mEventMap;
	}

	/*
	 *How to implement in mechs.
	 *
	 *1st call (letting other shards know that an event is going to happen)
	 * - execute if <NOT INSTANT> run eventupdate update <EVENT NAME> <TIME TILL START>
	 * 2nd call (letting other shards know that the event has started (forced re-sync))
	 * - execute if <NOT INSTANT> run eventupdate update <EVENT NAME> 0
	 * 3rd call (letting other shards know the event ended)
	 * - papiexecute @p eventupdate clear %network-shard_relay% <EVENT NAME>
	 *
	 *Example for Kaul:
	 *In monumenta:kaul/initialize
	 * - execute if score $KaulInstant temp matches 0 run eventupdate update KAUL 360
	 *In monumenta:kaul/kaul_start_final
	 * - eventupdate update KAUL 0
	 *In monumenta:kaul/kaul_end_final And monumenta:kaul/kill
	 * - papiexecute @p eventupdate clear %network-shard_relay% KAUL
	 */

	public static void registerCommand(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.eventupdate");

		CommandAPICommand root = new CommandAPICommand("eventupdate");

		root.withSubcommands(
			new CommandAPICommand("clear")
				.withPermission(perms)
				.withArguments(EVENT_NAME_ARGUMENT)
				.executes((sender, args) -> {
					boolean isPlayerSender = sender instanceof Player;
					if (isPlayerSender && Objects.equals(ServerProperties.getShardName(), "build")) {
						throw CommandAPI.failWithString("You cannot use this command on the build shard");
					}
					String shard = getThisShardName();

					String eventName = args.getUnchecked("Event Name");
					int timeLeft = -1;

					runCommandLogic(sender, shard, eventName, timeLeft);
				}),
			new CommandAPICommand("clear")
				.withPermission(perms)
				.withArguments(List.of(SHARD_ARGUMENT, EVENT_NAME_ARGUMENT))
				.executes((sender, args) -> {
					boolean isPlayerSender = sender instanceof Player;
					if (isPlayerSender && Objects.equals(ServerProperties.getShardName(), "build")) {
						throw CommandAPI.failWithString("You cannot use this command on the build shard");
					}

					String shard = args.getUnchecked("shard");
					String eventName = args.getUnchecked("Event Name");
					int timeLeft = -1;

					runCommandLogic(sender, shard, eventName, timeLeft);
				})
		);

		root.withSubcommand(
			new CommandAPICommand("update")
				.withPermission(perms)
				.withArguments(EVENT_NAME_ARGUMENT, new IntegerArgument("Time Left"))
				.executes((sender, args) -> {
					boolean isPlayerSender = sender instanceof Player;
					if (isPlayerSender && Objects.equals(ServerProperties.getShardName(), "build")) {
						throw CommandAPI.failWithString("You cannot use this command on the build shard");
					}
					String shard = getThisShardName();

					String eventName = args.getUnchecked("Event Name");
					Integer timeLeft = args.getUnchecked("Time Left");

					runCommandLogic(sender, shard, eventName, timeLeft);
				})
		).register();
	}

	public static void updateEvent(String event, int timeLeft) {
		updateEvent(event, getThisShardName(), timeLeft);
	}

	public static void updateEvent(String event, String shard, int timeLeft) {
		runCommandLogic(null, shard, event, timeLeft);
	}

	public static void clearEvent(String event) {
		updateEvent(event, -1);
	}

	public static void clearEvent(String event, String shard) {
		updateEvent(event, shard, -1);
	}

	public static String getThisShardName() {
		String shard;
		try {
			shard = NetworkRelayAPI.getShardName();
		} catch (Exception ex) {
			//If failed use the short version.
			shard = ServerProperties.getShardName();
		}
		return shard;
	}

	private static void runCommandLogic(@Nullable CommandSender sender, String shard, String eventName, int timeLeft) {
		String expectedShard = getThisShardName();
		eventName = eventName.toUpperCase(Locale.getDefault());

		if (!Objects.equals(expectedShard, shard)) {
			if (timeLeft == -1) {
				//send clear task to the actual shard.
				JsonObject data = new JsonObject();
				data.addProperty(Event.EVENT_PROP_KEY, eventName);
				data.addProperty(Event.TIME_PROP_KEY, timeLeft);

				try {
					NetworkRelayAPI.sendMessage(shard, PROXIED_TASK_BROADCAST_CHANNEL, data);
					MMLog.finer("Sent event clear task to shard " + shard);
				} catch (Exception ex) {
					MMLog.warning("Failed to send task to shard " + shard, ex);
				}
			}
			return;
		}
		String key = shard + SEPARATOR + eventName;

		if (mEventMap.containsKey(key)) {
			if (timeLeft == -1) {
				Event event = mEventMap.get(key);
				if (event instanceof ParentEvent parentEvent) {
					event.mTimeLeft = -1;
					parentEvent.broadcastPeriodicUpdate();
					//send the clear update before deleting.
				}
				//clear locally.
				mEventMap.remove(key);

				String msg = "Sucessfully cleared event " + key + ".";
				if (sender instanceof Player player) {
					player.sendMessage(Component.text(msg, NamedTextColor.GOLD));
				}
				MMLog.finer(msg);
				return;
			}

			Event event = mEventMap.get(key);
			event.setTimeLeft(timeLeft);

			if (event instanceof ParentEvent parentEvent) {
				//force send an update.
				parentEvent.broadcastPeriodicUpdate();
			}

			String msg = "Sucessfully updated event " + key + " with duration " + timeLeft + ".";
			if (sender instanceof Player player) {
				player.sendMessage(Component.text(msg, NamedTextColor.GOLD));
			}
			MMLog.finer(msg);
		} else if (timeLeft != -1) {
			ParentEvent event = new ParentEvent(shard, eventName, timeLeft);
			mEventMap.put(key, event);

			String msg = "Successfully created new event " + key + " with duration " + timeLeft + ".";
			if (sender instanceof Player player) {
				player.sendMessage(Component.text(msg, NamedTextColor.GOLD));
			}
			MMLog.finer(msg);
		}
	}

	public static void registerTask(Plugin plugin) {
		mPeriodicUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			//clean up event if delay is passed.
			mEventMap.values().removeIf((event) -> event instanceof ChildEvent child && child.mReceivedAt + CHILD_CLEANING_DELAY < Bukkit.getCurrentTick());
			mEventMap.forEach((key, value) -> value.tick());
		}, 20, 20);
	}

	public static void cancelTask() {
		if (mPeriodicUpdateTask != null && !mPeriodicUpdateTask.isCancelled()) {
			mPeriodicUpdateTask.cancel();
		}
	}

	public static void registerChildEvent(ChildEvent event) {
		String key = event.mShard + SEPARATOR + event.mEventName;

		if (mEventMap.containsKey(key)) {
			if (!(mEventMap.get(key) instanceof ChildEvent)) {
				return;
			}

			if (event.mTimeLeft == -1) {
				//force-clear
				mEventMap.remove(key);
				MMLog.finer("Deleted child event: " + key);
				return;
			}

			//replace current version.
			mEventMap.put(key, event);
			MMLog.finer("Updated child event: " + key);
		} else if (event.mTimeLeft != -1) {
			mEventMap.put(key, event);
			MMLog.finer("Created new child event: " + key);
		}
	}

	public static List<Event> getPerceptibleEvents(Player player) {
		List<Event> perceptibleEvents = new ArrayList<>();

		for (Event event : getCurrentEvents().values()) {
			KnownEvent knownEvent = KnownEvent.get(event.mEventName.toUpperCase(Locale.getDefault()));
			if (knownEvent != KnownEvent.UNKNOWN && knownEvent.mPossibilities.length > 0) {
				boolean canSee = false;
				for (EventRequirement possibility : knownEvent.mPossibilities) {
					OptionalInt score = ScoreboardUtils.getScoreboardValue(player, possibility.mScoreboard);
					if (score.isPresent() && score.getAsInt() >= possibility.mStep) {
						canSee = true;
						break;
					}
				}

				if (!canSee) {
					continue;
				}
			}

			perceptibleEvents.add(event);
		}
		perceptibleEvents.sort(Comparator.comparingInt(curr -> (curr.mTimeLeft + (curr.mStatus == EventStatus.IN_PROGRESS ? 1_000_000 : 0))));
		return perceptibleEvents;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onDestOfflineEvent(@NotNull DestOfflineEvent event) {
		String destination = event.getDest();
		if (destination == null) {
			return;
		}

		//Clear all events from the given destination.
		List<String> toRemove = new ArrayList<>();
		for (Map.Entry<String, Event> entry : mEventMap.entrySet()) {
			if (entry.getValue().mShard.equals(destination)) {
				toRemove.add(entry.getKey());
			}
		}

		for (String key : toRemove) {
			mEventMap.remove(key);
		}

		if (!toRemove.isEmpty()) {
			MMLog.finer("[Boss Event Relay/Dest Offline] Caught remote destination '" + destination + "' going offline, cleared all related events.");
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		if (event.getChannel().equals(UPDATE_BROADCAST_CHANNEL)) {
			JsonObject data = event.getData();
			if (!(data.get(BroadcastedEvents.Event.SHARD_PROP_KEY) instanceof JsonPrimitive shardPrimitive && shardPrimitive.isString())) {
				MMLog.warning(UPDATE_BROADCAST_CHANNEL + " failed to parse required String field '" + BroadcastedEvents.Event.SHARD_PROP_KEY + "'");
				return;
			}

			if (!(data.get(Event.EVENT_PROP_KEY) instanceof JsonPrimitive eventPrimitive && eventPrimitive.isString())) {
				MMLog.warning(UPDATE_BROADCAST_CHANNEL + " failed to parse required String field '" + BroadcastedEvents.Event.EVENT_PROP_KEY + "'");
				return;
			}

			if (!(data.get(Event.TIME_PROP_KEY) instanceof JsonPrimitive timePrimitive && timePrimitive.isNumber())) {
				MMLog.warning(UPDATE_BROADCAST_CHANNEL + " failed to parse required int field '" + BroadcastedEvents.Event.SHARD_PROP_KEY + "'");
				return;
			}

			if (!(data.get(Event.STATUS_PROP_KEY) instanceof JsonPrimitive statusPrimitive && statusPrimitive.isString())) {
				MMLog.warning(UPDATE_BROADCAST_CHANNEL + " failed to parse required String field '" + BroadcastedEvents.Event.STATUS_PROP_KEY + "'");
				return;
			}

			String shard = data.getAsJsonPrimitive(BroadcastedEvents.Event.SHARD_PROP_KEY).getAsString();
			if (Objects.equals(NetworkRelayAPI.getShardName(), shard)) {
				//should already have the parent version so ignore message.
				return;
			}

			String eventName = data.getAsJsonPrimitive(BroadcastedEvents.Event.EVENT_PROP_KEY).getAsString();
			int timeLeft = data.getAsJsonPrimitive(BroadcastedEvents.Event.TIME_PROP_KEY).getAsInt();
			String eventStatus = data.getAsJsonPrimitive(BroadcastedEvents.Event.STATUS_PROP_KEY).getAsString().toUpperCase(Locale.getDefault());

			BroadcastedEvents.ChildEvent childEvent = new BroadcastedEvents.ChildEvent(shard, eventName, timeLeft);
			childEvent.mStatus = BroadcastedEvents.EventStatus.contains(eventStatus) ? BroadcastedEvents.EventStatus.valueOf(eventStatus) : BroadcastedEvents.EventStatus.UNKNOWN;


			MMLog.finer("[Boss Event Relay/Update] Caught child event: " + childEvent.mShard + "@" + childEvent.mEventName);
			registerChildEvent(childEvent);
		} else if (event.getChannel().equals(PROXIED_TASK_BROADCAST_CHANNEL)) {
			JsonObject data = event.getData();

			if (!(data.get(Event.EVENT_PROP_KEY) instanceof JsonPrimitive eventPrimitive && eventPrimitive.isString())) {
				MMLog.warning(PROXIED_TASK_BROADCAST_CHANNEL + " failed to parse required String field '" + BroadcastedEvents.Event.EVENT_PROP_KEY + "'");
				return;
			}

			if (!(data.get(Event.TIME_PROP_KEY) instanceof JsonPrimitive timePrimitive && timePrimitive.isNumber())) {
				MMLog.warning(PROXIED_TASK_BROADCAST_CHANNEL + " failed to parse required int field '" + BroadcastedEvents.Event.SHARD_PROP_KEY + "'");
				return;
			}

			String shard = NetworkRelayAPI.getShardName();
			String eventName = data.getAsJsonPrimitive(Event.EVENT_PROP_KEY).getAsString();
			int timeLeft = data.getAsJsonPrimitive(BroadcastedEvents.Event.TIME_PROP_KEY).getAsInt();

			MMLog.finer("[Boss Event Relay/Task] Received Task: " + shard + "@" + eventName);
			runCommandLogic(Bukkit.createCommandSender((feedback) -> {
				if (feedback instanceof TextComponent textFeedback) {
					MMLog.finer("[Boss Event Relay/Task] " + textFeedback.content());
				}
			}), shard, eventName, timeLeft);
		}
	}

	public static class ParentEvent extends Event {
		private int mLastUpdateSent = -1;

		public ParentEvent(String shard, String eventName, int timeLeft) {
			super(shard, eventName, timeLeft);
		}

		@Override
		public void tick() {
			super.tick();

			if (mLastUpdateSent == -1 || mLastUpdateSent + BROADCAST_DELAY <= Bukkit.getCurrentTick()) {
				broadcastPeriodicUpdate();
			}
		}

		protected void broadcastPeriodicUpdate() {
			ChildEvent event = toChildEvent();

			JsonObject data = new JsonObject();
			data.addProperty(SHARD_PROP_KEY, event.mShard);
			data.addProperty(EVENT_PROP_KEY, event.mEventName);
			data.addProperty(TIME_PROP_KEY, event.mTimeLeft);
			data.addProperty(STATUS_PROP_KEY, event.mStatus.name());

			try {
				/* Send packet through network relay. */
				NetworkRelayAPI.sendBroadcastMessage(UPDATE_BROADCAST_CHANNEL, data);
				MMLog.finer("Broadcasted child version of: " + mShard + SEPARATOR + mEventName);
			} catch (Exception ex) {
				MMLog.warning("Failed to send event periodic update for: " + mShard + SEPARATOR + mEventName, ex);
			}

			mLastUpdateSent = Bukkit.getCurrentTick();
		}

		public ChildEvent toChildEvent() {
			ChildEvent childEvent = new ChildEvent(mShard, mEventName, mTimeLeft);
			childEvent.mStatus = mStatus;
			return childEvent;
		}
	}

	public static class ChildEvent extends Event {
		private final int mReceivedAt;

		public ChildEvent(String shard, String eventName, int timeLeft) {
			super(shard, eventName, timeLeft);
			mReceivedAt = Bukkit.getCurrentTick();
		}
	}

	public static class Event {
		public static final String SHARD_PROP_KEY = "shard";
		public static final String EVENT_PROP_KEY = "eventName";
		public static final String TIME_PROP_KEY = "timeLeft";
		public static final String STATUS_PROP_KEY = "status";

		public final String mShard;
		public final String mEventName;

		//In seconds.
		public int mTimeLeft;

		public EventStatus mStatus;

		public Event(String shard, String eventName, int timeLeft) {
			mShard = shard;
			mEventName = eventName;
			mTimeLeft = timeLeft;
			mStatus = mTimeLeft > 0 ? EventStatus.STARTING : EventStatus.IN_PROGRESS;
		}

		public void tick() {
			if (mTimeLeft == 0) {
				return;
			}

			mTimeLeft--;
			if (mTimeLeft == 0) {
				mStatus = EventStatus.IN_PROGRESS;
			}
		}

		public void setTimeLeft(int timeLeft) {
			mTimeLeft = timeLeft;
			mStatus = timeLeft > 0 ? EventStatus.STARTING : EventStatus.IN_PROGRESS;
		}

		public Component getDisplay() {
			KnownEvent known = getAsKnownEvent();

			boolean unknown = known == KnownEvent.UNKNOWN;
			TextColor eventColor = unknown ? NamedTextColor.YELLOW : known.mColor;
			Component eventNamePart = Component.text(unknown ? StringUtils.capitalize(mEventName.toLowerCase(Locale.getDefault())) : known.mDisplayName, eventColor, TextDecoration.BOLD);

			Component shardPart = Component.text((known.mDisplayShard ? " " + mShard : "") + ": ", NamedTextColor.GOLD);

			Component statusPart;
			if (mTimeLeft > 0) {
				statusPart = Component.text(mTimeLeft + "s", NamedTextColor.GREEN);
			} else {
				TextColor statusColor = (mStatus.mColor == null) ? NamedTextColor.RED : mStatus.mColor;
				statusPart = Component.text(mStatus.mDisplayedAs, statusColor, TextDecoration.BOLD);
			}

			return Component.empty()
				.append(eventNamePart)
				.append(shardPart)
				.append(statusPart);
		}

		public KnownEvent getAsKnownEvent() {
			return KnownEvent.get(mEventName.toUpperCase(Locale.getDefault()));
		}
	}

	public record EventRequirement(String mScoreboard, int mStep) {
	}

	public enum KnownEvent {
		KAUL("Kaul", NamedTextColor.DARK_GREEN, true, new EventRequirement("Quest21", 20), new EventRequirement("Corrupted", 1)),
		ELDRASK("Eldrask", NamedTextColor.AQUA, true, new EventRequirement("Quest101", 12), new EventRequirement("Teal", 1)),
		HEKAWT("Hekawt", NamedTextColor.GOLD, true, new EventRequirement("Quest101", 12), new EventRequirement("Fred", 1)),
		SIRIUS("Sirius", NamedTextColor.GRAY, true, new EventRequirement("Quest220", 8), new EventRequirement("Zenith", 1)),
		ALOC_ACOC(HuntsManager.QuarryType.ALOC_ACOC),
		CORE_ELEMENTAL(HuntsManager.QuarryType.CORE_ELEMENTAL),
		STEEL_WING_HAWK(HuntsManager.QuarryType.STEEL_WING_HAWK),
		THE_IMPENETRABLE(HuntsManager.QuarryType.THE_IMPENETRABLE),
		UAMIEL(HuntsManager.QuarryType.UAMIEL),
		EXPERIMENT_SEVENTY_ONE(HuntsManager.QuarryType.EXPERIMENT_SEVENTY_ONE),
		UNKNOWN("Unknown", null, true);

		public final String mDisplayName;
		public final @Nullable TextColor mColor;
		public final boolean mDisplayShard;

		//If none, will not require anything.
		public final EventRequirement[] mPossibilities;

		KnownEvent(String displayName, @Nullable TextColor color, boolean displayShard, EventRequirement... possibleRequirements) {
			mDisplayName = displayName;
			mColor = color;
			mPossibilities = possibleRequirements;
			mDisplayShard = displayShard;
		}

		KnownEvent(HuntsManager.QuarryType quarryType) {
			this(quarryType.getName(), quarryType.getColor(), false, new EventRequirement("HuntsLodge", 2));
		}

		public static String[] names() {
			return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
		}

		public static @NotNull KnownEvent get(@NotNull String name) {
			if (isEventKnown(name)) {
				return Enum.valueOf(KnownEvent.class, name);
			} else {
				return UNKNOWN;
			}
		}

		public static boolean isEventKnown(String key) {
			for (KnownEvent value : values()) {
				if (value.name().equals(key)) {
					return true;
				}
			}
			return false;
		}
	}

	public enum EventStatus {
		STARTING("Starting", NamedTextColor.GREEN),
		IN_PROGRESS("In-Progress", NamedTextColor.RED),
		UNKNOWN("Unknown", null);

		public final String mDisplayedAs;

		@Nullable
		public final TextColor mColor;

		EventStatus(String renderedAs, @Nullable TextColor color) {
			this.mDisplayedAs = renderedAs;
			this.mColor = color;
		}

		public static boolean contains(String key) {
			boolean contained = false;
			for (EventStatus value : values()) {
				if (value.name().equals(key)) {
					contained = true;
					break;
				}
			}

			return contained;
		}
	}
}
