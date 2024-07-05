package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.WindBombCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindBomb extends Ability {

	private static final int DURATION = 20 * 4;
	private static final double WEAKEN_EFFECT = 0.2;
	private static final double LAUNCH_VELOCITY = 1.2;
	private static final int SLOW_FALL_EFFECT = 0;
	private static final int COOLDOWN_1 = 20 * 15;
	private static final int COOLDOWN_2 = 20 * 10;
	private static final double DAMAGE_FRACTION_1 = 0.4;
	private static final double DAMAGE_FRACTION_2 = 0.5;
	private static final double MIDAIR_DAMAGE_BONUS = 0.2;
	private static final int RADIUS = 3;
	private static final double VELOCITY = 1.5;

	private static final int PULL_INTERVAL = 10;
	private static final double PULL_VELOCITY = 0.35;
	private static final double PULL_RADIUS = 10;
	private static final int PULL_DURATION = 3 * 20;
	private static final double PULL_RATIO = 0.12;

	public static final String CHARM_DURATION = "Wind Bomb Duration";
	public static final String CHARM_WEAKNESS = "Wind Bomb Weaken Amplifier";
	public static final String CHARM_COOLDOWN = "Wind Bomb Cooldown";
	public static final String CHARM_DAMAGE = "Wind Bomb Damage";
	public static final String CHARM_DAMAGE_MODIFIER = "Wind Bomb Damage Modifier";
	public static final String CHARM_RADIUS = "Wind Bomb Radius";
	public static final String CHARM_HEIGHT = "Wind Bomb Height";
	public static final String CHARM_PULL = "Wind Bomb Vortex Pull";
	public static final String CHARM_VORTEX_DURATION = "Wind Bomb Vortex Duration";
	public static final String CHARM_VORTEX_RADIUS = "Wind Bomb Vortex Radius";

	public static final AbilityInfo<WindBomb> INFO =
		new AbilityInfo<>(WindBomb.class, "Wind Bomb", WindBomb::new)
			.linkedSpell(ClassAbility.WIND_BOMB)
			.scoreboardId("WindBomb")
			.shorthandName("WB")
			.descriptions(
				String.format("Press the swap key while sneaking and holding a projectile weapon to throw a projectile that, " +
					              "upon contact with the ground or an enemy, deals %s%% of your projectile damage to mobs in a %d block radius and launches them into the air, " +
					              "giving them Slow Falling and %d%% Weaken for %ds. Cooldown: %ds.",
					(int) (DAMAGE_FRACTION_1 * 100), RADIUS, (int) (WEAKEN_EFFECT * 100), DURATION / 20, COOLDOWN_1 / 20),
				String.format("The damage is increased to %s%% of your projectile damage and the cooldown is reduced to %ds. " +
					              "Additionally, you passively deal %d%% more damage to airborne enemies.",
					(int) (DAMAGE_FRACTION_2 * 100), COOLDOWN_2 / 20, (int) (MIDAIR_DAMAGE_BONUS * 100)),
				String.format("On impact, generate a vortex that pulls mobs within %s blocks toward the center for %d seconds.", (int) PULL_RADIUS, PULL_DURATION / 20))
			.simpleDescription("Throw a bomb that damages and launches mobs up in the air, weakening them.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindBomb::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.TNT);

	private final double mDamageFraction;

	private final List<Triple<Snowball, Double, ItemStatManager.PlayerItemStats>> mProjectiles = new ArrayList<>();

	private final WindBombCS mCosmetic;

	public WindBomb(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageFraction = isLevelOne() ? DAMAGE_FRACTION_1 : DAMAGE_FRACTION_2;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WindBombCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		mCosmetic.onThrow(world, loc);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, mCosmetic.getProjectileName(), mCosmetic.getProjectileParticle());

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		double damage = ItemStatUtils.getAttributeAmount(mainhand, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
		damage *= mDamageFraction;
		damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.add(Triple.of(proj, damage, playerItemStats));
		putOnCooldown();

		// Clear out list just in case
		mProjectiles.removeIf(triple -> triple.getLeft().isDead() || !triple.getLeft().isValid());

		return true;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		Triple<Snowball, Double, ItemStatManager.PlayerItemStats> triple = null;
		for (Triple<Snowball, Double, ItemStatManager.PlayerItemStats> testTriple : mProjectiles) {
			if (testTriple.getLeft() == proj) {
				triple = testTriple;
				break;
			}
		}

		if (triple != null) {
			event.setCancelled(true);
			mProjectiles.remove(triple);
			double damage = triple.getMiddle();
			ItemStatManager.PlayerItemStats playerItemStats = triple.getRight();

			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
			Location loc = proj.getLocation();
			World world = proj.getWorld();
			mCosmetic.onLand(mPlugin, mPlayer, world, loc, radius);

			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
			double weaken = WEAKEN_EFFECT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
			// Velocity scales with the square root of the maximum height
			double velocityMultSquared = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, 1);
			float velocity = (float) (LAUNCH_VELOCITY * Math.sqrt(velocityMultSquared));
			Hitbox damageHitbox = new Hitbox.SphereHitbox(loc, radius);
			List<LivingEntity> launchedMobs = damageHitbox.getHitMobs();
			for (LivingEntity mob : launchedMobs) {
				if (damage > 0) {
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE_SKILL, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
				}
				if (!EntityUtils.isBoss(mob)) {
					mob.setVelocity(new Vector(0.f, velocity, 0.f));
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, duration, SLOW_FALL_EFFECT, true, false));
				}
				EntityUtils.applyWeaken(mPlugin, duration, weaken, mob);
			}

			if (isEnhanced()) {
				loc.add(0, 2, 0);
				mCosmetic.onVortexSpawn(mPlayer, world, loc);

				double pullVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_VELOCITY);
				double pullRadius = CharmManager.getRadius(mPlayer, CHARM_VORTEX_RADIUS, PULL_RADIUS);
				int pullDuration = CharmManager.getDuration(mPlayer, CHARM_VORTEX_DURATION, PULL_DURATION);
				Hitbox pullHitbox = new Hitbox.SphereHitbox(loc, pullRadius);

				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						mTicks++;
						if (mTicks % PULL_INTERVAL == 0) {
							for (LivingEntity mob : pullHitbox.getHitMobs()) {
								if (!EntityUtils.isCCImmuneMob(mob) || ZoneUtils.hasZoneProperty(mob.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
									Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
									double ratio = PULL_RATIO + vector.length() / pullRadius;
									Vector velocity = mob.getVelocity().add(vector.normalize().multiply(pullVelocity).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0)));
									if (launchedMobs.contains(mob)) {
										// If mob was launched by the ability, don't change their Y
										velocity.setY(mob.getVelocity().getY());
									}
									mob.setVelocity(velocity);
								}
							}
						}
						mCosmetic.onVortexTick(mPlayer, loc);
						if (mTicks >= pullDuration) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			proj.remove();
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (isLevelTwo() && LocationUtils.isAirborne(enemy) && event.getType() != DamageEvent.DamageType.TRUE) {
			event.updateDamageWithMultiplier(1 + MIDAIR_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MODIFIER));
		}
		return false;
	}
}
