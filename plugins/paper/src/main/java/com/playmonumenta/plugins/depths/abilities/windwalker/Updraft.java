package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class Updraft extends DepthsAbility {

	public static final String ABILITY_NAME = "Updraft";
	public static final int COOLDOWN = 8 * 20;
	private static final int RADIUS = 3;
	private static final float[] KNOCKBACK_SPEED = {0.7f, 0.9f, 1.1f, 1.3f, 1.5f};
	private static final String[] DESC_VALUES = {"4", "5", "6", "7", "8"};

	public Updraft(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.WHITE_DYE;
		mTree = DepthsTree.WINDWALKER;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.UPDRAFT;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action trigger) {
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Location pLoc = loc;
		pLoc.add(0, -0.75, 0);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 0.85f);

		new BukkitRunnable() {
			double mRadius = RADIUS;
			@Override
			public void run() {
				for (double j = 0; j < 360; j += 6) {
					double radian1 = Math.toRadians(j);
					pLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.CLOUD, pLoc, 3, 0, 0, 0, 0.125);
					pLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
				}
				this.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);

		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS, mPlayer)) {
			if (!e.getName().contains("Dionaea")) {
				launch(e);
			}
		}
		for (Player p : PlayerUtils.playersInRange(loc, RADIUS, true)) {
			launch(p);
		}
		for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
			if (entity != null && entity instanceof IronGolem) {
				launch(entity);
			}
		}
	}

	private void launch(Entity e) {
		World world = e.getWorld();
		Vector v = e.getVelocity();
		v.setY(KNOCKBACK_SPEED[mRarity - 1]);
		e.setVelocity(v);
		world.spawnParticle(Particle.END_ROD, e.getLocation(), 3, 0, 0.5, 0, 0.5);
		world.spawnParticle(Particle.BUBBLE_COLUMN_UP, e.getLocation(), 6, 0, 0, 0, 0.25);
	}

	@Override
	public String getDescription(int rarity) {
		return "Right click while sneaking to push players and enemies within a " + RADIUS + " block radius of you upwards by " + DepthsUtils.getRarityColor(rarity) + DESC_VALUES[rarity - 1] + ChatColor.WHITE + " blocks. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public boolean runCheck() {
		return (mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()));
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}
}