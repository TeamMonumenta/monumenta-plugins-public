package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class PassiveFloralInsignia extends Spell {

	public static final String SPELL_NAME = "Floral Insignia";
	public static final int COOLDOWN = 100;
	public static final int DRAW_INTERVAL = 10;
	public static final int DRAWS_PER_SHOT = COOLDOWN / DRAW_INTERVAL;
	public static final double Y_OFFSET = 14;

	private final Vector mTopVertex = new Vector(0, 4, 0);
	private final Vector mBotVertex = new Vector(0, -4, 0);
	private final Vector[] mMidVertices = {
		new Vector(1, 0, 0),
		new Vector(-1, 0, 0),
		new Vector(0, 0, 1),
		new Vector(0, 0, -1),
		new Vector(0.5, 0, 0.5),
		new Vector(-0.5, 0, 0.5),
		new Vector(0.5, 0, -0.5),
		new Vector(-0.5, 0, -0.5)
	};
	private final Particle.DustOptions mCrystalOptions = new Particle.DustOptions(Color.fromRGB(130, 242, 90), 1);
	private final Particle.DustOptions mFlowerOptions = new Particle.DustOptions(Color.fromRGB(237, 145, 242), 1);
	private final Particle.DustOptions mLaserOptions = new Particle.DustOptions(Color.fromRGB(252, 223, 78), 1);

	private final LivingEntity mBoss;
	private final int mFloorY;
	private final Location mInsigniaLoc;

	private int mSpellTicks = 0;
	private int mDraws = 0;

	public PassiveFloralInsignia(LivingEntity boss, int floorY) {
		mBoss = boss;
		mFloorY = floorY;
		mInsigniaLoc = boss.getLocation().add(0, Y_OFFSET, 0);
	}

	@Override
	public void run() {
		// Handle passive cooldown
		if (mSpellTicks < DRAW_INTERVAL) {
			mSpellTicks++;
			return;
		}
		mSpellTicks = 0;

		drawSpell();

		mDraws++;
		if (mDraws >= DRAWS_PER_SHOT) {
			mDraws = 0;
			shoot();
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), this::shoot, DRAW_INTERVAL);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), this::shoot, DRAW_INTERVAL * 2);
		} else if (mDraws == DRAWS_PER_SHOT - 1) {
			telegraphNextShot();
		}
	}

	private void drawSpell() {
		PlayerUtils.playersInRange(mInsigniaLoc, 100, true).forEach(player -> {
			Vector dir = LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(player), mInsigniaLoc);
			Location mCenter = mInsigniaLoc.clone().setDirection(dir);

			Vector axis1 = VectorUtils.rotationToVector(mCenter.getYaw(), mCenter.getPitch() - 90);
			Vector axis2 = axis1.clone().crossProduct(mCenter.getDirection());

			Location topVertexLoc = relativeToAbsolute(mInsigniaLoc, mTopVertex, axis1, dir, axis2);
			Location botVertexLoc = relativeToAbsolute(mInsigniaLoc, mBotVertex, axis1, dir, axis2);
			for (int i = 0; i < mMidVertices.length; i++) {
				Location vertexLoc = relativeToAbsolute(mInsigniaLoc, mMidVertices[i], axis1, dir, axis2);
				Location nextVertexLoc;
				if (i == mMidVertices.length - 1) {
					nextVertexLoc = relativeToAbsolute(mInsigniaLoc, mMidVertices[0], axis1, dir, axis2);
				} else {
					nextVertexLoc = relativeToAbsolute(mInsigniaLoc, mMidVertices[i + 1], axis1, dir, axis2);
				}
				new PPLine(Particle.REDSTONE, vertexLoc, topVertexLoc).countPerMeter(0.7).data(mCrystalOptions).spawnForPlayer(ParticleCategory.BOSS, player);
				new PPLine(Particle.REDSTONE, vertexLoc, botVertexLoc).countPerMeter(0.7).data(mCrystalOptions).spawnForPlayer(ParticleCategory.BOSS, player);
				new PPLine(Particle.REDSTONE, vertexLoc, nextVertexLoc).countPerMeter(0.7).data(mCrystalOptions).spawnForPlayer(ParticleCategory.BOSS, player);
			}
			new PPFlower(Particle.REDSTONE, mInsigniaLoc, 4).normal(dir).data(mFlowerOptions).petals(7).angleStep(0.1).spawnForPlayer(ParticleCategory.BOSS, player);
		});
	}

	private void shoot() {
		mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 10, 1.5f);
		mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 10, 1.5f);
		PlayerUtils.playersInRange(mInsigniaLoc, 100, true).forEach(player -> {
			FlowerPower.launchEnergyLaser(player, mInsigniaLoc, mBoss, mLaserOptions, mFloorY, mActiveRunnables, true);
		});
	}

	private void telegraphNextShot() {
		mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, 1.5f);
		mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, 1.5f);
		mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, 1.5f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, 1.5f);
			mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, 1.5f);
			mInsigniaLoc.getWorld().playSound(mInsigniaLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10, 1.5f);
		}, 5);
	}

	private Location relativeToAbsolute(Location center, Vector relative, Vector xVec, Vector yVec, Vector zVec) {
		Location absolute = center.clone();
		absolute.add(xVec.clone().multiply(relative.getX()));
		absolute.add(yVec.clone().multiply(relative.getY()));
		absolute.add(zVec.clone().multiply(relative.getZ()));
		return absolute;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

}
