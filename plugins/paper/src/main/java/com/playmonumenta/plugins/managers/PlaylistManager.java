package com.playmonumenta.plugins.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public class PlaylistManager implements Listener {

	private static final String KEY_PLUGIN_DATA = "MonumentaPlaylists";

	private final Map<UUID, PlaylistData> mData = new HashMap<>();

	public PlaylistData getData(Player player) {
		return mData.computeIfAbsent(player.getUniqueId(), key -> new PlaylistData());
	}


	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject json = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (json == null) {
			return;
		}
		mData.put(player.getUniqueId(), PlaylistData.fromJson(json));
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		PlaylistData playlistData = mData.get(event.getPlayer().getUniqueId());
		if (playlistData == null) {
			return;
		}
		event.setPluginData(KEY_PLUGIN_DATA, playlistData.toJson());
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!player.isOnline()) {
				mData.remove(player.getUniqueId());
			}
		}, 10);
	}

	public static class PlaylistData {

		public List<PlaylistTrack> mPlaylistTracks = new ArrayList<>();
		public int mCurrentTrackIndex = 0;

		private static PlaylistData fromJson(JsonObject json) {
			PlaylistData data = new PlaylistData();
			for (JsonElement jsonElement : json.getAsJsonArray("playlistTracks")) {
				data.mPlaylistTracks.add(PlaylistTrack.fromJson(jsonElement.getAsJsonObject()));
			}
			data.mCurrentTrackIndex = json.getAsJsonPrimitive("currentTrackIndex").getAsInt();
			return data;
		}

		private JsonObject toJson() {
			JsonObject json = new JsonObject();
			JsonArray tracksJson = new JsonArray();
			for (PlaylistTrack track : mPlaylistTracks) {
				tracksJson.add(track.toJson());
			}
			json.add("playlistTracks", tracksJson);
			json.addProperty("currentTrackIndex", mCurrentTrackIndex);
			return json;
		}
	}

	public static class PlaylistTrack {

		public Material mMaterial;
		public String mName;
		public TextColor mColor;
		public String mLocation;
		public String mComposer;
		public String mTrack;
		public double mDuration;

		public PlaylistTrack(Material material, String name, TextColor color, String location, @Nullable String composer, String track, double duration) {
			mMaterial = material;
			mName = name;
			mColor = color;
			mLocation = location;
			mComposer = composer != null ? composer : "";
			mTrack = track;
			mDuration = duration;
		}

		public JsonObject toJson() {
			JsonObject json = new JsonObject();

			json.addProperty("material", String.valueOf(mMaterial));
			json.addProperty("name", mName);
			json.addProperty("color", mColor.asHexString());
			json.addProperty("location", mLocation);
			json.addProperty("composer", mComposer);
			json.addProperty("track", mTrack);
			json.addProperty("duration", mDuration);

			return json;
		}

		private static PlaylistTrack fromJson(JsonObject json) {
			Material material = Material.getMaterial(json.getAsJsonPrimitive("material").getAsString());
			String name = json.getAsJsonPrimitive("name").getAsString();
			TextColor color = TextColor.fromCSSHexString(json.getAsJsonPrimitive("color").getAsString());
			String location = json.getAsJsonPrimitive("location").getAsString();
			String composer = json.getAsJsonPrimitive("composer").getAsString();
			String track = json.getAsJsonPrimitive("track").getAsString();
			double duration = json.getAsJsonPrimitive("duration").getAsDouble();

			return new PlaylistTrack(material, name, color, location, composer, track, duration);
		}
	}
}
