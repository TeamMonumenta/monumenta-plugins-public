package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.assassin.BodkinBlitzCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BodkinBlitz extends MultipleChargeAbility {

	private static final int COOLDOWN_1 = 20 * 20;
	private static final int COOLDOWN_2 = 20 * 18;
	private static final int BONUS_DMG_1 = 7;
	private static final int BONUS_DMG_2 = 14;
	private static final int STEALTH_DURATION_1 = 20;
	private static final int STEALTH_DURATION_2 = 30;
	private static final int DISTANCE_1 = 10;
	private static final int DISTANCE_2 = 14;
	private static final int TELEPORT_TICKS = 4;
	private static final int MAX_CHARGES = 2;

	public static final String CHARM_COOLDOWN = "Bodkin Blitz Cooldown";
	public static final String CHARM_CHARGE = "Bodkin Blitz Charge";
	public static final String CHARM_DAMAGE = "Bodkin Blitz Damage";
	public static final String CHARM_STEALTH = "Bodkin Blitz Stealth Duration";
	public static final String CHARM_DISTANCE = "Bodkin Blitz Distance";

	public static final AbilityInfo<BodkinBlitz> INFO =
		new AbilityInfo<>(BodkinBlitz.class, "Bodkin Blitz", BodkinBlitz::new)
			.linkedSpell(ClassAbility.BODKIN_BLITZ)
			.scoreboardId("BodkinBlitz")
			.shorthandName("BB")
			.descriptions(
				String.format("Sneak right click while holding two swords to teleport %s blocks forwards. Gain %s second of Stealth upon teleporting. Upon teleporting, your next melee attack deals %s bonus damage if your target is not focused on you. This ability cannot be used in safe zones. Cooldown: %ss. Charges: %s.",
					DISTANCE_1,
					STEALTH_DURATION_1 / 20,
					BONUS_DMG_1,
					COOLDOWN_1 / 20,
					MAX_CHARGES
				),
				String.format("Range increased to %s blocks, Stealth increased to %s seconds. Upon teleporting, your next melee attack deals %s bonus damage if your target is not focused on you. Cooldown: %ss.",
					DISTANCE_2,
					STEALTH_DURATION_2 / 20.0,
					BONUS_DMG_2,
					COOLDOWN_2 / 20))
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BodkinBlitz::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(new ItemStack(Material.BLAZE_POWDER, 1));

	private final int mStealthDuration;
	private final double mBonusDmg;

	private @Nullable BukkitRunnable mRunnable = null;
	private boolean mTeleporting = false;
	private int mTicks;

	private int mLastCastTicks = 0;
	private final BodkinBlitzCS mCosmetic;

	public BodkinBlitz(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGE);
		mCharges = getTrackedCharges();

		mStealthDuration = CharmManager.getDuration(player, CHARM_STEALTH, (isLevelOne() ? STEALTH_DURATION_1 : STEALTH_DURATION_2));
		mBonusDmg = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? BONUS_DMG_1 : BONUS_DMG_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BodkinBlitzCS(), BodkinBlitzCS.SKIN_LIST);
	}

	public void cast() {
		if (mTeleporting
			    || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		Location loc = mPlayer.getLocation();

		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 4 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		mTeleporting = true;

		World world = mPlayer.getWorld();
		mCosmetic.blitzStartSound(world, loc);

		new BukkitRunnable() {
			final BoundingBox mPlayerBox = mPlayer.getBoundingBox();
			final Vector mDirection = mPlayer.getLocation().getDirection().normalize();
			final double mDistance = CharmManager.getRadius(mPlayer, CHARM_DISTANCE, isLevelOne() ? DISTANCE_1 : DISTANCE_2);
			final double mDistancePerTick = mDistance / TELEPORT_TICKS;
			int mTick = 0;
			Location mLastTpLoc = mPlayer.getLocation();

			@Override
			public void run() {
				// The teleport is run over multiple ticks so that we can have a small projectile animation.
				BoundingBox travelBox = mPlayerBox.clone();
				boolean isBlocked = LocationUtils.travelTillObstructed(world, travelBox,
					mDistancePerTick, mDirection, 0.1, true,
					loc -> mCosmetic.blitzTrailEffect(mPlayer, loc, mDirection), 1, 1);
				Location tpLoc = travelBox.getCenter().toLocation(world).add(0, -travelBox.getHeight() / 2, 0);
				if (isBlocked) {
					// If no spot was found, then you've literally hit a wall. Stop iterating.
					mTick = TELEPORT_TICKS;
					if (travelBox.equals(mPlayerBox)) { // started when already obstructed - use previous iteration's end location
						tpLoc = mLastTpLoc;
					}
				} else {
					// Shift player box by travel distance for next tick's check
					// Does not use travelBox as that may have been shifted to evade obstacles
					mPlayerBox.shift(mDirection.clone().multiply(mDistancePerTick));
					mLastTpLoc = tpLoc;
				}

				// Don't allow teleporting outside the world border
				if (!tpLoc.getWorld().getWorldBorder().isInside(tpLoc)) {
					this.cancel();
					return;
				}

				// Teleport player
				mTick++;
				if (mTick >= TELEPORT_TICKS) {
					tpLoc.setDirection(mPlayer.getLocation().getDirection());
					mPlayer.teleport(tpLoc);

					mTeleporting = false;

					mCosmetic.blitzEndEffect(world, mPlayer, tpLoc);

					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.FAST_DIGGING, 5, 19, true, false));

					AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration);

					mTicks = 100;
					if (mRunnable == null || mRunnable.isCancelled()) {
						mRunnable = new BukkitRunnable() {
							@Override
							public void run() {
								mCosmetic.blitzBuffEffect(mPlayer);
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

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mRunnable != null && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH)) {
			mTicks = 0;
			mRunnable.cancel();
			mRunnable = null;
			if (enemy instanceof Mob m) {
				if (m.getTarget() == null || !m.getTarget().getUniqueId().equals(mPlayer.getUniqueId())) {
					Location entityLoc = m.getLocation().clone().add(0, 1, 0);

					World world = entityLoc.getWorld();
					mCosmetic.blitzOnDamage(world, mPlayer, entityLoc);

					DamageUtils.damage(mPlayer, m, DamageType.MELEE_SKILL, mBonusDmg, mInfo.getLinkedSpell(), true);
				}
			}
		}
		return false; // already prevents multiple calls by clearing mRunnable
	}

}
