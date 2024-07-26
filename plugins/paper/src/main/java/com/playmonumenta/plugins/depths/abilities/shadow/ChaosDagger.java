package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ChaosDagger extends DepthsAbility {

	public static final String ABILITY_NAME = "Chaos Dagger";
	public static final int COOLDOWN = 22 * 20;
	public static final double[] DAMAGE = {1.0, 1.25, 1.5, 1.75, 2.0, 2.5};
	private static final double VELOCITY = 0.5;
	public static final int STUN_DURATION = 3 * 20;
	public static final int DAMAGE_DURATION = 5 * 20;
	private static final int TARGET_RADIUS = 20;
	private static final int ELITE_RADIUS = 5;
	private static final int STEALTH_DURATION = 30;
	public static final String NO_GLOWING_CLEAR_TAG = "ChaosDaggerNoGlowingClear";

	public static final String CHARM_COOLDOWN = "Chaos Dagger Cooldown";

	public static final DepthsAbilityInfo<ChaosDagger> INFO =
		new DepthsAbilityInfo<>(ChaosDagger.class, ABILITY_NAME, ChaosDagger::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.CHAOS_DAGGER)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChaosDagger::cast, DepthsTrigger.SWAP))
			.displayItem(Material.ITEM_FRAME)
			.descriptions(ChaosDagger::getDescription);

	private final int mStunDuration;
	private final double mDamageMultiplier;
	private final int mStealthDuration;

	private @Nullable Entity mHitMob;

	public ChaosDagger(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.CHAOS_DAGGER_STUN_DURATION.mEffectName, STUN_DURATION);
		mDamageMultiplier = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.CHAOS_DAGGER_DAMAGE_MULTIPLIER.mEffectName);
		mStealthDuration = CharmManager.getDuration(mPlayer, CharmEffects.CHAOS_DAGGER_STEALTH_DURATION.mEffectName, STEALTH_DURATION);
	}

	public boolean cast() {
		if (isOnCooldown() ||
			    EntityUtils.getNearestMob(mPlayer.getLocation(), 20.0) == null) {
			return false;
		}
		putOnCooldown();
		mHitMob = null;

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		double velocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.CHAOS_DAGGER_VELOCITY.mEffectName, VELOCITY);
		Item dagger = AbilityUtils.spawnAbilityItem(world, loc, Material.NETHERITE_SWORD, "Chaos Dagger", false, velocity, true, true);
		ScoreboardUtils.addEntityToTeam(dagger, "ChaosDagger", NamedTextColor.DARK_PURPLE);

		world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 2.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.0f, 0.7f);

		new BukkitRunnable() {

			int mExpire = 0;
			@Nullable LivingEntity mTarget;
			@Nullable Location mLastLocation = null;

			@Override
			public void run() {
				mExpire++;
				if (mExpire >= 10 * 20) {
					dagger.remove();
					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.addCooldown(mPlayer, ClassAbility.CHAOS_DAGGER, 0);
					this.cancel();
				}
				Location tLoc = dagger.getLocation();

				List<LivingEntity> veryNearbyMobs = EntityUtils.getNearbyMobs(tLoc, ELITE_RADIUS);
				veryNearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				veryNearbyMobs.removeIf(Entity::isInvulnerable);
				veryNearbyMobs.removeIf(mob -> !(EntityUtils.isBoss(mob) || EntityUtils.isElite(mob)));
				mTarget = EntityUtils.getNearestMob(tLoc, veryNearbyMobs);

				if (mTarget == null) {
					List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(tLoc, TARGET_RADIUS);
					nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
					nearbyMobs.removeIf(Entity::isInvulnerable);
					mTarget = EntityUtils.getNearestMob(tLoc, nearbyMobs);
				}

				if (mTarget == null) {
					dagger.remove();
					this.cancel();
					return;
				}

				BoundingBox targetBox = mTarget.getBoundingBox();
				if (mTarget instanceof Shulker) {
					targetBox.expand(0.3);
				}
				if (targetBox.overlaps(dagger.getBoundingBox())) {
					new PartialParticle(Particle.EXPLOSION_NORMAL, tLoc, 30, 2, 0, 2).spawnAsPlayerActive(mPlayer);
					world.playSound(tLoc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.3f, 1.4f);
					world.playSound(tLoc, Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 0.7f, 1.4f);
					world.playSound(tLoc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.5f);
					world.playSound(tLoc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
					world.playSound(tLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 0.1f);
					mHitMob = mTarget;
					if (EntityUtils.isBoss(mTarget)) {
						EntityUtils.applySlow(mPlugin, mStunDuration, 0.99f, mTarget);
					} else {
						EntityUtils.applyStun(mPlugin, mStunDuration, mTarget);
					}
					PotionUtils.applyColoredGlowing("ChaosDaggerGlowing", mTarget, NamedTextColor.DARK_PURPLE, DAMAGE_DURATION);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> mHitMob = null, DAMAGE_DURATION);

					dagger.remove();
					this.cancel();
				} else {
					new PartialParticle(Particle.SPELL_WITCH, tLoc, 5, 0.2, 0.2, 0.2, 0.65).spawnAsPlayerActive(mPlayer);

					Vector dir = tLoc.subtract(mTarget.getLocation().toVector().clone().add(new Vector(0, 0.5, 0))).toVector();

					if (dir.length() < 0.001) {
						/* If the direction magnitude is too small, escape, rather than divide by zero / infinity */
						dagger.remove();
						this.cancel();
						return;
					}

					dir = dir.normalize().multiply(VELOCITY * -1.0);

					if (mLastLocation != null && tLoc.distance(mLastLocation) < 0.05) {
						dir.setY(dir.getY() + 1.0);
					}

					dagger.setVelocity(dir);
					mLastLocation = tLoc;

				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (enemy == mHitMob && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)) {
			event.setDamage(event.getFlatDamage() * (1 + mDamageMultiplier));
			mHitMob = null;
			mPlugin.mEffectManager.clearEffects(enemy, "ChaosDaggerGlowing");
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (enemy.isDead() || enemy.getHealth() < 0) {
					AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration);
				}
			}, 1);

			Location loc = enemy.getLocation();
			World world = loc.getWorld();
			world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.3f, 2.0f);
			world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.5f);
			world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.7f, 0.1f);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 2.0f);
			world.playSound(loc, Sound.ENTITY_PHANTOM_BITE, SoundCategory.PLAYERS, 0.7f, 0.1f);
			world.playSound(loc, Sound.ENTITY_PHANTOM_BITE, SoundCategory.PLAYERS, 0.7f, 0.9f);
			world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.3f, 2.0f);

		}
		return false; // only changes event damage, and also prevents multiple calls itself by clearing mHitMob
	}

	private static Description<ChaosDagger> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ChaosDagger>(color)
			.add("Swap hands to throw a cursed dagger that stuns an enemy for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION)
			.add(" seconds (rooting bosses instead). The next time you deal non-ability melee or projectile damage within ")
			.addDuration(DAMAGE_DURATION)
			.add(" seconds, you deal ")
			.addPercent(a -> a.mDamageMultiplier, DAMAGE[rarity - 1], false, true)
			.add(" more damage multiplicatively. If this damage kills the target, gain stealth for ")
			.addDuration(a -> a.mStealthDuration, STEALTH_DURATION)
			.add(" seconds. The dagger prioritizes nearby Elites and Bosses but can hit any mob in its path.")
			.addCooldown(COOLDOWN);
	}


}

