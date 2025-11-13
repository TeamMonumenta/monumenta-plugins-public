package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.AlchemicalArtilleryCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

public class AlchemicalArtillery extends Ability {
	private static final int COOLDOWN = 20 * 6;

	private static final double ARTILLERY_1_DAMAGE_MULTIPLIER = 0.75;
	private static final double ARTILLERY_2_DAMAGE_MULTIPLIER = 1.5;
	private static final double ARTILLERY_1_DAMAGE_RAW = 5;
	private static final double ARTILLERY_2_DAMAGE_RAW = 6.5;
	private static final double ARTILLERY_RANGE_MULTIPLIER = 1.5;
	private static final int ARTILLERY_POTION_COST = 2;
	private static final int AFTERSHOCK_DELAY = 20;
	private static final int AFTERSHOCK_COUNT = 1;
	private static final double AFTERSHOCK_DAMAGE_MULTIPLIER = 0.15;

	public static final String CHARM_COOLDOWN = "Alchemical Artillery Cooldown";
	public static final String CHARM_DAMAGE = "Alchemical Artillery Damage";
	public static final String CHARM_RADIUS = "Alchemical Artillery Radius";
	public static final String CHARM_VELOCITY = "Alchemical Artillery Velocity";
	public static final String CHARM_SIZE = "Alchemical Artillery Size";
	public static final String CHARM_AFTERSHOCK_DAMAGE = "Alchemical Artillery Aftershock Damage";
	public static final String CHARM_AFTERSHOCK_DELAY = "Alchemical Artillery Aftershock Delay";
	public static final String CHARM_AFTERSHOCK_COUNT = "Alchemical Artillery Aftershocks";
	public static final String CHARM_COST = "Alchemical Artillery Potion Cost";

	public static final AbilityInfo<AlchemicalArtillery> INFO =
		new AbilityInfo<>(AlchemicalArtillery.class, "Alchemical Artillery", AlchemicalArtillery::new)
			.linkedSpell(ClassAbility.ALCHEMICAL_ARTILLERY)
			.scoreboardId("Alchemical")
			.shorthandName("AA")
			.actionBarColor(TextColor.color(255, 0, 0))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Launch a bomb in the direction you're looking, which applies your selected potion's effects on impact.")
			.quest216Message("-------o-------b-------")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", AlchemicalArtillery::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.CROSSBOW);

	private final double mRadiusMult;
	private final double mDamageMult;
	private final double mDamageRaw;
	private final int mDelay;
	private final double mAftershockMult;
	private final double mAftershockCount;
	private final int mCost;

	private @Nullable AlchemistPotions mAlchemistPotions;

	private final AlchemicalArtilleryCS mCosmetic;

