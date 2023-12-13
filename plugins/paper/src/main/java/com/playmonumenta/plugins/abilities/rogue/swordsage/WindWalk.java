package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage.WindWalkCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindWalk extends MultipleChargeAbility {

	private static final int WIND_WALK_1_COOLDOWN = 25 * 20;
	private static final int WIND_WALK_2_COOLDOWN = 20 * 20;
	private static final int WIND_WALK_MAX_CHARGES = 2;
	private static final int WIND_WALK_DURATION = 20 * 2;
	private static final int WIND_WALK_RADIUS = 3;
	private static final double WIND_WALK_Y_VELOCITY = 0.2;
	private static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.16;
	private static final double WIND_WALK_VELOCITY_BONUS = 1.4;
	private static final int WIND_WALK_CDR = 20 * 4;

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
					WIND_WALK_1_COOLDOWN / 20,
					WIND_WALK_MAX_CHARGES
				),
				String.format("Casting this ability reduces the cooldown of all other abilities by %s seconds. Cooldown: %ss.",
					WIND_WALK_CDR / 20,
					WIND_WALK_2_COOLDOWN / 20))
			.simpleDescription("Dash forwards, stunning and levitating mobs along the path.")
			.cooldown(WIND_WALK_1_COOLDOWN, WIND_WALK_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WindWalk::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.QUARTZ);

	private final int mDuration;
	private final WindWalkCS mCosmetic;

	private int mLastCastTicks = 0;

	public WindWalk(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = WIND_WALK_DURATION;
		mMaxCharges = WIND_WALK_MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGE);
		mCharges = getTrackedCharges();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WindWalkCS());
	}

	public boolean cast() {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}

		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		putOnCooldown();
		walk();

		if (isLevelTwo()) {
			mPlugin.mTimers.updateCooldownsExcept(mPlayer, ClassAbility.WIND_WALK, CharmManager.getDuration(mPlayer, CHARM_COOLDOWN_REDUCTION, WIND_WALK_CDR));
		}
		return true;
	}

	public void walk() {
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		mCosmetic.initialEffects(mPlayer, loc, world);
		Vector direction = loc.getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * WIND_WALK_Y_VELOCITY_MULTIPLIER + WIND_WALK_Y_VELOCITY, 0);
		mPlayer.setVelocity(direction.multiply(WIND_WALK_VELOCITY_BONUS).add(yVelocity));

		cancelOnDeath(new BukkitRunnable() {
			final List<LivingEntity> mMobsNotHit = EntityUtils.getNearbyMobs(mPlayer.getLocation(), 32);
			int mTicks = 0;

			@Override
			public void run() {
				if (mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded()) {
					this.cancel();
					return;
				}

				mCosmetic.trailEffect(mPlayer, mTicks);

				Iterator<LivingEntity> iter = mMobsNotHit.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();

					if (mob.getLocation().distance(mPlayer.getLocation()) < WIND_WALK_RADIUS) {
						if (!EntityUtils.isBoss(mob)) {
							mCosmetic.enemyStunEffect(mPlayer, mob, world);

							EntityUtils.applyStun(mPlugin, mDuration, mob);

							if (EntityUtils.isElite(mob)) {
								mCosmetic.eliteStunEffect(mPlayer, mob, world);
							} else {
								mCosmetic.nonEliteStunEffect(mPlayer, mob);

								mob.setVelocity(mob.getVelocity().setY(0.5));
								PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.LEVITATION, mDuration, 0, true, false));
							}
						}

						iter.remove();
					}
				}

				Material block = mPlayer.getLocation().getBlock().getType();
				if (mTicks > 1 && (PlayerUtils.isOnGround(mPlayer) || block == Material.WATER || block == Material.LAVA || block == Material.LADDER)) {
					this.cancel();
					return;
				}
				mTicks++;
			}

		}.runTaskTimer(mPlugin, 0, 1));
	}

}
