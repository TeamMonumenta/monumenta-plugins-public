package com.playmonumenta.plugins.depths.abilities.flamecaller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

import net.md_5.bungee.api.ChatColor;

public class Flamestrike extends DepthsAbility {
	public static final String ABILITY_NAME = "Flamestrike";
	public static final int COOLDOWN = 10 * 20;
	public static final double[] DAMAGE = {14, 17.5, 21, 24.5, 28};
	public static final int SIZE = 6;
	public static final double DOT_ANGLE = 1d / 3;
	public static final int FIRE_TICKS = 4 * 20;
	public static final float KNOCKBACK = 0.5f;

	public Flamestrike(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.FLAMESTRIKE;
		mDisplayItem = Material.FLINT_AND_STEEL;
		mTree = DepthsTree.FLAMECALLER;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		putOnCooldown();

		double damage = DAMAGE[mRarity - 1];
		Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SIZE, mPlayer)) {
			Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toMobVector) > DOT_ANGLE) {
				EntityUtils.applyFire(mPlugin, FIRE_TICKS, mob, mPlayer);
				EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell, true, true, true, false);
				MovementUtils.knockAway(mPlayer, mob, KNOCKBACK);
			}
		}

		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.SMOKE_LARGE, mPlayer.getLocation(), 15, 0.05, 0.05, 0.05, 0.1);
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					world.spawnParticle(Particle.FLAME, l, 2, 0.15, 0.15, 0.15, 0.15);
					world.spawnParticle(Particle.SMOKE_NORMAL, l, 3, 0.15, 0.15, 0.15, 0.1);
				}

				if (mRadius >= SIZE + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.25f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return (mPlayer.isSneaking() && !isOnCooldown() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while sneaking to create a torrent of flames, dealing " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " damage to all enemies in front of you within a " + SIZE + " block cube around you, setting them on fire for " + FIRE_TICKS / 20 + " seconds and knocking them away. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FLAMECALLER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}
}