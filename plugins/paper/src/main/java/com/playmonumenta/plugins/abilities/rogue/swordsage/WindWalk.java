package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WindWalk extends MultipleChargeAbility {

	private static final int WIND_WALK_COOLDOWN = 20 * 25;
	private static final int WIND_WALK_MAX_CHARGES = 2;
	private static final int WIND_WALK_1_DURATION = 20 * 2;
	private static final int WIND_WALK_2_DURATION = 20 * 4;
	private static final int WIND_WALK_VULNERABILITY_DURATION_INCREASE = 20 * 3;
	private static final double WIND_WALK_VULNERABILITY_AMPLIFIER = 0.3;
	private static final int WIND_WALK_RADIUS = 3;
	private static final double WIND_WALK_Y_VELOCITY = 0.2;
	private static final double WIND_WALK_Y_VELOCITY_MULTIPLIER = 0.2;
	private static final double WIND_WALK_VELOCITY_BONUS = 1.5;

	private final int mDuration;

	private int mLastCastTicks = 0;

	public WindWalk(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Wind Walk");
		mInfo.mLinkedSpell = ClassAbility.WIND_WALK;
		mInfo.mScoreboardId = "WindWalk";
		mInfo.mShorthandName = "WW";
		mInfo.mDescriptions.add("Press the swap key while holding two swords to dash in the target direction, stunning and levitating enemies for 2 seconds. Elites are not levitated. Cooldown: 25s. Charges: 2.");
		mInfo.mDescriptions.add("Now afflicts 30% Vulnerability; enemies are stunned and levitated for 4 seconds.");
		mInfo.mCooldown = WIND_WALK_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.QUARTZ, 1);
		mDuration = isLevelOne() ? WIND_WALK_1_DURATION : WIND_WALK_2_DURATION;
		mMaxCharges = WIND_WALK_MAX_CHARGES;
		mCharges = getTrackedCharges();
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
			|| !InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			return;
		}

		event.setCancelled(true);

		int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		putOnCooldown();

		walk();
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
							if (isLevelTwo()) {
								EntityUtils.applyVulnerability(mPlugin, mDuration + WIND_WALK_VULNERABILITY_DURATION_INCREASE, WIND_WALK_VULNERABILITY_AMPLIFIER, mob);
							}

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
