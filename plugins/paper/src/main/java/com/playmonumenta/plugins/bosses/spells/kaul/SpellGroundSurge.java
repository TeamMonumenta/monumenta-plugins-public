package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellGroundSurge extends Spell {
	private static final String SPELL_NAME = "Ground Surge";
	private static final String SLOWNESS_TAG = "GroundSurgeSlowness";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;

	private final ChargeUpManager mChargeUp;

	public SpellGroundSurge(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;

		mChargeUp = Kaul.defaultChargeUp(mBoss, (int) (20 * 2.5), SPELL_NAME);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME);
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
			new BaseMovementSpeedModifyEffect((int) (20 * 2.75), -0.3));

		List<Player> players = Kaul.getArenaParticipants(mCenter);
		BukkitRunnable runnable = new BukkitRunnable() {
			float mPitch = 0;

			@Override
			public void run() {
				Location loc = mBoss.getLocation();
				mPitch += 0.025f;
				if (mChargeUp.getTime() % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 3, mPitch);
				}

				new PartialParticle(Particle.BLOCK_CRACK, loc, 8, 0.4, 0.1, 0.4, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);

				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.reset();
					if (players.isEmpty()) {
						return;
					}

					final int targets;
					switch (players.size()) {
						case 1 -> targets = 1;
						case 2 -> targets = 2;
						case 3 -> targets = 3;
						case 4, 5, 6 -> targets = 4;
						case 7, 8, 9, 10 -> targets = 5;
						case 11, 12, 13, 14, 15 -> targets = 6;
						case 16, 17, 18, 19, 20 -> targets = 7;
						default -> targets = 8;
					}

					List<Player> toHit = new ArrayList<>();
					Collections.shuffle(players);
					for (int i = 0; i < targets; i++) {
						toHit.add(players.removeLast());
					}

					Location nLoc = mBoss.getLocation().add(0, 0.5, 0);
					for (Player target : toHit) {
						launchBeam(target, nLoc, players);
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void launchBeam(Player target, Location nLoc, List<Player> otherPlayers) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mInnerTicks = 0;
			final World mWorld = mBoss.getWorld();
			final Vector mDisplacement = LocationUtils.getDirectionTo(target.getLocation(), nLoc).setY(0).normalize().multiply(1.1);
			Location mBoxLoc = nLoc.clone();

			@Override
			public void run() {
				mInnerTicks++;
				mBoxLoc.add(mDisplacement);
				if (mBoxLoc.getBlock().getType().isSolid()) {
					mBoxLoc = LocationUtils.emergeFromGround(mBoxLoc, mBoxLoc.getY() + 3);
				}
				if (!mBoxLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
					mBoxLoc = LocationUtils.fallToGround(mBoxLoc, mBoxLoc.getY() - 3);
				}

				if (mInnerTicks >= 45) {
					this.cancel();
				}

				mWorld.playSound(mBoxLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.75f, 1);

				new PartialParticle(Particle.BLOCK_CRACK, mBoxLoc, 20, 0.5, 0.5, 0.5, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.FLAME, mBoxLoc, 15, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.LAVA, mBoxLoc, 2, 0.5, 0.5, 0.5, 0.25).spawnAsEntityActive(mBoss);

				List<Player> hitPlayers = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(mBoxLoc.clone().add(0, 0.5, 0), 0.65, 0.65, 0.65)).getHitPlayers(true);
				if (hitPlayers.isEmpty()) {
					return;
				}

				this.cancel();

				for (Player player : hitPlayers) {
					DamageUtils.damage(mBoss, player, DamageType.BLAST, 24, null, false, true, SPELL_NAME);
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_TAG, new PercentSpeed(20 * 20, -0.5, SLOWNESS_TAG));
					MovementUtils.knockAway(mBoss.getLocation(), player, 0.3f, 1f);

					new PartialParticle(Particle.SMOKE_LARGE, mBoxLoc, 20, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.FLAME, mBoxLoc, 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);

					mWorld.playSound(mBoxLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);

					if (otherPlayers.isEmpty()) {
						return;
					}
					launchBeam(otherPlayers.removeLast(), player.getLocation().add(0, 0.5, 0), List.of());
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public boolean canRun() {
		if (mBoss instanceof Mob mob) {
			LivingEntity target = mob.getTarget();
			return target instanceof Player;
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}
