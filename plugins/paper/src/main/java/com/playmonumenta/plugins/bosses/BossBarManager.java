package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BossBarManager {
	@FunctionalInterface
	public interface BossHealthAction {
		void run(LivingEntity boss);
	}

	private final LivingEntity mBoss;
	private final int mRange;
	private final PriorityQueue<Map.Entry<Integer, BossHealthAction>> mEvents;
	private final BossBar mBar;
	private final boolean mCapDamage;
	private final Function<LivingEntity, Location> mLocationFunction;

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events) {
		this(boss, range, color, style, events, true);
	}

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog) {
		this(boss, range, color, style, events, bossFog, true);
	}

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage) {
		this(boss, range, color, style, events, bossFog, capDamage, Entity::getLocation);
	}

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage, Location centerLocation) {
		this(boss, range, color, style, events, bossFog, capDamage, b -> centerLocation);
	}

	public BossBarManager(LivingEntity boss, int range, BossBar.Color color, BossBar.Overlay style, @Nullable Map<Integer, BossHealthAction> events, boolean bossFog, boolean capDamage, Function<LivingEntity, Location> locationFunction) {
		mBoss = boss;
		mRange = range;
		mEvents = new PriorityQueue<>(Math.max(events != null ? events.size() : 1, 1), Comparator.comparing(entry -> -entry.getKey()));
		if (events != null) {
			mEvents.addAll(events.entrySet());
		}
		mCapDamage = capDamage;
		mLocationFunction = locationFunction;

		mBar = BossBar.bossBar(Component.text(mBoss.getName()), (float) 0, color, style, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
		if (bossFog) {
			mBar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
			mBar.addFlag(BossBar.Flag.DARKEN_SCREEN);
		}

		Location loc = locationFunction.apply(boss);
		for (Player player : mBoss.getWorld().getPlayers()) {
			if (player.getLocation().distanceSquared(loc) < range * range) {
				mBar.addViewer(player);
			}
		}
	}

	public void setBossFog(boolean value) {
		if (value) {
			mBar.addFlag(BossBar.Flag.CREATE_WORLD_FOG);
			mBar.addFlag(BossBar.Flag.DARKEN_SCREEN);
		} else {
			mBar.removeFlag(BossBar.Flag.CREATE_WORLD_FOG);
			mBar.removeFlag(BossBar.Flag.DARKEN_SCREEN);
		}
	}

	public void update() {
		if (mBoss.getHealth() <= 0) {
			mBoss.getWorld().hideBossBar(mBar);
		}

		Location loc = mLocationFunction.apply(mBoss);
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getWorld().equals(loc.getWorld()) && player.getLocation().distanceSquared(loc) < mRange * mRange) {
				mBar.addViewer(player);
			} else {
				mBar.removeViewer(player);
			}
		}

		double maxHealth = EntityUtils.getMaxHealth(mBoss);
		OptionalDouble forceProgress = progressEvents();
		double progress = forceProgress.orElse(mBoss.getHealth() / maxHealth);

		if (!Double.isFinite(progress) || progress > 1.0f || progress < 0f) {
			MMLog.warning("Boss '" + mBoss.getName() + "' has invalid health " +
				mBoss.getHealth() + " out of max " + maxHealth);
		} else {
			forceProgress.ifPresent(p -> mBoss.setHealth(maxHealth * p));
			mBar.progress((float) progress);
		}
	}

	public OptionalDouble progressEvents() {
		double maxHealth = EntityUtils.getMaxHealth(mBoss);
		double currentPercent = mBoss.getHealth() / maxHealth * 100;
		if (mEvents == null) {
			return OptionalDouble.empty();
		}
		while (true) {
			@Nullable
			Map.Entry<Integer, BossHealthAction> entry = mEvents.peek();
			if (entry == null || entry.getKey() < currentPercent) {
				return OptionalDouble.empty();
			}

			MMLog.fine("Running BossHealthAction for %s at %s%% health.".formatted(MessagingUtils.plainText(mBoss.name()), currentPercent));
			entry.getValue().run(mBoss);
			mEvents.remove();
			if (mCapDamage) {
				return OptionalDouble.of(entry.getKey() / 100.0);
			}
		}
	}

	public void setTitle(String newTitle) {
		mBar.name(Component.text(newTitle));
	}

	public void setColor(BossBar.Color barColor) {
		mBar.color(barColor);
	}

	public void remove() {
		mBoss.getWorld().hideBossBar(mBar);
	}

	public boolean capsDamage() {
		return mCapDamage;
	}

	public Optional<Integer> getNextHealthThreshold() {
		return Optional.ofNullable(mEvents.peek()).map(Map.Entry::getKey);
	}

	public boolean removeHealthEvent(int percent) {
		return mEvents != null && mEvents.removeIf(entry -> entry.getKey() == percent);
	}
}