	public AlchemicalArtillery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadiusMult = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ARTILLERY_RANGE_MULTIPLIER);
		mDamageMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? ARTILLERY_1_DAMAGE_MULTIPLIER : ARTILLERY_2_DAMAGE_MULTIPLIER);
		mDamageRaw = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? ARTILLERY_1_DAMAGE_RAW : ARTILLERY_2_DAMAGE_RAW);
		mDelay = CharmManager.getDuration(mPlayer, CHARM_AFTERSHOCK_DELAY, AFTERSHOCK_DELAY);
		mAftershockMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_AFTERSHOCK_DAMAGE, AFTERSHOCK_DAMAGE_MULTIPLIER);
		mAftershockCount = AFTERSHOCK_COUNT + (int) CharmManager.getLevel(mPlayer, CHARM_AFTERSHOCK_COUNT);
		mCost = ARTILLERY_POTION_COST + (int) CharmManager.getLevel(mPlayer, CHARM_COST);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AlchemicalArtilleryCS());

		Bukkit.getScheduler().runTask(plugin,
			() -> mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class));
	}

	public boolean cast() {
		if (mAlchemistPotions == null || isOnCooldown()) {
			return false;
		}

		// Cast new grenade
		if (mAlchemistPotions.decrementCharges(mCost)) {
			putOnCooldown();
			Location loc = mPlayer.getEyeLocation();
			spawnGrenade(loc, mAlchemistPotions.isGruesomeMode());
		}
		return true;
	}

	private void spawnGrenade(Location loc, boolean isGruesome) {
		if (mAlchemistPotions == null) {
			return;
		}

		mCosmetic.onSpawn(loc.getWorld(), loc);
		double baseVelocityMultiplier = (mAlchemistPotions.getSpeed() - 1) / 2 + 1;
		double velocityMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, baseVelocityMultiplier);
		Vector vel = loc.getDirection().normalize().multiply(velocityMultiplier);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		Entity e = LibraryOfSoulsIntegration.summon(loc, "AlchemicalGrenade");
		if (e instanceof MagmaCube grenade) {
			grenade.setSize((int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SIZE, 2));

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
			EntityUtils.makeItemInvulnerable(physicsItem);

			new BukkitRunnable() {
				int mTicks = 0;
				final ItemStatManager.PlayerItemStats mPlayerItemStats = playerItemStats;
				private final MagmaCube mGrenade = grenade;
				private final Item mPhysicsItem = physicsItem;
				private final boolean mIsGruesome = isGruesome;

				@Override
				public void run() {
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
					if (!mGrenade.isValid() || mPhysicsItem.isOnGround() || mTicks > 120 || mGrenade.isInLava() || hasCollidedWithEnemy(mGrenade)) {
						explode(mGrenade.getLocation().subtract(0, 0.1, 0), mPlayerItemStats, mIsGruesome);
						mGrenade.remove();
						mPhysicsItem.remove();
						this.cancel();
						return;
					}

					mCosmetic.periodicEffects(mPlayer, mGrenade, mPhysicsItem, mTicks);

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private boolean hasCollidedWithEnemy(MagmaCube grenade) {
		Hitbox hitbox = new Hitbox.AABBHitbox(grenade.getWorld(), grenade.getBoundingBox());
		return !hitbox.getHitMobs().isEmpty();
	}

	private void explode(Location loc, ItemStatManager.PlayerItemStats playerItemStats, boolean isGruesome) {
		if (!mPlayer.isOnline() || mAlchemistPotions == null) {
			return;
		}

		double damage = mDamageRaw + mAlchemistPotions.getDamage(playerItemStats) * mDamageMult;
		double potionRadius = mAlchemistPotions.getRadius(playerItemStats);

		double radius = mRadiusMult * potionRadius;
		mCosmetic.explosionEffect(mPlayer, loc, radius);
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
		List<LivingEntity> mobs = hitbox.getHitMobs();

		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, true, false);
			applyEffects(mob, playerItemStats, isGruesome, false);

			if (!EntityUtils.isBoss(mob)) {
				MovementUtils.knockAwayRealistic(loc, mob, 1f, 0.5f, true);
			}
		}

		if (isEnhanced()) {
			aftershock(loc, radius, damage, playerItemStats, isGruesome);
		}
	}

	private void aftershock(Location loc, double radius, double damage, ItemStatManager.PlayerItemStats playerItemStats, boolean isGruesome) {
		double finalDamage = damage * mAftershockMult;
		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				Hitbox hitbox = new Hitbox.SphereHitbox(loc, radius);
				List<LivingEntity> mobs = hitbox.getHitMobs();

				mCosmetic.aftershockEffect(mPlayer, loc, radius, mobs);

				for (LivingEntity mob : mobs) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, ClassAbility.ALCHEMICAL_ARTILLERY_AFTERSHOCK, playerItemStats), finalDamage, true, false, false);
					applyEffects(mob, playerItemStats, isGruesome, true);
				}

				mCount++;
				if (mCount >= mAftershockCount) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, mDelay, mDelay);
	}

	private void applyEffects(LivingEntity entity, ItemStatManager.PlayerItemStats playerItemStats, boolean isGruesome, boolean invert) {
		if (mAlchemistPotions == null) {
			return;
		}

		boolean isGruesomeFinal = isGruesome ^ invert;

		// Return if isGruesome and doesn't have gruesome selected.
		GruesomeAlchemy gruesome = Plugin.getInstance().mAbilityManager.getPlayerAbilities(mPlayer).getAbilityIgnoringSilence(GruesomeAlchemy.class);
		if (isGruesomeFinal && gruesome == null) {
			return;
		}

		mAlchemistPotions.applyEffects(entity, isGruesomeFinal, playerItemStats);
	}

	private static Description<AlchemicalArtillery> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to launch a heavy bomb that explodes on contact with the ground, lava, or a hostile, or after 6 seconds, dealing ")
			.add(a -> a.mDamageRaw, ARTILLERY_1_DAMAGE_RAW, false, Ability::isLevelOne)
			.add(" + ")
			.addPercent(a -> a.mDamageMult, ARTILLERY_1_DAMAGE_MULTIPLIER, false, Ability::isLevelOne)
			.add(" of your potion's damage and applying your selected potion's effects, in an area that is ")
			.addPercent(a -> a.mRadiusMult, ARTILLERY_RANGE_MULTIPLIER)
			.add(" of your potion's radius. The initial speed of the bomb scales with your projectile speed. This costs you ")
			.add(a -> a.mCost, ARTILLERY_POTION_COST, true)
			.add(" potions.")
			.addCooldown(COOLDOWN);
	}

	private static Description<AlchemicalArtillery> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The damage of the bomb is increased to ")
			.add(a -> a.mDamageRaw, ARTILLERY_2_DAMAGE_RAW, false, Ability::isLevelTwo)
			.add(" + ")
			.addPercent(a -> a.mDamageMult, ARTILLERY_2_DAMAGE_MULTIPLIER, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<AlchemicalArtillery> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Add ")
			.add(a -> a.mAftershockCount, AFTERSHOCK_COUNT)
			.add(" aftershock to the explosion, which happens ")
			.addDuration(a -> a.mDelay, AFTERSHOCK_DELAY, true)
			.add(" second after each explosion or aftershock, and deals ")
			.addPercent(a -> a.mAftershockMult, AFTERSHOCK_DAMAGE_MULTIPLIER)
			.add(" of the original explosion damage. If possible, the aftershock also applies the potion effect opposite of the one that you have selected.");
	}
}
