package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class WhirlingBlade extends MultipleChargeAbility {

	private static final int BLADE_1_DAMAGE = 9;
	private static final int BLADE_2_DAMAGE = 14;
	private static final float BLADE_1_KNOCKBACK = 0.4f;
	private static final float BLADE_2_KNOCKBACK = 1.2f;
	private static final double THROW_RADIUS = 3;
	private static final double BLADE_RADIUS = 1;
	private static final int BLADE_MAX_CHARGES = 2;
	private static final int BLADE_COOLDOWN = 20 * 8;

	public static final String CHARM_DAMAGE = "Whirling Blade Damage";
	public static final String CHARM_KNOCKBACK = "Whirling Blade Knockback";
	public static final String CHARM_RADIUS = "Whirling Blade Radius";
	public static final String CHARM_CHARGES = "Whirling Blade Charges";
	public static final String CHARM_COOLDOWN = "Whirling Blade Cooldown";

	private final double mDamage;
	private final float mKnockback;

	private int mLastCastTicks = 0;
	private @Nullable WindBomb mWindBomb;

	public WhirlingBlade(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Whirling Blade");
		mInfo.mScoreboardId = "WhirlingBlade";
		mInfo.mShorthandName = "WB";
		mInfo.mDescriptions.add("Use the swap key while holding a weapon and not looking up to throw a whirling blade that circles around you, knocking back and dealing " + BLADE_1_DAMAGE + " melee damage to enemies it hits. Cooldown: 8s. Charges: 2.");
		mInfo.mDescriptions.add("The damage is increased to " + BLADE_2_DAMAGE + " and the knockback is greatly increased.");
		mInfo.mLinkedSpell = ClassAbility.WHIRLING_BLADE;
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? BLADE_1_DAMAGE : BLADE_2_DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, isLevelOne() ? BLADE_1_KNOCKBACK : BLADE_2_KNOCKBACK);
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_COOLDOWN, BLADE_COOLDOWN);
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);
		mMaxCharges = BLADE_MAX_CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mWindBomb = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, WindBomb.class);
			});
		}
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mWindBomb != null && mPlayer.isSneaking()) {
			return;
		}

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (ItemUtils.isSomeBow(inMainHand) || ItemUtils.isSomeBow(inOffHand) || ItemUtils.isSomePotion(inMainHand) || inMainHand.getType().isBlock()
				|| inMainHand.getType().isEdible() || inMainHand.getType() == Material.TRIDENT || inMainHand.getType() == Material.COMPASS || inMainHand.getType() == Material.SHIELD) {
			return;
		}

		// Player is looking up, do not cast (conflict with Swiftness)
		if (mPlayer.getLocation().getPitch() < -45) {
			return;
		}

		int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident. Also, strange bug, this seems to trigger twice when right clicking, but not the
		// case for stuff like Bodkin Blitz. This check also fixes that bug.
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		double throwRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, THROW_RADIUS);
		double bladeRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BLADE_RADIUS);
		new BukkitRunnable() {
			World mWorld = mPlayer.getWorld();
			Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
			Vector mEyeDir = mLoc.getDirection();

			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, 4 * throwRadius, mPlayer);

			double mStartAngle = Math.atan(mEyeDir.getZ()/mEyeDir.getX());
			int mIncrementDegrees = 0;
			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (mEyeDir.getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI * 90 / 180;
				}
				Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
				Vector direction = new Vector(Math.cos(mStartAngle - Math.PI * mIncrementDegrees / 180), 0, Math.sin(mStartAngle - Math.PI * mIncrementDegrees / 180));
				Location bladeLoc1 = mLoc.clone().add(direction.clone().multiply(throwRadius));
				Location bladeLoc2 = mLoc.clone().add(direction.clone().multiply(throwRadius / 2));
				Location bladeLoc3 = mLoc.clone().add(direction.clone().multiply(throwRadius / 4));
				BoundingBox mBox1 = BoundingBox.of(bladeLoc1, bladeRadius, bladeRadius, bladeRadius);
				BoundingBox mBox2 = BoundingBox.of(bladeLoc2, bladeRadius / 2, bladeRadius / 2, bladeRadius / 2);
				BoundingBox mBox3 = BoundingBox.of(bladeLoc3, bladeRadius / 4, bladeRadius / 4, bladeRadius / 4);
				Iterator<LivingEntity> mobIter = mMobs.iterator();
				while (mobIter.hasNext()) {
					LivingEntity mob = mobIter.next();
					if (mBox1.overlaps(mob.getBoundingBox()) || mBox2.overlaps(mob.getBoundingBox()) || mBox3.overlaps(mob.getBoundingBox())) {
						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.mLinkedSpell, true);
						MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
						mobIter.remove();
					}
				}

				new PartialParticle(Particle.SWEEP_ATTACK, bladeLoc1, 3, 0.35, 0, 0.35, 1).spawnAsPlayerActive(mPlayer);

				mIncrementDegrees += 30;
				if (mIncrementDegrees > 360) {
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
					this.cancel();
				}
				mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
