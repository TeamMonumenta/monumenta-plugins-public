package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Transposing implements Enchantment, Listener {

	private static final double MAX_TRANSPOSE_DISTANCE = 80.0;
	private static final int SWAP_COOLDOWN_TICKS = 100;

	private static final Map<UUID, Integer> mSwapCooldowns = new HashMap<>();

	@Override
	public String getName() {
		return "Transposing";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TRANSPOSING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public void onPlayerSwapHands(Plugin plugin, Player player, double value, PlayerSwapHandItemsEvent event) {
		ItemStack itemFromMain = event.getMainHandItem();
		ItemStack itemFromOff = event.getOffHandItem();
		int transposingId = getTransposingId(itemFromMain);
		if (transposingId == 0) {
			transposingId = getTransposingId(itemFromOff);
		}
		if (transposingId == 0) {
			return;
		}
		event.setCancelled(true);

		int currentTick = Bukkit.getCurrentTick();
		int lastSwapTick = mSwapCooldowns.getOrDefault(player.getUniqueId(), 0);
		if (currentTick - lastSwapTick < SWAP_COOLDOWN_TICKS) {
			player.sendActionBar(Component.text("Totem of Transposing is on cooldown!", NamedTextColor.YELLOW));
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.3f, 0.7f);
			return;
		}
		findAndTranspose(plugin, player, transposingId);
	}

	private void findAndTranspose(Plugin plugin, Player initiator, int initiatorId) {
		for (Player potentialPartner : initiator.getWorld().getPlayers()) {
			if (potentialPartner.equals(initiator)) {
				continue;
			}

			if (partnerHasMatchingTotem(potentialPartner, initiatorId)) {
				String failReason = getTransposeFailReason(initiator, potentialPartner);
				if (failReason != null) {
					if (failReason.equals("void")) {
						initiator.sendMessage(formatMessage("You... monster!"));
						potentialPartner.sendMessage(formatMessage("Be careful who you trust..."));
					} else {
						sendMessageToBoth(initiator, potentialPartner, formatMessage(failReason));
					}
				} else {
					performSwap(plugin, initiator, potentialPartner);
				}
				return;
			}
		}
		initiator.sendMessage(formatMessage("No partner with a matching totem was found."));
	}

	private boolean partnerHasMatchingTotem(Player partner, int requiredId) {
		for (ItemStack item : partner.getInventory().getContents()) {
			if (getTransposingId(item) == requiredId) {
				return true;
			}
		}
		return false;
	}

	private @Nullable String getTransposeFailReason(Player p1, Player p2) {
		if (!p1.getWorld().equals(p2.getWorld()) || p1.getLocation().distanceSquared(p2.getLocation()) > MAX_TRANSPOSE_DISTANCE * MAX_TRANSPOSE_DISTANCE) {
			return "Can't transpose that far!";
		}
		if (p1.getY() < 0) {
			return "void";
		}
		if (p1.getScoreboardTags().contains("SQRacer") || p2.getScoreboardTags().contains("SQRacer")) {
			return "Can't transpose while racing!";
		}
		if (StasisListener.isInStasis(p1) || StasisListener.isInStasis(p2)) {
			return "Can't transpose while in Stasis!";
		}
		if (p1.getScoreboardTags().contains(Constants.Tags.NO_TRANSPOSING) || p2.getScoreboardTags().contains(Constants.Tags.NO_TRANSPOSING)) {
			return "A powerful spell is interfering with your Totem of Transposing's signal.";
		}
		if (ZoneUtils.hasZoneProperty(p1, ZoneUtils.ZoneProperty.ADVENTURE_MODE) || ZoneUtils.hasZoneProperty(p1, ZoneUtils.ZoneProperty.LOOTROOM)) {
			return "A powerful spell is interfering with your Totem of Transposing's signal.";
		}
		if (ZoneUtils.hasZoneProperty(p2, ZoneUtils.ZoneProperty.ADVENTURE_MODE) || ZoneUtils.hasZoneProperty(p2, ZoneUtils.ZoneProperty.LOOTROOM)) {
			return "A powerful spell is interfering with your Totem of Transposing's signal.";
		}
		if (ZoneUtils.hasZoneProperty(p1, ZoneUtils.ZoneProperty.NO_TRANSPOSING) || ZoneUtils.hasZoneProperty(p2, ZoneUtils.ZoneProperty.NO_TRANSPOSING)) {
			return "A powerful spell is interfering with your Totem of Transposing's signal.";
		}
		return null;
	}

	private void performSwap(Plugin plugin, Player p1, Player p2) {
		Location loc1 = p1.getLocation();
		Location loc2 = p2.getLocation();

		p1.teleport(loc2);
		p2.teleport(loc1);

		playTransposeEffect(loc1);
		playTransposeEffect(loc2);

		sendMessageToBoth(p1, p2, formatMessage(p1.getName() + " transposed with " + p2.getName() + "!"));

		int currentTick = Bukkit.getCurrentTick();
		mSwapCooldowns.put(p1.getUniqueId(), currentTick);
		mSwapCooldowns.put(p2.getUniqueId(), currentTick);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (p1.isOnline()) {
				p1.sendActionBar(Component.text("Totem of Transposing is ready!", NamedTextColor.GREEN));
				p1.playSound(p1.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1f, 1.5f);
			}
		}, SWAP_COOLDOWN_TICKS);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (p2.isOnline()) {
				p2.sendActionBar(Component.text("Totem of Transposing is ready!", NamedTextColor.GREEN));
				p2.playSound(p2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1f, 1.5f);
			}
		}, SWAP_COOLDOWN_TICKS);
	}

	private void playTransposeEffect(Location location) {
		location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
		location.getWorld().spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
		location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, location.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
	}

	private int getTransposingId(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}

		Integer transposingId = NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound("Monumenta");
			if (monumenta != null) {
				ReadableNBT playerModified = monumenta.getCompound("PlayerModified");
				if (playerModified != null) {
					return playerModified.getInteger("TransposingID");
				}
			}
			return null;
		});
		return transposingId == null ? 0 : transposingId;
	}

	private void sendMessageToBoth(Player p1, Player p2, Component message) {
		p1.sendMessage(message);
		p2.sendMessage(message);
	}

	private Component formatMessage(String message) {
		return Component.text("☀ " + message, NamedTextColor.YELLOW);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		mSwapCooldowns.remove(event.getPlayer().getUniqueId());
	}
}
