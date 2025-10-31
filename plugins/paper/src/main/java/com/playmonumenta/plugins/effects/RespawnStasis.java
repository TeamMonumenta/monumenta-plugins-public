package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RespawnStasis extends Stasis {
	public static final String effectID = "RespawnStasis";

	public static final String NAME = "RespawnStasis";

	public static final int DURATION = 20 * 60;

	public static final int MINIMUM_DURATION = 10;
	public static final int SPECTATE_DURATION = 3 * 20;
	public static final String SPECTATE_DISABLE_TAG = "RespawnStasisSpectateDisable";
	public static final String TEMP_NO_SPECTATE_TAG = "TempRespawnStasisNoSpectate";

	int mShatter;
	int mShatterLevel;

	private boolean mRemoveActionbar = false;
	private final @Nullable Location mDeathLocation;
	private final @Nullable Location mRespawnLocation;
	public boolean mCanStopSpectating = false;

	public RespawnStasis(@Nullable Location deathLocation, @Nullable Location respawnLocation) {
		super(DURATION, effectID);
		mDeathLocation = deathLocation;
		mRespawnLocation = respawnLocation;
	}

	private RespawnStasis(int duration) {
		super(duration, effectID);
		mDeathLocation = null;
		mRespawnLocation = null;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			mShatter = Shattered.getHighestShatterLevelEquipped(player);
			mShatterLevel = Shattered.getShatteredLevelsEquipped(player);

			if (mDeathLocation != null && mRespawnLocation != null
				&& mDeathLocation.getWorld().equals(mRespawnLocation.getWorld())
				&& mDeathLocation.getY() > 0
				&& player.hasPermission("monumenta.deathspectate")
				&& !player.getScoreboardTags().contains(SPECTATE_DISABLE_TAG)
				&& !ZoneUtils.hasZoneProperty(mDeathLocation, ZoneUtils.ZoneProperty.NO_SPECTATOR_ON_DEATH)
				&& !ZoneUtils.hasZoneProperty(mRespawnLocation, ZoneUtils.ZoneProperty.NO_SPECTATOR_ON_RESPAWN)
				&& !player.getScoreboardTags().contains(TEMP_NO_SPECTATE_TAG)
			) {
				Location spectateLocation = findSpectateLocation(mDeathLocation, player);
				ArmorStand stand = spectateLocation.getWorld().spawn(spectateLocation, ArmorStand.class, s -> {
					s.setInvisible(true);
					s.setMarker(true);
					s.setInvulnerable(true);
					s.setGravity(false);
				});
				EntityUtils.setRemoveEntityOnUnload(stand);

				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					if (player.getWorld() != stand.getWorld()) {
						return;
					}
					player.setGameMode(GameMode.SPECTATOR);
					player.setSpectatorTarget(stand);

					Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
						if (player.isOnline() && getDuration() > 0) {
							removeSpectator(player);
						}
						if (stand.isValid()) {
							stand.remove();
						}
					}, SPECTATE_DURATION);
				});
			} else {
				if (player.getGameMode() == GameMode.SPECTATOR) {
					Location respawnLocation = PlayerUtils.getRespawnLocationAndClear(player, player.getWorld(), player.getRespawnLocation());
					player.teleport(respawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
					ZoneUtils.setExpectedGameMode(player);
				}
				startEffects(player);
			}

			player.removeScoreboardTag(TEMP_NO_SPECTATE_TAG);

			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> showMessages(player, false));

			AbilityManager.getManager().updateSilence(player, true);
		}
	}

	private void startEffects(Player player) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, getDuration(), 0, false, false, false));
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, getDuration(), 0, false, false, false));
		// The following lines are from base Stasis with sound, glowing, and action bar message removed; and also potion effects' icons and particles removed
		Plugin.getInstance().mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(getDuration(), -1, SPEED_EFFECT_NAME).displays(false));
	}

	private static Location findSpectateLocation(Location deathLocation, Player player) {
		Vector dir = deathLocation.getDirection();
		dir = dir.setY(0);
		if (dir.lengthSquared() < 0.001) {
			return deathLocation;
		}
		dir = dir.normalize();
		dir.setY(-0.66);
		dir = dir.normalize().multiply(0.25);
		Location loc = deathLocation.clone().add(0, 1.6, 0);
		for (int i = 0; i < 30; i++) {
			loc.subtract(dir);
			if (loc.getBlock().isSolid() || loc.getBlock().getRelative(BlockFace.UP).isSolid() || LocationUtils.collidesWithBlocks(player.getBoundingBox().shift(loc.clone().subtract(player.getLocation())), player.getWorld())) {
				loc.add(dir);
				return loc;
			}
		}

		return loc;
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		mCanStopSpectating = true;
		if (entity instanceof Player player) {
			player.removePotionEffect(PotionEffectType.BLINDNESS);
			player.removePotionEffect(PotionEffectType.INVISIBILITY);
			player.removePotionEffect(PotionEffectType.SLOW);
			player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
			showMessages(player, true);
			if (player.getGameMode() == GameMode.SPECTATOR) {
				if (mRespawnLocation != null) {
					player.teleport(mRespawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
				}
				ZoneUtils.setExpectedGameMode(player);
			}
		}
		super.entityLoseEffect(entity);
	}

	// This is overridden from Stasis to remove action bar message and particles
	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (!(entity instanceof Player player)) {
			return;
		}
		if (player.getGameMode() != GameMode.SPECTATOR) {
			new PartialParticle(Particle.SMOKE_NORMAL, entity.getLocation().add(0, 0.5, 0), 25)
				.delta(0.2, 0.5, 0.2)
				.spawnAsEntityActive(entity);
			new PartialParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 1)
				.delta(0.3, 0.5, 0.3)
				.spawnAsEntityActive(entity);
		} else {
			Block block = player.getLocation().getBlock();
			if (block.isSolid() || block.getRelative(BlockFace.UP).isSolid()) {
				removeSpectator(player);
			}
		}
		if (oneHertz) {
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
					.append(Component.text(StringUtils.multiplierToPercentage(Shattered.getMultiplier(mShatterLevel)) + "% more damage", NamedTextColor.RED))
					.append(Component.text(" until you repair your gear by ", NamedTextColor.GOLD))
					.append(Component.text("collecting your grave", NamedTextColor.AQUA))
					.append(Component.text("!", NamedTextColor.GOLD)));
			} else {
				player.sendActionBar(Component.text("You will take ", NamedTextColor.GOLD)
					.append(Component.text(StringUtils.multiplierToPercentage(Shattered.getMultiplier(mShatterLevel)) + "% more damage", NamedTextColor.RED))
					.append(Component.text(" until you repair your gear by ", NamedTextColor.GOLD))
					.append(Component.text("using anvils on it", NamedTextColor.AQUA))
					.append(Component.text("!", NamedTextColor.GOLD)));
			}
			mRemoveActionbar = false;
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

	private void removeSpectator(Player player) {
		mCanStopSpectating = true;
		ZoneUtils.setExpectedGameMode(player);
		if (mRespawnLocation != null) {
			player.teleport(mRespawnLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
		}
		startEffects(player);
	}

	@Override
	public boolean pauseInSpectatorMode() {
		return false;
	}

	@Override
	public boolean skipInRespawnRefresh() {
		return true;
	}

	public static Stasis deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		return new RespawnStasis(duration);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return null;
	}

	@Override
	public @Nullable String getDisplayedName() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("RespawnStasis duration:%d", this.getDuration());
	}

}
