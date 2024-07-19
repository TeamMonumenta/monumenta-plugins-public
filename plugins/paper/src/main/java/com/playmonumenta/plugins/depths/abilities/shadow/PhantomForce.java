package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.PhantomForceBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PhantomForce extends DepthsAbility {
	public static final int SPAWN_COUNT = 3;
	public static final int TWISTED_SPAWN_COUNT = 5;
	public static final double MOVEMENT_SPEED = 5;
	public static final int DURATION = 15 * 20;
	public static final double[] DAMAGE = {5, 6.5, 8, 9.5, 11, 14};
	public static final double[] WEAKEN_AMOUNT = {0.20, 0.25, 0.30, 0.35, 0.40, 0.50};
	public static final int WEAKEN_DURATION = 4 * 20;

	public static final DepthsAbilityInfo<PhantomForce> INFO =
		new DepthsAbilityInfo<>(PhantomForce.class, "Phantom Force", PhantomForce::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SPAWNER)
			.linkedSpell(ClassAbility.PHANTOM_FORCE)
			.displayItem(Material.CHARCOAL)
			.descriptions(PhantomForce::getDescription)
			.singleCharm(false);

	private final int mSpawnCount;
	private final double mDamage;
	private final double mWeakenAmount;
	private final int mWeakenDuration;
	private final int mDuration;
	private final double mSpeed;

	public PhantomForce(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSpawnCount = (mRarity == 6 ? TWISTED_SPAWN_COUNT : SPAWN_COUNT) + (int) CharmManager.getLevel(mPlayer, CharmEffects.PHANTOM_FORCE_SPAWN_COUNT.mEffectName);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PHANTOM_FORCE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mWeakenAmount = WEAKEN_AMOUNT[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PHANTOM_FORCE_WEAKEN_AMOUNT.mEffectName);
		mWeakenDuration = CharmManager.getDuration(mPlayer, CharmEffects.PHANTOM_FORCE_WEAKEN_DURATION.mEffectName, WEAKEN_DURATION);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.PHANTOM_FORCE_VEX_DURATION.mEffectName, DURATION);
		mSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PHANTOM_FORCE_MOVEMENT_SPEED.mEffectName, MOVEMENT_SPEED);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, int rarity, Location loc) {
		int spawnCount = (rarity == 6 ? TWISTED_SPAWN_COUNT : SPAWN_COUNT) + (int) CharmManager.getLevel(player, CharmEffects.PHANTOM_FORCE_SPAWN_COUNT.mEffectName);
		double damage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.PHANTOM_FORCE_DAMAGE.mEffectName, DAMAGE[rarity - 1]);
		double weakenAmount = WEAKEN_AMOUNT[rarity - 1] + CharmManager.getLevelPercentDecimal(player, CharmEffects.PHANTOM_FORCE_WEAKEN_AMOUNT.mEffectName);
		int weakenDuration = CharmManager.getDuration(player, CharmEffects.PHANTOM_FORCE_WEAKEN_DURATION.mEffectName, WEAKEN_DURATION);
		int duration = CharmManager.getDuration(player, CharmEffects.PHANTOM_FORCE_VEX_DURATION.mEffectName, DURATION);
		double speed = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.PHANTOM_FORCE_MOVEMENT_SPEED.mEffectName, MOVEMENT_SPEED);
		onSpawnerBreak(plugin, player, loc, spawnCount, damage, weakenAmount, weakenDuration, duration, speed);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, Location loc, int count, double damage, double weakenAmount, int weakenDuration, int duration, double speed) {
		loc.getWorld().playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 0.5f);
		loc.getWorld().playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.7f, 0.8f);
		new PartialParticle(Particle.SPELL_MOB, loc, 20).delta(0.5).spawnAsPlayerActive(player);

		for (int i = 0; i < count; i++) {
			Location spawnLoc = loc.clone().add(VectorUtils.rotateYAxis(new Vector(1, 0, 0), 360D * i / count));

			Vex vex = (Vex) LibraryOfSoulsIntegration.summon(spawnLoc, "PhantomForce");
			if (vex == null) {
				MMLog.warning("Failed to summon PhantomForce");
				return;
			}
			PhantomForceBoss phantomForceBoss = BossUtils.getBossOfClass(vex, PhantomForceBoss.class);
			if (phantomForceBoss == null) {
				MMLog.warning("Failed to get PhantomForceBoss");
				return;
			}
			ItemStatManager.PlayerItemStats playerItemStats = plugin.mItemStatManager.getPlayerItemStatsCopy(player);
			phantomForceBoss.spawn(player, damage, weakenAmount, weakenDuration, playerItemStats);

			new PartialParticle(Particle.SQUID_INK, vex.getEyeLocation(), 1).spawnAsPlayerActive(player);

			new BukkitRunnable() {
				final Vex mBoss = Objects.requireNonNull(vex);
				@Nullable
				LivingEntity mTarget;
				int mTicks = 0;
				double mRadian = 0;

				@Override
				public void run() {
					Location pLoc = LocationUtils.getEntityCenter(mBoss).subtract(mBoss.getLocation().getDirection().multiply(0.5));
					new PartialParticle(Particle.SMOKE_NORMAL, pLoc, 4).delta(0.1).extra(0.05).spawnAsPlayerActive(player);
					new PartialParticle(Particle.FALLING_DUST, pLoc, 1).data(Material.BLACK_CONCRETE.createBlockData()).spawnAsPlayerActive(player);

					// aggro
					mTarget = mBoss.getTarget();
					if (mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0 || !mTarget.isValid()) {
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 30, mBoss);
						nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
						nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageType.MELEE_SKILL));
						if (!nearbyMobs.isEmpty()) {
							Collections.shuffle(nearbyMobs);
							LivingEntity randomMob = nearbyMobs.get(0);
							// LivingEntity randomMob = EntityUtils.getNearestMob(mBoss.getLocation(), nearbyMobs);
							if (randomMob != null) {
								mBoss.setTarget(randomMob);
								mTarget = randomMob;

								loc.getWorld().playSound(loc, Sound.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 0.75f, 0.75f);
								loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 0.75f, 0.75f);
								Vector dir = LocationUtils.getHalfHeightLocation(mTarget).subtract(mBoss.getLocation()).toVector().normalize();
								new PPLine(Particle.SMOKE_NORMAL, loc, LocationUtils.getEntityCenter(mTarget))
									.countPerMeter(2)
									.directionalMode(true)
									.delta(dir.getX(), dir.getY(), dir.getZ())
									.extra(0.12)
									.includeEnd(false)
									.spawnAsPlayerActive(player);
							}
						}
					}

					// movement
					Location vexLoc = mBoss.getLocation();
					if (mTarget != null && !mTarget.isDead()) {
						mBoss.setCharging(true);
						Vector direction = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(mTarget), vexLoc);
						double yDiff = (mTarget.getLocation().getY() - mBoss.getLocation().getY()) * 0.1;
						if (yDiff > direction.getY()) {
							direction.setY(yDiff);
						}
						vexLoc.setDirection(direction);
						// set speed
						vexLoc.add(direction.multiply(speed * 1 / 20));
						// attack
						if (mBoss.getBoundingBox().overlaps(mTarget.getBoundingBox())) {
							mBoss.attack(mTarget);
						}
					} else {
						mBoss.setCharging(false);
					}
					mBoss.teleport(vexLoc.clone().add(0, FastMath.sin(mRadian) * 0.05, 0));
					mRadian += Math.PI / 20D;

					if (mTicks > duration || !mBoss.isValid()) {
						new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 40).delta(0.5).extra(0.2).spawnAsPlayerActive(player);
						new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation(), 10).delta(0.5).data(Material.BLACK_CONCRETE.createBlockData()).spawnAsPlayerActive(player);

						mBoss.remove();
						this.cancel();
						return;
					}
					mTicks++;
				}
			}.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			onSpawnerBreak(mPlugin, mPlayer, BlockUtils.getCenteredBlockBaseLocation(block), mSpawnCount, mDamage, mWeakenAmount, mWeakenDuration, mDuration, mSpeed);
		}
		return true;
	}

	private static Description<PhantomForce> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<PhantomForce>(color)
			.add("Breaking a spawner spawns ")
			.add(a -> a.mSpawnCount, rarity == 6 ? TWISTED_SPAWN_COUNT : SPAWN_COUNT)
			.add(" vexes that target your enemies, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage on contact and applying ")
			.addPercent(a -> a.mWeakenAmount, WEAKEN_AMOUNT[rarity - 1], false, true)
			.add(" Weaken for ")
			.addDuration(a -> a.mWeakenDuration, WEAKEN_DURATION)
			.add(" seconds. Vexes last for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and have a movement speed of ")
			.add(a -> a.mSpeed, MOVEMENT_SPEED)
			.add(" blocks per second.");
	}
}
