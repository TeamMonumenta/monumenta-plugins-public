package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.WindBombAirTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
	private static final String AIR_TAG = "WindBombAirTag";

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
					              "Additionally, you deal %d%% more damage to enemies affected by this skill for 4 seconds.",
					(int) (DAMAGE_FRACTION_2 * 100), COOLDOWN_2 / 20, (int) (MIDAIR_DAMAGE_BONUS * 100)),
				String.format("On impact, generate a vortex that pulls mobs within %s blocks toward the center for %d seconds.", (int) PULL_RADIUS, PULL_DURATION / 20))
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindBomb::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.TNT, 1));

	private final double mDamageFraction;

	private final List<Triple<Snowball, Double, ItemStatManager.PlayerItemStats>> mProjectiles = new ArrayList<>();

	public WindBomb(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageFraction = isLevelOne() ? DAMAGE_FRACTION_1 : DAMAGE_FRACTION_2;
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.25f);
		Snowball proj = world.spawn(mPlayer.getEyeLocation(), Snowball.class);
		proj.setVelocity(loc.getDirection().normalize().multiply(VELOCITY));
		proj.setShooter(mPlayer);
		mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.CLOUD);
		proj.customName(Component.text("Wind Bomb"));

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		double damage = ItemStatUtils.getAttributeAmount(mainhand, ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND);
		damage *= mDamageFraction;
		damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.add(Triple.of(proj, damage, playerItemStats));
		putOnCooldown();

		// Clear out list just in case
		mProjectiles.removeIf(triple -> triple.getLeft().isDead() || !triple.getLeft().isValid());
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
			mProjectiles.remove(triple);
			double damage = triple.getMiddle();
			ItemStatManager.PlayerItemStats playerItemStats = triple.getRight();

			Location loc = proj.getLocation();
			World world = proj.getWorld();

			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1.25f);

			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
			new BukkitRunnable() {

				@Override
				public void run() {
					for (double j = 0; j < 360; j += 6) {
						double radian1 = Math.toRadians(j);
						loc.add(FastUtils.cos(radian1) * radius, 0.15, FastUtils.sin(radian1) * radius);
						new PartialParticle(Particle.CLOUD, loc, 3, 0, 0, 0, 0.125).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						loc.subtract(FastUtils.cos(radian1) * radius, 0.15, FastUtils.sin(radian1) * radius);
					}
					this.cancel();
				}
			}.runTaskTimer(mPlugin, 0, 1);

			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
			double weaken = WEAKEN_EFFECT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
			// Velocity scales with the square root of the maximum height
			double velocityMultSquared = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, 1);
			float velocity = (float) (LAUNCH_VELOCITY * Math.sqrt(velocityMultSquared));
			Hitbox damageHitbox = new Hitbox.SphereHitbox(loc, radius);
			for (LivingEntity mob : damageHitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE_SKILL, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
				if (!EntityUtils.isBoss(mob)) {
					mob.setVelocity(new Vector(0.f, velocity, 0.f));
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, duration, SLOW_FALL_EFFECT, true, false));
				}
				EntityUtils.applyWeaken(mPlugin, duration, weaken, mob);
				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(mob, AIR_TAG + mPlayer.getName(), new WindBombAirTag(duration, MIDAIR_DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MODIFIER), mPlayer));
				}
			}

			if (isEnhanced()) {
				loc.add(0, 2, 0);
				world.spawnParticle(Particle.CLOUD, loc, 35, 4, 4, 4, 0.125);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 25, 2, 2, 2, 0.125);
				world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1f);
				world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.8f, 1);

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
								if (!(EntityUtils.isBoss(mob) || mob.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag) || ZoneUtils.hasZoneProperty(mob.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES))) {
									Vector vector = mob.getLocation().toVector().subtract(loc.toVector());
									double ratio = PULL_RATIO + vector.length() / pullRadius;
									Vector velocity = mob.getVelocity().add(vector.normalize().multiply(pullVelocity).multiply(-ratio).add(new Vector(0, 0.1 + 0.2 * ratio, 0)));
									if (mPlugin.mEffectManager.hasEffect(mob, AIR_TAG)) {
										// If mob was launched by the ability, don't change their Y
										velocity.setY(mob.getVelocity().getY());
									}
									mob.setVelocity(velocity);
								}
							}
						}
						world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 6, 2, 2, 2, 0.1);
						world.spawnParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05);
						world.spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15);
						if (mTicks >= pullDuration) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			proj.remove();
		}
	}
}
