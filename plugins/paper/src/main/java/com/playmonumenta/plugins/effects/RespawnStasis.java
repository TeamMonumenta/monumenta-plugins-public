package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class RespawnStasis extends Stasis {
	public static final String effectID = "RespawnStasis";

	public static final String NAME = "RespawnStasis";

	public static final int DURATION = 20 * 60;

	public static final int MINIMUM_DURATION = 10;

	int mShatter;

	private boolean mRemoveActionbar = false;

	public RespawnStasis() {
		super(DURATION, effectID);
	}

	private RespawnStasis(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			mShatter = Shattered.getHighestShatterLevelEquipped(player);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, getDuration(), 0, false, false, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, getDuration(), 0, false, false, false));

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> showMessages(player, false));

			// The following lines are from base Stasis with sound, glowing, and action bar message removed; and also potion effects' icons and particles removed
			player.addScoreboardTag(Constants.Tags.STASIS);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, getDuration(), 9, false, false, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, getDuration(), 4, false, false, false));
			AbilityManager.getManager().updateSilence(player, true);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.removePotionEffect(PotionEffectType.BLINDNESS);
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
			player.removePotionEffect(PotionEffectType.SLOW);
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			showMessages(player, true);
		}
		super.entityLoseEffect(entity);
	}

	// This is overridden from Statis to remove action bar message and particles
	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.SMOKE_NORMAL, entity.getLocation().add(0, 0.5, 0), 25)
			.delta(0.2, 0.5, 0.2)
			.spawnAsEntityActive(entity);
		new PartialParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 1)
			.delta(0.3, 0.5, 0.3)
			.spawnAsEntityActive(entity);
		if (oneHertz && entity instanceof Player player) {
			showMessages(player, false);
		}
	}

	private void showMessages(Player player, boolean fadeOut) {
		if (!fadeOut || mRemoveActionbar) {
			Component subtitle;
			if (mShatter == 1) {
				subtitle = Component.text("Your gear is shattered!", NamedTextColor.RED);
			} else if (mShatter > 1) {
				subtitle = Component.text("Your gear is ", NamedTextColor.RED).append(Component.text("heavily shattered!", NamedTextColor.RED).decorate(TextDecoration.BOLD));
			} else {
				subtitle = Component.text("Nothing shattered.", NamedTextColor.GRAY);
			}
			MessagingUtils.sendTitle(player, Component.text("You Died", NamedTextColor.RED), subtitle, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(fadeOut ? 0 : 2000), Duration.ofMillis(1000)));
		}
		if (fadeOut) {
			if (mRemoveActionbar) {
				player.sendActionBar(Component.empty());
			}
			return;
		}
		if (mShatter > 0 && getDuration() >= DURATION - 20 * 10) {
			// For the first 10 seconds, show hint on how to repair gear if shattered
			if (mShatter == 1) {
				player.sendActionBar(Component.text("You will take ", NamedTextColor.GOLD)
					                     .append(Component.text("a lot more damage", NamedTextColor.RED))
					                     .append(Component.text(" until you repair your gear by ", NamedTextColor.GOLD))
					                     .append(Component.text("collecting your grave", NamedTextColor.AQUA))
					                     .append(Component.text("!", NamedTextColor.GOLD)));
				mRemoveActionbar = false;
			} else {
				player.sendActionBar(Component.text("You will take ", NamedTextColor.GOLD)
					                     .append(Component.text("massively more damage", NamedTextColor.RED).decorate(TextDecoration.BOLD))
					                     .append(Component.text(" until you repair your gear by ", NamedTextColor.GOLD))
					                     .append(Component.text("using anvils on it", NamedTextColor.AQUA))
					                     .append(Component.text("!", NamedTextColor.GOLD)));
				mRemoveActionbar = false;
			}
		} else if (getDuration() >= DURATION / 2) {
			// After 10 seconds, tell the player that they can click to respawn (or after the minimal duration if not showing the shatter message)
			if (getDuration() <= DURATION - MINIMUM_DURATION) {
				player.sendActionBar(Component.text("(click any time to respawn)", NamedTextColor.WHITE));
				mRemoveActionbar = true;
			}
		} else {
			// After half the duration is over, inform the player that they will be force-respawned soon
			player.sendActionBar(Component.text("You will be forcibly respawned in " + (getDuration() / 20) + " seconds!", NamedTextColor.WHITE));
			mRemoveActionbar = true;
		}
	}

	public static Stasis deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		return new RespawnStasis(duration);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("RespawnStasis duration:%d", this.getDuration());
	}

}
