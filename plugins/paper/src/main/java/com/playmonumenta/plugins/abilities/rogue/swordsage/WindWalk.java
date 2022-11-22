package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindWalk extends MultipleChargeAbility {

	private static final int WIND_WALK_COOLDOWN = 20 * 25;
	private static final int WIND_WALK_MAX_CHARGES = 2;
	private static final int WIND_WALK_DURATION = 20 * 2;
	private static final int WIND_WALK_RADIUS = 3;
	private static final double WIND_WALK_Y_VELOCITY = 0.2;
	private static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.2;
	private static final double WIND_WALK_VELOCITY_BONUS = 1.5;
	private static final int WIND_WALK_CDR = 20 * 2;

	public static final String CHARM_COOLDOWN = "Wind Walk Cooldown";
	public static final String CHARM_CHARGE = "Wind Walk Charge";
	public static final String CHARM_COOLDOWN_REDUCTION = "Wind Walk Cooldown Reduction";

	public static final AbilityInfo<WindWalk> INFO =
		new AbilityInfo<>(WindWalk.class, "Wind Walk", WindWalk::new)
			.linkedSpell(ClassAbility.WIND_WALK)
			.scoreboardId("WindWalk")
			.shorthandName("WW")
			.descriptions(
				String.format("Press the swap key while holding two swords to dash in the target direction, stunning and levitating enemies for %s seconds. " +
					              "Elites are not levitated. Cooldown: %ss. Charges: %s.",
					WIND_WALK_DURATION / 20,
					WIND_WALK_COOLDOWN / 20,
					WIND_WALK_MAX_CHARGES
				),
				String.format("Casting this ability reduces the cooldown of all other abilities by %s seconds.",
					WIND_WALK_CDR / 20))
			.cooldown(WIND_WALK_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindWalk::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(new ItemStack(Material.QUARTZ, 1));

	private final int mDuration;

	private int mLastCastTicks = 0;

	public WindWalk(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = WIND_WALK_DURATION;
		mMaxCharges = WIND_WALK_MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGE);
		mCharges = getTrackedCharges();
	}

	public void cast() {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		putOnCooldown();
		walk();

		if (isLevelTwo()) {
			mPlugin.mTimers.updateCooldownsExcept(mPlayer, mInfo.getLinkedSpell(), WIND_WALK_CDR + CharmManager.getExtraDuration(mPlayer, CHARM_COOLDOWN_REDUCTION));
		}
	}

	public void walk() {
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1f);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
		Vector direction = loc.getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(WIND_WALK_VELOCITY_BONUS).add(yVelocity));

		new BukkitRunnable() {
			final List<LivingEntity> mMobsNotHit = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 32);
			boolean mTickOne = true;
			@Override
			public void run() {
				if (mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
					this.cancel();
					return;
				}

				new PartialParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation().add(0, 1, 0), 7, 0.25, 0.45, 0.25, 0).spawnAsPlayerActive(mPlayer);

				Iterator<LivingEntity> iter = mMobsNotHit.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();

					if (mob.getLocation().distance(mPlayer.getLocation()) < WIND_WALK_RADIUS) {
						if (!EntityUtils.isBoss(mob)) {
							new PartialParticle(Particle.SWEEP_ATTACK, mob.getLocation().add(0, 1, 0), 16, 0.5, 0.5, 0.5, 0).spawnAsPlayerActive(mPlayer);
							world.playSound(mob.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 1.25f);

							EntityUtils.applyStun(mPlugin, mDuration, mob);

							if (EntityUtils.isElite(mob)) {
								new PartialParticle(Particle.EXPLOSION_NORMAL, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
								world.playSound(mob.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.75f, 0.75f);
							} else {
								new PartialParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);

								mob.setVelocity(mob.getVelocity().setY(0.5));
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, mDuration, 0, true, false));
							}
						}

						iter.remove();
					}
				}

				Material block = mPlayer.getLocation().getBlock().getType();
				if (!mTickOne && (mPlayer.isOnGround() || block == Material.WATER || block == Material.LAVA || block == Material.LADDER)) {
					this.cancel();
					return;
				}
				mTickOne = false;
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

}
