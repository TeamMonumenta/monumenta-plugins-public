package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.HuntingCompanionCS;
import com.playmonumenta.plugins.effects.HealPlayerOnDeath;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Strider;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class HuntingCompanion extends Ability {
	private static final int COOLDOWN = 24 * 20;
	private static final int DURATION = 12 * 20;
	private static final int TICK_INTERVAL = 5;
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
	private static final String HEAL_EFFECT = "HuntingCompanionHealPlayerOnDeathEffect";

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

	public static final AbilityInfo<HuntingCompanion> INFO =
		new AbilityInfo<>(HuntingCompanion.class, "Hunting Companion", HuntingCompanion::new)
			.linkedSpell(ClassAbility.HUNTING_COMPANION)
			.scoreboardId("HuntingCompanion")
			.shorthandName("HC")
			.descriptions(
				"Swap hands while holding a projectile weapon to summon an invulnerable fox companion. " +
					"The fox attacks the nearest mob within " + DETECTION_RANGE + " blocks. " +
					"The fox prioritizes the first enemy you hit with a projectile after summoning, which can be reapplied once that target dies. " +
					"The fox deals damage equal to " + (int) (100 * DAMAGE_FRACTION_1) + "% of your mainhand's projectile damage, amplified by both melee and projectile damage from gear. " +
					"Once per mob, the fox stuns upon attack for " + STUN_TIME_1 / 20 + " seconds, except for elites and bosses. " +
					"When a mob that was damaged by the fox dies, you heal " + (int) (HEALING_PERCENT * 100) + "% of your max health. " +
					"The fox disappears after " + DURATION / 20 + " seconds. " +
					"If used while in water, an axolotl is spawned instead, and if used while in lava, a strider is spawned instead. Cooldown: " + COOLDOWN / 20 + "s.",
				"Damage is increased to " + (int) (100 * DAMAGE_FRACTION_2) + "% of your projectile damage and the stun time is increased to " + STUN_TIME_2 / 20 + " seconds.",
				"Also summon an invulnerable eagle (parrot). " +
					"The eagle deals the same damage as the fox and targets similarly, although the two will always avoid targeting the same mob at once. " +
					"The eagle can swoop towards its target. " +
					"The eagle applies " + (int) (BLEED_AMOUNT * 100) + "% Bleed for " + BLEED_DURATION / 20 + "s instead of stunning, which can be reapplied on a mob. " +
					"If used in water, a dolphin is spawned instead.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HuntingCompanion::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.SWEET_BERRIES, 1));

	private final HashMap<Mob, LivingEntity> mSummons;
	private final double mDamageFraction;
	private final int mStunDuration;
	private final int mBleedDuration;
	private final double mBleedAmount;
	private final double mHealingPercent;
	private @Nullable BukkitRunnable mRunnable;

	private final HuntingCompanionCS mCosmetic;

	public HuntingCompanion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageFraction = isLevelOne() ? DAMAGE_FRACTION_1 : DAMAGE_FRACTION_2;
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, (isLevelOne() ? STUN_TIME_1 : STUN_TIME_2));
		mBleedDuration = isEnhanced() ? CharmManager.getDuration(mPlayer, CHARM_BLEED_DURATION, BLEED_DURATION) : 0;
		mBleedAmount = isEnhanced() ? BLEED_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED_AMPLIFIER) : 0;
		mHealingPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEALING_PERCENT);
		mSummons = new HashMap<>();
		mRunnable = null;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HuntingCompanionCS(), HuntingCompanionCS.SKIN_LIST);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		clearSummons();

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		double multiply = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
		double damage = mDamageFraction * ItemStatUtils.getAttributeAmount(inMainHand, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND) * (multiply != 0 ? multiply : 1);
		damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Location loc = mPlayer.getLocation();
		boolean isInWater = LocationUtils.isLocationInWater(loc);
		boolean isInLava = loc.getBlock().getType() == Material.LAVA;
		int foxCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_FOXES);

		// anti lag check
		if (foxCount > 5) {
			Class<? extends Entity> type = !isInLava ? (!isInWater ? Fox.class : Axolotl.class) : Strider.class;
			if (loc.getWorld().getNearbyEntitiesByType(type, loc, 50).size() > 10) {
				return;
			}
		}

		String foxName = !isInLava ? (!isInWater ? mCosmetic.getFoxName() : mCosmetic.getAxolotlName()) : mCosmetic.getStriderName();
		for (int i = 0; i < foxCount; i++) {
			summon(foxName, damage, playerItemStats, false);
		}
		if (isEnhanced()) {
			int eagleCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_EAGLES);
			String eagleName = !isInWater ? mCosmetic.getEagleName() : mCosmetic.getDolphinName();
			for (int i = 0; i < eagleCount; i++) {
				summon(eagleName, damage, playerItemStats, true);
			}
		}

		BukkitRunnable cosmeticRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				for (Mob summon : new ArrayList<>(mSummons.keySet())) {
					if (summon.isDead() || !summon.isValid()) {
						mSummons.remove(summon);
					} else {
						mCosmetic.tick(summon, mPlayer, mSummons.get(summon), mT);
					}
				}
				if (mSummons.isEmpty()) {
					this.cancel();
				}
			}
		};
		cosmeticRunnable.runTaskTimer(mPlugin, 0, 1);

		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		World world = mPlayer.getWorld();
		mRunnable = new BukkitRunnable() {
			int mTicksElapsed = 0;

			@Override
			public void run() {
				if (mTicksElapsed >= duration) {
					mSummons.keySet().forEach(summon -> mCosmetic.onDespawn(world, summon.getLocation(), summon, mPlayer));
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
						if (!EntityUtils.isHostileMob(summon.getTarget())) {
							summon.setTarget(null);
						}
						if (summon.getTarget() == null || summon.getTarget().isDead()) {
							LivingEntity nearestMob = findNearestNonTargetedMob(summon);
							if (nearestMob != null) {
								summon.setTarget(nearestMob);
								mCosmetic.onAggroSounds(world, nearestMob.getLocation(), summon);
							}
						}
					}
				}

				mTicksElapsed += TICK_INTERVAL;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				cosmeticRunnable.cancel();
			}
		};
		mRunnable.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!mSummons.containsValue(enemy) && event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj && EntityUtils.isAbilityTriggeringProjectile(proj, false)) {
			Mob nearestSummon = findNearestNonTargetingSummon(enemy);
			if (nearestSummon != null) {
				mSummons.replace(nearestSummon, enemy);

				World world = nearestSummon.getWorld();
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
				mCosmetic.onAggro(world, nearestSummon.getLocation(), mPlayer, nearestSummon);
				PotionUtils.applyPotion(mPlayer, enemy, new PotionEffect(PotionEffectType.GLOWING, DURATION, 0, true, false));
			}
		}

		return true; // only one targeting instance per tick
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		clearSummons();
	}

	@Override
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		//Clear summons when teleporting to a different world or more than 100 blocks away
		if (event.getFrom().getWorld() != event.getTo().getWorld() || event.getFrom().distance(event.getTo()) > 100) {
			clearSummons();
		}
	}

	private void clearSummons() {
		mSummons.keySet().forEach(Entity::remove);
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

	// Relies on mobs from the LoS. These mobs must have the tags UNPUSHABLE, boss_ccimmune, boss_canceldamage, and summon_ignore and must be invulnerable
	private void summon(String name, double damage, ItemStatManager.PlayerItemStats playerItemStats, boolean eagle) {
		Location loc = mPlayer.getLocation();
		Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
		Vector sideOffset = getSideOffset(loc, facingDirection, eagle);
		if (sideOffset == null) {
			return;
		}
		loc.add(sideOffset).add(facingDirection.clone().setY(0).normalize().multiply(-0.25));
		if (eagle) {
			loc.add(0, 2, 0);
		}

		Creature summon = (Creature) LibraryOfSoulsIntegration.summon(loc, name);
		if (summon == null) {
			MMLog.warning("Failed to spawn " + name + " from Library of Souls");
			return;
		}

		summon.setVelocity(facingDirection.clone().setY(eagle ? -JUMP_HEIGHT : JUMP_HEIGHT).normalize().multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, VELOCITY)));
		summon.teleport(summon.getLocation().setDirection(facingDirection));

		List<UUID> stunnedMobs = new ArrayList<>();
		if (damage > 0) {
			try {
				NmsUtils.getVersionAdapter().setHuntingCompanion(summon, target -> {
					DamageUtils.damage(mPlayer, target, new DamageEvent.Metadata(DamageType.MELEE_SKILL, ClassAbility.HUNTING_COMPANION, playerItemStats), damage, true, true, false);

					if (!eagle) {
						UUID uuid = target.getUniqueId();
						if (!stunnedMobs.contains(uuid)) {
							EntityUtils.applyStun(mPlugin, mStunDuration, target);
							stunnedMobs.add(uuid);
						}
					}

					if (eagle) {
						EntityUtils.applyBleed(mPlugin, mBleedDuration, mBleedAmount, target);
					}

					mPlugin.mEffectManager.addEffect(target, HEAL_EFFECT, new HealPlayerOnDeath(60 * 20, EntityUtils.getMaxHealth(mPlayer) * mHealingPercent, mPlayer));

					mCosmetic.onAttack(summon.getWorld(), summon.getLocation(), summon);
				}, 4);
			} catch (Exception e) {
				MMLog.warning("Catch an exception while creating " + name + ". Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (summon instanceof Fox fox && LocationUtils.isInSnowyBiome(loc)) {
			fox.setFoxType(Fox.Type.SNOW);
		}

		Attribute attribute;
		if (summon instanceof Parrot) {
			attribute = Attribute.GENERIC_FLYING_SPEED;
		} else {
			attribute = Attribute.GENERIC_MOVEMENT_SPEED;
		}
		EntityUtils.setAttributeBase(summon, attribute, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, EntityUtils.getAttributeBaseOrDefault(summon, attribute, 0)));

		mSummons.put(summon, null);

		mCosmetic.onSummon(loc.getWorld(), loc, mPlayer, summon);
	}

	private @Nullable Vector getSideOffset(Location loc, Vector facingDirection, boolean eagle) {
		Vector perp = new Vector(-facingDirection.getZ(), 0, facingDirection.getX()).normalize(); //projection of the perpendicular vector to facingDirection onto the xz plane
		// Eagles and dolphins spawn on opposite side by default
		if (eagle) {
			perp.multiply(-1);
		}
		Vector sideOffset = new Vector();
		Location pos = loc.clone().add(perp);
		Location neg = loc.clone().subtract(perp);
		if (canSpawnAt(pos)) {
			sideOffset = perp;
		} else if (canSpawnAt(neg)) {
			sideOffset = perp.clone().multiply(-1);
		} else if (!loc.isChunkLoaded()) {
			// Player is standing somewhere that's not loaded, abort
			return null;
		}
		return sideOffset;
	}

	private boolean canSpawnAt(Location test) {
		if (test.isChunkLoaded()) {
			Block block = test.getBlock();
			if (!block.isSolid()) {
				Block block1 = block.getRelative(BlockFace.UP);
				if (!block1.isSolid()) {
					Block block2 = block1.getRelative(BlockFace.UP);
					if (!block2.isSolid()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private @Nullable Mob findNearestNonTargetingSummon(LivingEntity target) {
		Location targetLoc = target.getLocation();
		List<LivingEntity> summons = new ArrayList<>(mSummons.keySet());

		summons.removeIf(summon -> summon instanceof Mob mob && mSummons.get(mob) != null);
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

		nearbyMobs.removeIf(DamageUtils::isImmuneToDamage);
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		List<LivingEntity> unfilteredNearbyMobs = new ArrayList<>(nearbyMobs);

		if (summon instanceof Fox || summon instanceof Strider) {
			nearbyMobs.removeIf(mob -> Math.abs(mob.getLocation().getY() - summonLoc.getY()) > MAX_TARGET_Y);
			nearbyMobs.removeIf(mob -> EntityUtils.isFlyingMob(EntityUtils.getEntityStackBase(mob)));
		} else if (summon instanceof Axolotl || summon instanceof Dolphin) {
			nearbyMobs.removeIf(mob -> !EntityUtils.isInWater(mob));
		}

		for (Mob otherSummon : mSummons.keySet()) {
			LivingEntity otherTarget = otherSummon.getTarget();
			if (otherTarget != null) {
				nearbyMobs.remove(otherTarget);
			}
		}

		// if there are no other mobs to target, we can double up
		if (nearbyMobs.isEmpty()) {
			return EntityUtils.getNearestMob(summon.getLocation(), unfilteredNearbyMobs);
		}

		return EntityUtils.getNearestMob(summon.getLocation(), nearbyMobs);
	}
}
