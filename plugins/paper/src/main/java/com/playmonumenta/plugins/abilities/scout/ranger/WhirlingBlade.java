package com.playmonumenta.plugins.abilities.scout.ranger;

import java.util.Iterator;
import java.util.List;

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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class WhirlingBlade extends MultipleChargeAbility {

	private static final int BLADE_1_DAMAGE = 10;
	private static final int BLADE_2_DAMAGE = 15;
	private static final float BLADE_1_KNOCKBACK = 0.4f;
	private static final float BLADE_2_KNOCKBACK = 1.2f;
	private static final double THROW_RADIUS = 3;
	private static final double BLADE_RADIUS = THROW_RADIUS/3;
	private static final int BLADE_1_MAX_CHARGES = 2;
	private static final int BLADE_2_MAX_CHARGES = 2;
	private static final int BLADE_1_COOLDOWN = 20 * 8;
	private static final int BLADE_2_COOLDOWN = 20 * 8;

	private final int mDamage;
	private final float mKnockback;

	private int mLastCastTicks = 0;

	public WhirlingBlade(Plugin plugin, Player player) {
		super(plugin, player, "Whirling Blade",  BLADE_1_MAX_CHARGES, BLADE_2_MAX_CHARGES);
		mInfo.mScoreboardId = "WhirlingBlade";
		mInfo.mShorthandName = "WB";
		mInfo.mDescriptions.add("Use the swap key while holding a weapon to throw a whirling blade that circles around you, knocking back and dealing 10 damage to enemies it hits. Cooldown: 8s. Charges: 2.");
		mInfo.mDescriptions.add("The damage is increased to 15 and the knockback is greatly increased.");
		mInfo.mLinkedSpell = Spells.WHIRLING_BLADE;
		mDamage = getAbilityScore() == 1 ? BLADE_1_DAMAGE : BLADE_2_DAMAGE;
		mKnockback = getAbilityScore() == 1 ? BLADE_1_KNOCKBACK : BLADE_2_KNOCKBACK;
		mInfo.mCooldown = getAbilityScore() == 1 ? BLADE_1_COOLDOWN : BLADE_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {

		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();
		if (InventoryUtils.isBowItem(inMainHand) || InventoryUtils.isBowItem(inOffHand) || InventoryUtils.isPotionItem(inMainHand) || inMainHand.getType().isBlock()
				|| inMainHand.getType().isEdible() || inMainHand.getType() == Material.TRIDENT || inMainHand.getType() == Material.COMPASS || inMainHand.getType() == Material.SHIELD) {
			return;
		}

		event.setCancelled(true);

		int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident. Also, strange bug, this seems to trigger twice when right clicking, but not the
		// case for stuff like Bodkin Blitz. This check also fixes that bug.
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		new BukkitRunnable() {
			World mWorld = mPlayer.getWorld();
			Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
			Vector mEyeDir = mLoc.getDirection();

			// Convoluted range parameter makes sure we grab all possible entities to be hit without recalculating manually
			List<LivingEntity> mMobs = EntityUtils.getNearbyMobs(mLoc, 4*THROW_RADIUS, mPlayer);

			double mStartAngle = Math.atan(mEyeDir.getZ()/mEyeDir.getX());
			int mIncrementDegrees = 0;
			@Override
			public void run() {
				if (mIncrementDegrees == 0) {
					if (mEyeDir.getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI*90/180;
				}
				Location mLoc = mPlayer.getEyeLocation().add(0, -0.5, 0);
				Vector direction = new Vector(Math.cos(mStartAngle - Math.PI*mIncrementDegrees/180), 0, Math.sin(mStartAngle - Math.PI*mIncrementDegrees/180));
				Location bladeLoc1 = mLoc.clone().add(direction.clone().multiply(THROW_RADIUS));
				Location bladeLoc2 = mLoc.clone().add(direction.clone().multiply(THROW_RADIUS/2));
				Location bladeLoc3 = mLoc.clone().add(direction.clone().multiply(THROW_RADIUS/4));
				BoundingBox mBox1 = BoundingBox.of(bladeLoc1, BLADE_RADIUS, BLADE_RADIUS, BLADE_RADIUS);
				BoundingBox mBox2 = BoundingBox.of(bladeLoc2, BLADE_RADIUS/2, BLADE_RADIUS/2, BLADE_RADIUS/2);
				BoundingBox mBox3 = BoundingBox.of(bladeLoc3, BLADE_RADIUS/4, BLADE_RADIUS/4, BLADE_RADIUS/4);
				Iterator<LivingEntity> mobIter = mMobs.iterator();
				while (mobIter.hasNext()) {
					LivingEntity mob = mobIter.next();
					if (mBox1.overlaps(mob.getBoundingBox()) || mBox2.overlaps(mob.getBoundingBox()) || mBox3.overlaps(mob.getBoundingBox())) {
						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell);
						MovementUtils.knockAway(mPlayer, mob, mKnockback);
						mobIter.remove();
					}
				}

				mWorld.spawnParticle(Particle.SWEEP_ATTACK, bladeLoc1, 3, 0.35, 0, 0.35, 1);

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