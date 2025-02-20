package com.playmonumenta.plugins.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritIce;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.windwalker.OneWithTheWind;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles communication with an (optional) client mod.
 */
public class ClientModHandler {

	public static final String CHANNEL_ID = "monumenta:client_channel_v1";

	private static @MonotonicNonNull ClientModHandler INSTANCE = null;

	private final MonumentaClasses mClasses;

	private final Plugin mPlugin;

	private final Gson mGson;

	public ClientModHandler(Plugin plugin) {
		this.mPlugin = plugin;
		mGson = new GsonBuilder().create();
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_ID);
		mClasses = new MonumentaClasses();
		INSTANCE = this;
	}

	public static void updateAbility(Player player, ClassAbility classAbility) {
		if (INSTANCE == null) {
			return;
		}
		Ability ability = INSTANCE.mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilityIgnoringSilence(classAbility);
		if (ability == null) {
			return;
		}
		updateAbility(player, ability);
	}

	/**
	 * Sends an ability update to the player.
	 * <p>
	 * Does nothing if the player does not have a compatible client mod installed, or if the ability makes no sense to send to clients (see {@link #shouldHandleAbility(Player, Ability)}).
	 */
	public static void updateAbility(Player player, Ability ability) {
		if (INSTANCE == null || !playerHasClientMod(player) || !shouldHandleAbility(player, ability)) {
			return;
		}
		ClassAbility classAbility = ability.getInfo().getLinkedSpell();
		int remainingCooldown = classAbility == null ? 0 : INSTANCE.mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
		int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;

		AbilityUpdatePacket packet = new AbilityUpdatePacket();
		packet.name = getAbilityName(ability);
		packet.remainingCooldown = remainingCooldown;
		packet.remainingCharges = charges;
		packet.mode = ability.getMode();
		packet.initialDuration = ability.getInitialDuration();
		packet.remainingDuration = ability.getRemainingDuration();
		INSTANCE.sendPacket(player, packet);
	}

	/**
	 * Sends a class update to the player.
	 * <p>
	 * Does nothing if the player does not have a compatible client mod installed.
	 */
	public static void updateAbilities(Player player) {
		if (INSTANCE == null || !playerHasClientMod(player)) {
			return;
		}

		ClassUpdatePacket.ClientModAbilityInfo[] abilities =
			INSTANCE.mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilitiesIgnoringSilence().stream()
				.filter(ability -> shouldHandleAbility(player, ability))
				.map(ability -> {
					ClassAbility classAbility = ability.getInfo().getLinkedSpell();
					int remainingCooldown = classAbility == null ? 0 : INSTANCE.mPlugin.mTimers.getCooldown(player.getUniqueId(), classAbility);
					int charges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getCharges() : 0;
					int maxCharges = ability instanceof AbilityWithChargesOrStacks ? ((AbilityWithChargesOrStacks) ability).getMaxCharges() : 0;

					ClassUpdatePacket.ClientModAbilityInfo info = new ClassUpdatePacket.ClientModAbilityInfo();
					info.name = getAbilityName(ability);
					info.className = getAbilityClassName(ability);
					info.remainingCooldown = remainingCooldown;
					info.initialCooldown = ability.getInfo().getModifiedCooldown(player, ability.getAbilityScore());
					info.remainingCharges = charges;
					info.maxCharges = maxCharges;
					info.mode = ability.getMode();
					info.initialDuration = ability.getInitialDuration();
					info.remainingDuration = ability.getRemainingDuration();
					return info;
				})
				.sorted(Comparator.comparing(i -> i.name == null ? "" : i.name))
				.toArray(ClassUpdatePacket.ClientModAbilityInfo[]::new);

		ClassUpdatePacket packet = new ClassUpdatePacket();
		packet.abilities = abilities;
		INSTANCE.sendPacket(player, packet);
	}

	public static void updateEffects(Entity entity) {
		if (INSTANCE == null
			|| !(entity instanceof Player player)
			|| !playerHasClientMod(player)) {
			return;
		}
		EffectInfo[] effectsList = INSTANCE.mPlugin.mEffectManager.getPriorityEffects(player).entrySet().stream()
			.filter(effect -> effect != null && effect.getValue().doesDisplay() && effect.getValue().getDisplayedName() != null)
			.map(effect -> {
				EffectInfo info = new EffectInfo();
				mapEffectToEffectInfo(effect.getValue(), info, effect.getKey(), false);
				return info;
			})
			.sorted((effect1, effect2) -> effect2.displayPriority - effect1.displayPriority)
			.toArray(EffectInfo[]::new);

		MassEffectUpdatePacket packet = new MassEffectUpdatePacket();
		packet.effects = effectsList;
		INSTANCE.sendPacket(player, packet);
	}

	public static void updateEffect(Entity entity, Effect effect, String source, boolean remove) {
		if (INSTANCE == null
			|| !(entity instanceof Player player)
			|| !playerHasClientMod(player)
			|| effect == null
			|| !effect.doesDisplay()
			|| effect.getDisplayedName() == null) {
			return;
		}
		EffectInfo info = new EffectInfo();
		mapEffectToEffectInfo(effect, info, source, remove);
		EffectUpdatePacket packet = new EffectUpdatePacket();
		packet.effect = info;
		INSTANCE.sendPacket(player, packet);
	}

	public static void mapEffectToEffectInfo(Effect effect, EffectInfo info, String source, boolean remove) {
		info.UUID = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
		info.duration = remove ? 0 : effect.getDuration();
		info.name = effect.getDisplayedName() != null ? effect.getDisplayedName() : "";
		info.percentage = effect.getDisplay() != null && MessagingUtils.plainText(effect.getDisplay()).contains("%");
		if (effect.getSpecificDisplay() != null && Objects.equals(MessagingUtils.plainText(effect.getSpecificDisplay()), info.name)) {
			info.power = 0;
		} else {
			info.power = (info.percentage ? effect.getMagnitude() * 100 : effect.getMagnitude());
		}
		info.positive = effect.isBuff() == effect.getMagnitude() >= 0;
		info.displayPriority = effect.getDisplayPriority();
	}

	public static void silenced(Player player, int duration) {
		if (INSTANCE == null) {
			return;
		}
		PlayerStatusPacket packet = new PlayerStatusPacket();
		packet.silenceDuration = duration;
		INSTANCE.sendPacket(player, packet);
	}

	public static void updateStrikeChests(Player player, int newLimit, @Nullable Integer count) {
		if (INSTANCE == null) {
			return;
		}
		StrikeChestUpdatePacket packet = new StrikeChestUpdatePacket();
		packet.newLimit = newLimit;
		packet.count = count;
		INSTANCE.sendPacket(player, packet);
	}

	public static void sendLocationPacket(Player player, String content) {
		String shard;
		try {
			shard = NetworkRelayAPI.getShardName();
		} catch (Exception ex) {
			//If failed use the short version.
			shard = ServerProperties.getShardName();
		}

		sendLocationPacket(player, shard, content);
	}

	/**
	 * This overload should only be (publicly) used in the case the current shard is sending the player on another shard
	 * whose name does not bear the one of the content the player is being sent to
	 */
	public static void sendLocationPacket(Player player, @NotNull String shard, String content) {
		LocationUpdatedPacket packet = new LocationUpdatedPacket();
		packet.shard = shard;
		packet.content = content;
		INSTANCE.sendPacket(player, packet);
	}

	private void sendPacket(Player player, Packet packet) {
		player.sendPluginMessage(mPlugin, CHANNEL_ID, mGson.toJson(packet).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @return Whether we're sending data for the given ability to clients
	 */
	private static boolean shouldHandleAbility(Player player, Ability ability) {
		return ability != null
			&& (ability.getInfo().getBaseCooldown(player, ability.getAbilityScore()) > 0 || ability instanceof AbilityWithChargesOrStacks
			|| ability instanceof AlchemicalArtillery || ability instanceof Swiftness || ability instanceof OneWithTheWind); // these are passives with modes
	}

	private static @Nullable String getAbilityName(Ability ability) {
		// The ClassAbility name is preferable if it exists (e.g. for the two Elemental Spirits)
		if (ability.getInfo().getLinkedSpell() != null) {
			return ability.getInfo().getLinkedSpell().getName();
		}
		return ability.getInfo().getDisplayName();
	}

	private static @Nullable String getAbilityClassName(Ability ability) {
		if (ability instanceof DepthsAbility) {
			DepthsTree depthsTree = ((DepthsAbility) ability).getInfo().getDepthsTree();
			if (depthsTree != null) {
				return depthsTree.getDisplayName();
			}
		}
		if (ability instanceof AlchemistPotions) {
			return "Alchemist";
		}
		if (ability instanceof ElementalSpiritIce) {
			return "Mage";
		}
		for (PlayerClass playerClass : INSTANCE.mClasses.mClasses) {
			Predicate<AbilityInfo<?>> sameClass = abi -> abi.getAbilityClass() == ability.getClass();
			if (playerClass.mAbilities.stream().anyMatch(sameClass)
				|| playerClass.mSpecOne.mAbilities.stream().anyMatch(sameClass)
				|| playerClass.mSpecTwo.mAbilities.stream().anyMatch(sameClass)) {
				return playerClass.mClassName;
			}
		}
		return null;
	}

	public static boolean playerHasClientMod(Player player) {
		return player.getListeningPluginChannels().contains(CHANNEL_ID);
	}

	// ------ packets ------

	private interface Packet {
	}

	/**
	 * Sent whenever a player's class is updated.
	 */
	@SuppressWarnings("unused")
	private static class ClassUpdatePacket implements Packet {

		final String _type = "ClassUpdatePacket";

		ClientModAbilityInfo @Nullable [] abilities;

		public static class ClientModAbilityInfo {

			@Nullable String name;
			@Nullable String className;

			int remainingCooldown;
			int initialCooldown;

			int remainingCharges;
			int maxCharges;

			@Nullable String mode;

			@Nullable Integer remainingDuration;
			@Nullable Integer initialDuration;

		}

	}

	/**
	 * Sent whenever an ability is used or changed in any way
	 */
	@SuppressWarnings("unused")
	private static class AbilityUpdatePacket implements Packet {

		final String _type = "AbilityUpdatePacket";

		@Nullable String name;

		// className is not required, as a player should never have multiple abilities with the same name

		int remainingCooldown;

		int remainingCharges;

		@Nullable String mode;

		@Nullable Integer remainingDuration;
		@Nullable Integer initialDuration;

	}

	/**
	 * Custom player status effects that effect skills
	 */
	@SuppressWarnings("unused")
	private static class PlayerStatusPacket implements Packet {

		final String _type = "PlayerStatusPacket";

		int silenceDuration;

	}


	/**
	 * Sent whenever the number of chests in a strike changes
	 */
	@SuppressWarnings("unused")
	private static class StrikeChestUpdatePacket implements Packet {

		final String _type = "StrikeChestUpdatePacket";

		int newLimit;

		@Nullable Integer count;

	}

	@SuppressWarnings("unused")
	public static class MassEffectUpdatePacket implements Packet {
		String _type = "MassEffectUpdatePacket";

		//when received, will clear stored effects.
		public @Nullable EffectInfo[] effects;
	}

	@SuppressWarnings("unused")
	public static class EffectUpdatePacket implements Packet {
		String _type = "EffectUpdatePacket";

		public @Nullable EffectInfo effect;
	}

	@SuppressWarnings("NullAway")
	public static class EffectInfo {
		public String UUID;
		public int displayPriority;

		public String name;
		public Integer duration;
		public double power;

		public boolean positive;
		public boolean percentage;
	}


	/**
	 * Should be sent on login, shard change and after a player enters a new content (most likely will be tied to the instance bot)
	 */
	@SuppressWarnings("unused")
	public static class LocationUpdatedPacket implements Packet {
		String _type = "LocationUpdatedPacket";

		/**
		 * the Shard the player is on.
		 */
		@MonotonicNonNull
		String shard;

		/**
		 * the content the player is playing, content can be the same as shard if the shard has the same name.
		 * on player plots, this will reflect the current instance.
		 */
		@Nullable
		String content;
	}

}
