package com.playmonumenta.plugins.delves.abilities;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.delves.DelvesManager.getRank;

public class Haunted {

	public static final String DESCRIPTION = "Your regrets haunt you.";

	public static Component[] rankDescription(int level) {
		if (level == 1) {
			return new Component[]{
				Component.text("A looming figure haunts you relentlessly,"),
				Component.text("only moving when you do.")
			};
		} else if (level == 2) {
			return new Component[]{
				Component.text("Your looming consequence only moves when you are not looking at it."),
				Component.text("Letting it get too close will send you into paranoia.")
			};
		}
		return new Component[0]; // Return empty array as fallback for other levels
	}

	public static final double MAX_SPEED = 0.5;
	public static final double LEVEL_2_SPEED_MULTIPLIER = 0.2;
	public static final double DAMAGE = 0.4; //percentage
	public static final double RANGE = 50;
	private static final double RANGE_SQUARED = RANGE * RANGE;
	private static final double RANGE_SQUARED_HALF = RANGE_SQUARED / 2;
	public static final double RANGE_SOUND = 16;
	private static final double RANGE_SOUND_SQUARED = RANGE_SOUND * RANGE_SOUND;
	public static final double RANGE_PARANOID = 3;
	private static final double RANGE_PARANOID_SQUARED = RANGE_PARANOID * RANGE_PARANOID;
	public static final double RANGE_HIT = 1;
	private static final double RANGE_HIT_SQUARED = RANGE_HIT * RANGE_HIT;
	public static final double VERTICAL_SPEED_DEBUFF = 3; // This makes Looming Consequence move slower vertically
	public static final double PLAYER_VERTICAL_CHANGE_DEBUFF = 2; // This makes the player's vertical movement factor less into Looming Consequence movement

