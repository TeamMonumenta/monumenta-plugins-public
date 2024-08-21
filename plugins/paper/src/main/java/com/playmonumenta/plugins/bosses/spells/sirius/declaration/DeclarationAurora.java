package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.CustomTimerEffect;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DeclarationAurora extends Spell {
	private final Location mCenter;
	private final Plugin mPlugin;
	private final Sirius mSirius;
	private List<Location> mPowerLocation;
	private List<Entity> mPowerEntities;
	private static final int DURATION = 10 * 20;
	private static final int RADIUS = 3;


	//x 12 either side
	//z 35 either side
	public DeclarationAurora(Plugin plugin, Location center, Sirius sirius) {
		mCenter = center;
		mPlugin = plugin;
		mSirius = sirius;

		//will be reset but gets rid of null warnings
		mPowerLocation = new ArrayList<>();
		mPowerEntities = new ArrayList<>();
	}

	@Override
	public void run() {
		for (Player p : mSirius.getPlayers()) {
			p.playSound(p, Sound.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundCategory.HOSTILE, 1, 1);
			p.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1, 1.5f);
		}
		mSirius.mAnimationLock = true;
		for (Player p : mSirius.getPlayers()) {
			MessagingUtils.sendNPCMessage(p, "Aurora", Component.text("I have plucked forth stardust for our battle. Gather it, quickly!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
		}
		mPowerLocation = new ArrayList<>();
		mPowerEntities = new ArrayList<>();
		int mSpawns = mSirius.getPlayers().size() - 1 + 3;
		generateEnergy(Math.min(mSpawns, 18));
		new BukkitRunnable() {
			int mTicks = 0;
			int mKilled = 0;
			int mSpawned = mPowerEntities.size();
			final ChargeUpManager mManager = new ChargeUpManager(mSirius.mBoss, DURATION, Component.text(mSpawns + " Star Energy Remaining!", NamedTextColor.DARK_PURPLE), BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10, 75);

			@Override
			public void run() {
				mManager.nextTick();
				List<Entity> mDead = new ArrayList<>();
				for (Entity entity : mPowerEntities) {
					if (entity.isDead()) {
						LivingEntity livingEntity = ((LivingEntity) entity).getKiller();
						if (livingEntity != null) {
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(livingEntity, Sirius.PARTICIPATION_TAG, new CustomTimerEffect(DURATION, "Participated").displays(false));
						}
						mKilled++;
						mDead.add(entity);
					}
				}
				mSpawned += mDead.size();
				mPowerEntities.removeAll(mDead);
				if (mSpawns > mSpawned) {
					generateEnergy(Math.min(mSpawns - mSpawned, 18 - mPowerEntities.size()));
				}
				if (mPowerEntities.isEmpty()) {
					//PEW PEW
					mManager.remove();
					for (Player p : mSirius.getPlayers()) {
						MessagingUtils.sendNPCMessage(p, "Aurora", Component.text("That should be enough. Let me shape it into a weapon... \nStrike the Herald! Now!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
						//p.sendMessage(Component.text("[Aurora]", NamedTextColor.GOLD).append(Component.text(" That should be enough. Let me shape it into a weapon... \n Strike the Herald! Now!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)));
					}
					//Bukkit.getScheduler().runTaskLater(mPlugin, () -> mSirius.changeHp(true, 2), 1);
					damagePhaseAnimation();
					this.cancel();
				}
				if (mSirius.mBoss.isDead()) {
					mManager.remove();
					for (Entity entity : mPowerEntities) {
						entity.remove();
					}
					this.cancel();
				}
				if (mTicks >= DURATION) {
					mManager.remove();
					for (Entity entity : mPowerEntities) {
						entity.remove();
					}
					for (Player p : mSirius.getPlayers()) {
						MessagingUtils.sendNPCMessage(p, "Aurora", Component.text("No! I needed more! Incompetence!", NamedTextColor.DARK_PURPLE));
					}
					mSirius.mAnimationLock = false;
					mSirius.changeHp(true, -5);
					this.cancel();
				}
				int mRemaining = mSpawns - mKilled;
				mManager.setTitle(Component.text(mRemaining + " Star Energy Remaining!", NamedTextColor.DARK_PURPLE));
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void damagePhaseAnimation() {

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Location mPortalLocation = mSirius.mBoss.getLocation().add(0, 20, 0);
				//portal generation
				if (mTicks % 5 == 0) {
					new PPCircle(Particle.END_ROD, mPortalLocation, 3.1).ringMode(true).count(30).spawnAsBoss();
					for (double i = 0; i < 3; i += 0.5) {
						new PPCircle(Particle.REDSTONE, mPortalLocation, 3 - i).count(15).ringMode(true)
							.data(calculateColorProgress((int) i, 3)).spawnAsBoss();
					}
				}
				if (mTicks == 20) {
					orbitalBeam(mPortalLocation);
				}
				if (mTicks > 31) {
					mSirius.mAnimationLock = false;
					mSirius.startDamagePhase(
						"Aurora",
						Component.text("You've done it! Sirius has fallen back once again!", NamedTextColor.DARK_PURPLE),
						"Aurora",
						Component.text("No! Your attacks were not enough!", NamedTextColor.DARK_PURPLE)
					);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 2, 1);
	}

	private void orbitalBeam(Location mPortalLoc) {
		new BukkitRunnable() {
			int mTicks = 0;
			double mRadius = 0.1;
			double mInnerRadius = -0.5;
			final double mIncrease = RADIUS / 10.0;
			final Location mGround = mSirius.mBoss.getLocation().subtract(0, 2, 0);

			@Override
			public void run() {
				for (double radius = (mInnerRadius < 0 ? 0 : mInnerRadius); radius < mRadius; radius += 0.5) {
					for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 6) {
						Location loc = mPortalLoc.clone().add(FastUtils.cos(theta) * radius, 0, FastUtils.sin(theta) * radius);
						for (double i = mPortalLoc.getY(); i > mGround.getY(); i -= 0.5) {
							new PartialParticle(Particle.REDSTONE, loc, 1).data(calculateColorProgress((int) (mPortalLoc.getY() - i), 20)).delta(0.25).spawnAsBoss();
							loc.subtract(0, 0.5, 0);
						}
						World world = loc.getWorld();
						world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 2f, 2f);
						world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 0.6f, 0.1f);
						world.playSound(loc, Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 1.1f, 0.1f);
						world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.4f, 2f);
						world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 2f, 1.3f);
					}
				}
				if (mTicks >= 10) {
					mSirius.changeHp(true, 2);
					this.cancel();
				}
				mRadius += mIncrease * 2;
				mInnerRadius += mIncrease;
				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}


	private Particle.DustOptions calculateColorProgress(int distance, int maxDistance) {
		Particle.DustOptions data;
		int halfDistance = maxDistance / 2;
		if (distance < halfDistance) {
			// Transition from start to mid
			data = new Particle.DustOptions(
				ParticleUtils.getTransition(Color.fromRGB(162, 0, 211), Color.fromRGB(208, 96, 213), Math.min(distance / (double) halfDistance, 1)),
				2f
			);
		} else {
			// Transition from mid to end
			data = new Particle.DustOptions(
				ParticleUtils.getTransition(Color.fromRGB(208, 96, 213), Color.fromRGB(211, 211, 211), Math.min((distance - halfDistance) / (double) halfDistance, 1)),
				2f
			);
		}
		return data;
	}

	private void generateEnergy(int count) {
		mPowerLocation.clear();
		for (int i = 0; i < Math.min(count, 18); i++) {
			Location loc = mCenter.clone().add(FastUtils.randomIntInRange(-12, 12), 10 + FastUtils.randomIntInRange(-3, 3), FastUtils.randomIntInRange(-36, 36));
			if (loc.getBlock().getType().isSolid() && !mPowerLocation.contains(loc)) {
				i--;
			} else {
				//avoid generating star energy in walls.
				mPowerLocation.add(loc);
			}
		}
		for (Location loc : mPowerLocation) {
			/*
			 * Invisible, Glowing, No ai, no gravity, 1hp small magma cube
			 */
			new PPExplosion(Particle.END_ROD, loc).count(3).delta(1).spawnAsBoss();
			Entity mSpawn = LibraryOfSoulsIntegration.summon(loc, "StarEnergy");
			if (mSpawn != null) {
				mPowerEntities.add(mSpawn);
				GlowingManager.startGlowing(mSpawn, NamedTextColor.DARK_PURPLE, -1, GlowingManager.BOSS_SPELL_PRIORITY);
				World world = mSpawn.getWorld();
				world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 2, 0.1f);
				world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 1f);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 2f, 0.1f);
					world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 1.3f);
				}, 4);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 2f, 0.6f);
					world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 1.6f);
				}, 8);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, 2f, 0.9f);
					world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 2f);
					world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.1f, 2f);
				}, 12);
			}
		}
	}


}



