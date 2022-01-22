package com.playmonumenta.bungeecord.voting;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.integrations.NetworkRelayIntegration;
import com.playmonumenta.bungeecord.utils.FileUtils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/*
 * This is all of the voting data for one player
 * This includes their "scores", and when they will be eligible to vote next
 * It is saved to disk every time it is updated - there is no need to save it again when stopping bungee or the player logs out
 */
public class VoteContext {
	private final ReadWriteLock mLock = new ReentrantReadWriteLock();

	private final Plugin mPlugin;
	private final UUID mUUID;

	/* This must be reset externally! */
	private int mVotesThisWeek;
	private int mVotesTotal;
	private int mVotesUnclaimed;
	private int mRaffleEntries;
	private int mRaffleWinsTotal;
	private int mRaffleWinsUnclaimed;

	/*
	 * Map of the time when a site will go off cooldown
	 * Entries are removed as time has been passed
	 */
	private final Map<String, Long> mOffCooldownTimes;

	private VoteContext(Plugin plugin, UUID uuid, Map<String, Long> offCooldownTimes, int votesThisWeek, int votesTotal,
	                    int votesUnclaimed, int raffleEntries, int raffleWinsTotal, int raffleWinsUnclaimed) throws Exception {
		mPlugin = plugin;
		mUUID = uuid;
		mOffCooldownTimes = offCooldownTimes;

		mVotesThisWeek = votesThisWeek;
		mVotesTotal = votesTotal;
		mVotesUnclaimed = votesUnclaimed;
		mRaffleEntries = raffleEntries;
		mRaffleWinsTotal = raffleWinsTotal;
		mRaffleWinsUnclaimed = raffleWinsUnclaimed;
	}

	private void save() {
		final String fileLocation = mPlugin.getDataFolder() + File.separator + "votes" + File.separator + mUUID.toString() + ".json";

		try {
			FileUtils.writeFile(fileLocation, toString());
		} catch (Exception e) {
			mPlugin.getLogger().severe("Failed to write vote data to " + fileLocation);
			e.printStackTrace();
		}
	}

	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject root = new JsonObject();

		mLock.readLock().lock();

		root.addProperty("votesThisWeek", mVotesThisWeek);
		root.addProperty("votesTotal", mVotesTotal);
		root.addProperty("votesUnclaimed", mVotesUnclaimed);
		root.addProperty("raffleEntries", mRaffleEntries);
		root.addProperty("raffleWinsTotal", mRaffleWinsTotal);
		root.addProperty("raffleWinsUnclaimed", mRaffleWinsUnclaimed);
		JsonObject cooldownObj = new JsonObject();
		for (Map.Entry<String, Long> entry : mOffCooldownTimes.entrySet()) {
			cooldownObj.addProperty(entry.getKey(), entry.getValue());
		}
		root.add("offCooldownTimes", cooldownObj);

		mLock.readLock().unlock();

		String content = gson.toJson(root);

