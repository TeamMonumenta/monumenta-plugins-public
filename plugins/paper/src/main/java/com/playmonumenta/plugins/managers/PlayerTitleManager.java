package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enchantments.Sustenance;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;

public class PlayerTitleManager {

	private static class LineMetadata {
		private final Entity mEntity;
		private final double mHeight;

		public LineMetadata(Entity entity, double height) {
			mEntity = entity;
			mHeight = height;
		}
	}

	private static class PlayerMetadata {
		private final List<LineMetadata> mLines;
		private final List<Component> mDisplay;

		public PlayerMetadata(List<LineMetadata> lines, List<Component> display) {
			mLines = lines;
			mDisplay = display;
		}
	}

	private static final int UPDATE_INTERVAL = 2;

	private static final Map<UUID, PlayerMetadata> METADATA = new HashMap<>();

	private int mTick = 0;

	public static void start() {
		PlayerTitleManager playerTitleManager = new PlayerTitleManager();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(Plugin.getInstance(), playerTitleManager::tick, UPDATE_INTERVAL, UPDATE_INTERVAL);
	}

	// called every UPDATE_INTERVAL ticks
	public void tick() {
		mTick++;
		Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
		for (Player player : onlinePlayers) {
			PlayerMetadata metadata = METADATA.get(player.getUniqueId());

			// If the player became invalid (died, maybe more), logged out (despite being online?), or became spectator/vanished remove all titles
			if (player.isDead() || !player.isOnline() || PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
				if (metadata != null) {
					METADATA.remove(player.getUniqueId());
					destroyEntities(metadata);
				}
				continue;
			}

			// Force new entities to be created when the player switches worlds
			if (metadata != null && !metadata.mLines.isEmpty() && !player.getWorld().equals(metadata.mLines.get(0).mEntity.getLocation().getWorld())) {
				METADATA.remove(player.getUniqueId());
				destroyEntities(metadata);
				metadata = null;
			}

			// Create new entities if necessary
			if (metadata == null) {
				metadata = createLines(player, getDisplay(player));
				METADATA.put(player.getUniqueId(), metadata);
			}

			if (mTick % 2 == 0) {
				// Check if display has changed and update if so
				List<Component> display = getDisplay(player);
				if (!metadata.mDisplay.equals(display)) {

					// create new lines
					int existingSize = metadata.mLines.size();
					if (display.size() > existingSize) {
						while (display.size() > metadata.mLines.size()) {
							metadata.mLines.add(createLine(player, display.get(metadata.mLines.size()), metadata.mLines.size()));
						}
					}

					// delete removed lines
					for (int i = metadata.mLines.size() - 1; i >= display.size(); i--) {
						metadata.mLines.remove(i).mEntity.remove();
					}

					// update changed lines
					int updatedSize = Math.min(existingSize, display.size());
					for (int i = 0; i < updatedSize; i++) {
						metadata.mLines.get(i).mEntity.customName(display.get(i));
					}

					metadata.mDisplay.clear();
					metadata.mDisplay.addAll(display);
				}
			}

			// Move titles
			for (LineMetadata line : metadata.mLines) {
				line.mEntity.teleport(player.getEyeLocation().add(0, line.mHeight, 0));
			}
		}

		// When a player logs off, destroy the titles
		for (Iterator<Map.Entry<UUID, PlayerMetadata>> iterator = METADATA.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<UUID, PlayerMetadata> entry = iterator.next();
			if (Bukkit.getPlayer(entry.getKey()) == null) {
				iterator.remove();
				PlayerMetadata metadata = entry.getValue();
				destroyEntities(metadata);
			}
		}

	}

	private static PlayerMetadata createLines(Player targetPlayer, List<Component> display) {
		List<LineMetadata> lines = new ArrayList<>();
		for (int i = 0; i < display.size(); i++) {
			lines.add(createLine(targetPlayer, display.get(i), i));
		}
		return new PlayerMetadata(lines, display);
	}

	private static LineMetadata createLine(Player targetPlayer, Component text, int index) {

		double height = 0.15 + index * 0.25;

		Entity line = targetPlayer.getWorld().spawn(targetPlayer.getEyeLocation().add(0, height, 0), BlockDisplay.class, entity -> {
			EntityUtils.setRemoveEntityOnUnload(entity);
			entity.customName(text);
			entity.setCustomNameVisible(true);
			entity.setTransformationMatrix(new Matrix4f().scale(0));
			entity.setTeleportDuration(UPDATE_INTERVAL + 1);
			targetPlayer.hideEntity(Plugin.getInstance(), entity);
		});

		return new LineMetadata(line, height);
	}

	private static List<Component> getDisplay(Player player) {
		List<Component> result = new ArrayList<>();

		// lowest: optional title
		Cosmetic title = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.TITLE);
		if (title != null) {
			result.add(Component.text(title.mName, NamedTextColor.GRAY));
		}

		// Track if player cannot heal
		boolean hasAntiHeal = false;
		ItemStatManager.PlayerItemStats.ItemStatsMap playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(player).getItemStats();
		double antiHealFromEnchants = Sustenance.getHealingMultiplier(playerItemStats.get(EnchantmentType.SUSTENANCE), playerItemStats.get(EnchantmentType.CURSE_OF_ANEMIA));

		PercentHeal antiHeal = Plugin.getInstance().mEffectManager.getActiveEffect(player, PercentHeal.class);
		if ((antiHeal != null && antiHeal.getValue() <= -1) || antiHealFromEnchants <= 0) {
			hasAntiHeal = true;
		}

		// middle: health
		int health = (int) Math.round(player.getHealth());
		int maxHealth = (int) Math.round(EntityUtils.getMaxHealth(player));
		float redFactor = Math.max(0, Math.min(1, 1.25f * health / maxHealth - 0.25f)); // 100% red at 20% HP or below, white at full HP
		Component healthLine = Component.text(health + "/" + maxHealth + " \u2665", hasAntiHeal ? TextColor.fromHexString("#5D2D87") : TextColor.color(1f, redFactor, redFactor)); // if you have anitheal, set color to purple
		int absorption = (int) Math.round(AbsorptionUtils.getAbsorption(player));
		if (absorption > 0) {
			healthLine = healthLine.append(Component.text(" +" + absorption, NamedTextColor.YELLOW));
		}
		result.add(healthLine);

		// top: name
		result.add(Component.text(player.getName(), NamedTextColor.WHITE));

		return result;
	}

	private void destroyEntities(PlayerMetadata metadata) {
		for (LineMetadata line : metadata.mLines) {
			line.mEntity.remove();
		}
	}

}
