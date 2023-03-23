package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PredatorStrikeCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class PredatorStrike extends Ability {

	private static final int COOLDOWN_1 = 20 * 18;
	private static final int COOLDOWN_2 = 20 * 14;
	private static final double DAMAGE_MULTIPLIER = 2.0;
	private static final double DISTANCE_SCALE_1 = 0.1;
	private static final double DISTANCE_SCALE_2 = 0.15;
	private static final int MAX_RANGE = 30;
	private static final int MAX_DAMAGE_RANGE = 12;
	private static final double EXPLODE_RADIUS = 1.25;
	private static final double HITBOX_LENGTH = 0.5;

	public static final String CHARM_COOLDOWN = "Predator Strike Cooldown";
	public static final String CHARM_DAMAGE = "Predator Strike Damage";
	public static final String CHARM_RADIUS = "Predator Strike Radius";

	public static final AbilityInfo<PredatorStrike> INFO =
		new AbilityInfo<>(PredatorStrike.class, "Predator Strike", PredatorStrike::new)
			.linkedSpell(ClassAbility.PREDATOR_STRIKE)
			.scoreboardId("PredatorStrike")
			.shorthandName("PrS")
			.descriptions(
				String.format("Left-clicking with a projectile weapon while not sneaking will prime a Predator Strike that unprimes after 5s. " +
					              "When you fire a critical projectile, it will instantaneously travel in a straight line " +
					              "for up to %d blocks or until it hits an enemy or block and damages enemies in a %s block radius. " +
					              "This ability deals %s%% of your projectile damage increased by %s%% for every block of distance from you and the target " +
					              "(up to %d blocks, or %s%% total). Cooldown: %ds.",
					MAX_RANGE, EXPLODE_RADIUS, StringUtils.multiplierToPercentage(DAMAGE_MULTIPLIER), StringUtils.multiplierToPercentage(DISTANCE_SCALE_1), MAX_DAMAGE_RANGE,
					StringUtils.multiplierToPercentage(MAX_DAMAGE_RANGE * DISTANCE_SCALE_1 + DAMAGE_MULTIPLIER), COOLDOWN_1 / 20),
				String.format("Damage now increases by %s%% for each block of distance (up to %s%% in total). Cooldown: %ds.", StringUtils.multiplierToPercentage(DISTANCE_SCALE_2),
					StringUtils.multiplierToPercentage(MAX_DAMAGE_RANGE * DISTANCE_SCALE_2 + DAMAGE_MULTIPLIER), COOLDOWN_2 / 20))
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", PredatorStrike::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.SPECTRAL_ARROW, 1));

	private boolean mActive = false;
	private final double mDistanceScale;
	private final double mExplodeRadius;

	private final PredatorStrikeCS mCosmetic;

	public PredatorStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDistanceScale = isLevelOne() ? DISTANCE_SCALE_1 : DISTANCE_SCALE_2;
		mExplodeRadius = CharmManager.getRadius(player, CHARM_RADIUS, EXPLODE_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PredatorStrikeCS(), PredatorStrikeCS.SKIN_LIST);
	}

	public void cast() {
		if (mActive || isOnCooldown()) {
			return;
		}
		mActive = true;
		ClientModHandler.updateAbility(mPlayer, this);
		mCosmetic.strikeSoundReady(mPlayer.getWorld(), mPlayer);
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mCosmetic.strikeTick(mPlayer, mTicks);

				if (!mActive || mTicks >= 20 * 5) {
					mActive = false;
					this.cancel();
					ClientModHandler.updateAbility(mPlayer, PredatorStrike.this);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!mActive || !EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			return true;
		}
		mActive = false;
		putOnCooldown();
		projectile.remove();
		mPlugin.mProjectileEffectTimers.removeEntity(projectile);

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
		BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		box.shift(direction);

		World world = mPlayer.getWorld();
		mCosmetic.strikeLaunch(world, mPlayer);

		Set<LivingEntity> nearbyMobs = new HashSet<>(EntityUtils.getNearbyMobs(loc, MAX_RANGE));

		boolean hit = false;
		for (double r = 0; r < MAX_RANGE; r += HITBOX_LENGTH) {
			Location bLoc = box.getCenter().toLocation(world);
			mCosmetic.strikeParticleProjectile(mPlayer, bLoc);

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(direction.multiply(0.5));
				mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, bLoc, mExplodeRadius), bLoc, mPlayer);
				explode(bLoc);
				hit = true;
				break;
			}

			for (LivingEntity mob : nearbyMobs) {
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isHostileMob(mob)) {
						mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, bLoc, mExplodeRadius), bLoc, mPlayer);
						explode(bLoc);
						hit = true;
						break;
					}
				}
			}

			if (hit) {
				break;
			}
			box.shift(shift);
		}
		if (!hit) {
			Location bLoc = box.getCenter().toLocation(world);
			if (mCosmetic instanceof FireworkStrikeCS) {
				mCosmetic.strikeImpact(() -> mCosmetic.strikeExplode(world, mPlayer, bLoc, mExplodeRadius), bLoc, mPlayer);
			}
		}
		return true;
	}

	private void explode(Location loc) {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mExplodeRadius);
		List<LivingEntity> mobs = hitbox.getHitMobs();
		if (!mobs.isEmpty()) {
			double damage = ItemStatUtils.getAttributeAmount(item, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND);
			damage += PointBlank.apply(mPlayer, loc, ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.POINT_BLANK));
			damage += Sniper.apply(mPlayer, loc, ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.SNIPER));
			damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
			damage *= DAMAGE_MULTIPLIER + mDistanceScale * Math.min(mPlayer.getLocation().distance(loc), MAX_DAMAGE_RANGE);

			for (LivingEntity mob : mobs) {
				MovementUtils.knockAway(loc, mob, 0.25f, 0.25f, true);
				DamageUtils.damage(mPlayer, mob, DamageType.PROJECTILE_SKILL, damage, mInfo.getLinkedSpell(), true);
			}
		}

		//Get enchant levels on weapon
		ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.FIRE_ASPECT);
		int fire = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.FIRE_ASPECT);
		int ice = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.ICE_ASPECT);
		int thunder = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.THUNDER_ASPECT);
		int decay = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.DECAY);
		int bleed = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.BLEEDING);
		int earth = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.EARTH_ASPECT);
		int wind = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.WIND_ASPECT);

		if (ice > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.3f);
			new PartialParticle(Particle.SNOW_SHOVEL, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius).spawnAsPlayerActive(mPlayer);
		}
		if (thunder > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f, 0.8f);
			new PartialParticle(Particle.REDSTONE, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f)).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f)).spawnAsPlayerActive(mPlayer);
		}
		if (decay > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.7f);
			new PartialParticle(Particle.SQUID_INK, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius).spawnAsPlayerActive(mPlayer);
		}
		if (bleed > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7f, 0.7f);
			new PartialParticle(Particle.REDSTONE, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f)).spawnAsPlayerActive(mPlayer);
		}
		if (wind > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.30f);
			mPlayer.getWorld().spawnParticle(Particle.CLOUD, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius);
		}
		if (earth > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.getWorld().spawnParticle(Particle.FALLING_DUST, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, Material.COARSE_DIRT.createBlockData());
			mPlayer.getWorld().spawnParticle(Particle.REDSTONE, loc, 12, mExplodeRadius, mExplodeRadius, mExplodeRadius, new Particle.DustOptions(Color.fromRGB(120, 148, 82), 0.75f));
		}
		if (fire > 0) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.6f, 0.9f);
			mPlayer.getWorld().spawnParticle(Particle.LAVA, loc, 25, mExplodeRadius, mExplodeRadius, mExplodeRadius);
		}
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}
