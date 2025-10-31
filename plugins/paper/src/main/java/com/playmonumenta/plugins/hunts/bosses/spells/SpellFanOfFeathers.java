package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellFanOfFeathers extends Spell {
	public static final int COOLDOWN = 20;
	//speed of the bullet
	private static final double SPEED = 1.75;
	//Damage of the spell
	private static final int DAMAGE = 75;
	//How many non-precise feathers
	private static final int ADDITIONAL_FEATHER_COUNT = 5;
	private static final double BOUNDING_BOX_SIZE = 0.875;
	private static final Color STEEL_COLOR = Color.fromRGB(88, 90, 99);
	private static final Particle.DustOptions STEEL = new Particle.DustOptions(STEEL_COLOR, 1f);
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SteelWingHawk mHawk;
	private boolean mOnCooldown = false;

	public SpellFanOfFeathers(Plugin plugin, LivingEntity boss, SteelWingHawk hawk) {
		mPlugin = plugin;
		mBoss = boss;
		mHawk = hawk;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, COOLDOWN + 20);
		mHawk.mFeathers -= 3;
		Location loc = mBoss.getLocation();
		List<Player> pList = PlayerUtils.playersInRange(mHawk.mSpawnLoc, SteelWingHawk.OUTER_RADIUS, true);
		Collections.shuffle(pList);
		for (int i = 0; i < Math.round(pList.size() / 2.0 + 0.4); i++) {
			Location pLoc = pList.get(i).getLocation();
			for (int j = 0; j < ADDITIONAL_FEATHER_COUNT; j++) {
				feather(loc, pLoc.clone().add(FastUtils.randomFloatInRange(-3, 3), 0, FastUtils.randomIntInRange(-3, 3)));
			}
			feather(loc, pLoc);
		}
	}

	//runnable that manages one feather
	private void feather(Location start, Location end) {
		Location featherLoc = start.clone();
		Vector vec = LocationUtils.getDirectionTo(end, start).multiply(SPEED);
		mBoss.getWorld().playSound(featherLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.7f, 2f);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || mTicks > 5 * 20) {
					this.cancel();
				}
				checkForCollision(featherLoc, vec, this);
				featherLoc.add(vec);
				new PartialParticle(Particle.REDSTONE, featherLoc, 20).delta(0.1).data(STEEL).spawnAsBoss();
				new PartialParticle(Particle.END_ROD, featherLoc, 1).delta(0.08).spawnAsBoss();
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	//Checks if feather hits
	private void checkForCollision(Location loc, Vector vec, BukkitRunnable runnable) {
		BoundingBox box = new BoundingBox().shift(loc).expand(BOUNDING_BOX_SIZE);
		Vector movement = vec.clone().multiply(0.5);
		for (int i = 0; i < 2; i++) {
			box.shift(movement);
			for (Player p : PlayerUtils.playersInRange(box.getCenter().toLocation(mBoss.getWorld()), 5, true)) {
				if (p.getBoundingBox().overlaps(box)) {
					DamageUtils.damage(mBoss, p, DamageEvent.DamageType.PROJECTILE, DAMAGE, null, false, true, "Fan of Feathers");
					new PPExplosion(Particle.REDSTONE, box.getCenter().toLocation(mBoss.getWorld())).data(STEEL).spawnAsBoss();
					//good solid thud
					mBoss.getWorld().playSound(box.getCenter().toLocation(mBoss.getWorld()), Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 1, 1.5f);
					runnable.cancel();
					return;
				}
				if (loc.getBlock().getType().isSolid()) {
					runnable.cancel();
					return;
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && mHawk.mFeathers >= 3;
	}
}
