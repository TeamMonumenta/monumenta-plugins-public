package com.playmonumenta.plugins.depths.bosses.spells;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellAbyssalSpawnPassive extends Spell {

	public static final int LEASH_DISTANCE = 30;
	private static final Particle.DustOptions VEX_COLOR = new Particle.DustOptions(Color.fromRGB(0, 41, 58), 1.0f);

	private Entity mLauncher;
	private List<LivingEntity> mVexes;
	private int mTicks = 0;


	public SpellAbyssalSpawnPassive(LivingEntity mBoss, List<LivingEntity> vexes) {
		mLauncher = mBoss;
		mVexes = vexes;
		//Give vexes immortality
		for (LivingEntity vex : mVexes) {
			vex.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10000000, 4));
		}
	}

	@Override
	public void run() {
		//Runs every 5 ticks
		mTicks += 5;

		for (LivingEntity vex : mVexes) {

			if (vex == null || vex.isDead()) {
				continue;
			}

			//Leave particle trail
			vex.getWorld().spawnParticle(Particle.REDSTONE, vex.getLocation(), 2, 0.2, 0.2, 0.2, VEX_COLOR);

			//Teleport if either is too far away from davey
			if (vex.getLocation().distance(mLauncher.getLocation()) > LEASH_DISTANCE) {
				vex.teleport(mLauncher);
			}

			//Set target to nearest player if null
			if (((Mob) vex).getTarget() == null) {
				((Mob) vex).setTarget(EntityUtils.getNearestPlayer(vex.getLocation(), 30));
			}

			Location mLoc = vex.getLocation();
			World mWorld = vex.getWorld();

			for (double deg = 0; deg < 360; deg += 6) {
				mWorld.spawnParticle(Particle.DOLPHIN, mLoc.clone().add(2 * FastUtils.cos(deg), 0, 2 * FastUtils.sin(deg)), 1, 0, 0, 0, 0);
				mWorld.spawnParticle(Particle.DOLPHIN, mLoc.clone().add(2 * FastUtils.cos(deg), 2 * FastUtils.sin(deg), 0), 1, 0, 0, 0, 0);
				mWorld.spawnParticle(Particle.DOLPHIN, mLoc.clone().add(0, 2 * FastUtils.sin(deg), 2 * FastUtils.cos(deg)), 1, 0, 0, 0, 0);
			}

			if (mTicks % 20 == 0) {
				for (Player player : PlayerUtils.playersInRange(vex.getLocation(), 2, true)) {
					BossUtils.bossDamagePercent((LivingEntity) mLauncher, player, 0.2, (Location) null, "Abyssal Spawn");
				}
			}


		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
