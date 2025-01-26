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
import com.playmonumenta.plugins.server.properties.ServerProperties;
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
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public class Grappling implements Enchantment {
	private static final double MAX_VERTICAL_PER_LEVEL = 1;
	private static final double MAX_HORIZONTAL_PER_LEVEL = 0.5;
	private static final double FALL_DR_PER_LEVEL = -0.015;
	private static final int PICKUP_RANGE_SQUARED = 12;
	public static final double PLAYER_HORIZONTAL_SPEED = 15.0 / 200;
	public static final double MOB_HORIZONTAL_SPEED = 29.0 / 200;
	private static final int COOLDOWN = 5 * 20;
	public static String MAX_CHARGES_SCOREBOARD = "GrapplingMaxCharges";
	public static final Material COOLDOWN_ITEM = Material.CHAIN;
	private static final HashMap<UUID, PickupHook> mPlayerHookMap = new HashMap<>();
	private static final HashMap<UUID, Integer> mPlayerShotsFiredMap = new HashMap<>();
	private static final HashMap<UUID, Integer> mPlayerMostRecentPingMap = new HashMap<>();

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
	public void onProjectileLaunch(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile projectile) {
		ItemStack bow = player.getInventory().getItemInMainHand();
		if (ItemStatUtils.getEnchantmentLevel(bow, getEnchantmentType()) < 1
			|| ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			|| projectile.getScoreboardTags().contains("NoGrapple")) {
			return;
		}
		if (plugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(getEnchantmentType()))) {
			if (!projectile.getScoreboardTags().contains("SourceQuickDraw")) {
				player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(bow) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
				event.setCancelled(true);
			}
			return;
		}

		if (player.getGameMode() != GameMode.CREATIVE) {
			// Decrement charges
			int maxCharges = 1;
			if (ServerProperties.getShardName().startsWith("dev")
				|| ServerProperties.getShardName().contains("dungeon")) {
				maxCharges = ScoreboardUtils.getScoreboardValue(player, MAX_CHARGES_SCOREBOARD).orElse(1);
			}
			synchronized (player) {
				int shotsFired = mPlayerShotsFiredMap.getOrDefault(player.getUniqueId(), 0) + 1;
				mPlayerShotsFiredMap.put(player.getUniqueId(), shotsFired);
				if (shotsFired >= maxCharges) {
					plugin.mEffectManager.addEffect(player, ItemCooldown.toSource(getEnchantmentType()), new ItemCooldown(COOLDOWN, bow, COOLDOWN_ITEM, plugin));
				}
				player.sendActionBar(Component.text("Grappling Charges: " + (maxCharges - shotsFired), NamedTextColor.YELLOW));
			}
		}
		plugin.mGrapplingListener.registerArrow(projectile, level);

		// Get player's ping, and add it to the map
		PingListener.submitPingAction(player, (ping) -> mPlayerMostRecentPingMap.put(player.getUniqueId(), ping), Constants.TICKS_PER_SECOND * 5, true, null);

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (projectile.isValid()) {
				doFailEffects(player, projectile.getLocation());
				projectile.remove();
				decrementShotsFired(player);
			}
		}, COOLDOWN);
	}

	@Override
	public void onLoadCrossbow(Plugin plugin, Player player, double value, EntityLoadCrossbowEvent event) {
		ItemStack bow = event.getCrossbow();
		if (ItemStatUtils.getEnchantmentLevel(bow, getEnchantmentType()) > 0 && plugin.mEffectManager.hasEffect(player, ItemCooldown.toSource(getEnchantmentType()))) {
			player.sendMessage(Component.text("Your " + ItemUtils.getPlainName(bow) + " is still on cooldown!", TextColor.fromHexString("#D02E28")));
			event.setCancelled(true);
		}
	}

	// Called by GrapplingListener
	public static void handleProjectileHit(Player player, double level, ProjectileHitEvent event, Projectile proj) {
		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			|| proj.getScoreboardTags().contains("NoGrapple")) {
			return;
		}

		if (event.getHitBlock() != null) {
			handleBlock(player, level, proj, event.getHitBlock());
		} else if (event.getHitEntity() != null && event.getHitEntity() instanceof Mob m) {
			// If we don't have this runnable, velocity is substantially weaker when pulling players towards mobs.
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> handleMob(player, level, event, proj, m), 1);
		}
	}

	private static void handleBlock(Player player, double level, Projectile proj, Block hitBlock) {
		proj.remove();

		Vector v = hitBlock.getLocation().subtract(player.getLocation()).toVector();

		if (v.lengthSquared() > level * level) {
			doFailEffects(player, hitBlock.getLocation().add(0.5, 0.5, 0.5));

			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> decrementShotsFired(player), COOLDOWN);
			return;
		}

		Location hookLoc = hitBlock.getLocation().add(0.5, 0.5, 0.5).subtract(v.normalize());
		spawnPickupHook(player, level, hookLoc, v, hitBlock);
		doSucceedEffects(player, hookLoc, hitBlock.getType());
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
			return;
		}

		pullPlayer(player, level, hook);
	}

	private void pullPlayer(Player player, double level, PickupHook hook) {
		// Pull player
		Block hitBlock = hook.getHitBlock();
		new PartialParticle(Particle.BLOCK_CRACK, hook.getLocation(), 40, 0.25, 0.25, 0.25, hitBlock.getType().createBlockData()).spawnAsPlayerActive(player);

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


		Vector v = hitBlock.getLocation().subtract(playerLoc).toVector();

		double vertDist = v.getY();
		if (vertDist > MAX_VERTICAL_PER_LEVEL * level) {
			// Cap vertical movement
			hitBlock = hitBlock.getLocation().subtract(0, vertDist - MAX_VERTICAL_PER_LEVEL * level, 0).getBlock();
		}

		Location landingZone = hitBlock.getLocation()
			.add(0.5, 1, 0.5)     // Add 0.5 x and z because coordinates will place you at the northeast corner; add 1 y so we are on top of block
			.add(0, 2, 0);        // Add 2 so we overshoot a bit and fall onto the location

		// Apply fall damage reduction until player lands
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> Plugin.getInstance().mEffectManager.addEffect(player, "GrapplingFallDR", new GrapplingFallDR(200, FALL_DR_PER_LEVEL * level)), 5);

		double r = Math.max(0.5, v.length() / 10);
		new PPCircle(Particle.SPIT, player.getLocation(), r).countPerMeter(10).delta(0).ringMode(false).spawnAsPlayerActive(player);
		new PPCircle(Particle.SPELL, player.getLocation(), r).countPerMeter(10).delta(0).spawnAsPlayerActive(player);
		player.getWorld().playSound(player, Sound.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.PLAYERS, 1f, 1f);
		player.getWorld().playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
		player.getWorld().playSound(player, Sound.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 1f, 1f);

		Vector angleCheck = hitBlock.getLocation().add(0.5, 1, 0.5).subtract(playerLoc).toVector();
		double hypotenuse = angleCheck.length();
		double downAngle = Math.acos(angleCheck.getY() / hypotenuse);
		if (downAngle > Math.toRadians(100)) {
			// Pull differently if player is above hook
			v = hitBlock.getLocation().subtract(playerLoc).toVector();
			v.multiply(PLAYER_HORIZONTAL_SPEED * 2);
			v.setY(v.getY() * 1.5);
			player.setVelocity(v);
			hook.mPulledOnce = true;
			return;
		}
		hook.pullTowards(player, level);
		player.setVelocity(calcVelocity(landingZone, playerLoc, PLAYER_HORIZONTAL_SPEED, level));
	}

	private static void spawnPickupHook(Player player, double level, Location spawnLocation, Vector pointingVector, Block hitBlock) {
		// Maybe someday we can have a custom texture for the hook... a man can dream
		PickupHook hook = new PickupHook(spawnLocation, pointingVector, hitBlock);

		// Players should only have one hook out at a time
		if (mPlayerHookMap.get(player.getUniqueId()) != null) {
			PickupHook oldHook = mPlayerHookMap.remove(player.getUniqueId());
			oldHook.remove();
		}
		mPlayerHookMap.put(player.getUniqueId(), hook);
		int maxCharges = ScoreboardUtils.getScoreboardValue(player, MAX_CHARGES_SCOREBOARD).orElse(1);

		NamedTextColor glowColor = switch (maxCharges - mPlayerShotsFiredMap.getOrDefault(player.getUniqueId(), 0)) {
			case 0 -> NamedTextColor.RED;
			case 1 -> NamedTextColor.GOLD;
			case 2 -> NamedTextColor.GREEN;
			case 3 -> NamedTextColor.BLUE;
			default -> NamedTextColor.LIGHT_PURPLE;
		};

		GlowingManager.startGlowing(hook.getDisplay(), glowColor, COOLDOWN, 0);

		Effect cooldownEffect = Plugin.getInstance().mEffectManager.getActiveEffect(player, ItemCooldown.toSource(EnchantmentType.GRAPPLING));
		int cooldownRemaining = cooldownEffect != null ? cooldownEffect.getDuration() : COOLDOWN;
		int hz = 20;
		new BukkitRunnable() {
			int mTicks = 0;
			boolean mRechargeImmediately = false;

			@Override
			public void run() {
				if (!hook.mPulledOnce) {
					new PPLine(Particle.CRIT, player.getLocation(), hook.getLocation()).shift((20 - (mTicks % 20)) / 20.0).countPerMeter(1).delta(0.05).extra(0.075).spawnAsPlayerActive(player);
				}
				if (mTicks >= cooldownRemaining * hz / 20) {
					mRechargeImmediately = true;
					this.cancel();
				}
				if (!hook.isValid()) {
					this.cancel();
				}
				if (hook.getLocation().distanceSquared(player.getLocation()) < PICKUP_RANGE_SQUARED) {
					if (player.isSneaking()) {
						Plugin.getInstance().mEffectManager.clearEffects(player, ItemCooldown.toSource(EnchantmentType.GRAPPLING));
						player.setCooldown(COOLDOWN_ITEM, 0);
						doPickupAnimation(player);
						mRechargeImmediately = true;
						this.cancel();
					}
				}
				if (hook.getLocation().distanceSquared(player.getLocation()) > level * level) {
					doFailEffects(player, hook.getLocation());
					this.cancel();
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				if (mPlayerHookMap.get(player.getUniqueId()) == hook) {
					mPlayerHookMap.remove(player.getUniqueId());
				}
				hook.remove(mRechargeImmediately ? 0 : COOLDOWN - mTicks, player);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 20 / hz);
	}

	private static void handleMob(Player player, double level, ProjectileHitEvent event, Projectile proj, Mob mob) {
		double distance = mob.getLocation().distance(player.getLocation());
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> decrementShotsFired(player), COOLDOWN);
		if (distance > level) {
			doFailEffects(player, mob.getLocation());
			return;
		}
		// Cancel the knockback from the bow shot
		event.setCancelled(true);
		proj.remove();

		doSucceedEffects(player, mob.getLocation(), Material.REDSTONE_BLOCK);
		if (player.isSneaking()) {
			pullMob(player, mob, MOB_HORIZONTAL_SPEED, level);
		} else {
			pullMob(mob, player, PLAYER_HORIZONTAL_SPEED, level);
		}
	}

	public static void pullMob(LivingEntity stationary, LivingEntity mover, double speed, double level) {
		if (EntityUtils.isBoss(mover) || EntityUtils.isCCImmuneMob(mover)) {
			return;
		}
		mover.setVelocity(calcVelocity(stationary.getEyeLocation(), mover.getLocation(), speed, level));
	}

	/**
	 * Calculates a velocity vector so that the target will land there after exactly one second.
	 *
	 * @param landingZone Location for target to land
	 * @param target      Entity being launched
	 * @return Velocity
	 */
	private static Vector calcVelocity(Location landingZone, Entity target, double level) {
		return calcVelocity(landingZone, target.getLocation(), MOB_HORIZONTAL_SPEED, level);
	}

	private static Vector calcVelocity(Location landingZone, Location estimatedLocationAfterLag, double flightSpeed, double level) {
		Vector distVector = landingZone.subtract(estimatedLocationAfterLag).toVector();

		double yDist = distVector.getY();
		// from wiki: displacement d(N) = y0 + 50(v0 + 3.92) * (1 - 0.98^N) - 3.92N where N is number of ticks, y0 is starting height, v0 is starting velocity

		// Approximation for velocity needed to get specific height
		double v0;
		if (yDist <= 0) {
			v0 = -Math.pow(-yDist / 5.93, 0.578) + 0.8;
		} else {
			v0 = Math.pow(yDist / 5.93, 0.578);
		}

		distVector.setY(0);
		if (distVector.lengthSquared() > MAX_HORIZONTAL_PER_LEVEL * MAX_HORIZONTAL_PER_LEVEL * level * level) {
			distVector.normalize();
			distVector.multiply(MAX_HORIZONTAL_PER_LEVEL * level);
		}
		distVector.multiply(flightSpeed);
		distVector.setY(v0);

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

	private static synchronized void decrementShotsFired(Player player) {
		int shotsFired = mPlayerShotsFiredMap.getOrDefault(player.getUniqueId(), 0);
		if (shotsFired > 0) {
			Plugin.getInstance().mEffectManager.clearEffects(player, ItemCooldown.toSource(EnchantmentType.GRAPPLING));
			player.setCooldown(COOLDOWN_ITEM, 0);
			if (shotsFired == 1) {
				// Remove players who have 0 shots fired after decrementing
				mPlayerShotsFiredMap.remove(player.getUniqueId());
			} else {
				mPlayerShotsFiredMap.put(player.getUniqueId(), shotsFired - 1);
			}
			player.sendActionBar(Component.text("Grappling Charges: "
					+ (ScoreboardUtils.getScoreboardValue(player, MAX_CHARGES_SCOREBOARD).orElse(1) - shotsFired + 1),
				NamedTextColor.YELLOW));
		}
	}

	private static class PickupHook {
		private final ArmorStand mCarrier;
		private final ItemDisplay mDisplay;
		private final Block mHitBlock;
		private boolean mPulledOnce = false;

		public PickupHook(Location spawnLocation, Vector pointingVector, Block hitBlock) {
			mCarrier = spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, carrier -> {
				carrier.setVisible(false);
				carrier.setGravity(false);
				carrier.setSmall(true);
				carrier.setBasePlate(false);
				carrier.setCollidable(false);
			});
			mDisplay = spawnLocation.getWorld().spawn(spawnLocation, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.ARROW));
				DisplayEntityUtils.rotateToPointAtLoc(display, pointingVector.normalize().setY(-pointingVector.getY()), 0, -Math.PI / 4);
				Transformation comicallyLarge = display.getTransformation();
				display.setTransformation(new Transformation(comicallyLarge.getTranslation(),
					comicallyLarge.getLeftRotation(),
					comicallyLarge.getScale().mul(0.5f),
					comicallyLarge.getRightRotation()));
			});
			mHitBlock = hitBlock;
		}

		public void remove() {
			mCarrier.remove();
			mDisplay.remove();
		}

		public void remove(int rechargeDelay, Player player) {
			this.remove();
			if (rechargeDelay == 0) {
				decrementShotsFired(player);
			} else {
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> decrementShotsFired(player), rechargeDelay);
			}
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

		public Block getHitBlock() {
			return mHitBlock;
		}
	}
}