	private static void followPlayer(Player p, ArmorStand armorStand, int level) {
		Vector playerYDivider = new Vector(1, PLAYER_VERTICAL_CHANGE_DEBUFF, 1);
		new BukkitRunnable() {
			Location mPLoc = p.getLocation();
			double mRadian = 0;
			double mSpeed = MAX_SPEED;

			int mHitTimer = 0;
			int mRangeCD = 0;

			@Override
			public void run() {
				if (!p.isOnline()) {
					this.cancel();
					armorStand.getEquipment().clear();
					armorStand.setGlowing(false);
					return;
				} else if (!armorStand.isValid()) {
					armorStand.getEquipment().clear();
					armorStand.setGlowing(false);
					applyModifiers(p);
					this.cancel();
					return;
				} else if (p.isDead()) {
					return;
				}

				if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
					return;
				}

				Location sLoc = armorStand.getLocation();
				double distanceSquared = armorStand.getLocation().distanceSquared(p.getLocation());

				if (distanceSquared > RANGE_SQUARED) {
					armorStand.remove();
					this.cancel();
					summonBeast(p, p.getLocation().add(p.getLocation().getDirection().multiply(-10)));
					return;
				} else if (level == 2 && distanceSquared < RANGE_PARANOID_SQUARED) {
					getParanoid(p, armorStand);
					this.cancel();
					return;
				} else if (distanceSquared > RANGE_SQUARED_HALF) {
					mSpeed = MAX_SPEED * 2;
				} else {
					mSpeed = MAX_SPEED;
				}

				if (EntityUtils.isInWater(p)) {
					mSpeed *= 0.8;
				}


				// Bobbing motion for visual effect
				Vector direction = LocationUtils.getDirectionTo(p.getLocation(), sLoc);
				sLoc.setDirection(direction);
				armorStand.setHeadPose(new EulerAngle(Math.toRadians(sLoc.getPitch()), 0, 0));
				armorStand.teleport(sLoc.add(0, (FastMath.sin(mRadian) * 0.35) / 10, 0));
				mRadian += Math.PI / 20D;

				// Hit detection
				if (level == 1 && mHitTimer <= 0 && distanceSquared < RANGE_HIT_SQUARED) {
					p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 1f, 2f);
					p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1f, 0.5f);

					Location loc = p.getLocation().add(0, 1, 0);
					BossUtils.bossDamagePercent(armorStand, p, DAMAGE);
					if (p.isDead()) {
						p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
						p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1f, 0.65f);

						new PartialParticle(Particle.SOUL, loc, 70, 0, 0, 0, 0.15).spawnAsPlayerActive(p);
						new PartialParticle(Particle.SMOKE_LARGE, loc, 40, 0, 0, 0, 0.185).spawnAsPlayerActive(p);
					} else {
						new PartialParticle(Particle.SMOKE_LARGE, loc, 15, 0, 0, 0, 0.125).spawnAsPlayerActive(p);
					}
					mHitTimer = 10;
				}
				if (mHitTimer > 0) {
					mHitTimer--;
				}

				// Visuals
				new PartialParticle(Particle.SMOKE_LARGE, armorStand.getLocation().add(0, 1, 0), 1, 0.3, 0.4, 0.3, 0).spawnAsEntityActive(armorStand);
				new PartialParticle(Particle.SOUL, armorStand.getLocation().add(0, 1, 0), 1, 0.3, 0.4, 0.3, 0.025).spawnAsEntityActive(armorStand);

				// Sounds
				if (distanceSquared <= RANGE_SOUND_SQUARED && mRangeCD <= 0) {
					p.playSound(armorStand.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.HOSTILE, 1.75f, 0f);
					mRangeCD = 70;
				}
				if (mRangeCD > 0) {
					mRangeCD--;
				}

				// Check movement based on the level
				double pMovedTick = 1;
				if (level == 1) {
					// Level 1: Move if the player is moving
					boolean playerIsMoving = mPLoc.distanceSquared(p.getLocation()) > 0.01; // Using distance squared for performance
					if (!playerIsMoving) {
						return;
					}
					pMovedTick = mPLoc.clone().subtract(p.getLocation()).toVector() // Delta between last location and current location
					.divide(playerYDivider).length(); // Decrease movement based on vertical player movement
					if (pMovedTick < 0.005) {
						return;
					} else if (pMovedTick > 1) {
						pMovedTick = 1;
					}
				} else if (level == 2) {
					// Level 2: Move if the player is not looking at it
					Location armorStandCenter = armorStand.getLocation().add(0, 1, 0);
					boolean playerHasLineOfSight = (p.getLocation().distanceSquared(armorStandCenter) <= 20 * 20 || p.hasLineOfSight(armorStandCenter)) && isInPlayerConeView(p, armorStandCenter, 65);
					if (playerHasLineOfSight) {
						return;
					}
					pMovedTick = LEVEL_2_SPEED_MULTIPLIER;
				}

				armorStand.teleport(sLoc.add(direction.clone().multiply(mSpeed * pMovedTick)));
				mPLoc = p.getLocation();
			}
		}.runTaskTimer(Plugin.getInstance(), 0L, 1L);
	}


	private static void summonBeast(Player player, Location loc) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		for (Entity nearbyEntity : player.getLocation().getNearbyEntities(100, 100, 100)) {
			if (nearbyEntity instanceof ArmorStand && nearbyEntity.getScoreboardTags().contains(phantomName + player.getUniqueId())) {
				return;
			}
		}
		ArmorStand armorStand = Objects.requireNonNull((ArmorStand) LibraryOfSoulsIntegration.summon(loc, "LoomingConsequence"));
		armorStand.addScoreboardTag(phantomName + player.getUniqueId());
		GlowingManager.startGlowing(armorStand, NamedTextColor.DARK_GRAY, -1, 0, p -> p == player, null);
		followPlayer(player, armorStand, getRank(player, DelvesModifier.HAUNTED));
	}

	public static void moveBackwards(Player player, int multiplier) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		for (Entity nearbyEntity : player.getLocation().getNearbyEntities(20, 20, 20)) {
			if (nearbyEntity instanceof ArmorStand && nearbyEntity.getScoreboardTags().contains(phantomName + player.getUniqueId())) {
				Vector direction = nearbyEntity.getLocation().getDirection();
				direction.multiply(-multiplier);
				nearbyEntity.teleport(nearbyEntity.getLocation().add(direction));
			}
		}
	}

	public static void applyModifiers(Player p) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		Location loc = p.getLocation().add(p.getLocation().getDirection().multiply(-10));
		ArmorStand armorStand = null;
		List<Entity> nearbyEntities = (List<Entity>) p.getWorld().getNearbyEntities(p.getLocation(), 100, 100, 100);
		for (Entity nearbyEntity : nearbyEntities) {
			if (nearbyEntity instanceof ArmorStand stand && nearbyEntity.getScoreboardTags().contains(phantomName + p.getUniqueId())) {

				// We found the old "hidden" armor stand, so now we use its location to spawn a fresh Shade, and remove the old one.
				Location standLoc = stand.getLocation();
				armorStand = Objects.requireNonNull((ArmorStand) LibraryOfSoulsIntegration.summon(standLoc, "LoomingConsequence"));
				armorStand.addScoreboardTag(phantomName + p.getUniqueId());

				stand.remove();
			}
		}
		if (armorStand == null) {
			BukkitScheduler scheduler = Bukkit.getScheduler();
			scheduler.runTaskLater(Plugin.getInstance(), () -> {
				summonBeast(p, loc);
			}, 100L);
		} else {
			followPlayer(p, armorStand, getRank(p, DelvesModifier.HAUNTED));
		}
	}

	public static boolean isInPlayerConeView(Player player, Location targetLocation, double fovLimit) {
		double angle = Math.acos(player.getEyeLocation().getDirection().normalize().dot(targetLocation.clone().subtract(player.getEyeLocation()).toVector().normalize()));
		return angle <= Math.toRadians(fovLimit);
	}

	private static Location getRandomLocationNearPlayer(Player player, int minXZDistance, int maxXZDistance, int maxYDistance) {
		Location dummy = new Location(player.getWorld(), 0, 1, 0);
		Location playerLocation = player.getLocation();
		Location randomLoc = null;

		for (int attempts = 0; attempts < 20; attempts++) {
			double randomX = playerLocation.getX() + (Math.random() * (maxXZDistance - minXZDistance) + minXZDistance) * (Math.random() > 0.5 ? 1 : -1);
			double randomZ = playerLocation.getZ() + (Math.random() * (maxXZDistance - minXZDistance) + minXZDistance) * (Math.random() > 0.5 ? 1 : -1);
			double randomY = playerLocation.getY() + (Math.random() * (2 * maxYDistance)) - maxYDistance;
			randomLoc = new Location(player.getWorld(), randomX, randomY, randomZ);

			if (player.hasLineOfSight(randomLoc)) {
				break;
			}
			randomLoc = null;
		}

		return randomLoc != null ? randomLoc : dummy;
	}


	private static void getParanoid(Player player, ArmorStand armorStand) {
		// put player in spooked mode
		String newPhantomName = "spooked";
		armorStand.remove();
		player.teleport(LocationUtils.fallToGround(player.getLocation(), player.getLocation().getY() - 4));
		player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 320, 10));
		Plugin.getInstance().mEffectManager.addEffect(player, "stasis", new Stasis(320));
		SkullMeta meta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);
		PlayerProfile headProfile = Bukkit.createProfile(UUID.randomUUID());
		headProfile.setProperty(new ProfileProperty("textures", "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjM2NDAxYjgxZWY5NTdhZjJkNzc2NjQ5MDNiMmQ1ZDBhZWQ0MTEzZGExNmU2YzYyOGNmZjE5Njc5M2Q1NjcyMSJ9fX0="));
		meta.setPlayerProfile(headProfile);

		new BukkitRunnable() {
			int mTicks = 0;
			List<Entity> mHiddenEntities = new ArrayList<>();
			@Override
			public void run() {
				if (mTicks == 1) {
					for (Entity entity : player.getNearbyEntities(35, 35, 35)) {
						player.hideEntity(Plugin.getInstance(), entity);
						mHiddenEntities.add(entity);
					}
				}
				// play the ghost sequence
				switch (mTicks) {
					case 37, 119, 189, 259:
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 62, 2));
						player.getWorld().playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.PLAYERS, 0.5f, 1f);
						new BukkitRunnable() {
							@Override
							public void run() {
								for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
									if (entity instanceof ArmorStand armorStand) {
										if (armorStand.getScoreboardTags().contains(newPhantomName + player.getUniqueId())) {
											DamageUtils.damage(armorStand, player, DamageEvent.DamageType.TRUE, Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue() * 0.4);
											armorStand.remove();
											getSpooked(player, meta);
										}
									}
								}
							}
						}.runTaskLater(Plugin.getInstance(), 12L);
						break;
					case 93, 163, 233:
						Location loc = getRandomLocationNearPlayer(player, 6, 11, 5);
						if (loc.getY() != 1) {
							ArmorStand spookArmorStand = Objects.requireNonNull((ArmorStand) LibraryOfSoulsIntegration.summon(loc, "LoomingConsequence"));
							spookArmorStand.addScoreboardTag(newPhantomName + player.getUniqueId());
							spookArmorStand.setMarker(false);
							spookArmorStand.teleport(spookArmorStand.getLocation().setDirection(player.getLocation().toVector().subtract(spookArmorStand.getLocation().toVector())));
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.55f, 1f);
							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.PLAYERS, 1.5f, 1f);
							GlowingManager.startGlowing(spookArmorStand, NamedTextColor.BLACK, -1, 0, p -> p == player, null);
						}
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 1f, 0.5f);
						break;
					default:
						break;
				}

				// check to eradicate the ghosts
				Entity targetEntity = player.getTargetEntity(30);
				if (targetEntity != null) {
					if (targetEntity.getScoreboardTags().contains(newPhantomName + player.getUniqueId())) {
						targetEntity.remove();
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, SoundCategory.HOSTILE, 1f, 1f);
					}
				}

				// heartbeat
				if (mTicks % 20 == 0) {
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.HOSTILE, 1f, 1f);
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.HOSTILE, 1f, 1f);
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.HOSTILE, 1f, 1f);
				}

				// end
				if (mTicks >= 330 || player.isDead() || !player.isValid()) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.25f);
					this.cancel();
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				for (Entity entity : mHiddenEntities) {
					player.showEntity(Plugin.getInstance(), entity);
				}
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					Location sumBeast = player.getLocation().add(player.getLocation().getDirection().multiply(20));
					sumBeast.setY(player.getLocation().getY() + 1);
					summonBeast(player, sumBeast);
				}, 120L);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private static void getSpooked(Player p, SkullMeta meta) {
		Vector direction = p.getLocation().getDirection().normalize();
		Location armorStandLocation = p.getLocation().clone().add(direction.multiply(5));
		armorStandLocation.setY(p.getLocation().getY() - 5);
		ArmorStand armorStand = (ArmorStand) p.getWorld().spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);
		Vector directionToPlayer = p.getLocation().toVector().subtract(armorStand.getLocation().toVector()).normalize();
		armorStand.setRotation(armorStand.getLocation().setDirection(directionToPlayer).getYaw(), armorStand.getLocation().getPitch());
		armorStand.setVisible(true);
		armorStand.setGravity(false);
		armorStand.setMarker(true);
		armorStand.setCollidable(false);
		armorStand.addScoreboardTag(Constants.Tags.REMOVE_ON_UNLOAD);
		armorStand.setBasePlate(false);
		armorStand.setArms(true);
		armorStand.setHeadPose(new EulerAngle(Math.toRadians(357), Math.toRadians(0), Math.toRadians(0)));
		armorStand.setBodyPose(new EulerAngle(Math.toRadians(30), Math.toRadians(0), Math.toRadians(0)));
		armorStand.setLeftArmPose(new EulerAngle(Math.toRadians(215), Math.toRadians(325), Math.toRadians(0)));
		armorStand.setRightArmPose(new EulerAngle(Math.toRadians(215), Math.toRadians(40), Math.toRadians(0)));
		armorStand.setLeftLegPose(new EulerAngle(Math.toRadians(30), Math.toRadians(0), Math.toRadians(0)));
		armorStand.setRightLegPose(new EulerAngle(Math.toRadians(32), Math.toRadians(0), Math.toRadians(0)));

		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		item.setItemMeta(meta);
		ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		ItemMeta chestplateMeta = chestplate.getItemMeta();
		chestplateMeta.displayName(Component.text("Shroud of Dread"));
		chestplate.setItemMeta(chestplateMeta);
		ItemUtils.setPlainTag(chestplate);
		ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
		ItemMeta leggingsMeta = leggings.getItemMeta();
		leggingsMeta.displayName(Component.text("Tenebrous Robe"));
		leggings.setItemMeta(leggingsMeta);
		ItemUtils.setPlainTag(leggings);
		armorStand.getEquipment().setHelmet(item);
		armorStand.getEquipment().setChestplate(chestplate);
		armorStand.getEquipment().setLeggings(leggings);


		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Location locationInFront = p.getLocation().add(p.getLocation().getDirection().normalize().multiply(0.8));
			locationInFront.setY(locationInFront.getY() - 0.25);
			armorStand.teleport(locationInFront);
			armorStand.setRotation(armorStand.getLocation().setDirection(directionToPlayer).getYaw(), armorStand.getLocation().getPitch());
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1f, 1f);
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 1f, 1f);
		}, 10L);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), armorStand::remove, 20L);
	}

}
