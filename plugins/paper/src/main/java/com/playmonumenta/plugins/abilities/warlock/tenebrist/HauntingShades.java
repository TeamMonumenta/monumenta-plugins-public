package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;

public class HauntingShades extends MultipleChargeAbility {

	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "HauntingShadesTickRightClicked";
	private static final String ATTR_NAME = "HauntingShadesExtraSpeedAttr";

	private static final int COOLDOWN = 15 * 20;
	private static final int SHADES_DURATION = 8 * 20;
	private static final int SHADES_CHARGES_1 = 2;
	private static final int SHADES_CHARGES_2 = 3;
	private static final int VULN_1 = 1;
	private static final int VULN_2 = 3;
	private static final double EXTRA_SPEED = 0.1;
	private static final int EFFECT_DURATION = 20 * 2;
	private static final int RANGE = 10;
	private static final int AOE_RANGE = 6;
	private static final double HITBOX_LENGTH = 0.55;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	private final int mVuln;

	private int mRightClicks = 0;

	public HauntingShades(Plugin plugin, Player player) {
		super(plugin, player, "Haunting Shades", SHADES_CHARGES_1, SHADES_CHARGES_2);
		mInfo.mLinkedSpell = Spells.HAUNTING_SHADES;
		mInfo.mScoreboardId = "HauntingShades";
		mInfo.mShorthandName = "HS";
		mInfo.mDescriptions.add("Double right-click while with a scythe to conjure a Shade at the target block or mob location. Mobs within 6 blocks of a Shade are afflicted with 10% Vulnerability, and players within 6 blocks of the shade are given 10% speed. A Shade fades back into darkness after 8 seconds. Cooldown: 15s. Charges: 2.");
		mInfo.mDescriptions.add("Your number of charges increases to 3 and mobs within 6 blocks of a Shade are afflicted with 20% Vulnerability.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mVuln = getAbilityScore() == 1 ? VULN_1 : VULN_2;
	}

	@Override
	public boolean runCheck() {
		return (InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand()) && !mPlayer.isSneaking() && !mPlayer.isSprinting());
	}

	@Override
	public void cast(Action action) {
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, CHECK_ONCE_THIS_TICK_METAKEY)) {
			mRightClicks++;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (mRightClicks > 0) {
						mRightClicks--;
					}
					this.cancel();
				}
			}.runTaskLater(mPlugin, 5);
		}
		if (mRightClicks < 2) {
			return;
		} else {
			mRightClicks = 0;

			if (!consumeCharge()) {
				return;
			}

			Location loc = mPlayer.getEyeLocation();
			Vector direction = loc.getDirection();
			Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
			BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
			box.shift(direction);

			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.65f);

			Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(loc, RANGE));

			for (double r = 0; r < RANGE; r += HITBOX_LENGTH) {
				Location bLoc = box.getCenter().toLocation(world);

				world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075);
				world.spawnParticle(Particle.REDSTONE, bLoc, 16, 0.2, 0.2, 0.2, 0.1, COLOR);

				Iterator<LivingEntity> iter = nearbyMobs.iterator();
				while (iter.hasNext()) {
					LivingEntity mob = iter.next();
					if (mob.getBoundingBox().overlaps(box)) {
						if (EntityUtils.isHostileMob(mob)) {
							placeShade(bLoc);
							return;
						}
					}
				}

				if (bLoc.getBlock().getType().isSolid()) {
					bLoc.subtract(direction.multiply(0.5));
					placeShade(bLoc);
					return;
				}

				box.shift(shift);
			}
			placeShade(box.getCenter().toLocation(world));
		}
	}

	private void placeShade(Location bLoc) {
		World world = mPlayer.getWorld();
		bLoc.setDirection(mPlayer.getLocation().toVector().subtract(bLoc.toVector()).normalize());
		ArmorStand mStand = (ArmorStand) LibraryOfSoulsIntegration.summon(bLoc, "HauntingShade");
		mStand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
		mStand.setGravity(false);
		mStand.setCanMove(false);
		mStand.setSilent(true);
		mStand.setCustomName("Haunting Shade");
		mStand.setCustomNameVisible(false);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				Location l = bLoc;
				mT++;
				if (mT % 5 == 0) {
					List<Player> affectedPlayers = PlayerUtils.playersInRange(l, AOE_RANGE, true);
				    Set<LivingEntity> affectedMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(l, AOE_RANGE));
				    for (Player p : affectedPlayers) {
						mPlugin.mEffectManager.addEffect(p, "HauntingShadesExtraSpeed", new PercentSpeed(EFFECT_DURATION, EXTRA_SPEED, ATTR_NAME));
				    }
				    for (LivingEntity m : affectedMobs) {
				    	PotionUtils.applyPotion(mPlayer, m, new PotionEffect(PotionEffectType.UNLUCK, EFFECT_DURATION, mVuln, true, false));
				    }
				}

				if (mT % 10 == 0) {
					new BukkitRunnable() {
						double mRadius = 0;
						final Location mLoc = l;
						@Override
						public void run() {
							mRadius += 1.25;
							for (double j = 0; j < 360; j += 30) {
								double radian1 = Math.toRadians(j);
								mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
								world.spawnParticle(Particle.REDSTONE, mLoc, 3, 0.2, 0.2, 0.2, 0.1, COLOR);
								world.spawnParticle(Particle.SMOKE_NORMAL, mLoc, 1, 0, 0, 0, 0.15);
								mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
							}
							if (mRadius >= AOE_RANGE + 1) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}

				if (mT % 20 == 0) {
					world.playSound(l, Sound.ENTITY_BLAZE_HURT, 0.3f, 0.5f);
				}

				if (mT >= SHADES_DURATION || mPlayer.isDead() || !mPlayer.isValid()) {
					mStand.remove();
					world.spawnParticle(Particle.REDSTONE, bLoc, 45, 0.2, 1.1, 0.2, 0.1, COLOR);
					world.spawnParticle(Particle.SMOKE_NORMAL, bLoc, 40, 0.3, 1.1, 0.3, 0.15);
					world.playSound(bLoc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.5f);
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}




