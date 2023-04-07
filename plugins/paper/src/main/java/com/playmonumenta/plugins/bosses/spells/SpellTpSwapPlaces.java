package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellTpSwapPlaces extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mLauncher;
	private final int mCooldown;
	private final int mTargetRange;
	private final int mTeleportRange;
	private final int mDuration;
	private final ParticlesList mParticles;

	public SpellTpSwapPlaces(Plugin plugin, LivingEntity launcher, int cooldown) {
		this(plugin, launcher, cooldown, 16, 50);
	}

	public SpellTpSwapPlaces(Plugin plugin, LivingEntity launcher, int cooldown, int range, int duration) {
		this(plugin, launcher, cooldown, range, range, duration, ParticlesList.fromString("[(PORTAL,10,1,1,1,0.03)]"));
	}

	public SpellTpSwapPlaces(Plugin plugin, LivingEntity launcher, int cooldown, int targetRange, int teleportRange, int duration, ParticlesList particles) {
		mPlugin = plugin;
		mLauncher = launcher;
		mCooldown = cooldown;
		mTargetRange = targetRange;
		mTeleportRange = teleportRange;
		mDuration = duration;
		mParticles = particles;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mLauncher.getLocation(), mTargetRange, false);
		while (!players.isEmpty()) {
			Player target = players.get(FastUtils.RANDOM.nextInt(players.size()));

			/* Do not teleport to players in safezones */
			if (ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)) {
				/* This player is in a safe area - don't tp to them */
				players.remove(target);
			} else {

				animation(target);
				break;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void launch(Player target) {
		// set targetLoc to target and slightly elevate it
		Location targetLoc = target.getLocation();
		targetLoc.setY(target.getLocation().getY() + 0.1f);

		// set mobLoc to mLauncher and slightly elevate it
		Location mobLoc = mLauncher.getLocation();
		mobLoc.setY(mLauncher.getLocation().getY() + 0.1f);
		mobLoc.setYaw(target.getLocation().getYaw());
		mobLoc.setPitch(target.getLocation().getPitch());

		World world = mLauncher.getWorld();

		if (targetLoc.getWorld() != world || targetLoc.distance(mobLoc) > mTeleportRange) {
			return;
		}

		new PartialParticle(Particle.SPELL_WITCH, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.SMOKE_LARGE, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2, 0), 12, 0, 0.45, 0, 0.125).spawnAsEntityActive(mLauncher);
		EntityUtils.teleportStack(mLauncher, targetLoc);
		target.teleport(mobLoc);
		new PartialParticle(Particle.SPELL_WITCH, targetLoc.clone().add(0, mLauncher.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.SMOKE_LARGE, targetLoc.clone().add(0, mLauncher.getHeight() / 2, 0), 12, 0, 0.45, 0, 0.125).spawnAsEntityActive(mLauncher);
		world.playSound(mLauncher.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 3f, 0.7f);
	}

	private void animation(Player target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 1.4f, 0.5f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				Location particleLoc = mLauncher.getLocation().add(new Location(mLauncher.getWorld(), -0.5f, 0f, 0.5f));
				mParticles.spawn(mLauncher, particleLoc);
				mParticles.spawn(mLauncher, target.getLocation());

				if (EntityUtils.shouldCancelSpells(mLauncher) || target.isDead()) {
					this.cancel();
				}

				if (mTicks > mDuration) {
					launch(target);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

}
