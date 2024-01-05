package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCosmicPortals extends Spell {

	private boolean mOnCooldown;
	private Sirius mSirius;
	private Plugin mPlugin;
	private static final int COOLDOWN = 15 * 20;
	private static final int DURATION = 4 * 20;

	private static final int PORTALHEIGHT = 3;
	private static final double PORTALSPERPLAYER = 0.25;
	private static final int RADIUS = 2;

	public SpellCosmicPortals(Sirius sirius, Plugin plugin) {
		mOnCooldown = false;
		mSirius = sirius;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		//teleports players 15 blocks behind sirius in the balcony row.
		double mPortalCount = mSirius.getPlayersInArena(false).size() * PORTALSPERPLAYER;
		List<Player> mTargets = mSirius.getPlayersInArena(true);
		Collections.shuffle(mTargets);
		for (int i = 0; i < mPortalCount; i++) {
			World world = mTargets.get(i).getWorld();
			Location loc = mTargets.get(i).getLocation();
			world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1f, 0.6f);
			world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.6f);
			world.playSound(loc, Sound.ENTITY_MOOSHROOM_CONVERT, SoundCategory.HOSTILE, 0.3f, 0.6f);
			world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 0.3f, 1.4f);
			portal(mTargets.get(i));
		}
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void portal(Player target) {
		int mRadius = RADIUS;
		if (mSirius.mBlocks <= 10) {
			mRadius++;
		}
		int finalMRadius = mRadius;
		new BukkitRunnable() {
			int mTicks = 0;
			Location mPortalLoc = target.getLocation();

			@Override
			public void run() {
				if (mTicks < DURATION - 2 * 20) {
					mPortalLoc = target.getLocation().add(0, 0.1, 0);
				}
				new PPCircle(Particle.REDSTONE, mPortalLoc, finalMRadius).ringMode(true).count(30).data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.0f)).spawnAsBoss();
				if (mTicks >= DURATION - 2 * 20 && mTicks % 10 == 0) {
					new PPSpiral(Particle.END_ROD, mPortalLoc, finalMRadius).count(10).spawnAsBoss();
				}

				if (mTicks > DURATION) {
					for (Player p : mPortalLoc.getNearbyPlayers(finalMRadius)) {
						int rand = FastUtils.randomIntInRange(0, 1);
						Location tpLoc = mSirius.mBoss.getLocation().add(15, PORTALHEIGHT, (Math.pow(-1, rand) * 30));
						//stops falling off the back
						if (tpLoc.getX() - mSirius.mStartLocation.getX() > 35) {
							tpLoc.setX(mSirius.mStartLocation.getX() + 35);
						}
						if (tpLoc.getBlock().isSolid()) {
							tpLoc = LocationUtils.fallToGround(tpLoc.clone().add(0, 10, 0), tpLoc.getY());
						}
						p.playSound(p, Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 0.4f, 0.9f);
						p.playSound(p, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 0.8f, 0.8f);
						p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.4f);
						p.playSound(p, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.HOSTILE, 2f, 0.6f);
						p.teleport(tpLoc);
					}
					this.cancel();
				}
				if (mSirius.mBoss.isDead()) {
					this.cancel();
				}
				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}
}
