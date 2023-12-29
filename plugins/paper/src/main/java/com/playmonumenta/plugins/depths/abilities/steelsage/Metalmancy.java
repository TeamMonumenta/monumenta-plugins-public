package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Metalmancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Metalmancy";

	public static final double[] DAMAGE = {10, 12.5, 15, 17.5, 20, 25};
	public static final int[] DURATION = {10 * 20, 11 * 20, 12 * 20, 13 * 20, 14 * 20, 18 * 20};
	public static final int COOLDOWN = 32 * 20;
	public static final String GOLEM_NAME = "SteelConstruct";
	public static final double VELOCITY = 2;
	public static final int DETECTION_RANGE = 32;
	public static final int TICK_INTERVAL = 5;
	private static final double MAX_TARGET_Y = 4;

	public static final String CHARM_COOLDOWN = "Metalmancy Cooldown";

	public static final DepthsAbilityInfo<Metalmancy> INFO =
		new DepthsAbilityInfo<>(Metalmancy.class, ABILITY_NAME, Metalmancy::new, DepthsTree.STEELSAGE, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.METALMANCY)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Metalmancy::cast, DepthsTrigger.SWAP))
			.displayItem(Material.IRON_BLOCK)
			.descriptions(Metalmancy::getDescription);

	private final int mDuration;
	private final double mDamage;

	private final Map<Mob, LivingEntity> mSummons = new HashMap<>();
	private final Map<Mob, BukkitRunnable> mRunnables = new HashMap<>();

	public Metalmancy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.METALMANCY_DURATION.mEffectName, DURATION[mRarity - 1]);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.METALMANCY_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return resetTarget();
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
		Mob golem = (Mob) LibraryOfSoulsIntegration.summon(mPlayer.getLocation().add(facingDirection).add(0, 1, 0), GOLEM_NAME);
		if (golem == null) {
			return false;
		}
		golem.setVelocity(facingDirection.multiply(VELOCITY));

		MetalmancyBoss metalmancyBoss = BossUtils.getBossOfClass(golem, MetalmancyBoss.class);
		if (metalmancyBoss == null) {
			MMLog.warning("Failed to get MetalmancyBoss");
			return false;
		}
		mSummons.put(golem, null);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		metalmancyBoss.spawn(mPlayer, mDamage, playerItemStats);

		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicksElapsed = 0;
			@Override
			public void run() {
				boolean isOutOfTime = mTicksElapsed >= mDuration;
				if (isOutOfTime) {
					Location golemLoc = golem.getLocation();
					world.playSound(golemLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.8f, 1.0f);
					new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, golemLoc, 15).delta(0.5).extra(0.1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_NORMAL, golemLoc, 20).delta(0.5).extra(0.1).spawnAsPlayerActive(mPlayer);

					golem.remove();
					mSummons.remove(golem);
					this.cancel();
				}

				LivingEntity target = mSummons.get(golem);
				if (target != null && target.isValid()) {
					golem.setTarget(target);
				} else {
					mSummons.replace(golem, null);
					if (golem.getTarget() == null) {
						golem.setTarget(findNearestNonTargetedMob(golem));
					}
				}

				mTicksElapsed += TICK_INTERVAL;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				golem.remove();
				mSummons.remove(golem);
				mRunnables.remove(golem);
			}
		};
		runnable.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		mRunnables.put(golem, runnable);

		return true;
	}

	private @Nullable LivingEntity findNearestNonTargetedMob(LivingEntity summon) {
		Location summonLoc = summon.getLocation();
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(summon.getLocation(), DETECTION_RANGE);
		nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.PROJECTILE_SKILL));
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
		mSummons.keySet().stream().map(Mob::getTarget).filter(Objects::nonNull).forEach(nearbyMobs::remove);

		List<LivingEntity> unfilteredNearbyMobs = new ArrayList<>(nearbyMobs);

		nearbyMobs.removeIf(mob -> Math.abs(mob.getLocation().getY() - summonLoc.getY()) > MAX_TARGET_Y);
		nearbyMobs.removeIf(mob -> EntityUtils.isFlyingMob(EntityUtils.getEntityStackBase(mob)));

		// if there are no other mobs to target, we can double up
		if (nearbyMobs.isEmpty()) {
			return EntityUtils.getNearestMob(summon.getLocation(), unfilteredNearbyMobs);
		}

		return EntityUtils.getNearestMob(summon.getLocation(), nearbyMobs);
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		// This despawns the summons and clears all lists. new ArrayList needed to prevent CME
		new ArrayList<>(mRunnables.values()).forEach(BukkitRunnable::cancel);
	}

	private boolean resetTarget() {
		LivingEntity newTarget = EntityUtils.getEntityAtCursor(mPlayer, 40, e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());
		if (newTarget != null) {
			Mob summon = findNearestAvailableSummon(newTarget);
			if (summon != null) {
				mSummons.put(summon, newTarget);
				summon.setTarget(newTarget);
				PotionUtils.applyColoredGlowing("MetalmancyTarget", newTarget, NamedTextColor.RED, 10);
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 1.5f);
				return true;
			}
		}
		return false;
	}

	private @Nullable Mob findNearestAvailableSummon(LivingEntity target) {
		List<Mob> summons = new ArrayList<>(mSummons.keySet());

		// If a summon is already targeting this mob, choose that summon
		for (Mob summon : summons) {
			if (summon.getTarget() == target) {
				return summon;
			}
		}

		Location targetLoc = target.getLocation();
		summons.removeIf(summon -> summon.getLocation().distance(targetLoc) > DETECTION_RANGE);

		List<Mob> nonTargetingSummons = new ArrayList<>(summons);
		nonTargetingSummons.removeIf(summon -> mSummons.get(summon) != null);

		if (nonTargetingSummons.isEmpty()) {
			nonTargetingSummons = summons;
		}

		return EntityUtils.getNearestMob(targetLoc, nonTargetingSummons);
	}

	public static boolean isMetalmancy(Entity entity) {
		return entity.getScoreboardTags().contains(MetalmancyBoss.identityTag);
	}

	private static Description<Metalmancy> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Metalmancy>(color)
			.add("Swap hands to summon an invulnerable steel construct. The Construct attacks the nearest mob within ")
			.add(DETECTION_RANGE)
			.add(" blocks. The Construct deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" projectile damage and taunts non-boss enemies it hits. The Construct disappears after ")
			.addDuration(a -> a.mDuration, DURATION[rarity - 1], false, true)
			.add(" seconds. Triggering while on cooldown will set the Construct's target to the mob you are looking at.")
			.addCooldown(COOLDOWN);
	}


}
