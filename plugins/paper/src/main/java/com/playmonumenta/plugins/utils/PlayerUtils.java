package com.playmonumenta.plugins.utils;

import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSetSlotHandle;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.depths.abilities.curses.CurseOfDependency;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.effects.hexfall.Reincarnation;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackHealingDone;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.player.activity.ActivityManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.managers.RespawningStructure;
import io.papermc.paper.entity.TeleportFlag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class PlayerUtils {
	public static void callAbilityCastEvent(Player player, Ability ability, ClassAbility spell) {
		AbilityCastEvent event = new AbilityCastEvent(player, ability, spell);
		Bukkit.getPluginManager().callEvent(event);
	}

	public static void awardStrike(Plugin plugin, Player player, String reason) {
		int strikes = ScoreboardUtils.getScoreboardValue(player, "Strikes").orElse(0);
		strikes++;
		ScoreboardUtils.setScoreboardValue(player, "Strikes", strikes);

		Location loc = player.getLocation();
		String oobLoc = "[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";

		player.sendMessage(Component.text("WARNING: " + reason, NamedTextColor.RED));
		player.sendMessage(Component.text("Location: " + oobLoc, NamedTextColor.RED));
		player.sendMessage(Component.text("This is an automated message generated by breaking a game rule.", NamedTextColor.YELLOW));
		player.sendMessage(Component.text("You have been teleported to spawn and given slowness for 5 minutes.", NamedTextColor.YELLOW));
		player.sendMessage(Component.text("There is no further punishment, but please do follow the rules.", NamedTextColor.YELLOW));

		plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
			new PotionEffect(PotionEffectType.SLOW, 5 * 20 * 60, 3, false, true));

		player.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
	}

	public static boolean playerCountsForLootScaling(Player player) {
		return player.getGameMode() != GameMode.SPECTATOR
			&& (player.getGameMode() != GameMode.CREATIVE || !Plugin.IS_PLAY_SERVER)
			&& ActivityManager.getManager().isActive(player)
			&& !Shattered.hasMaxShatteredItemEquipped(player);
	}

	public static List<Player> playersInLootScalingRange(Location loc) {
		// In dungeons, all players in the same world (i.e. the entire dungeon) are in range
		boolean isDungeon = ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) > 0;
		if (isDungeon) {
			return loc.getWorld().getPlayers().stream()
				.filter(PlayerUtils::playerCountsForLootScaling)
				.toList();
		}

		// In a POI, all players within the same POI are in range
		StructuresPlugin structuresPlugin = StructuresPlugin.getInstance();
		if (structuresPlugin.mRespawnManager != null) {
			List<RespawningStructure> structures = structuresPlugin.mRespawnManager.getStructures(loc.toVector(), false)
				.stream().filter(structure -> structure.isWithin(loc)).toList();
			if (!structures.isEmpty()) {
				return loc.getWorld().getPlayers().stream()
					.filter(p -> playerCountsForLootScaling(p) && playerIsInPOI(structures, p))
					.toList();
			}
		}

		// Otherwise, perform no loot scaling
		return Collections.emptyList();
	}

	public static List<Player> playersInLootScalingRange(Player player, boolean excludeTarget) {
		List<Player> players = new ArrayList<>(playersInLootScalingRange(player.getLocation()));
		if (excludeTarget) {
			players.remove(player);
		} else if (!players.contains(player)) { // add player if it doesn't exist and excludeTarget is false
			players.add(player);
		}
		return players;
	}

	public static boolean playerIsInPOI(List<RespawningStructure> structures, Player player) {
		return structures.stream().anyMatch(structure -> structure.isWithin(player));
	}

	public static boolean playerIsInPOI(Location loc, Player player) {
		StructuresPlugin structuresPlugin = StructuresPlugin.getInstance();

		if (structuresPlugin.mRespawnManager != null) {
			return playerIsInPOI(structuresPlugin.mRespawnManager.getStructures(loc.toVector(), true), player);
		} else {
			return false;
		}
	}

	public static boolean playerIsInPOI(Player player) {
		return playerIsInPOI(player.getLocation(), player);
	}


	/**
	 * Given a list of players, a location, and settings, returns the players that are close enough to <code>range</code>.
	 *
	 * @param ps                   Players we will check
	 * @param loc                  The location to check
	 * @param range                The valid range
	 * @param includeNonTargetable Whether to include non-targetable players
	 * @param includeDead          Whether to include players that are currently dead
	 * @return The players that fit the criteria
	 */
	public static List<Player> playersInRange(Iterable<Player> ps, Location loc, double range, boolean includeNonTargetable, boolean includeDead) {
		List<Player> players = new ArrayList<>();

		double rangeSquared = range * range;
		for (Player player : ps) {
			if (player.getLocation().distanceSquared(loc) < rangeSquared
				&& player.getGameMode() != GameMode.SPECTATOR
				&& (includeNonTargetable || !AbilityUtils.isStealthed(player))
				&& (includeDead || !Plugin.getInstance().mEffectManager.hasEffect(player, RespawnStasis.class))) {
				players.add(player);
			}
		}

		return players;
	}

	public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable, boolean includeDead) {
		return playersInRange(loc.getWorld().getPlayers(), loc, range, includeNonTargetable, includeDead);
	}

	public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable) {
		return playersInRange(loc, range, includeNonTargetable, false);
	}

	public static List<Player> playersInXZRange(Location loc, double range, boolean includeNonTargetable, boolean includeDead) {
		List<Player> players = new ArrayList<>();

		for (Player player : loc.getWorld().getPlayers()) {
			if (LocationUtils.xzDistance(player.getLocation(), loc) <= range
				&& player.getGameMode() != GameMode.SPECTATOR
				&& (includeNonTargetable || !AbilityUtils.isStealthed(player))
				&& (includeDead || !Plugin.getInstance().mEffectManager.hasEffect(player, RespawnStasis.class))) {
				players.add(player);
			}
		}

		return players;
	}

	public static List<Player> playersInXZRange(Location loc, double range, boolean includeNonTargetable) {
		return playersInXZRange(loc, range, includeNonTargetable, false);
	}

	public static List<Player> otherPlayersInRange(Player player, double radius, boolean includeNonTargetable) {
		List<Player> players = playersInRange(player.getLocation(), radius, includeNonTargetable);
		players.remove(player);
		return players;
	}

	public static boolean isCursed(Plugin plugin, Player p) {
		return plugin.mEffectManager.hasEffect(p, Lich.curseSource);
	}

	public static void removeCursed(Plugin plugin, Player p) {
		setCursedTicks(plugin, p, 0);
		p.removePotionEffect(PotionEffectType.BAD_OMEN);
		p.removePotionEffect(PotionEffectType.UNLUCK);
	}

	public static void setCursedTicks(Plugin plugin, Player p, int ticks) {
		NavigableSet<Effect> cursed = plugin.mEffectManager.getEffects(p, Lich.curseSource);
		if (cursed != null) {
			for (Effect curse : cursed) {
				curse.setDuration(ticks);
			}
		}
	}

	public static double healPlayer(Plugin plugin, Player player, double healAmount) {
		return healPlayer(plugin, player, healAmount, null);
	}

	// Returns the change in player's health
	public static double healPlayer(Plugin plugin, Player player, double healAmount, @Nullable Player sourcePlayer) {
		if (healAmount <= 0 || player.isDead()) {
			return 0;
		}

		boolean sourceIsNotTarget = sourcePlayer != null && player != sourcePlayer;
		if (sourceIsNotTarget) {
			double healBonus = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TRIAGE) * 0.05;
			healAmount *= 1 + healBonus;
		}

		EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, healAmount, EntityRegainHealthEvent.RegainReason.CUSTOM);
		if (sourceIsNotTarget) {
			CurseOfDependency.OTHER_PLAYER_EVENT = event;
		}
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			double oldHealth = player.getHealth();
			double newHealth = Math.min(oldHealth + event.getAmount(), EntityUtils.getMaxHealth(player));
			player.setHealth(newHealth);

			// Add to activity
			if (sourcePlayer != null && player != sourcePlayer && ActivityManager.getManager().isActive(player)) {
				ActivityManager.getManager().addHealingDealt(sourcePlayer, healAmount);
			}
			double amountHealed = newHealth - oldHealth;
			if (sourcePlayer != null) {
				StatTrackHealingDone.healingDone(sourcePlayer, amountHealed);
			}
			return amountHealed;
		}

		return 0;
	}


	public static Location getRightSide(Location location, double distance) {
		double angle = location.getYaw() / 57.296;
		return location.clone().subtract(new Vector(FastUtils.cos(angle), 0, FastUtils.sin(angle)).normalize().multiply(distance));
	}

	/* Audience of nearby players for sending messages */
	public static Audience nearbyPlayersAudience(Location loc, int radius) {
		return Audience.audience(loc.getNearbyPlayers(radius));
	}

	public static Audience nearbyOtherPlayersAudience(Player player, int radius) {
		return Audience.audience(otherPlayersInRange(player, radius, true));
	}

	/* Command should use @s for targeting selector */
	public static void executeCommandOnNearbyPlayers(Location loc, int radius, String command) {
		for (Player player : loc.getNearbyPlayers(radius)) {
			// getNearbyPlayers returns players in a cube, not a sphere, so we need this additional check
			if (loc.distanceSquared(player.getLocation()) > radius * radius) {
				continue;
			}
			executeCommandOnPlayer(player, command);
		}
	}

	public static void executeCommandOnPlayer(Player player, String command) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute as " + player.getUniqueId() + " at @s run " + command);
	}

	// How far back the player drew their bow,
	// vs what its max launch speed would be.
	// Launch velocity used to calculate is specifically for PLAYERS shooting BOWS!
	// Returns from 0.0 to 1.0, with 1.0 being full draw
	public static double calculateBowDraw(AbstractArrow arrowlike) {
		double currentSpeed = arrowlike.getVelocity().length();
		double maxLaunchSpeed = Constants.PLAYER_BOW_INITIAL_SPEED;

		return Math.min(
			1,
			currentSpeed / maxLaunchSpeed
		);
	}

	/*
	 * Whether the player meets the conditions for a crit,
	 * emulating the vanilla check in full.
	 *
	 * Ie, no critting while sprinting.
	 */
	public static boolean isCriticalAttack(Player player) {
		// NMS EntityHuman:
		// float f = (float)this.b((AttributeBase)GenericAttributes.ATTACK_DAMAGE);
		//     f1 = EnchantmentManager.a(this.getItemInMainHand(), ((EntityLiving)entity).getMonsterType());
		// float f2 = this.getAttackCooldown(0.5F);
		// f *= 0.2F + f2 * f2 * 0.8F;
		// f1 *= f2;
		// if (f > 0.0F || f1 > 0.0F) {
		//     boolean flag = f2 > 0.9F;
		//     boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround && !this.isClimbing() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && entity instanceof EntityLiving;
		//     flag2 = flag2 && !this.isSprinting();
		return (
			isFallingAttack(player)
				&& !player.isInWater()
				&& !player.isSprinting()
		);
	}

	/*
	 * Whether the player meets the conditions for a crit,
	 * emulating the vanilla check,
	 * except the not in water and not sprinting requirements.
	 *
	 * This is used because MM has historically had a non-exact crit check,
	 * that allowed things like crit-triggered abilities to trigger off non-crit
	 * melee damage while sprinting.
	 */
	public static boolean isFallingAttack(Player player) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
				&& player.getFallDistance() > 0
				&& isFreeFalling(player)
				&& !player.hasPotionEffect(PotionEffectType.BLINDNESS)
				&& !player.isInsideVehicle()
			//TODO pass in the Entity in question to check if LivingEntity
		);
	}

	/*
	 * Whether the player is considered to be freely falling in air or liquid.
	 * They cannot be on the ground or climbing.
	 */
	public static boolean isFreeFalling(Player player) {
		if (!isOnGround(player) && player.getLocation().isChunkLoaded()) {
			return !BlockUtils.isClimbable(player.getLocation().getBlock());
		}

		return false;
	}

	public static boolean hasLineOfSight(Player player, Block block) {
		Location fromLocation = player.getEyeLocation();
		Location toLocation = block.getLocation();
		int range = (int) fromLocation.distance(toLocation) + 1;
		Vector direction = toLocation.toVector().subtract(fromLocation.toVector()).normalize();

		try {
			BlockIterator bi = new BlockIterator(fromLocation.getWorld(), fromLocation.toVector(), direction, 0, range);

			while (bi.hasNext()) {
				Block b = bi.next();

				// If block is occluding (shouldn't include transparent blocks, liquids etc.),
				// line of sight is broken, return false
				if (BlockUtils.isLosBlockingBlock(b.getType()) && b != block) {
					return false;
				}
			}
		} catch (IllegalStateException e) {
			// Thrown sometimes when chunks aren't loaded at exactly the right time
			return false;
		}

		return true;
	}

	/*
	 * Whether the player meets the conditions for a sweeping attack,
	 * emulating the vanilla check, except the on ground,
	 * movement increment limit, sword, and proximity requirements.
	 */
	public static boolean isNonFallingAttack(
		Player player,
		Entity enemy
	) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
				&& !isCriticalAttack(player)
				&& !player.isSprinting()
			// Last check on horizontal movement increment requires an internal
			// vanilla collision adjustment Vec3D.
			// It is not simply player.getVelocity(); that is used elsewhere
		);
	}

	/*
	 * Whether the player meets the conditions for a sweeping attack,
	 * emulating the vanilla check, except the movement increment limit, sword,
	 * and proximity requirements.
	 */
	public static boolean isSweepingAttack(
		Player player,
		Entity enemy
	) {
		// NMS Entity:
		// this.z = this.A;
		// this.A = (float)((double)this.A + (double)MathHelper.sqrt(c(vec3d1)) * 0.6D);
		// public static double c(Vec3D vec3d) {
		//     return vec3d.x * vec3d.x + vec3d.z * vec3d.z;
		// }
		//
		// NMS EntityHuman:
		// if (this.isSprinting() && flag) {
		//     flag1 = true;
		// }
		// double d0 = (double)(this.A - this.z);
		// if (flag && !flag2 && !flag1 && this.onGround && d0 < (double)this.dN()) {
		//     ItemStack itemStack = this.b((EnumHand)EnumHand.MAIN_HAND);
		//     if (itemStack.getItem() instanceof ItemSword) {
		//     List<EntityLiving> list = this.world.a(EntityLiving.class, entity.getBoundingBox().grow(1.0D, 0.25D, 1.0D));
		return (
			isNonFallingAttack(player, enemy)
				&& isOnGround(player)
		);
	}

	public static boolean checkPlayer(Player player) {
		return player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR;
	}

	/*
	 * Returns players within a bounding box of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInBox(
		Location boxCenter,
		double totalWidth,
		double totalHeight
	) {
		return boxCenter.getNearbyPlayers(
			totalWidth / 2,
			totalHeight / 2,
			PlayerUtils::checkPlayer
		);
	}

	/*
	 * Returns players within a cube of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInCube(
		Location cubeCenter,
		double sideLength
	) {
		return playersInBox(cubeCenter, sideLength, sideLength);
	}

	/*
	 * Returns players within a sphere of the specified dimensions.
	 *
	 * Measures based on feet location.
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInSphere(
		Location sphereCenter,
		double radius
	) {
		Collection<Player> spherePlayers = playersInCube(sphereCenter, radius * 2);
		double radiusSquared = radius * radius;
		spherePlayers.removeIf((Player player) -> sphereCenter.distanceSquared(player.getLocation()) > radiusSquared);

		return spherePlayers;
	}

	/*
	 * Returns players within an upright cylinder of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInCylinder(
		Location cylinderCenter,
		double radius,
		double totalHeight
	) {
		Collection<Player> cylinderPlayers = playersInBox(cylinderCenter, radius * 2, totalHeight);
		double centerY = cylinderCenter.getY();
		cylinderPlayers.removeIf((Player player) -> {
			Location flattenedLocation = player.getLocation();
			flattenedLocation.setY(centerY);
			return cylinderCenter.distanceSquared(flattenedLocation) > radius * radius;
		});

		return cylinderPlayers;
	}

	public static boolean isMage(Player player) {
		return AbilityUtils.getClassNum(player) == Mage.CLASS_ID;
	}

	public static boolean isWarrior(Player player) {
		return AbilityUtils.getClassNum(player) == Warrior.CLASS_ID;
	}

	public static boolean isCleric(Player player) {
		return AbilityUtils.getClassNum(player) == Cleric.CLASS_ID;
	}

	public static boolean isRogue(Player player) {
		return AbilityUtils.getClassNum(player) == Rogue.CLASS_ID;
	}

	public static boolean isAlchemist(Player player) {
		return AbilityUtils.getClassNum(player) == Alchemist.CLASS_ID;
	}

	public static boolean isScout(Player player) {
		return AbilityUtils.getClassNum(player) == Scout.CLASS_ID;
	}

	public static boolean isWarlock(Player player) {
		return AbilityUtils.getClassNum(player) == Warlock.CLASS_ID;
	}

	public static void resetAttackCooldown(Player player) {
		NmsUtils.getVersionAdapter().setAttackCooldown(player, 0);
	}

	public static double getJumpHeight(Player player) {
		PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP);
		double jumpLevel = (jump == null ? -1 : jump.getAmplifier());
		return jumpLevel < 0 ? 1.2523 : 0.0308354 * jumpLevel * jumpLevel + 0.744631 * jumpLevel + 1.836131; // Quadratic function taken from mc wiki - thanks mojank!
	}

	public static void cancelGearSpeed(Player player) {
		double speed = EntityUtils.getAttributeOrDefault(player, Attribute.GENERIC_MOVEMENT_SPEED, 0.1);
		// Sprinting adds 30% speed, and we don't want to have that be factored in the penalty.
		if (player.isSprinting()) {
			speed /= 1.3;
		}
		double multiplier = 1 / (speed / 0.1) - 1;
		EntityUtils.addAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED,
			new AttributeModifier(Constants.ANTI_SPEED_MODIFIER, multiplier, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
		);
	}

	/**
	 * Just to quarantine the deprecation warnings tbh
	 */
	public static boolean isOnGround(Player player) {
		return player.isOnGround();
	}

	public static boolean isOnGroundOrMountIsOnGround(Player player) {
		if (isOnGround(player)) {
			return true;
		}
		Entity base = EntityUtils.getEntityStackBase(player);
		return base.isOnGround();
	}

	/**
	 * Same ^
	 *
	 * @param shoulder True for right shoulder, false for left shoulder
	 */
	public static Entity getPlayerShoulderEntity(Player player, boolean shoulder) {
		return shoulder ? player.getShoulderEntityRight() : player.getShoulderEntityLeft();
	}

	/**
	 * Same ^
	 *
	 * @param shoulder True for right shoulder, false for left shoulder
	 */
	public static void setPlayerShoulderEntity(Player player, @Nullable Entity shoulderEntity, boolean shoulder) {
		if (shoulder) {
			player.setShoulderEntityRight(shoulderEntity);
		} else {
			player.setShoulderEntityLeft(shoulderEntity);
		}
	}

	public static boolean canRiptide(Player player) {
		return canRiptide(player, player.getInventory().getItemInMainHand());
	}

	public static boolean canRiptide(Player player, ItemStack mainhand) {
		return (LocationUtils.isLocationInWater(player.getLocation()) || player.isInRain()) && ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.RIPTIDE);
	}

	// Can be called sync or async
	public static List<String> sortedPlayerNamesFromUuids(Collection<UUID> playerUuids) {
		Set<String> playerNames = new HashSet<>();

		for (UUID playerUuid : playerUuids) {
			String playerName = MonumentaRedisSyncIntegration.cachedUuidToName(playerUuid);
			playerNames.add(playerName != null ? playerName : playerUuid.toString());
		}

		return StringUtils.sortedStrings(playerNames);
	}

	public static List<String> sortedPlayerNames(Collection<Player> players) {
		return StringUtils.sortedStrings(players.stream().map(Player::getName).toList());
	}

	public static void playerTeleport(Player player, Location location) {
		player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
	}

	public static boolean hasUnlockedIsles(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Quest101").orElse(0) >= 12;
	}

	public static boolean hasUnlockedRing(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "R3Access").orElse(0) >= 1;
	}

	public static Location getRespawnLocationAndClear(Player player, World world, @Nullable Location originalRespawnLocation) {
		Location realRespawnLocation = player.getPotentialBedLocation();
		boolean mightBeBedSpawn = false;
		if (realRespawnLocation == null || realRespawnLocation.getWorld() != world) {
			realRespawnLocation = world.getSpawnLocation();
		} else {
			mightBeBedSpawn = realRespawnLocation.getBlock().getBlockData() instanceof Bed;
			if (mightBeBedSpawn) {
				// Add some y value to not break the bed on respawn (and respawn on top of it instead),
				// except if the block above is unbreakable.
				if (realRespawnLocation.clone().add(0, 1, 0).getBlock().getType().getHardness() >= 0) {
					realRespawnLocation.add(0, 0.6, 0);
				}
			}
			if (originalRespawnLocation != null) {
				realRespawnLocation.setPitch(originalRespawnLocation.getPitch());
				realRespawnLocation.setYaw(originalRespawnLocation.getYaw());
			}
		}
		// spawn locations are stored as ints, need to add (0.5, 0, 0.5) to get the center of the block
		realRespawnLocation.add(0.5, 0, 0.5);

		// If vanilla moved the respawn location, move it back to the real location, as long as that location is in a survival zone,
		// and break blocks around the respawn location as if broken with an iron pickaxe.
		// Only breaks block on play for non-creative/spectator players.
		// (does not check for whether the player is in adventure mode, as a player's game mode is not yet updated)
		int distanceToCheck = mightBeBedSpawn ? 3 : 1; // bed spawn point may be up to 2 blocks away from the bed
		boolean useRealLocation = originalRespawnLocation == null || (Math.abs(originalRespawnLocation.getX() - realRespawnLocation.getX()) < distanceToCheck && Math.abs(originalRespawnLocation.getZ() - realRespawnLocation.getZ()) < distanceToCheck);
		if (useRealLocation && ZoneUtils.isMineable(realRespawnLocation)) {
			if (Plugin.IS_PLAY_SERVER
				    && player.getGameMode() != GameMode.CREATIVE
				    && player.getGameMode() != GameMode.SPECTATOR) {
				boolean couldNotBreakBlock = false;
				BoundingBox playerBox = BoundingBox.of(realRespawnLocation.clone().add(-0.3, 0, -0.3), realRespawnLocation.clone().add(0.3, 1.8, 0.3));
				ItemStack ironPick = new ItemStack(Material.IRON_PICKAXE);
				for (Block collidingBlock : NmsUtils.getVersionAdapter().getCollidingBlocks(world, playerBox, true)) {
					if (ZoneUtils.isMineable(collidingBlock.getLocation())
						    && collidingBlock.getType().getHardness() >= 0) {
						collidingBlock.breakNaturally(ironPick);
					} else {
						couldNotBreakBlock = true;
					}
				}
				if (couldNotBreakBlock) {
					// Warn about respawning inside solid blocks, but check if the player can crawl in the space first because that won't cause issues
					BoundingBox crawlingBox = BoundingBox.of(realRespawnLocation.clone().add(-0.3, 0, -0.3), realRespawnLocation.clone().add(0.3, 0.6, 0.3));
					if (NmsUtils.getVersionAdapter().hasCollisionWithBlocks(player.getWorld(), crawlingBox, true)) {
						AuditListener.log("Player " + player.getName() + " respawned inside unbreakable blocks at " + realRespawnLocation + "!");
					}
				}
			}
			return realRespawnLocation;
		}
		return originalRespawnLocation != null ? originalRespawnLocation : world.getSpawnLocation();
	}

	/**
	 * Kills the player regardless of stasis or invulnerability
	 * @param player the player to kill
	 * @param damager the entity responsible for the kill
	 * @param cause the cause of the kill
	 */
	public static void killPlayer(Player player, @Nullable LivingEntity damager, @Nullable String cause) {
		killPlayer(player, damager, cause, true, true, false);
	}

	/**
	 * Kills the player
	 * @param player the player to kill
	 * @param damager the entity responsible for the kill
	 * @param cause the cause of the kill
	 * @param bypassStasis whether the kill should bypass the stasis effect
	 * @param bypassInvuln whether the kill should bypass the player's invulnerability status
	 * @param bypassReincarn whether the kill should bypass the player's reincarnation effect
	 */
	public static void killPlayer(Player player, @Nullable LivingEntity damager, @Nullable String cause, boolean bypassStasis, boolean bypassInvuln, boolean bypassReincarn) {
		if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
			Plugin monumentaPlugin = Plugin.getInstance();
			monumentaPlugin.mEffectManager.clearEffects(player, VoodooBonds.PROTECTION_EFFECT);
			if (bypassStasis) {
				monumentaPlugin.mEffectManager.clearEffects(player, Stasis.GENERIC_NAME);
			}
			if (bypassInvuln && player.isInvulnerable()) {
				player.setInvulnerable(false);
			}
			if (bypassReincarn) {
				monumentaPlugin.mEffectManager.clearEffects(player, Reincarnation.GENERIC_NAME);
			}
			DamageUtils.damage(damager, player, DamageEvent.DamageType.TRUE, 1000000, null, true, false, cause);
		}
	}

	/**
	 * Forces an update of specific items in the player's inventory. Similar to {@link Player#updateInventory()}, but offers better performance when only a few items should be updated.
	 */
	public static void resendItems(Player player, EquipmentSlot... slots) {
		for (EquipmentSlot slot : slots) {
			int packetSlotId = switch (slot) {
				case HAND, OFF_HAND -> 45;
				case FEET -> 8;
				case LEGS -> 7;
				case CHEST -> 6;
				case HEAD -> 5;
			};
			PacketPlayOutSetSlotHandle packet = PacketPlayOutSetSlotHandle.createNew(0, packetSlotId, ItemUtils.clone(player.getInventory().getItem(slot)));
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, PacketContainer.fromPacket(packet.getRaw()), true);
		}
	}

}
