package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.GrapplingFallDR;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.protocollib.PingListener;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Grappling implements Enchantment {
	private static final double MAX_VERTICAL_DISTANCE_PER_LEVEL = 1;
	private static final double MAX_HORIZONTAL_DISTANCE_PER_LEVEL = 0.5;
	private static final double FALL_DR_PER_LEVEL = -0.015;
	private static final int PICKUP_RANGE_SQUARED = 12; // Squared distance a player can pick up a hook to replenish charges
	public static final double PLAYER_HORIZONTAL_SPEED = 15.0 / 200; // Speed constant used for players. Derived through trial and error.
	public static final double MOB_HORIZONTAL_SPEED = 29.0 / 200; // Speed constant used for mobs. Derived through trial and error.
	private static final int COOLDOWN = 5 * 20;
	public static String MAX_CHARGES_SCOREBOARD = "GrapplingMaxCharges";
	public static final Material COOLDOWN_ITEM = Material.CHAIN;
	private static final HashMap<UUID, PickupHook> mPlayerHookMap = new HashMap<>(); // Tracks which hook belongs to which player
	private static final HashMap<UUID, Integer> mPlayerShotsFiredMap = new HashMap<>(); // Tracks how many shots each player has fired
	private static final HashMap<UUID, Integer> mPlayerMostRecentPingMap = new HashMap<>(); // Tracks a player's ping. Used for lag compensation
	private static final HashMap<UUID, BukkitTask> mPlayerCooldownMap = new HashMap<>();

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.GRAPPLING;
	}

	@Override
	public String getName() {
		return "Grappling";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public void onLoadCrossbow(Plugin plugin, Player player, double value, EntityLoadCrossbowEvent event) {
		ItemStack bow = event.getCrossbow();
		// Don't allow loading crossbows if the player is out of charges
		if (ItemStatUtils.getEnchantmentLevel(bow, getEnchantmentType()) > 0 && plugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(getEnchantmentType()))) {
			player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(bow) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
			event.setCancelled(true);
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile projectile) {
		ItemStack bow = player.getInventory().getItemInMainHand();
		if (ItemStatUtils.getEnchantmentLevel(bow, getEnchantmentType()) < 1
			|| (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.FORCE_ENABLE_GRAPPLING_HOOK))
			|| projectile.getScoreboardTags().contains("NoGrapple")) {
			return;
		}
		// Check for cooldown
		if (plugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(getEnchantmentType()))) {
			player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(bow) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
			event.setCancelled(true);
			return;
		}

		if (player.getGameMode() != GameMode.CREATIVE) {
			// Decrement charges
			int maxCharges = ScoreboardUtils.getScoreboardValue(player, MAX_CHARGES_SCOREBOARD).orElse(1);
			synchronized (player) {
				int shotsFired = mPlayerShotsFiredMap.getOrDefault(player.getUniqueId(), 0) + 1;
				mPlayerShotsFiredMap.put(player.getUniqueId(), shotsFired);
				if (shotsFired >= maxCharges) {
					plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(COOLDOWN, bow, COOLDOWN_ITEM, plugin));
				}
				player.sendActionBar(Component.text("Grappling Charges: " + (maxCharges - shotsFired), NamedTextColor.YELLOW));
			}
		}

		// Registers the projectile with the Grappling Listener (see also com.playmonumenta.plugins.listeners.GrapplingListener)
		// This ensures the player is able to fire the Grappling hook, and switch weapons before the projectile lands
		plugin.mGrapplingListener.registerArrow(projectile, level);

		// Get player's current ping, and track it for lag compensation
		PingListener.submitPingAction(player, (ping) -> mPlayerMostRecentPingMap.put(player.getUniqueId(), ping), Constants.TICKS_PER_SECOND * 5, true, null);

		// After cooldown period, check if the projectile is still active. If it is, remove it
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (projectile.isValid()) {
				doFailEffects(player, projectile.getLocation());
				projectile.remove();
			}
		}, COOLDOWN);

		// If the player doesn't already have a cooldown counting down, start one
		if (!mPlayerCooldownMap.containsKey(player.getUniqueId())) {
			mPlayerCooldownMap.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> decrementShotsFired(player), COOLDOWN));
		}
	}

	// Called by GrapplingListener when a Grappling projectile collides with something
	public static void handleProjectileHit(Player player, double level, ProjectileHitEvent event, Projectile proj) {
		if ((ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.FORCE_ENABLE_GRAPPLING_HOOK))
			|| proj.getScoreboardTags().contains("NoGrapple")) {
			return;
		}

		if (event.getHitBlock() != null) {
			handleBlock(player, level, proj, event.getHitBlock());
		} else if (event.getHitEntity() != null) {
			if (event.getHitEntity() instanceof Mob m) {
				handleMob(player, level, event, proj, m);
			} else {
				// Grappling does not work on non-mob entities
				doFailEffects(player, event.getHitEntity().getLocation());
			}
		}
	}

	// Called when a Grappling projectile hits a block
	private static void handleBlock(Player player, double level, Projectile proj, Block hitBlock) {
		proj.remove();

		Vector v = hitBlock.getLocation().subtract(player.getLocation()).toVector();

		// Check if projectile is within max distance
		if (v.lengthSquared() > level * level) {
			doFailEffects(player, hitBlock.getLocation().add(0.5, 0.5, 0.5));
			return;
		}

		Location hookLoc = hitBlock.getLocation()
			.add(0.5, 0.5, 0.5) // Add 0.5 so we reference the center of a block, rather than the corner
			.subtract(v.normalize());    // Move hookLoc towards the edge of the block
		spawnPickupHook(player, level, hookLoc, v, hitBlock.getType(), null);
		doSucceedEffects(player, hookLoc, hitBlock.getType());
	}

	// Called when a Grappling projectile hits a mob
	private static void handleMob(Player player, double level, ProjectileHitEvent event, Projectile proj, Mob mob) {
		// Cancel the knockback from the bow shot
		event.setCancelled(true);
		proj.remove();

		Vector v = mob.getLocation().subtract(player.getLocation()).toVector();

		// Check if projectile is within max distance
		if (v.lengthSquared() > level * level) {
			doFailEffects(player, mob.getLocation().add(0, mob.getHeight() / 2, 0));
			return;
		}

		spawnPickupHook(player, level, mob.getLocation(), v, Material.REDSTONE_BLOCK, mob);
		doSucceedEffects(player, mob.getLocation(), Material.REDSTONE_BLOCK);
	}

	// Spawns a "pickup hook" where a projectile lands
	private static void spawnPickupHook(Player player, double grapplingLevel, Location spawnLocation, Vector pointingVector, Material particleMaterial, @Nullable Mob hitMob) {
		PickupHook hook = new PickupHook(spawnLocation, pointingVector, particleMaterial, hitMob);

		// Players should only have one hook out at a time
		if (mPlayerHookMap.get(player.getUniqueId()) != null) {
			PickupHook oldHook = mPlayerHookMap.remove(player.getUniqueId());
			oldHook.remove();
		}
		mPlayerHookMap.put(player.getUniqueId(), hook);
		int maxCharges = ScoreboardUtils.getScoreboardValue(player, MAX_CHARGES_SCOREBOARD).orElse(1);

		// Decide hook color based on a player's remaining charges
		NamedTextColor glowColor = switch (maxCharges - mPlayerShotsFiredMap.getOrDefault(player.getUniqueId(), 0)) {
			case 0 -> NamedTextColor.RED;
			case 1 -> NamedTextColor.GOLD;
			case 2 -> NamedTextColor.GREEN;
			case 3 -> NamedTextColor.BLUE;
			default -> NamedTextColor.LIGHT_PURPLE;
		};

		// Start glowing effect for hook and/or mob
		GlowingManager.startGlowing(hook.getDisplay(), glowColor, COOLDOWN, 0);
		GlowingManager.ActiveGlowingEffect mobGlow = hitMob == null ? null : GlowingManager.startGlowing(hitMob, glowColor, COOLDOWN, 0);

		// Get remaining cooldown to adjust life of the final arrow
		Effect cooldownEffect = Plugin.getInstance().mEffectManager.getActiveEffect(player, ItemCooldown.toSource(EnchantmentType.GRAPPLING));
		int cooldownRemaining = cooldownEffect == null ? COOLDOWN : cooldownEffect.getDuration();

		int hz = 20;
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				// Cancel glow if the hook has been pulled
				if (hook.mPulledOnce) {
					if (mobGlow != null) {
						mobGlow.clear();
					}
				}
				// Draw a line as long as the hook hasn't been pulled
				if (!hook.mPulledOnce) {
					drawRope(player, hook.getLocation(), (20 - (mTicks % 20)) / 20.0);
				}

				if (!hook.isValid()) {
					this.cancel();
				}

				if (mTicks >= cooldownRemaining * hz / 20) {
					this.cancel();
				}
				// Cancel early and refund a charge if the player sneaks near their hook
				if (hook.getLocation().distanceSquared(player.getLocation()) < PICKUP_RANGE_SQUARED) {
					if (player.isSneaking()) {
						Plugin.getInstance().mEffectManager.clearEffects(player, ItemCooldown.toSource(EnchantmentType.GRAPPLING));
						player.setCooldown(COOLDOWN_ITEM, 0);
						doPickupAnimation(player);
						mPlayerCooldownMap.remove(player.getUniqueId()).cancel();
						decrementShotsFired(player);
						this.cancel();
					}
				}
				// Cancel early if the player goes out of range
				if (hook.getLocation().distanceSquared(player.getLocation()) > grapplingLevel * grapplingLevel) {
					doFailEffects(player, hook.getLocation());
					this.cancel();
				}
				// Keep the hook at the embedded mob's position
				if (hook.getEmbedMob() != null && !hook.mPulledOnce) {
					hook.mCarrier.teleport(hook.getEmbedMob().getLocation().add(0, hook.getEmbedMob().getHeight() / 2, 0));
				}
				// Drop the hook on the ground if the embedded mob dies early
				if (hook.getEmbedMob() != null && !hook.getEmbedMob().isValid() && !hook.mPulledOnce) {
					hook.mPulledOnce = true;
					hook.mCarrier.setGravity(true);
					hook.mCarrier.addPassenger(hook.mDisplay);
					hook.mDisplay.setItemStack(new ItemStack(Material.ARROW)); // Make the item display visible
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				if (mPlayerHookMap.get(player.getUniqueId()) == hook) {
					mPlayerHookMap.remove(player.getUniqueId());
				}
				hook.remove();
				if (mobGlow != null) {
					mobGlow.clear();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 20 / hz);
	}

	@Override
	public void onPlayerInteract(Plugin plugin, Player player, double level, PlayerInteractEvent event) {
		if (!event.getAction().isLeftClick()
			|| !mPlayerHookMap.containsKey(player.getUniqueId())) {
			return;
		}

		PickupHook hook = mPlayerHookMap.get(player.getUniqueId());
		if (hook.mPulledOnce) {
			return;
		}

		if (player.isSneaking()) {
			hook.pullTowards(player, level);
			if (hook.mEmbedMob != null) {
				pullMob(player, hook.mEmbedMob, MOB_HORIZONTAL_SPEED, level);
			}
			return;
		}

		pullPlayer(player, level, hook);
	}

	private void pullPlayer(Player player, double level, PickupHook hook) {
		// Pull player
		new PartialParticle(Particle.BLOCK_CRACK, hook.getLocation(), 40, 0.25, 0.25, 0.25, hook.getHitBlock().createBlockData()).spawnAsPlayerActive(player);

		// Get the ping stored from when the player fired the hook
		int ping = 100; // Default value of 100ms
		if (mPlayerMostRecentPingMap.containsKey(player.getUniqueId())) {
			ping = mPlayerMostRecentPingMap.remove(player.getUniqueId());
		}

		// Based off of the player's current velocity and ping, estimate where they're going to be by the time they actually get sent flying
		double tickDelay = Math.ceil(ping / (1000f / Constants.TICKS_PER_SECOND));
		double yDisplacementInNTicks;
		if (PlayerUtils.isOnGround(player)) {
			yDisplacementInNTicks = 0;
		} else {
			// from wiki: displacement d(N) = y0 + 50(v0 + 3.92) * (1 - 0.98^N) - 3.92N where N is number of ticks, y0 is starting height, v0 is starting velocity
			yDisplacementInNTicks = 50 * (player.getVelocity().getY() + 3.92) * (1 - Math.pow(0.98, tickDelay)) - 3.92 * tickDelay;
		}
		Location playerLoc = player.getLocation().add(0, yDisplacementInNTicks, 0);


		Vector v = hook.getLocation().clone().subtract(playerLoc).toVector();

		double vertDist = v.getY();
		// Cap vertical movement
		if (vertDist > MAX_VERTICAL_DISTANCE_PER_LEVEL * level) {
			hook.getLocation().subtract(0, vertDist - MAX_VERTICAL_DISTANCE_PER_LEVEL * level, 0).getBlock();
		}

		Location landingZone = hook.getLocation().clone()
			.add(0.5, 1, 0.5)     // Add 0.5 x and z because coordinates will place you at the northeast corner; add 1 y so we are on top of block
			.add(0, 2, 0);        // Add 2 so we overshoot a bit and fall onto the location

		// Apply fall damage reduction until player lands
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> Plugin.getInstance().mEffectManager.addEffect(player, "GrapplingFallDR", new GrapplingFallDR(200, FALL_DR_PER_LEVEL * level)), 5);

		doLaunchEffects(player, Math.max(0.5, v.length() / 10));

		// Pull differently if player is above the hook or not
		// Calculates the angle such that 0 degrees is perfectly above the player, 180 degrees is perfectly below the player
		Vector angleCheck = hook.getLocation().clone().add(0.5, 1, 0.5).subtract(playerLoc).toVector();
		double hypotenuse = angleCheck.length();
		double downAngle = Math.acos(angleCheck.getY() / hypotenuse);
		if (downAngle > Math.toRadians(135)) {
			v = hook.getLocation().clone().subtract(playerLoc).toVector();
			v.multiply(PLAYER_HORIZONTAL_SPEED * 2);
			v.setY(v.getY() * 1.5);
			player.setVelocity(v);
			hook.mPulledOnce = true;
		} else {
			hook.pullTowards(player, level);
			player.setVelocity(calcVelocity(landingZone, playerLoc, PLAYER_HORIZONTAL_SPEED, level));
		}
	}

	public static void pullMob(LivingEntity stationary, LivingEntity mover, double speed, double level) {
		if (EntityUtils.isBoss(mover) || EntityUtils.isCCImmuneMob(mover)) {
			if (stationary instanceof Player p) {
				p.playSound(p.getLocation(), Sound.BLOCK_WET_GRASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
				p.playSound(p.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
				p.playSound(p.getLocation(), Sound.ENTITY_LEASH_KNOT_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				new PPLine(Particle.SMOKE_NORMAL, p.getEyeLocation(), mover.getLocation().add(0, mover.getHeight() / 2, 0)).countPerMeter(2).spawnAsPlayerActive(p);
			}
			return;
		}
		mover.setVelocity(calcVelocity(stationary.getEyeLocation(), mover.getLocation(), speed, level));
	}


	// Calculates a velocity vector so that the target will land at the landing zone after about one second.
	// Horizontal velocity may cap, so the target may fall short (can be increased with MAX_HORIZONTAL_PER_LEVEL)
	private static Vector calcVelocity(Location landingZone, Entity target, double level) {
		return calcVelocity(landingZone, target.getLocation(), MOB_HORIZONTAL_SPEED, level);
	}

	private static Vector calcVelocity(Location landingZone, Location estimatedLocationAfterLag, double flightSpeed, double level) {
		Vector distVector = landingZone.subtract(estimatedLocationAfterLag).toVector();    // Get vector from target location (after lag compensation) to landing zone

		double yDist = distVector.getY();

		// from wiki: displacement d(N) = y_0 + 50(v_0 + 3.92) * (1 - 0.98^N) - 3.92N where N is number of ticks, y_0 is starting height, v_0 is starting velocity
		// The actual formula to get v_0 is messy and computationally expensive, these constants are a close enough approximation for our purposes.
		double v0;
		if (yDist <= 0) {
			v0 = -Math.pow(-yDist / 5.93, 0.578) + 0.8;
		} else {
			v0 = Math.pow(yDist / 5.93, 0.578);
		}

		distVector.setY(0); // "Flatten" vector so we can check horizontal distance
		// Cap horizontal speed at MAX_HORIZONTAL_PER_LEVEL * level
		if (distVector.lengthSquared() > MAX_HORIZONTAL_DISTANCE_PER_LEVEL * MAX_HORIZONTAL_DISTANCE_PER_LEVEL * level * level) {
			distVector.normalize();
			distVector.multiply(MAX_HORIZONTAL_DISTANCE_PER_LEVEL * level);
		}
		distVector.multiply(flightSpeed); // Set the horizontal velocity
		distVector.setY(v0); // Re-add vertical velocity

		return distVector;
	}

	private static void doFailEffects(Player player, Location lineEnd) {
		player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 0.8f, 0.75f);
		player.playSound(player.getLocation(), Sound.ENTITY_LEASH_KNOT_BREAK, SoundCategory.PLAYERS, 0.8f, 0.5f);
		new PPLine(Particle.SMOKE_NORMAL, player.getEyeLocation(), lineEnd).countPerMeter(2).spawnAsPlayerActive(player);
	}

	private static void doSucceedEffects(Player player, Location target, Material particleMaterial) {
		player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_HURT, SoundCategory.PLAYERS, 0.8f, 2f);
		player.playSound(player.getLocation(), Sound.ENTITY_LEASH_KNOT_BREAK, SoundCategory.PLAYERS, 0.8f, 1f);
		player.playSound(player.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 0.8f, 0.75f);

		new PartialParticle(Particle.BLOCK_CRACK, target, 40, 0.25, 0.25, 0.25, particleMaterial.createBlockData()).spawnAsPlayerActive(player);
		player.getWorld().playSound(target, Sound.BLOCK_STONE_FALL, SoundCategory.PLAYERS, 5f, 1f);
	}

	private static void doPickupAnimation(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_STONE_FALL, SoundCategory.PLAYERS, 5f, 1f);
		player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 5f, 1f);
		player.playSound(player.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 5f, 1.5f);
	}

	private static void drawRope(Player player, Location hookLoc, double shiftAmount) {
		new PPLine(Particle.CRIT, player.getLocation(), hookLoc).shift(shiftAmount).countPerMeter(1).delta(0.05).extra(0.075).spawnAsPlayerActive(player);
	}

	private static void doLaunchEffects(Player player, double radius) {
		new PPCircle(Particle.SPIT, player.getLocation(), radius).countPerMeter(10).delta(0).ringMode(false).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL, player.getLocation(), radius).countPerMeter(10).delta(0).spawnAsPlayerActive(player);
		player.getWorld().playSound(player, Sound.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.PLAYERS, 1f, 1f);
		player.getWorld().playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
		player.getWorld().playSound(player, Sound.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 1f, 1f);
	}

	private static synchronized void decrementShotsFired(Player player) {
		mPlayerCooldownMap.remove(player.getUniqueId());
		int shotsFired = mPlayerShotsFiredMap.getOrDefault(player.getUniqueId(), 0);
		if (shotsFired > 0) {
			Plugin.getInstance().mEffectManager.clearEffects(player, ItemCooldown.toSource(EnchantmentType.GRAPPLING));
			player.setCooldown(COOLDOWN_ITEM, 0);
			if (shotsFired == 1) {
				// Remove players who have 0 shots fired after decrementing
				mPlayerShotsFiredMap.remove(player.getUniqueId());
			} else {
				mPlayerShotsFiredMap.put(player.getUniqueId(), shotsFired - 1);
				mPlayerCooldownMap.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> decrementShotsFired(player), COOLDOWN));
			}
			player.sendActionBar(Component.text("Grappling Charges: "
					+ (ScoreboardUtils.getScoreboardValue(player, MAX_CHARGES_SCOREBOARD).orElse(1) - shotsFired + 1),
				NamedTextColor.YELLOW));
		}
	}

	public static void untrackPlayer(Player player) {
		if (mPlayerHookMap.containsKey(player.getUniqueId())) {
			mPlayerHookMap.get(player.getUniqueId()).remove();
			mPlayerHookMap.remove(player.getUniqueId());
		}
		mPlayerShotsFiredMap.remove(player.getUniqueId());
		mPlayerMostRecentPingMap.remove(player.getUniqueId());
		BukkitTask task = mPlayerCooldownMap.remove(player.getUniqueId());
		if (task != null) {
			task.cancel();
		}
	}

	public static boolean playerHoldingHook(Player player) {
		return ItemStatUtils.getEnchantmentLevel(player.getInventory().getItemInMainHand(), EnchantmentType.GRAPPLING) > 0;
	}

	private static class PickupHook {
		private final ArmorStand mCarrier;
		private final ItemDisplay mDisplay;
		private final Material mHitBlock;
		@Nullable
		public Mob mEmbedMob;
		private boolean mPulledOnce = false;

		public PickupHook(Location spawnLocation, Vector pointingVector, Material hitBlock, @Nullable Mob embedMob) {
			mCarrier = spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, carrier -> {
				carrier.setVisible(false);
				carrier.setGravity(false);
				carrier.setSmall(true);
				carrier.setBasePlate(false);
				carrier.setCollidable(false);
			});
			mDisplay = spawnLocation.getWorld().spawn(spawnLocation, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.ARROW));
				DisplayEntityUtils.rotateToPointAtLoc(display, pointingVector.normalize(), 0, 5 * Math.PI / 4);
			});
			mHitBlock = hitBlock;
			mEmbedMob = embedMob;
			if (mEmbedMob != null) {
				mCarrier.teleport(mEmbedMob.getEyeLocation());
				mDisplay.setItemStack(new ItemStack(Material.AIR));
			}
		}

		public void remove() {
			mCarrier.remove();
			mDisplay.remove();
		}

		public void pullTowards(Player player, double level) {
			if (mPulledOnce) {
				return;
			}
			mPulledOnce = true;
			mCarrier.setGravity(true);
			mCarrier.addPassenger(mDisplay);
			Vector v = getLocation().subtract(player.getLocation()).toVector().multiply(0.5);
			mCarrier.setVelocity(calcVelocity(player.getLocation().add(v),
				mCarrier, level));
			mDisplay.setItemStack(new ItemStack(Material.ARROW));
		}

		public boolean isValid() {
			return mCarrier.isValid();
		}

		public Location getLocation() {
			return mCarrier.getLocation();
		}

		public ItemDisplay getDisplay() {
			return mDisplay;
		}

		public Material getHitBlock() {
			return mHitBlock;
		}

		public @Nullable Mob getEmbedMob() {
			return mEmbedMob;
		}
	}
}
