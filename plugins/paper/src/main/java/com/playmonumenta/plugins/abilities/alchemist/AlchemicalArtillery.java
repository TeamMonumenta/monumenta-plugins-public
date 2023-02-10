package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AlchemicalArtillery extends PotionAbility {
	private static final int COOLDOWN = 20 * 6;

	private static final double ARTILLERY_1_DAMAGE_MULTIPLIER = 1.5;
	private static final double ARTILLERY_2_DAMAGE_MULTIPLIER = 2.5;
	private static final double ARTILLERY_RANGE_MULTIPLIER = 1.5;
	private static final int ARTILLERY_POTION_COST = 2;
	private static final int ENHANCEMENT_EXPLOSION_DELAY = 20;
	private static final double ENHANCEMENT_EXPLOSION_DAMAGE_MULTIPLIER = 0.1;
	public static final String CHARM_DELAY = "Alchemical Artillery Delay";
	public static final String CHARM_RADIUS = "Alchemical Artillery Radius";
	public static final String CHARM_KNOCKBACK = "Alchemical Artillery Knockback";
	public static final String CHARM_EXPLOSION_MULTIPLIER = "Alchemical Artillery Explosion Damage Multiplier";

	public static final AbilityInfo<AlchemicalArtillery> INFO =
		new AbilityInfo<>(AlchemicalArtillery.class, "Alchemical Artillery", AlchemicalArtillery::new)
			.linkedSpell(ClassAbility.ALCHEMICAL_ARTILLERY)
			.scoreboardId("Alchemical")
			.shorthandName("AA")
			.descriptions(
				("Pressing the Drop Key while holding an Alchemist Bag and not sneaking launches a heavy bomb that " +
				"explodes on contact with the ground, lava, or a hostile, or after 6 seconds, dealing %s%% of your " +
				"potion's damage, in an area that is %s%% of your potion's radius. The initial speed of the bomb " +
				"scales with your projectile speed. This costs you %s potions. %ss cooldown.")
					.formatted(
							StringUtils.multiplierToPercentage(ARTILLERY_1_DAMAGE_MULTIPLIER),
							StringUtils.multiplierToPercentage(ARTILLERY_RANGE_MULTIPLIER),
							ARTILLERY_POTION_COST,
							StringUtils.ticksToSeconds(COOLDOWN)
					),
				"The damage of the bomb is increased to %s%%"
					.formatted(StringUtils.multiplierToPercentage(ARTILLERY_2_DAMAGE_MULTIPLIER)),
				("%ss after the bomb lands, a secondary explosion deals %s%% of the bomb's damage and applies " +
				"the effects of your currently selected potion type.")
					.formatted(
							StringUtils.ticksToSeconds(ENHANCEMENT_EXPLOSION_DELAY),
							StringUtils.multiplierToPercentage(ENHANCEMENT_EXPLOSION_DAMAGE_MULTIPLIER)
					)
			)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AlchemicalArtillery::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(new ItemStack(Material.CROSSBOW, 1));

	private @Nullable AlchemistPotions mAlchemistPotions;

	public AlchemicalArtillery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void cast() {
		if (mAlchemistPotions == null || isOnCooldown()) {
			return;
		}

		// Cast new grenade
		if (mAlchemistPotions.decrementCharges(ARTILLERY_POTION_COST)) {
			putOnCooldown();
			Location loc = mPlayer.getEyeLocation();
			spawnGrenade(loc);
		}
	}

	private void spawnGrenade(Location loc) {
		if (mAlchemistPotions == null) {
			return;
		}

		loc.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 0.5f, 0.5f);
		Vector vel = loc.getDirection().normalize().multiply((mAlchemistPotions.getSpeed() - 1) / 2 + 1);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Entity e = LibraryOfSoulsIntegration.summon(loc, "AlchemicalGrenade");
		if (e instanceof MagmaCube grenade) {
			// Adjust the Y velocity to make the arc easier to calculate and use
			double velY = vel.getY();
			if (velY > 0 && velY < 0.2) {
				vel.setY(0.2);
			}

			// Mount the grenade on a dropped item to use its physics instead
			Item physicsItem = (Item) loc.getWorld().spawnEntity(loc, EntityType.DROPPED_ITEM);
			ItemStack itemStack = new ItemStack(Material.GUNPOWDER);
			itemStack.setAmount(1);
			physicsItem.setItemStack(itemStack);
			physicsItem.setCanPlayerPickup(false);
			physicsItem.setCanMobPickup(false);
			physicsItem.setVelocity(vel);
			physicsItem.addPassenger(grenade);
			physicsItem.setInvulnerable(true);

			new BukkitRunnable() {
				int mTicks = 0;
				ItemStatManager.PlayerItemStats mPlayerItemStats = playerItemStats;
				private MagmaCube mGrenade = grenade;
				private Item mPhysicsItem = physicsItem;
				@Override
				public void run() {
					if (mGrenade == null) {
						this.cancel();
						return;
					}

					if (!mPlayer.isOnline()) {
						mGrenade.remove();
						mPhysicsItem.remove();
						this.cancel();
						return;
					}

					// Explosion conditions
					// - If the grenade somehow died
					// - If the physics item hit the ground
					// - If the grenade hit lava
					// - If the grenade has collided with any enemy
					// - If 6 seconds have passed (probably stuck in webs)
					if (mGrenade.isDead() || mPhysicsItem.isOnGround() || mTicks > 120 || mGrenade.isInLava() || hasCollidedWithEnemy(mGrenade)) {
						explode(mGrenade.getLocation().subtract(0, 0.1, 0), mPlayerItemStats);
						mGrenade.remove();
						mPhysicsItem.remove();
						this.cancel();
						return;
					}

					Location particleLoc = mGrenade.getLocation().add(0, 1, 0);
					new PartialParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SMOKE_LARGE, particleLoc, 2, 0, 0, 0, 0.05).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.FLAME, particleLoc, 3, 0, 0, 0, 0.05).spawnAsPlayerActive(mPlayer);

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private boolean hasCollidedWithEnemy(MagmaCube grenade) {
		Hitbox hitbox = new Hitbox.AABBHitbox(grenade.getWorld(), grenade.getBoundingBox());
		if (hitbox.getHitMobs().size() > 0) {
			return true;
		}
		return false;
	}

	private void explode(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		if (!mPlayer.isOnline() || mAlchemistPotions == null) {
			return;
		}

		double potionDamage = mAlchemistPotions.getDamage(playerItemStats);
		double potionRadius = mAlchemistPotions.getRadius(playerItemStats);

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, potionRadius * ARTILLERY_RANGE_MULTIPLIER);
		explosionEffect(loc, radius);
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
		List<LivingEntity> mobs = hitbox.getHitMobs();

		double finalDamage = potionDamage * (isLevelOne() ? ARTILLERY_1_DAMAGE_MULTIPLIER : ARTILLERY_2_DAMAGE_MULTIPLIER);

		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), finalDamage, true, true, false);

			if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAwayRealistic(loc, mob, 1f, 0.5f, true);
			}
		}

		if (isEnhanced()) {
			new BukkitRunnable() {
				Location mLoc = loc;
				double mRadius = radius;
				double mDamage = finalDamage;
				private @Nullable ItemStatManager.PlayerItemStats mCastPlayerItemStats = playerItemStats;

				@Override
				public void run() {
					Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, mRadius);
					List<LivingEntity> mobs = hitbox.getHitMobs();
					World world = mLoc.getWorld();

					if (mobs.size() == 0) {
						world.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 1.75f);
						new PartialParticle(Particle.EXPLOSION_LARGE, mLoc, 5, 0.2, 0.2, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
						return;
					}

					for (LivingEntity mob : mobs) {
						DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), mCastPlayerItemStats), mDamage * 0.1, true, true, false);
						applyEffects(mob, playerItemStats);
						new PartialParticle(Particle.BLOCK_CRACK, mob.getLocation(), 25, 0, 0, 0, 1, world.getBlockData(mob.getLocation().clone().subtract(0, 1, 0))).spawnAsPlayerActive(mPlayer);

						Location mobLoc = mob.getLocation();
						new PartialParticle(Particle.EXPLOSION_LARGE, mobLoc, 5, 0.2, 0.2, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(mobLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 1.75f);
					}
				}
			}.runTaskLater(mPlugin, 20);
		}
	}

	private void explosionEffect(Location loc, double radius) {
		ParticleUtils.explodingRingEffect(mPlugin, loc, radius, 1, 10,
			Arrays.asList(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> {
					new PartialParticle(Particle.FLAME, location, 1, 0, 0, 0, 0.0025).spawnAsPlayerActive(mPlayer);
				})
			)
		);
		ParticleUtils.drawRing(loc, 45, new Vector(0, 1, 0), radius,
			(loc1, t) -> new PartialParticle(Particle.REDSTONE, loc1, 1, 0, 0, 0, 0.0025, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f)).spawnAsPlayerActive(mPlayer)
		);
		new PartialParticle(Particle.FLAME, loc, 100, radius/2, 0, radius/2, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1.5f, 2f);
	}

	private void applyEffects(LivingEntity entity, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return;
		}
		mAlchemistPotions.applyEffects(entity, mAlchemistPotions.isGruesomeMode(), playerItemStats);
	}
}
