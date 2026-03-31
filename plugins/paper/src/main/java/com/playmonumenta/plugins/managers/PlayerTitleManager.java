package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enchantments.Sustenance;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PlayerTitleManager implements Listener {
	private static @Nullable PlayerTitleManager INSTANCE = null;
	private @Nullable BukkitRunnable mHealthChangeRunnable = null;

	private PlayerTitleManager() {
	}

	public static PlayerTitleManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new PlayerTitleManager();
		}
		return INSTANCE;
	}

	public void onEnable(Plugin plugin, PluginManager manager) {
		if (mHealthChangeRunnable != null) {
			MMLog.severe("The Player Title Manager appears to have been enabled twice somehow");
			mHealthChangeRunnable.cancel();
		}

		manager.registerEvents(getInstance(), plugin);
		mHealthChangeRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				try {
					handlePlayerHealthChanges();
				} catch (Exception ex) {
					MMLog.severe("Caught exception handling player health changes, somehow: ", ex);
				}
			}
		};
		mHealthChangeRunnable.runTaskTimer(plugin, 0, 4);
	}

	public void onDisable() {
		if (mHealthChangeRunnable != null) {
			mHealthChangeRunnable.cancel();
			mHealthChangeRunnable = null;
		}
	}

	private static final float MAGIC_HEIGHT_START = 0.0f;
	private static final float MAGIC_HEIGHT_STEP = 0.25f;

	public static class NameTagData {
		public final Interaction mInteraction;
		public final TextDisplay mNametag;
		public Component mText = Component.empty();
		public boolean mDirty = false;

		public NameTagData(Interaction interaction, TextDisplay nametag) {
			this.mInteraction = interaction;
			this.mNametag = nametag;
		}
	}

	public static class NameTag {
		public final UUID mUuid; // player UUID this nametag is for
		public final Map<String, NameTagData> mEntities = new WeakHashMap<>(); // all entities used for this nameta
		public final Set<UUID> mViewers = Collections.newSetFromMap(new WeakHashMap<>());
		public float mHeight = MAGIC_HEIGHT_START;

		public NameTag(Player player) {
			this.mUuid = player.getUniqueId();


			update(player, true);
		}

		public void update(Player player) {
			update(player, false);
		}

		private void update(Player player, final boolean first) {
			if (!player.getUniqueId().equals(mUuid)) {
				// updating for wrong player?
				return;
			}
			mHeight = MAGIC_HEIGHT_START;

			setup(player, "title", getTitleDisplay(player));
			setup(player, "health", getHealthDisplay(player));
			setup(player, "name", getNameDisplay(player));

			if (!first) {
				updatePlayers(player);
			}
		}

		private void setup(Player player, String name, Component text) {
			mEntities.compute(name, (key, existing) -> {
				if (text == null || text.equals(Component.empty())) {
					// TODO: implement a recreation system
					return existing;
				}
				if (existing == null) {
					Interaction interaction = (Interaction) NmsUtils.getVersionAdapter().spawnWorldlessEntity(EntityType.INTERACTION, player.getWorld());
					TextDisplay nametag = (TextDisplay) NmsUtils.getVersionAdapter().spawnWorldlessEntity(EntityType.TEXT_DISPLAY, player.getWorld());
					existing = new NameTagData(interaction, nametag);

					// nametag prep
					existing.mNametag.setPersistent(false);
					existing.mNametag.setCustomNameVisible(true);
					existing.mNametag.setViewRange(256f);
					existing.mNametag.setInvisible(true);
					existing.mNametag.setDefaultBackground(false);
					existing.mNametag.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

					// interaction prep
					existing.mInteraction.setPersistent(false);
					existing.mInteraction.setInteractionHeight(mHeight);
					existing.mInteraction.setInteractionWidth(0f);
					existing.mInteraction.setInvisible(true);
					existing.mInteraction.setPose(Pose.SNIFFING);
				}

				if (text.equals(existing.mText)) {
					return existing;
				}
				existing.mText = text;
				existing.mDirty = true;

				existing.mNametag.customName(text);

				mHeight += MAGIC_HEIGHT_STEP;
				return existing;
			});
		}

		public static Component getNameDisplay(Player player) {
			return Component.text(player.getName(), NamedTextColor.WHITE);
		}

		public static Component getHealthDisplay(Player player) {
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
			return healthLine;
		}

		public static Component getTitleDisplay(Player player) {
			// lowest: optional title
			Cosmetic title = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.TITLE);
			if (title != null) {
				return Component.text(title.mName, NamedTextColor.GRAY);
			}
			return Component.empty();
		}

		public void addPlayer(Player clientPlayer, Player targetPlayer) {
			if (!targetPlayer.getUniqueId().equals(mUuid)) {
				// adding for wrong player?
				return;
			}
			mViewers.add(clientPlayer.getUniqueId());
			Set<Map.Entry<Entity, Entity>> entities2 = Collections.newSetFromMap(new WeakHashMap<>());
			for (Map.Entry<String, NameTagData> entry : mEntities.entrySet()) {
				NameTagData data = entry.getValue();
				entities2.add(Map.entry(data.mInteraction, data.mNametag));
			}
			NmsUtils.getVersionAdapter().spawnPlayerNametag(clientPlayer, targetPlayer, entities2);
		}

		private void updatePlayers(Player targetPlayer) {
			Iterator<UUID> iterator = mViewers.iterator();
			while (iterator.hasNext()) {
				UUID viewer = iterator.next();
				Player clientPlayer = Bukkit.getPlayer(viewer);
				if (clientPlayer == null) {
					iterator.remove();
					continue;
				}
				updatePlayer(clientPlayer, targetPlayer);
			}
			for (NameTagData data : mEntities.values()) {
				data.mDirty = false;
			}
		}

		/*
		 * This only updates the nametag text, not anything else
		 */
		private void updatePlayer(Player clientPlayer, Player targetPlayer) {
			if (!targetPlayer.getUniqueId().equals(mUuid)) {
				// updating for wrong player?
				return;
			}
			List<Entity> entities2 = new ArrayList<>();
			for (NameTagData data : mEntities.values()) {
				if (!data.mDirty) {
					continue;
				}
				entities2.add(data.mNametag);
			}
			NmsUtils.getVersionAdapter().updatePlayerNametag(clientPlayer, entities2.toArray(new Entity[0]));
		}

		public void removePlayer(Player clientPlayer, Player targetPlayer) {
			if (!targetPlayer.getUniqueId().equals(mUuid)) {
				// removing for wrong player?
				return;
			}
			mViewers.remove(clientPlayer.getUniqueId());
			List<Entity> entities2 = new ArrayList<>();
			for (NameTagData data : mEntities.values()) {
				entities2.add(data.mInteraction);
				entities2.add(data.mNametag);
			}
			NmsUtils.getVersionAdapter().removePlayerNametag(clientPlayer, targetPlayer, entities2.toArray(new Entity[0]));
		}
	}

	private final Map<UUID, NameTag> mTrackedEntities = new WeakHashMap<>();

	@EventHandler(ignoreCancelled = false)
	public void playerTrackEvent(PlayerTrackEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();
		if (entity instanceof Player targetPlayer) {
			// need to delay by a tick to ensure packet order
			targetPlayer.getScheduler().run(Plugin.getInstance(), (task) -> {
				mTrackedEntities.compute(targetPlayer.getUniqueId(), (uuid, existing) -> {
					if (existing == null) {
						existing = new NameTag(targetPlayer);
					}
					existing.addPlayer(player, targetPlayer);
					return existing;
				});
			}, null);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void playerUntrackEvent(PlayerUntrackEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();
		if (entity instanceof Player targetPlayer) {
			mTrackedEntities.computeIfPresent(targetPlayer.getUniqueId(), (uuid, existing) -> {
				existing.removePlayer(player, targetPlayer);
				if (existing.mViewers.isEmpty()) {
					return null;
				}
				return existing;
			});
		}
	}

	public void handlePlayerHealthChanges() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			NameTag nameTag = mTrackedEntities.get(player.getUniqueId());
			if (nameTag != null) {
				nameTag.update(player);
			}
		}
	}
}
