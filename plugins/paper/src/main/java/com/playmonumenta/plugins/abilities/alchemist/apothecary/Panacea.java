package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class Panacea extends Ability {

	private static final double PANACEA_DAMAGE_FRACTION = 0.6;
	private static final int PANACEA_1_SHIELD = 2;
	private static final int PANACEA_2_SHIELD = 4;
	private static final int PANACEA_MAX_SHIELD = 16;
	private static final int PANACEA_ABSORPTION_DURATION = 20 * 24;
	// Calculate the range with MAX_DURATION * MOVE_SPEED
	private static final int PANACEA_MAX_DURATION = 20 * 2;
	private static final double PANACEA_MOVE_SPEED = 0.35;
	private static final double PANACEA_RADIUS = 1.5;
	private static final int PANACEA_1_SLOW_TICKS = (int) (1.5 * 20);
	private static final int PANACEA_2_SLOW_TICKS = 2 * 20;
	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);
	private static final int COOLDOWN = 20 * 20;

	public static final String CHARM_DAMAGE = "Panacea Damage";
	public static final String CHARM_ABSORPTION = "Panacea Absorption Health";
	public static final String CHARM_ABSORPTION_MAX = "Panacea Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Panacea Absorption Duration";
	public static final String CHARM_MOVEMENT_DURATION = "Panacea Movement Duration";
	public static final String CHARM_MOVEMENT_SPEED = "Panacea Movement Speed";
	public static final String CHARM_RADIUS = "Panacea Radius";
	public static final String CHARM_SLOW_DURATION = "Panacea Slow Duration";
	public static final String CHARM_COOLDOWN = "Panacea Cooldown";

	public static final AbilityInfo<Panacea> INFO =
		new AbilityInfo<>(Panacea.class, "Panacea", Panacea::new)
			.linkedSpell(ClassAbility.PANACEA)
			.scoreboardId("Panacea")
			.shorthandName("Pn")
			.descriptions(
				"Shift left click with a projectile weapon to shoot a mixture that deals 60% of your potion damage and applies 100% Slow for 1.5s to every enemy touched " +
					"and adds 2 absorption health to other players, lasting 24 seconds, maximum 16. " +
					"After hitting a block or traveling 10 blocks, the mixture traces and returns to you, able to damage enemies and shield allies a second time. Cooldown: 20s.",
				"Absorption health added is increased to 4, and Slow duration is increased to 2s.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Panacea::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.PURPLE_CONCRETE_POWDER, 1));

	private final double mShield;
	private final int mSlowTicks;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public Panacea(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlowTicks = CharmManager.getDuration(mPlayer, CHARM_SLOW_DURATION, (isLevelOne() ? PANACEA_1_SLOW_TICKS : PANACEA_2_SLOW_TICKS));
		mShield = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? PANACEA_1_SHIELD : PANACEA_2_SHIELD);
		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 1.25f);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_WITCH, mPlayer.getLocation(), 25, 0.2, 0, 0.2, 1).spawnAsPlayerActive(mPlayer);

		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, PANACEA_RADIUS);
		double moveSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MOVEMENT_SPEED, PANACEA_MOVE_SPEED);
		int maxDuration = CharmManager.getDuration(mPlayer, CHARM_MOVEMENT_DURATION, PANACEA_MAX_DURATION);

		double maxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MAX, PANACEA_MAX_SHIELD);
		int absorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, PANACEA_ABSORPTION_DURATION);

		if (mAlchemistPotions != null) {
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mAlchemistPotions.getDamage() * PANACEA_DAMAGE_FRACTION);

			cancelOnDeath(new BukkitRunnable() {
				final Location mLoc = mPlayer.getEyeLocation();
				final BoundingBox mBox = BoundingBox.of(mLoc, radius, radius, radius);
				Vector mIncrement = mLoc.getDirection().multiply(moveSpeed);

				// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
				List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, moveSpeed * maxDuration + 2);
				List<Player> mPlayers = PlayerUtils.otherPlayersInRange(mPlayer, moveSpeed * maxDuration + 2, true);

				int mTicks = 0;
				int mReverseTick = 0;
				double mDegree = 0;
				boolean mReverse = false;

				@Override
				public void run() {
					mBox.shift(mIncrement);
					mLoc.add(mIncrement);
					Iterator<LivingEntity> mobIter = mMobs.iterator();
					while (mobIter.hasNext()) {
						LivingEntity mob = mobIter.next();
						if (mBox.overlaps(mob.getBoundingBox())) {
							DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, true, false);

							if (!EntityUtils.isBoss(mob)) {
								EntityUtils.applySlow(mPlugin, mSlowTicks, 1, mob);
							}
							mobIter.remove();
						}
					}
					Iterator<Player> playerIter = mPlayers.iterator();
					while (playerIter.hasNext()) {
						Player player = playerIter.next();
						if (mBox.overlaps(player.getBoundingBox())) {
							AbsorptionUtils.addAbsorption(player, mShield, maxAbsorption, absorptionDuration);
							playerIter.remove();
						}
					}

					mDegree += 12;
					Vector vec;
					double ratio = radius / PANACEA_RADIUS;
					for (int i = 0; i < 2; i++) {
						double radian1 = Math.toRadians(mDegree + (i * 180));
						vec = new Vector(FastUtils.cos(radian1) * 0.325, 0, FastUtils.sin(radian1) * 0.325);
						vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch() - 90);
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

						Location l = mLoc.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, l, (int) (5 * ratio * ratio), 0.1 * ratio, 0.1, 0.1 * ratio, APOTHECARY_LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.REDSTONE, l, (int) (5 * ratio * ratio), 0.1 * ratio, 0.1, 0.1 * ratio, APOTHECARY_DARK_COLOR).spawnAsPlayerActive(mPlayer);
					}
					new PartialParticle(Particle.SPELL_INSTANT, mLoc, (int) (5 * ratio * ratio), 0.35 * ratio, 0.35, 0.35 * ratio, 1).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_WITCH, mLoc, (int) (5 * ratio * ratio), 0.35 * ratio, 0.35, 0.35 * ratio, 1).spawnAsPlayerActive(mPlayer);

					if (!mReverse && (!mLoc.isChunkLoaded() || LocationUtils.collidesWithSolid(mLoc) || mTicks >= maxDuration)) {
						mMobs = EntityUtils.getNearbyMobs(mLoc, (0.3 + moveSpeed) * maxDuration + 2, mPlayer);
						mPlayers = PlayerUtils.otherPlayersInRange(mPlayer, (0.3 + moveSpeed) * maxDuration + 2, true);
						mReverse = true;
						mReverseTick = mTicks;
					}

					if (mReverse) {
						if (mTicks <= 0) {
							world.playSound(mLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 2.4f);
							new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1, 0), (int) (8 * ratio * ratio), 0.25 * ratio, 0.5, 0.25 * ratio, 0.5).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.SPELL, mPlayer.getLocation().add(0, 1, 0), (int) (8 * ratio * ratio), 0.35 * ratio, 0.5, 0.35 * ratio).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), (int) (25 * ratio * ratio), 0.35 * ratio, 0.5, 0.35 * ratio, APOTHECARY_LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
							this.cancel();
						}

						// The mIncrement is calculated by the distance to the player divided by the number of increments left
						mIncrement = mPlayer.getEyeLocation().toVector().subtract(mLoc.toVector()).multiply(1.0 / mTicks);

						// To make the particles function without rewriting the particle code, manually calculate and set pitch and yaw
						double x = mIncrement.getX();
						double y = mIncrement.getY();
						double z = mIncrement.getZ();
						// As long as Z is nonzero, we won't get division by 0
						if (z == 0) {
							z = 0.0001;
						}
						float pitch = (float) Math.toDegrees(-Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))));
						float yaw = (float) Math.toDegrees(Math.atan(-x / z));
						if (z < 0) {
							yaw += 180;
						}
						mLoc.setPitch(pitch);
						mLoc.setYaw(yaw);

						mTicks--;
					} else {
						mTicks++;
					}
				}

			}.runTaskTimer(mPlugin, 0, 1));
		}
	}

}
