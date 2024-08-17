package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.protocollib.GlowingReplacer;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class GlowingManager {

	public static int PLAYER_ABILITY_PRIORITY = 100;
	public static int BOSS_SPELL_PRIORITY = 1000;

	private static class GlowingInstance {
		final NamedTextColor mTeamColor;
		final Color mDisplayColor;
		final int mUntilTick;
		final @Nullable Predicate<Player> mVisibleToPlayers;
		final int mPriority;
		final @Nullable String mReference;

		public GlowingInstance(NamedTextColor teamColor, Color displayColor, int untilTick, @Nullable Predicate<Player> visibleToPlayers, int priority, @Nullable String reference) {
			mTeamColor = teamColor;
			mDisplayColor = displayColor;
			mUntilTick = untilTick;
			mVisibleToPlayers = visibleToPlayers;
			mPriority = priority;
			mReference = reference;
		}
	}

	private static class GlowingEntityData {
		final Entity mEntity;
		final @Nullable Color mOriginalColor;
		final boolean mOriginallyGlowing;
		final List<GlowingInstance> mInstances = new ArrayList<>();
		final Map<UUID, NamedTextColor> mSentPlayerData = new HashMap<>();

		public GlowingEntityData(Entity entity) {
			mEntity = entity;
			mOriginalColor = entity instanceof Display display ? display.getGlowColorOverride() : null;
			mOriginallyGlowing = entity.isGlowing(); // does not care about the potion effect, as that one is not modified by this manager
		}

		void addData(GlowingInstance data) {
			mInstances.add(data);
			mInstances.sort(Comparator.comparing((GlowingInstance d) -> d.mPriority).reversed());
		}

		@Nullable
		GlowingInstance getActiveInstance(Player player) {
			return mInstances.stream().filter(d -> d.mVisibleToPlayers == null || d.mVisibleToPlayers.test(player)).findFirst().orElse(null);
		}
	}

	public static class ActiveGlowingEffect {
		private final Entity mEntity;
		private final GlowingInstance mInstance;

		private ActiveGlowingEffect(Entity entity, GlowingInstance instance) {
			this.mEntity = entity;
			this.mInstance = instance;
		}

		public void clear() {
			GlowingManager.clear(this);
		}
	}

	private static final Map<String, GlowingEntityData> mData = new HashMap<>();

	private static @Nullable BukkitRunnable mTask;

	public static ActiveGlowingEffect startGlowing(Entity entity, NamedTextColor color, int duration, int priority) {
		return startGlowing(entity, color, duration, priority, null, null);
	}

	public static ActiveGlowingEffect startGlowing(Entity entity, NamedTextColor color, int duration, int priority, @Nullable Predicate<Player> visibleToPlayers, @Nullable String reference) {
		if (reference != null) {
			clear(entity, reference);
		}
		return startGlowing(entity, new GlowingInstance(color, Color.fromRGB(color.red(), color.green(), color.blue()),
			duration < 0 ? Integer.MAX_VALUE : Bukkit.getCurrentTick() + duration, visibleToPlayers, priority, reference));
	}

	/**
	 * Same as the other startGlowing, but for displays. Note that currently all players will see the same colour unlike for other entities.
	 * The duration and visibility check will still apply.
	 */
	public static ActiveGlowingEffect startGlowing(Display display, Color color, int duration, int priority, @Nullable Predicate<Player> visibleToPlayers) {
		return startGlowing(display, new GlowingInstance(NamedTextColor.WHITE, color,
			duration < 0 ? Integer.MAX_VALUE : Bukkit.getCurrentTick() + duration, visibleToPlayers, priority, null));
	}

	private static ActiveGlowingEffect startGlowing(Entity entity, GlowingInstance instance) {
		GlowingEntityData data = mData.computeIfAbsent(ScoreboardUtils.getScoreHolderName(entity), k -> new GlowingEntityData(entity));
		data.addData(instance);
		entity.setGlowing(true);
		update(entity, data);
		if (mTask == null) {
			startTask();
		}
		return new ActiveGlowingEffect(entity, instance);
	}

	private static @Nullable GlowingEntityData getEntityData(Entity entity) {
		return mData.get(ScoreboardUtils.getScoreHolderName(entity));
	}

	public static void clear(ActiveGlowingEffect effect) {
		GlowingEntityData data = getEntityData(effect.mEntity);
		if (data != null) {
			data.mInstances.remove(effect.mInstance);
		}
	}

	public static void clear(Entity entity, String reference) {
		GlowingEntityData data = getEntityData(entity);
		if (data != null) {
			data.mInstances.removeIf(i -> reference.equals(i.mReference));
		}
	}

	public static void clearAll(Entity entity) {
		GlowingEntityData data = getEntityData(entity);
		if (data != null) {
			data.mInstances.clear();
		}
	}

	public static boolean isGlowingForPlayer(Entity entity, Player player) {
		if (entity instanceof LivingEntity le && le.hasPotionEffect(PotionEffectType.GLOWING)) {
			return true;
		}
		GlowingEntityData glowingData = getEntityData(entity);
		return glowingData == null ? entity.isGlowing()
			       : (glowingData.mOriginallyGlowing || glowingData.mInstances.stream().anyMatch(d -> d.mVisibleToPlayers == null || d.mVisibleToPlayers.test(player)));
	}

	public static @Nullable NamedTextColor getTeamForPlayer(String entry, Player player) {
		GlowingEntityData glowingData = mData.get(entry);
		if (glowingData == null) {
			return null;
		}
		Optional<GlowingInstance> active = glowingData.mInstances.stream().filter(d -> d.mVisibleToPlayers == null || d.mVisibleToPlayers.test(player)).findFirst();
		return active.map(d -> d.mTeamColor).orElse(null);
	}

	private static void update(Entity entity, GlowingEntityData data) {
		for (Player player : entity.getTrackedPlayers()) {
			GlowingInstance activeInstance = data.getActiveInstance(player);
			NamedTextColor sentColor = data.mSentPlayerData.get(player.getUniqueId());
			if ((sentColor == null || activeInstance == null || !sentColor.equals(activeInstance.mTeamColor))
				    && !(sentColor == null && activeInstance == null)) {
				// colour has changed, send update packet
				if (activeInstance == null) {
					data.mSentPlayerData.remove(player.getUniqueId());
				} else {
					data.mSentPlayerData.put(player.getUniqueId(), activeInstance.mTeamColor);
				}
				if (entity instanceof Display display) {
					// TODO handle color of display entities if different between players once this is something we need
					if (activeInstance != null) {
						display.setGlowColorOverride(activeInstance.mDisplayColor);
					}
				} else {
					GlowingReplacer.sendTeamUpdate(entity, player,
						sentColor != null ? GlowingReplacer.getColoredGlowingTeamName(sentColor, entity instanceof Player) : null,
						activeInstance != null ? activeInstance.mTeamColor : null);
				}

				// if glowing also has changed (glowing from manager only and previous != current), send a glowing update
				if (!data.mOriginallyGlowing
					    && !(entity instanceof LivingEntity le && le.hasPotionEffect(PotionEffectType.GLOWING))
					    && ((sentColor == null) != (activeInstance == null))) {
					GlowingReplacer.resendEntityMetadataFlags(entity, player);
				}
			}
		}
	}

	private static void startTask() {
		mTask = new BukkitRunnable() {
			@Override
			public void run() {
				mData.values().removeIf(d -> !d.mEntity.isValid());

				if (mData.isEmpty()) {
					cancel();
					mTask = null;
					return;
				}

				int currentTick = Bukkit.getCurrentTick();

				for (Iterator<GlowingEntityData> iterator = mData.values().iterator(); iterator.hasNext(); ) {
					GlowingEntityData data = iterator.next();
					Entity entity = data.mEntity;

					data.mInstances.removeIf(d -> d.mUntilTick < currentTick);
					if (data.mInstances.isEmpty()) {
						if (!data.mOriginallyGlowing) {
							entity.setGlowing(false);
						}
						if (data.mOriginalColor != null && entity instanceof Display display) {
							display.setGlowColorOverride(data.mOriginalColor);
						}
						for (Player player : entity.getTrackedPlayers()) {
							NamedTextColor sentColor = data.mSentPlayerData.get(player.getUniqueId());
							if (sentColor != null) {
								GlowingReplacer.sendTeamUpdate(entity, player, GlowingReplacer.getColoredGlowingTeamName(sentColor, entity instanceof Player), null);
							}
						}
						iterator.remove();
						continue;
					}

					data.mSentPlayerData.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

					update(entity, data);
				}

			}
		};
		mTask.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

}
