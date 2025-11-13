package com.playmonumenta.plugins.depths.abilities.flamecaller;

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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Fireball extends DepthsAbility {

	public static final String ABILITY_NAME = "Fireball";
	private static final int COOLDOWN = 6 * 20;
	private static final double VELOCITY = 1.2;
	private static final int[] DAMAGE = {11, 13, 15, 17, 19, 23};
	private static final int RADIUS = 3;
	private static final int FIRE_TICKS = 3 * 20;

	private static final Color TIP_COLOR = Color.fromRGB(240, 43, 43);
	private static final Color TIP_COLOR_TRANSITION = Color.fromRGB(222, 38, 13);

	private static final Color BASE_COLOR = Color.fromRGB(255, 98, 41);
	private static final Color BASE_COLOR_TRANSITION = Color.fromRGB(219, 99, 0);

	public static final String CHARM_COOLDOWN = "Fireball Cooldown";

	public static final DepthsAbilityInfo<Fireball> INFO =
		new DepthsAbilityInfo<>(Fireball.class, ABILITY_NAME, Fireball::new, DepthsTree.FLAMECALLER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.FIREBALL)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Fireball::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.FIREWORK_STAR)
			.descriptions(Fireball::getDescription);

	private final double mRadius;
	private final double mVelocity;
	private final double mDamage;
	private final int mFireDuration;

	public Fireball(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FIREBALL_RADIUS.mEffectName, RADIUS);
		mVelocity = CharmManager.getRadius(mPlayer, CharmEffects.FIREBALL_VELOCITY.mEffectName, VELOCITY);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FIREBALL_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.FIREBALL_FIRE_DURATION.mEffectName, FIRE_TICKS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		Location loc = mPlayer.getEyeLocation();

		new PartialParticle(Particle.FLAME, mPlayer.getLocation().add(0, 1, 0), 15, 0, 0, 0, 0.075f).spawnAsPlayerActive(mPlayer);
		spawnGrenade(loc);
		return true;
	}

	private void spawnGrenade(Location loc) {

		loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, 0.75f);

		Vector vel = loc.getDirection().normalize().multiply(mVelocity);

		// Adjust the Y velocity to make the arc easier to calculate and use
		double velY = vel.getY();
		if (velY > 0 && velY < 0.2) {
			vel.setY(0.2);
		}

		Item physicsItem = (Item) loc.getWorld().spawnEntity(loc, EntityType.DROPPED_ITEM);
		ItemStack itemStack = new ItemStack(Material.MAGMA_BLOCK);
		itemStack.setAmount(1);
		physicsItem.setItemStack(itemStack);
		physicsItem.setCanPlayerPickup(false);
		physicsItem.setCanMobPickup(false);
		physicsItem.setVelocity(vel);
		EntityUtils.makeItemInvulnerable(physicsItem);

		new BukkitRunnable() {
			int mTicks = 0;
			float mPercent = 0;
			private final Item mPhysicsItem = physicsItem;

			@Override
			public void run() {
				if (!mPlayer.isOnline()) {
					mPhysicsItem.remove();
					this.cancel();
					return;
				}
				Location itemLoc = mPhysicsItem.getLocation().clone().add(0, 0.2, 0);
				itemLoc.add(mPhysicsItem.getVelocity().multiply(0.5));
				ParticleUtils.drawSphere(itemLoc, 7, 0.325, (loc1, t) ->
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc1, 1, 0.1, 0.1, 0.1, 0,
						new Particle.DustTransition(
							ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, mPercent),
							ParticleUtils.getTransition(BASE_COLOR_TRANSITION, TIP_COLOR_TRANSITION, mPercent),
							0.75f
						)).spawnAsPlayerActive(mPlayer));

				new PartialParticle(Particle.SMOKE_NORMAL, mPhysicsItem.getLocation().add(0, 0.2, 0), 2, 0.1, 0.1, 0.1, 0.05).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMALL_FLAME, mPhysicsItem.getLocation().add(0, 0.2, 0), 3, 0.1, 0.1, 0.1, 0.05).spawnAsPlayerActive(mPlayer);

				itemLoc.getWorld().playSound(mPhysicsItem.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.65f, 1.5f);
				// Explosion conditions
				// - If the physics item hit the ground
				// - If the physics item hit lava
				// - If the grenade has collided with any enemy
				// - If 6 seconds have passed (probably stuck in webs)
				if (mPhysicsItem.isOnGround() || mTicks > 120 || mPhysicsItem.isInLava() || hasCollidedWithEnemy(mPhysicsItem) || LocationUtils.collidesWithBlocks(BoundingBox.of(physicsItem.getLocation(), 0.25, 0.25, 0.25), mPhysicsItem.getWorld())) {
					explode(mPhysicsItem.getLocation().subtract(0, 0.1, 0));
					mPhysicsItem.remove();
					this.cancel();
					return;
				}

				mTicks++;
				if (mPercent < 1) {
					mPercent = (float) (mPercent + 0.05);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

	}

	private boolean hasCollidedWithEnemy(Item physicsItem) {
		Hitbox hitbox = new Hitbox.AABBHitbox(physicsItem.getWorld(), BoundingBox.of(physicsItem.getLocation(), 1.025, 1.025, 1.025));
		return !hitbox.getHitMobs().isEmpty();
	}

	private void explode(Location loc) {
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
		List<LivingEntity> mobs = hitbox.getHitMobs();

		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc.add(0, 0.2, 0), 0, 1, 0, 0, 15, 0.24f,
			true, 0, Particle.SMOKE_LARGE);

		new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 20, 0, 0, 0, 0.02 * mRadius).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.LAVA, loc, 40, mRadius / 4, 0, mRadius / 4, 0.04 * mRadius).spawnAsPlayerActive(mPlayer);

		if (loc.distance(mPlayer.getLocation()) >= 12) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.5f);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.5f, 0.75f);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.4f, 0.75f);
		}

		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 1.5f);
		loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, 0.75f);
		loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.8f, 0.75f);

		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, true);
			EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer);
		}
	}

	private static Description<Fireball> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to throw a fireball in the direction you are facing. When the fireball collides with a surface or enemy, it explodes, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage and sets enemies ablaze for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius.")
			.addCooldown(COOLDOWN);
	}
}
