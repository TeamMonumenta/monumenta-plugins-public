package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.bosses.abilities.HuntingCompanionBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Vex;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HuntingCompanion extends Ability {
	private static final int COOLDOWN = 24 * 20;
	private static final int DURATION = 12 * 20;
	private static final int TICK_INTERVAL = 5;
	public static final String FOX_NAME = "FoxCompanion";
	public static final String EAGLE_NAME = "EagleCompanion";
	private static final int DETECTION_RANGE = 32;
	private static final double DAMAGE_FRACTION_1 = 0.2;
	private static final double DAMAGE_FRACTION_2 = 0.4;
	private static final int STUN_TIME_1 = 2 * 20;
	private static final int STUN_TIME_2 = 3 * 20;
	private static final int BLEED_DURATION = 5 * 20;
	private static final double BLEED_AMOUNT = 0.2;
	private static final double VELOCITY = 0.9;
	private static final double JUMP_HEIGHT = 0.8;
	private static final double MAX_TARGET_Y = 4;
	private static final double HEALING_PERCENT = 0.05;

	public static final String CHARM_COOLDOWN = "Hunting Companion Cooldown";
	public static final String CHARM_DURATION = "Hunting Companion Duration";
	public static final String CHARM_STUN_DURATION = "Hunting Companion Stun Duration";
	public static final String CHARM_BLEED_DURATION = "Hunting Companion Bleed Duration";
	public static final String CHARM_BLEED_AMPLIFIER = "Hunting Companion Bleed Amplifier";
	public static final String CHARM_DAMAGE = "Hunting Companion Damage";
	public static final String CHARM_HEALING = "Hunting Companion Healing";
	public static final String CHARM_SPEED = "Hunting Companion Speed";
	public static final String CHARM_FOXES = "Hunting Companion Foxes";
	public static final String CHARM_EAGLES = "Hunting Companion Eagles";

	private HashMap<Mob, LivingEntity> mSummons;
	private final double mDamageFraction;
	private final int mStunTime;
	private boolean mHasWindBomb;
	private @Nullable BukkitRunnable mRunnable;

	public HuntingCompanion(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Hunting Companion");
		mInfo.mScoreboardId = "HuntingCompanion";
		mInfo.mShorthandName = "HC";
		mInfo.mDescriptions.add("Swap hands while holding a bow, crossbow, or trident to summon an invulnerable fox companion. The fox attacks the nearest mob within " + DETECTION_RANGE + " blocks. The fox prioritizes the first enemy you hit with a projectile after summoning, which can be reapplied once that target dies. The fox deals damage equal to " + (int) (100 * DAMAGE_FRACTION_1) + "% of your mainhand's projectile damage, amplified by both melee and projectile damage from gear. Once per mob, the fox stuns upon attack for " + STUN_TIME_1 / 20 + " seconds, except for elites and bosses. When a mob that was damaged by the fox dies, you heal 5% of your max health. The fox disappears after " + DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.");
		mInfo.mDescriptions.add("Damage is increased to " + (int) (100 * DAMAGE_FRACTION_2) + "% of your projectile damage and the stun time is increased to " + STUN_TIME_2 / 20 + " seconds.");
		mInfo.mDescriptions.add("Also summon an invulnerable eagle (parrot). The eagle deals the same damage as the fox and targets similarly, although the two will always avoid targeting the same mob at once. The eagle applies 20% Bleed for 5s isntead of stunning, which can be reapplied on a mob.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.HUNTING_COMPANION;
		mDisplayItem = new ItemStack(Material.SWEET_BERRIES, 1);

		mDamageFraction = isLevelOne() ? DAMAGE_FRACTION_1 : DAMAGE_FRACTION_2;
		mStunTime = (isLevelOne() ? STUN_TIME_1 : STUN_TIME_2) + CharmManager.getExtraDuration(mPlayer, CHARM_STUN_DURATION);

		mSummons = new HashMap<>();
		mRunnable = null;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mHasWindBomb = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, WindBomb.class) != null;
			});
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null || (mHasWindBomb && mPlayer.isSneaking())) {
			return;
		}

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (!isTimerActive() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && (ItemUtils.isBowOrTrident(inMainHand) || inMainHand.getType() == Material.SNOWBALL) && !ItemStatUtils.isShattered(inMainHand)) {
			putOnCooldown();

			clearSummons();

			double multiply = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
			double damage = mDamageFraction * ItemStatUtils.getAttributeAmount(mPlayer.getInventory().getItemInMainHand(), ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND) * (multiply != 0 ? multiply : 1);
			damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);

			double healingPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEALING_PERCENT);

			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

			int foxCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_FOXES);
			for (int i = 0; i < foxCount; i++) {
				spawnFox(damage, healingPercent, playerItemStats);
			}
			if (isEnhanced()) {
				int eagleCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_EAGLES);
				for (int i = 0; i < eagleCount; i++) {
					spawnEagle(damage, healingPercent, playerItemStats);
				}
			}

			int duration = DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
			World world = mPlayer.getWorld();
			mRunnable = new BukkitRunnable() {
				int mTicksElapsed = 0;

				@Override
				public void run() {
					if (mTicksElapsed >= duration) {
						for (Mob summon : mSummons.keySet()) {
							Location summonLoc = summon.getLocation();
							if (summon instanceof Fox) {
								world.playSound(summonLoc, Sound.ENTITY_FOX_SNIFF, 1.5f, 1.0f);
								world.playSound(summonLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
								world.playSound(summonLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
								world.playSound(summonLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
								new PartialParticle(Particle.SMOKE_NORMAL, summonLoc, 20).spawnAsPlayerActive(mPlayer);
							} else {
								eagleSounds(summonLoc);
								new PartialParticle(Particle.CLOUD, summonLoc, 15).spawnAsPlayerActive(mPlayer);
							}
						}

						clearSummons();

						return;
					}

					for (Mob summon : new ArrayList<>(mSummons.keySet())) {
						LivingEntity specifiedTarget = mSummons.get(summon);
						if (specifiedTarget != null) {
							if (specifiedTarget.isDead()) {
								mSummons.replace(summon, null);
							} else {
								summon.setTarget(specifiedTarget);
							}
						} else {
							if (summon.getTarget() == null || summon.getTarget().isDead()) {
								LivingEntity nearestMob = findNearestNonTargetedMob(summon);
								if (nearestMob != null) {
									summon.setTarget(nearestMob);
									if (summon instanceof Fox) {
										world.playSound(summon.getLocation(), Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
									} else {
										eagleSounds(summon.getLocation());
									}
								}
							}
						}
					}

					mTicksElapsed += TICK_INTERVAL;
				}
			};
			mRunnable.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && (event.getDamager() instanceof AbstractArrow || event.getDamager() instanceof Snowball)) {
			Mob nearestSummon = findNearestNonTargetingSummon(enemy);
			if (nearestSummon != null) {
				mSummons.replace(nearestSummon, enemy);

				World world = nearestSummon.getWorld();
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
				if (nearestSummon instanceof Fox) {
					world.playSound(nearestSummon.getLocation(), Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
				} else {
					eagleSounds(nearestSummon.getLocation());
				}
				new PartialParticle(Particle.VILLAGER_ANGRY, nearestSummon.getEyeLocation(), 25).spawnAsPlayerActive(mPlayer);
				PotionUtils.applyPotion(mPlayer, enemy, new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
			}
		}

		return true; // only one targeting instance per tick
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		clearSummons();
	}

	private void clearSummons() {
		for (LivingEntity summon : mSummons.keySet()) {
			summon.remove();
		}
		for (LivingEntity target : mSummons.values()) {
			if (target != null) {
				target.removePotionEffect(PotionEffectType.GLOWING);
			}
		}
		mSummons.clear();
		if (mRunnable != null) {
			mRunnable.cancel();
			mRunnable = null;
		}
	}

	private void spawnFox(double damage, double healingPercent, ItemStatManager.PlayerItemStats playerItemStats) {
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
		Vector perp = new Vector(-facingDirection.getZ(), 0, facingDirection.getX()).normalize(); //projection of the perpendicular vector to facingDirection onto the xz plane
		Vector sideOffset = new Vector(0, 0, 0);
		Location pos = loc.clone().add(perp);
		Location neg = loc.clone().subtract(perp);
		if (pos.isChunkLoaded() && !pos.getBlock().isSolid() && !pos.add(0, 1, 0).getBlock().isSolid()) {
			sideOffset = perp;
		} else if (neg.isChunkLoaded() && !neg.getBlock().isSolid() && !neg.add(0, 1, 0).getBlock().isSolid()) {
			sideOffset = perp.clone().multiply(-1);
		} else if (!loc.isChunkLoaded()) {
			// Player is standing somewhere that's not loaded, abort
			return;
		}

		loc.add(sideOffset).add(facingDirection.clone().setY(0).normalize().multiply(-0.25));

		Fox fox = (Fox) LibraryOfSoulsIntegration.summon(loc, FOX_NAME);
		if (fox == null) {
			MMLog.warning("Failed to spawn FoxCompanion from Library of Souls");
			return;
		}

		EntityUtils.setAttributeBase(fox, Attribute.GENERIC_MOVEMENT_SPEED, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, EntityUtils.getAttributeBaseOrDefault(fox, Attribute.GENERIC_MOVEMENT_SPEED, 0)));

		if (LocationUtils.isInSnowyBiome(loc)) {
			fox.setFoxType(Fox.Type.SNOW);
		}

		fox.setVelocity(facingDirection.clone().setY(JUMP_HEIGHT).normalize().multiply(VELOCITY));
		fox.teleport(fox.getLocation().setDirection(facingDirection));

		mSummons.put(fox, null);

		HuntingCompanionBoss huntingCompanionBoss = BossUtils.getBossOfClass(fox, HuntingCompanionBoss.class);
		if (huntingCompanionBoss == null) {
			MMLog.warning("Failed to get HuntingCompanionBoss for FoxCompanion");
			return;
		}
		huntingCompanionBoss.spawn(mPlayer, damage, mStunTime, 0, 0, healingPercent, playerItemStats);

		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.75f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
	}

	private void spawnEagle(double damage, double healingPercent, ItemStatManager.PlayerItemStats playerItemStats) {
		Location loc = mPlayer.getLocation();
		Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
		Vector perp = new Vector(-facingDirection.getZ(), 0, facingDirection.getX()).normalize(); //projection of the perpendicular vector to facingDirection onto the xz plane
		Vector sideOffset = new Vector(0, 0, 0);
		Location pos = loc.clone().add(perp);
		Location neg = loc.clone().subtract(perp);
		if (neg.isChunkLoaded() && !neg.getBlock().isSolid() && !neg.add(0, 1, 0).getBlock().isSolid() && !neg.add(0, 1, 0).getBlock().isSolid()) {
			sideOffset = perp.clone().multiply(-1);
		} else if (pos.isChunkLoaded() && !pos.getBlock().isSolid() && !pos.add(0, 1, 0).getBlock().isSolid() && !pos.add(0, 1, 0).getBlock().isSolid()) {
			sideOffset = perp;
		} else if (!loc.isChunkLoaded()) {
			// Player is standing somewhere that's not loaded, abort
			return;
		}

		loc.add(sideOffset).add(facingDirection.clone().setY(0).normalize().multiply(-0.25)).add(0, 2, 0);

		Mob eagle = (Mob) LibraryOfSoulsIntegration.summon(loc, EAGLE_NAME);
		if (eagle == null) {
			MMLog.warning("Failed to spawn EagleCompanion from Library of Souls");
			return;
		}
		try {
			NmsUtils.getVersionAdapter().setEagleCompanion((Creature) eagle, (target) -> {
				//this is just used to create a fake event that HuntingCompanionBoss will catch and handle using player stats
				DamageUtils.damage(eagle, target, DamageType.MELEE, 1.0);
				//if we want, we can move all the code of HuntingCompanionBoss.onDamage(..) here.
			}, 4);

		} catch (Exception e) {
			MMLog.warning("Catch an exception while creating EagleCompanion. Reason: " + e.getMessage());
			e.printStackTrace();
		}
		eagle.setVelocity(facingDirection.clone().setY(-JUMP_HEIGHT).normalize().multiply(VELOCITY));
		eagle.teleport(eagle.getLocation().setDirection(facingDirection));

		mSummons.put(eagle, null);

		HuntingCompanionBoss huntingCompanionBoss = BossUtils.getBossOfClass(eagle, HuntingCompanionBoss.class);
		if (huntingCompanionBoss == null) {
			MMLog.warning("Failed to get HuntingCompanionBoss for EagleCompanion");
			return;
		}
		huntingCompanionBoss.spawn(mPlayer, damage, 0, BLEED_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_BLEED_DURATION), BLEED_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED_AMPLIFIER), healingPercent, playerItemStats);

		eagleSounds(eagle.getLocation());
	}

	private @Nullable Mob findNearestNonTargetingSummon(LivingEntity target) {
		Location targetLoc = target.getLocation();
		List<LivingEntity> summons = new ArrayList<>(mSummons.keySet());

		summons.removeIf(summon -> mSummons.get(summon) != null);
		summons.removeIf(summon -> summon.getLocation().distance(targetLoc) > DETECTION_RANGE);

		LivingEntity nearestSummon = EntityUtils.getNearestMob(targetLoc, summons);
		if (nearestSummon instanceof Mob mob) {
			// Should always be a mob (unless null) since it is one of the summons
			return mob;
		} else {
			return null;
		}
	}

	private @Nullable LivingEntity findNearestNonTargetedMob(LivingEntity summon) {
		Location summonLoc = summon.getLocation();
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(summon.getLocation(), DETECTION_RANGE);

		nearbyMobs.removeIf(Entity::isInvulnerable);
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		if (!(summon instanceof Vex)) {
			nearbyMobs.removeIf((mob) -> Math.abs(mob.getLocation().getY() - summonLoc.getY()) > MAX_TARGET_Y);
		}
		for (Mob otherSummon : mSummons.keySet()) {
			LivingEntity otherTarget = otherSummon.getTarget();
			if (otherTarget != null) {
				nearbyMobs.remove(otherTarget);
			}
		}

		return EntityUtils.getNearestMob(summon.getLocation(), nearbyMobs);
	}

	public static void eagleSounds(Location loc) {
		loc.getWorld().playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 1, 2);
	}
}
