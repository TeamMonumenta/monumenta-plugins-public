package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class UnstableAmalgam extends Ability {

	private static final int UNSTABLE_AMALGAM_1_COOLDOWN = 20 * 20;
	private static final int UNSTABLE_AMALGAM_2_COOLDOWN = 16 * 20;
	private static final int UNSTABLE_AMALGAM_1_DAMAGE = 10;
	private static final int UNSTABLE_AMALGAM_2_DAMAGE = 15;
	private static final int UNSTABLE_AMALGAM_CAST_RANGE = 7;
	private static final int UNSTABLE_AMALGAM_DURATION = 3 * 20;
	private static final int UNSTABLE_AMALGAM_RADIUS = 4;
	private static final float UNSTABLE_AMALGAM_KNOCKBACK_SPEED = 2.5f;
	private static final int UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION = 20 * 8;
	private static final double UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE = 0.4;

	public static final String CHARM_COOLDOWN = "Unstable Amalgam Cooldown";
	public static final String CHARM_DAMAGE = "Unstable Amalgam Damage";
	public static final String CHARM_RANGE = "Unstable Amalgam Cast Range";
	public static final String CHARM_RADIUS = "Unstable Amalgam Radius";
	public static final String CHARM_DURATION = "Unstable Amalgam Duration";
	public static final String CHARM_KNOCKBACK = "Unstable Amalgam Knockback Speed";
	public static final String CHARM_INSTABILITY_DURATION = "Unstable Amalgam Instability Duration";
	public static final String CHARM_POTION_DAMAGE = "Unstable Amalgam Dropped Potion Damage Modifier";

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable Slime mAmalgam;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
	private final double mDamage;
	private final Map<ThrownPotion, ItemStatManager.PlayerItemStats> mEnhancementPotionPlayerStat;

	public UnstableAmalgam(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Unstable Amalgam");
		mInfo.mLinkedSpell = ClassAbility.UNSTABLE_AMALGAM;
		mInfo.mScoreboardId = "UnstableAmalgam";
		mInfo.mShorthandName = "UA";
		mInfo.mDescriptions.add("Shift left click while holding an Alchemist's Bag to consume a potion to place an Amalgam with 1 health at the location you are looking, up to 7 blocks away. Shift left click again to detonate it, dealing your Alchemist Potion's damage + 10 magic damage to mobs in a 4 block radius and applying potion effects from all abilities. The Amalgam also explodes when killed, or 3 seconds after being placed. Mobs and players in the radius are knocked away from the Amalgam. For each mob damaged, gain an Alchemist's Potion. Cooldown: 20s.");
		mInfo.mDescriptions.add("The damage is increased to 15 and the cooldown is reduced to 16s.");
		mInfo.mDescriptions.add("Enemies hit by the Amalgam's explosion become unstable. When an unstable mob is killed, a potion that deals 40% of your potion damage is dropped at its location. These potions apply both Brutal and Gruesome effects.");
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, isLevelOne() ? UNSTABLE_AMALGAM_1_COOLDOWN : UNSTABLE_AMALGAM_2_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.GUNPOWDER, 1);

		mAmalgam = null;
		mEnhancementPotionPlayerStat = new HashMap<>();

		mDamage = isLevelOne() ? UNSTABLE_AMALGAM_1_DAMAGE : UNSTABLE_AMALGAM_2_DAMAGE;

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void cast(Action action) {
		// cast preconditions
		if (mPlayer == null || mAlchemistPotions == null || !mPlayer.isSneaking() || !ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())) {
			return;
		}
		// prevent double casts in the same tick (while normal cast has this check, this ability is also cast on melee damage, which bypasses that check)
		if (!MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "UnstableAmalgamCast")) {
			return;
		}

		// explode existing amalgam
		if (mAmalgam != null) {
			explode(mAmalgam.getLocation());
			mAmalgam.remove();
			mAmalgam = null;
			return;
		}
		if (isTimerActive()) {
			return;
		}

		// cast new amalgam
		if (mAlchemistPotions.decrementCharge()) {
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			double step = 0.125;
			Vector dir = loc.getDirection().normalize().multiply(step);
			double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, UNSTABLE_AMALGAM_CAST_RANGE);
			for (double i = 0; i < range; i += step) {
				loc.add(dir);

				if (NmsUtils.getVersionAdapter().hasCollision(loc.getWorld(), BoundingBox.of(loc, 0.21, 0.21, 0.21))) {
					loc.subtract(dir);
					spawnAmalgam(loc);

					return;
				}
			}

			spawnAmalgam(loc);
		}
	}

	private void spawnAmalgam(Location loc) {
		if (mPlayer == null || mAlchemistPotions == null) {
			return;
		}

		mPlayerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		int duration = UNSTABLE_AMALGAM_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

		World world = loc.getWorld();
		Entity e = LibraryOfSoulsIntegration.summon(loc, "UnstableAmalgam");
		if (e instanceof Slime amalgam) {
			mAmalgam = amalgam;

			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mAmalgam == null) {
						this.cancel();
						return;
					}

					if (mPlayer == null || !mPlayer.isOnline()) {
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					}

					if (mAmalgam.isDead() || mTicks >= duration) {
						explode(mAmalgam.getLocation());
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					} else if (mTicks % (duration / 3) == 0) {
						new PartialParticle(Particle.FLAME, loc, 20, 0.02, 0.02, 0.02, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.7f);
					}

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void explode(Location loc) {
		if (mPlayer == null || !mPlayer.isOnline() || mAlchemistPotions == null) {
			return;
		}

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, UNSTABLE_AMALGAM_RADIUS);
		float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, UNSTABLE_AMALGAM_KNOCKBACK_SPEED);
		float playerVertical = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, 2);

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
		List<LivingEntity> mobs = hitbox.getHitMobs(mAmalgam);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mDamage + mAlchemistPotions.getDamage());
		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, mPlayerItemStats), damage, true, true, false);

			mAlchemistPotions.applyEffects(mob, false);
			mAlchemistPotions.applyEffects(mob, true);

			MovementUtils.knockAwayRealistic(loc, mob, knockback, 2f, true);
			mAlchemistPotions.incrementCharge();
		}

		if (isEnhanced()) {
			int duration = UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_INSTABILITY_DURATION);

			if (!mobs.isEmpty()) {
				unstableMobs(mobs, duration);
			}
		}

		if (!ZoneUtils.hasZoneProperty(loc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			for (Player player : hitbox.getHitPlayers(true)) {
				if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					if (!player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, "RocketJumper").orElse(0) == 100) {
						MovementUtils.knockAwayRealistic(loc, player, knockback, playerVertical, false);
					} else if (player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, "RocketJumper").orElse(1) > 0) {
						// by default any Alch can use Rocket Jump with their UA
						MovementUtils.knockAwayRealistic(loc, player, knockback, playerVertical, false);
					}
				}
			}
		}

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.25f);
		new PartialParticle(Particle.FLAME, loc, 115, 0.02, 0.02, 0.02, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 40, 0.02, 0.02, 0.02, 0.35).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 40, 0.02, 0.02, 0.02, 0.35).spawnAsPlayerActive(mPlayer);
	}


	private void unstableMobs(List<LivingEntity> mobs, int duration) {
		if (mPlayer == null || mAlchemistPotions == null) {
			return;
		}

		new BukkitRunnable() {
			int mTimes = 0;
			@Override public void run() {
				mTimes++;

				for (LivingEntity mob : new ArrayList<>(mobs)) {
					new PartialParticle(Particle.REDSTONE, mob.getEyeLocation(), 12, 0.5, 0.5, 0.5, 0.3, new Particle.DustOptions(Color.WHITE, 0.8f)).spawnAsPlayerActive(mPlayer);

					if (mob.isDead()) {
						mobs.remove(mob);
						ThrownPotion potion = mPlayer.launchProjectile(ThrownPotion.class);
						potion.teleport(mob.getEyeLocation());
						potion.setVelocity(new Vector(0, -1, 0));
						setEnhancementThrownPotion(potion, mPlayerItemStats);
					}
				}

				if (mobs.isEmpty() || mTimes >= duration || !mPlayer.isValid()) {
					cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void setEnhancementThrownPotion(ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		mEnhancementPotionPlayerStat.put(potion, playerItemStats);
		mAlchemistPotions.setPotionAlchemistPotionAesthetic(potion);
	}


	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (mAlchemistPotions == null) {
			return true;
		}
		ItemStatManager.PlayerItemStats stats = mEnhancementPotionPlayerStat.remove(potion);
		if (isEnhanced() && stats != null) {
			Location loc = potion.getLocation();

			double damage = mAlchemistPotions.getDamage() * (UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_POTION_DAMAGE));
			for (LivingEntity entity : new Hitbox.SphereHitbox(loc, mAlchemistPotions.getPotionRadius()).getHitMobs()) {
				DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, stats), damage, true, true, false);
				mAlchemistPotions.applyEffects(entity, true);
				mAlchemistPotions.applyEffects(entity, false);
			}
		}

		mEnhancementPotionPlayerStat.keySet().removeIf(pot -> pot.isDead() || !pot.isValid());
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
