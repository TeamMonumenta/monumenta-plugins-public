package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class TemporalFlux extends ZeroArgumentEffect {

	public static final String GENERIC_NAME = "Paradox";
	public static final String effectID = "TemporalFlux";
	public static final int MAX_TIME = 30 * 20;

	private final BossBar mBossBar;

	public TemporalFlux(int duration) {
		super(duration, effectID);
		mBossBar = BossBar.bossBar(Component.text("Paradox expires in " + (duration / 20) + "seconds!", NamedTextColor.BLUE), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
		deleteOnLogout(true);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.HOSTILE, 30, 1);
		}

		if (fourHertz) {
			float progress = ((float) getDuration() / (float) MAX_TIME);
			mBossBar.progress(progress);
			mBossBar.name(Component.text("Paradox expires in " + (getDuration() / 20) + " seconds!", NamedTextColor.BLUE));
			if (progress <= 0.01) {
				entity.hideBossBar(mBossBar);
				if (entity instanceof Player) {
					PlayerUtils.killPlayer((Player) entity, null, GENERIC_NAME, true, true, true);
				} else {
					MMLog.warning(() -> "[TemporalFlux] Warning: Instance of Paradox applied to non-player entity " + entity);
				}

				return;
			}
			if (progress <= 0.25) {
				mBossBar.color(BossBar.Color.RED);
			} else if (progress <= 0.5) {
				mBossBar.color(BossBar.Color.YELLOW);
			} else if (progress > 0.5) {
				mBossBar.color(BossBar.Color.BLUE);
			}
			if (getDuration() % (20 * 10) == 0) {
				entity.sendMessage(Component.text("Paradox has ", NamedTextColor.RED).append(Component.text(getDuration() / 20, NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text(" seconds remaining!")));
			}
			new PPCircle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 1)
				.count(20).delta(0.25, 0.1, 0.25).spawnAsBoss();
		}
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(getDisplayedName() + " \u2620", NamedTextColor.RED);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Paradox";
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			Component message = Component.text("")
				.append(Component.text(entity.getName(), NamedTextColor.BLUE, TextDecoration.BOLD))
				.append(Component.text(" has been given the Paradox effect!", NamedTextColor.BLUE));
			for (Player p : PlayerUtils.playersInRange(entity.getLocation(), 50, true)) {
				p.sendMessage(message);
			}
			player.sendMessage(
				Component.text("You have been inflicted with Paradox! Quickly transfer it using the ", NamedTextColor.RED)
					.append(Component.text("Temporal Exchanger", NamedTextColor.GOLD))
					.append(Component.text("!", NamedTextColor.WHITE))
			);
			player.playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 20, 1);
			player.showBossBar(mBossBar);
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player player) {
			player.hideBossBar(mBossBar);
			setDuration(0);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		entity.hideBossBar(mBossBar);
		entity.sendMessage(Component.text("You are no longer inflicted with Paradox; you are safe for now.", NamedTextColor.GRAY));
	}

	public static @Nullable TemporalFlux deserialize(JsonObject object, Plugin plugin) {
		return null;
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%d", this.getDuration());
	}

	@Override
	public boolean skipInRespawnRefresh() {
		return true;
	}

}
