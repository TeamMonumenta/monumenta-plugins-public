package com.playmonumenta.plugins.network;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.AbilityCastEvent;

/**
 * Handles communication with an (optional) client mod.
 */
public class ClientModHandler implements Listener {

	public static final String CHANNEL_ID = "monumenta:client_channel_v1";

	private final Plugin mPlugin;

	private final Gson mGson;

	public ClientModHandler(Plugin plugin) {
		this.mPlugin = plugin;
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_ID);
		mGson = new GsonBuilder().create();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void abilityCastEvent(AbilityCastEvent event) {
		updateAbility(event.getCaster(), event.getAbility());
	}

	public void updateAbility(Player player, ClassAbility classAbility) {
		Ability ability = mPlugin.mAbilityManager.getPlayerAbilities(player).getAbility(classAbility);
		if (ability == null) {
			return;
		}
		updateAbility(player, ability);
	}

	/**
	 * Sends an ability update to the player.
	 * <p>
	 * Does nothing if the player does not have a compatible client mod installed, or if the ability makes no sense to send to clients (see {@link #shouldHandleAbility(Ability)}).
	 */
	public void updateAbility(Player player, Ability ability) {
		if (!playerHasClientMod(player) || !shouldHandleAbility(ability)) {
			return;
		}
		sendPacket(player, prepareAbilityUpdatePacket(player, ability));
	}

	private AbilityUpdatePacket prepareAbilityUpdatePacket(Player player, Ability ability) {
		ClassAbility classAbility = ability.getInfo().mLinkedSpell;
		int remainingCooldown = classAbility == null ? 0 : mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
		int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;
		int maxCharges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getMaxCharges() : 0;

		AbilityUpdatePacket packet = new AbilityUpdatePacket();
		packet.mName = getAbilityName(ability);
		packet.mRemainingCooldown = remainingCooldown;
		packet.mInitialCooldown = ability.getInfo().mCooldown;
		packet.mRemainingCharges = charges;
		packet.mMaxCharges = maxCharges;
		return packet;
	}

	/**
	 * Sends a class update to the player.
	 * <p>
	 * Does nothing if the player does not have a compatible client mod installed.
	 */
	public void updateAbilities(Player player) {
		if (!playerHasClientMod(player)) {
			return;
		}

		AbilityUpdatePacket[] abilities = mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilities().stream()
			.filter(ClientModHandler::shouldHandleAbility)
			.map(ability -> prepareAbilityUpdatePacket(player, ability))
			.sorted(Comparator.comparing(p -> p.mName))
			.toArray(AbilityUpdatePacket[]::new);

		ClassUpdatePacket packet = new ClassUpdatePacket();
		packet.mAbilities = abilities;
		sendPacket(player, packet);
	}

	public void silenced(Player player, int duration) {
		PlayerStatusPacket packet = new PlayerStatusPacket();
		packet.mSilenceDuration = duration;
		sendPacket(player, packet);
	}

	private void sendPacket(Player player, Packet packet) {
		player.sendPluginMessage(mPlugin, CHANNEL_ID, mGson.toJson(packet).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @return Whether we're sending data for the given ability to clients
	 */
	private static boolean shouldHandleAbility(Ability ability) {
		return ability != null && (ability.getInfo().mCooldown > 0 || ability instanceof AbilityWithChargesOrStacks);
	}

	private static String getAbilityName(Ability ability) {
		// The ClassAbility name is preferable if it exists (e.g. for the two Elemental Spirits)
		if (ability.getInfo().mLinkedSpell != null) {
			return ability.getInfo().mLinkedSpell.getName();
		}
		return ability.getDisplayName();
	}

	private static boolean playerHasClientMod(Player player) {
		return player.getListeningPluginChannels().contains(CHANNEL_ID);
	}

	// ------ packets ------

	private interface Packet {
	}

	/**
	 * Sent whenever a player's class is updated.
	 */
	private static class ClassUpdatePacket implements Packet {

		@SerializedName("_type")
		final String mType = "ClassUpdatePacket";

		@SerializedName("abilities")
		AbilityUpdatePacket[] mAbilities;

	}

	/**
	 * Sent whenever an ability is used or changed in any way
	 */
	private static class AbilityUpdatePacket implements Packet {

		@SerializedName("_type")
		final String mType = "AbilityUpdatePacket";

		@SerializedName("name")
		String mName;

		@SerializedName("remainingCooldown")
		int mRemainingCooldown;

		@SerializedName("initialCooldown")
		int mInitialCooldown;

		@SerializedName("remainingCharges")
		int mRemainingCharges;

		@SerializedName("maxCharges")
		int mMaxCharges;

	}

	/**
	 * Custom player status effects that effect skills
	 */
	private static class PlayerStatusPacket implements Packet {

		@SerializedName("_type")
		final String mType = "PlayerStatusPacket";

		@SerializedName("silenceDuration")
		int mSilenceDuration;

	}

}
