package com.playmonumenta.plugins.abilities.rogue.assassin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class BodkinBlitz extends MultipleChargeAbility {

	private static final int BODKINBLITZ_1_COOLDOWN = 20 * 20;
	private static final int BODKINBLITZ_2_COOLDOWN = 20 * 18;
	private static final int BODKINBLITZ_1_BONUS_DMG = 10;
	private static final int BODKINBLITZ_2_BONUS_DMG = 20;
	private static final int BODKINBLITZ_1_STEALTH_DURATION = 20;
	private static final int BODKINBLITZ_2_STEALTH_DURATION = 30;
	private static final int BODKINBLITZ_1_STEP = 25;
	private static final int BODKINBLITZ_2_STEP = 35;
	private static final int BODKINBLITZ_MAX_CHARGES = 2;

	private final int mStealthDuration;
	private final int mBonusDmg;

	private BukkitRunnable mRunnable = null;
	private boolean mTeleporting = false;
	private int mTicks;

	public BodkinBlitz(Plugin plugin, Player player) {
		super(plugin, player, "Bodkin Blitz", BODKINBLITZ_MAX_CHARGES, BODKINBLITZ_MAX_CHARGES);
		mInfo.mLinkedSpell = ClassAbility.BODKIN_BLITZ;
		mInfo.mScoreboardId = "BodkinBlitz";
		mInfo.mShorthandName = "BB";
		mInfo.mDescriptions.add("Sneak right click while holding two swords to teleport 10 blocks forwards. Gain 1 second of Stealth upon teleporting. Upon teleporting, your next melee attack deals 10 bonus damage if your target is not focused on you. This ability cannot be used in safe zones. Cooldown: 20s. Charges: 2.");
		mInfo.mDescriptions.add("Range increased to 14 blocks, Stealth increased to 1.5 seconds. Upon teleporting, your next melee attack deals 20 bonus damage if your target is not focused on you. Cooldown: 18s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? BODKINBLITZ_1_COOLDOWN : BODKINBLITZ_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;

		mStealthDuration = getAbilityScore() == 1 ? BODKINBLITZ_1_STEALTH_DURATION : BODKINBLITZ_2_STEALTH_DURATION;
		mBonusDmg = getAbilityScore() == 1 ? BODKINBLITZ_1_BONUS_DMG : BODKINBLITZ_2_BONUS_DMG;
	}

	@Override
	public void cast(Action action) {
		if (mTeleporting || !mPlayer.isSneaking() || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
				|| !InventoryUtils.rogueTriggerCheck(mPlayer.getInventory().getItemInMainHand(), mPlayer.getInventory().getItemInOffHand())) {
			return;
		}

		Location loc = mPlayer.getLocation();
		// Smokescreen trigger conflict
		if (loc.getPitch() > 50) {
			return;
		}

		if (!consumeCharge()) {
			return;
		}

		mTeleporting = true;

		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_BREATH, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);

		new BukkitRunnable() {
			Location mTpLoc = mPlayer.getLocation();
			final Vector mShiftVec = mPlayer.getLocation().getDirection().normalize().multiply(0.1);
			final BoundingBox mPlayerBox = mPlayer.getBoundingBox();
			int mTick = 0;
			final int mStep = getAbilityScore() == 1 ? BODKINBLITZ_1_STEP : BODKINBLITZ_2_STEP;

			@Override
			public void run() {
				// Fire projectile.
				for (int i = 0; i < mStep; i++) {
					Location boxLoc = mPlayerBox.getCenter().toLocation(world);

					boolean isBlocked = true;
					BoundingBox testBox = mPlayerBox.clone();

					// Preliminary check on the spot the player is standing on, before shifting locations.
					if (testLocation(testBox)) {
						mTpLoc = testBox.getCenter().toLocation(world).add(0, -testBox.getHeight() / 2, 0);
						isBlocked = false;
					}

					if (isBlocked) {
						testBox.shift(0, -1, 0);
						for (int dy = 0; dy < 20; dy++) {
							// Start by scanning along the y-axis, from -1 to +1, to find the lowest available space.
							if (testLocation(testBox)) {
								mTpLoc = testBox.getCenter().toLocation(world).add(0, -testBox.getHeight() / 2, 0);
								isBlocked = false;
								break;
							}

							testBox.shift(0, 0.1, 0);
						}
					}

					if (isBlocked) {
						// If no spot was found, then you've literally hit a wall. Stop iterating.
						mTick = 4;
						break;
					}

					world.spawnParticle(Particle.FALLING_DUST, boxLoc, 5, 0.15, 0.45, 0.1,
							Bukkit.createBlockData("gray_concrete"));
					world.spawnParticle(Particle.CRIT, boxLoc, 4, 0.25, 0.5, 0.25, 0);
					world.spawnParticle(Particle.SMOKE_NORMAL, boxLoc, 5, 0.15, 0.45, 0.15, 0.01);

					mPlayerBox.shift(mShiftVec);

				}
				mTick++;
				// Each incrementation of j checks for 1.5 blocks, for a max of 4 (6 blocks).
				// This is so that we can have a small projectile animation.

				// Don't allow teleporting outside the world border
				if (!mTpLoc.getWorld().getWorldBorder().isInside(mTpLoc)) {
					this.cancel();
					return;
				}

				// Teleport player
				if (mTick >= 4) {
					mTpLoc.setDirection(mPlayer.getLocation().getDirection());
					mTpLoc.add(0, 0.1, 0);
					mPlayer.teleport(mTpLoc, TeleportCause.UNKNOWN);

					mTeleporting = false;

					world.playSound(mTpLoc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 2f);
					world.playSound(mTpLoc, Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
					world.playSound(mTpLoc, Sound.ITEM_TRIDENT_THROW, 1f, 0.5f);
					world.playSound(mTpLoc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
					world.playSound(mTpLoc, Sound.ENTITY_PHANTOM_HURT, 1f, 0.75f);
					world.playSound(mTpLoc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

					world.spawnParticle(Particle.SMOKE_LARGE, mTpLoc.clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.18);
					world.spawnParticle(Particle.SMOKE_LARGE, mTpLoc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.04);
					world.spawnParticle(Particle.SPELL_WITCH, mTpLoc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
					world.spawnParticle(Particle.SMOKE_NORMAL, mTpLoc.clone().add(0, 1, 0), 50, 0.75, 0.5, 0.75, 0.05);
					world.spawnParticle(Particle.CRIT, mTpLoc.clone().add(0, 1, 0), 25, 1, 1, 1, 0.3);

					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
							new PotionEffect(PotionEffectType.FAST_DIGGING, 5, 19, true, false));

					AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration);

					mTicks = 100;
					if (mRunnable == null || mRunnable.isCancelled()) {
						mRunnable = new BukkitRunnable() {
							@Override
							public void run() {
								world.spawnParticle(Particle.FALLING_DUST, mPlayer.getLocation().clone().add(0, 0.5, 0), 1, 0.35, 0.25, 0.35, Bukkit.createBlockData("gray_concrete"));
								if (mTicks <= 0) {
									mTicks = 0;
									this.cancel();
									mRunnable = null;
								}
								mTicks--;
							}
						};
						mRunnable.runTaskTimer(mPlugin, 0, 1);
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private boolean testLocation(BoundingBox box) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 0; y <= 2; y++) {
					// Checking the blocks around the hitbox.
					Block block = box.getCenter().toLocation(mPlayer.getWorld()).add(x * 0.4, y * 0.975 - box.getHeight() / 2, z * 0.4).getBlock();
					// A player's hitbox is 0.625 * 0.625 * 1.8125 blocks. Rounding up to 0.8 * 0.8 * 1.95 to be safe.

					if (block.getType().isSolid() && block.getBoundingBox().overlaps(box)) {
						// If a bad spot has already been found, then there's no need to check the rest-- this spot is invalid.
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mRunnable != null && event.getCause() == DamageCause.ENTITY_ATTACK) {
			mTicks = 0;
			mRunnable.cancel();
			mRunnable = null;
			if (event.getEntity() instanceof Mob) {
				Mob m = (Mob) event.getEntity();
				if (m.getTarget() == null || !m.getTarget().getUniqueId().equals(mPlayer.getUniqueId())) {
					Location entityLoc = m.getLocation().clone().add(0, 1, 0);

					World world = entityLoc.getWorld();
					world.playSound(entityLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 2f);
					world.playSound(entityLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 2f);
					world.spawnParticle(Particle.FALLING_DUST, entityLoc, 35, 0.35, 0.5, 0.35, Bukkit.createBlockData("gray_concrete"));
					world.spawnParticle(Particle.BLOCK_CRACK, entityLoc, 20, 0.25, 0.25, 0.25, 1, Bukkit.createBlockData("redstone_block"));

					event.setDamage(event.getDamage() + mBonusDmg);
				}
			}
		}
		return true;
	}

}