		return content;
	}

	protected static VoteContext fromString(Plugin plugin, UUID uuid, String str) throws Exception {
		JsonElement element;

		final Map<String, Long> offCooldownTimes = new HashMap<String, Long>();
		int votesThisWeek = 0;
		int votesTotal = 0;
		int votesUnclaimed = 0;
		int raffleEntries = 0;
		int raffleWinsTotal = 0;
		int raffleWinsUnclaimed = 0;

		if (str != null && !str.isEmpty()) {
			Gson gson = new Gson();

			JsonObject object = gson.fromJson(str, JsonObject.class);

			element = object.get("offCooldownTimes");
			if (element != null) {
				for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
					if (VoteManager.getSiteTimers().containsKey(entry.getKey())) {
						offCooldownTimes.put(entry.getKey(), entry.getValue().getAsLong());
					} else {
						plugin.getLogger().warning("Failed to load offCooldownTimes entry '" + entry.getKey() +
						                           "' for player '" + uuid.toString() + "'");
					}
				}
			} else {
				plugin.getLogger().warning("Failed to load 'offCooldownTimes' for player '" + uuid.toString() + "'");
			}
			element = object.get("votesThisWeek");
			if (element != null) {
				votesThisWeek = element.getAsInt();
			} else {
				plugin.getLogger().warning("Failed to load 'votesThisWeek' for player '" + uuid.toString() + "'");
			}
			element = object.get("votesTotal");
			if (element != null) {
				votesTotal = element.getAsInt();
			} else {
				plugin.getLogger().warning("Failed to load 'votesTotal' for player '" + uuid.toString() + "'");
			}
			element = object.get("votesUnclaimed");
			if (element != null) {
				votesUnclaimed = element.getAsInt();
			} else {
				plugin.getLogger().warning("Failed to load 'votesUnclaimed' for player '" + uuid.toString() + "'");
			}
			element = object.get("raffleEntries");
			if (element != null) {
				raffleEntries = element.getAsInt();
			} else {
				plugin.getLogger().warning("Failed to load 'raffleEntries' for player '" + uuid.toString() + "'");
			}
			element = object.get("raffleWinsTotal");
			if (element != null) {
				raffleWinsTotal = element.getAsInt();
			} else {
				plugin.getLogger().warning("Failed to load 'raffleWinsTotal' for player '" + uuid.toString() + "'");
			}
			element = object.get("raffleWinsUnclaimed");
			if (element != null) {
				raffleWinsUnclaimed = element.getAsInt();
			} else {
				plugin.getLogger().warning("Failed to load 'raffleWinsUnclaimed' for player '" + uuid.toString() + "'");
			}
		}

		return new VoteContext(plugin, uuid, offCooldownTimes, votesThisWeek, votesTotal,
		                       votesUnclaimed, raffleEntries, raffleWinsTotal, raffleWinsUnclaimed);
	}

	/*
	 * Either loads existing data or creates new initialized data for the player
	 */
	protected static VoteContext load(Plugin plugin, UUID uuid) throws Exception {
		if (uuid == null) {
			throw new Exception("UUID is null");
		}

		final String fileLocation = plugin.getDataFolder() + File.separator + "votes" + File.separator + uuid.toString() + ".json";
		final String str;

		try {
			str = FileUtils.readFile(fileLocation);
		} catch (FileNotFoundException e) {
			// Create a new one with the defaults
			return fromString(plugin, uuid, null);
		}
		if (str == null || str.isEmpty()) {
			// This is bad - if the file didn't exist, a FileNotFound exception should have been raised, not bad/empty data returned
			throw new Exception("No player data returned for '" + uuid.toString() + "'");
		}

		return fromString(plugin, uuid, str);
	}

	private String timeDeltaStr(long totalSecondsLeft) {
		long minutesLeft = (totalSecondsLeft / 60) % 60;
		long hoursLeft = (totalSecondsLeft / (60 * 60));
		String str = "";

		if (hoursLeft > 0) {
			str += Long.toString(hoursLeft) + " hour";
			if (hoursLeft != 1) {
				str += "s";
			}
		}

		if (minutesLeft > 0) {
			if (str.length() > 0) {
				str += " ";
			}
			str += Long.toString(minutesLeft) + " minute";
			if (minutesLeft != 1) {
				str += "s";
			}
		}

		if (hoursLeft <= 0 && minutesLeft <= 0) {
			/* The task doesn't tick fast enough to update at second-level granularity */
			str += "1 minute";
		}

		return str;
	}

	/* Pattern to extract the domain name from the full URL */
	private static final Pattern pattern = Pattern.compile("https*://([^/]*)/");

	protected BaseComponent[] getSiteInfo(boolean withClickEvents) {
		mLock.readLock().lock();

		ComponentBuilder builder = new ComponentBuilder("Sites:").color(ChatColor.GOLD);
		for (String site : VoteManager.getSiteTimers().keySet()) {
			/* If there are no click events, don't display the full site url, just the domain name */
			final String displaySite;
			if (withClickEvents) {
				displaySite = site;
			} else {
				Matcher matcher = pattern.matcher(site);
				if (matcher.find()) {
					displaySite = matcher.group(1);
				} else {
					displaySite = site;
				}
			}

			builder.append("\n");

			Long offCooldownTime = mOffCooldownTimes.get(site);
			if (offCooldownTime == null) {
				/* Off cooldown */
				builder.append(displaySite).color(ChatColor.GREEN);
			} else {
				/* Still on cooldown */
				long currentTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
				long totalSecondsLeft = offCooldownTime - currentTime;

				builder.append(displaySite + ": " + timeDeltaStr(totalSecondsLeft)).color(ChatColor.RED);
			}

			if (withClickEvents) {
				builder.event(new ClickEvent(ClickEvent.Action.OPEN_URL, site));
			}
		}

		mLock.readLock().unlock();

		return builder.create();
	}

	protected void sendVoteInfoShort(ProxiedPlayer player) {
		mLock.readLock().lock();
		int numSites = VoteManager.getSiteTimers().size() - mOffCooldownTimes.size();
		mLock.readLock().unlock();

		ComponentBuilder builder = new ComponentBuilder("You are eligible to ").color(ChatColor.GOLD)
		.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getSiteInfo(false)))
		.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote"))
		.append("/vote ").color(ChatColor.AQUA)
		.append("on " + Integer.toString(numSites) + " sites!").color(ChatColor.GOLD);

		player.sendMessage(builder.create());
	}

	protected void sendVoteInfoLong(ProxiedPlayer player) {
		mLock.readLock().lock();

		ComponentBuilder builder = new ComponentBuilder(
		    "Vote for Monumenta\n"
		).color(ChatColor.WHITE).bold(true)
		.append("Voting helps other people find Monumenta!\nClaim rewards at Pollmaster Tennenbaum in Sierhaven\n\n", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GOLD)
		.append("Voting status:\n").color(ChatColor.GOLD)
		.append("Total votes: " + Integer.toString(mVotesTotal) + "\n").color(ChatColor.WHITE)
		.append("Votes this week: " + Integer.toString(mVotesThisWeek) + "\n")
		.append("Unclaimed rewards: " + Integer.toString(mVotesUnclaimed) + "\n")
		.append("Raffle Entries: " + Integer.toString(mRaffleEntries) + "\n");

		if (mRaffleWinsTotal > 0) {
			builder = builder.append("Raffle wins: " + Integer.toString(mRaffleWinsTotal) + "\n");
			builder = builder.append("Unclaimed raffle rewards: " + Integer.toString(mRaffleWinsUnclaimed) + "\n").bold(mRaffleWinsUnclaimed > 0);
			if (mRaffleWinsUnclaimed > 0) {
				builder = builder.append("To claim your raffle reward, run /claimraffle");
			}
		}

		builder = builder.append("\n").append(getSiteInfo(true), ComponentBuilder.FormatRetention.NONE);

		mLock.readLock().unlock();

		player.sendMessage(builder.create());
	}

	/* Got vote - put vote on cooldown, increment scores, thank player for voting if they are online */
	protected void voteReceived(String matchingSite, long cooldownMinutes) {

		mLock.writeLock().lock();

		mVotesThisWeek++;
		mVotesTotal++;
		mVotesUnclaimed++;
		mRaffleEntries++;

		long currentTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
		mOffCooldownTimes.put(matchingSite, currentTime + (cooldownMinutes * 60));

		mLock.writeLock().unlock();

		ProxiedPlayer player = mPlugin.getProxy().getPlayer(mUUID);
		if (player != null) {
			player.sendMessage((new ComponentBuilder("Thanks for voting at " + matchingSite + "!").color(ChatColor.GOLD)).create());
			sendVoteInfoShort(player);
		}

		save();
	}

	protected void gotShardVoteCountRequest(String source, UUID uuid, int votesUnclaimed) {
		if (votesUnclaimed > 0) {
			/* Client is sending these votes back to us because they couldn't be redeemed */
			mPlugin.getLogger().info("Got " + Integer.toString(votesUnclaimed) + " unclaimed vote rewards back from '" +
			                         source + "' for " + uuid.toString());
			mVotesUnclaimed = votesUnclaimed;
		} else {
			mPlugin.getLogger().info("Got vote rewards request message from '" + source + "' for " + uuid.toString());
			NetworkRelayIntegration.sendGetVotesUnclaimedPacket(source, uuid, mVotesUnclaimed);
			mVotesUnclaimed = 0;
		}

		save();
	}

	protected void gotShardRaffleEligibilityRequest(String source, UUID uuid, boolean claimReward, boolean eligible) {
		if (!claimReward && !eligible) {
			/* Request eligibility */
			NetworkRelayIntegration.sendCheckRaffleEligibilityPacket(source, uuid, claimReward, mRaffleWinsUnclaimed > 0);
		} else if (!claimReward && eligible) {
			/* Sending back a failed claim */
			mRaffleWinsUnclaimed++;
			save();
		} else if (claimReward && !eligible) {
			/* Request eligibility */
			NetworkRelayIntegration.sendCheckRaffleEligibilityPacket(source, uuid, claimReward, mRaffleWinsUnclaimed > 0);

			if (mRaffleWinsUnclaimed > 0) {
				mRaffleWinsUnclaimed--;
				save();
			}
		}
	}

	/* Check if any are off cooldown, and prune the ones that are from the cooldown list
	 * Returns true if any voting site cooldown expired, false otherwise */
	protected boolean tick(long currentTime) {
		/* Read section - check if anything came off cooldown */
		mLock.readLock().lock();

		boolean anyOffCooldown = false;
		for (Long time : mOffCooldownTimes.values()) {
			if (currentTime > time) {
				anyOffCooldown = true;
				break;
			}
		}

		mLock.readLock().unlock();

		if (anyOffCooldown) {
			/* At least one timer expired - iterate again and clean up any expired entries */
			mLock.writeLock().lock();

			Iterator<Map.Entry<String, Long>> it = mOffCooldownTimes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Long> entry = it.next();

				if (currentTime > entry.getValue()) {
					it.remove();
				}
			}

			mLock.writeLock().unlock();

			save();
		}

		return anyOffCooldown;
	}
}
