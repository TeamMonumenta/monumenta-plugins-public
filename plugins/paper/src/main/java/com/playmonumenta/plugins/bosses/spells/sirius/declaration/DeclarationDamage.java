package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlight;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DeclarationDamage extends Spell {

	private Plugin mPlugin;
	private Sirius mSirius;
	private double mOrbRadius;
	private boolean mDone;
	private PassiveStarBlightConversion mConverter;

	private static final int RADIUS = 3;
	private static final int ANIMATIONDURATION = 5 * 20;
	private static final Color STARBLIGHT = Color.fromRGB(0, 128, 128);
	private static final double RADIUSCHANGE = RADIUS / (2.0 * 20) * 5.0;

	public DeclarationDamage(Plugin plugin, Sirius sirius, PassiveStarBlightConversion converter) {
		mPlugin = plugin;
		mSirius = sirius;
		mConverter = converter;
	}

	@Override
	public void run() {
		for (Player p : mSirius.getPlayersInArena(false)) {
			p.playSound(p, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1, 1);
		}
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (Player p : mSirius.getPlayersInArena(false)) {
				p.playSound(p, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1, 2);
			}
		}, 14);
		mOrbRadius = 0;
		Location mOldLoc = mSirius.mBoss.getLocation().clone();
		Location mPortalOne = mSirius.mBoss.getLocation().clone().add(-2, 3, 5);
		Location mPortalTwo = mSirius.mBoss.getLocation().clone().add(-2, 3, -5);
		Location mOrbLocation = mSirius.mBoss.getLocation().clone().add(-2, 3, 0);
		mDone = false;
		for (Player p : mSirius.getPlayersInArena(false)) {
			MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("Let the symbol of infinity form in the naked air. The blight must spread!", NamedTextColor.AQUA, TextDecoration.BOLD));
		}
		mSirius.startDamagePhase(
			"Tuulen",
			Component.text("Sirius has taken so much damage from your assault he couldn't fire his blast. Well done.", NamedTextColor.GRAY),
			"Tuulen",
			Component.text("No! You need to damage Sirius more if we ever want a chance at disrupting that blast!", NamedTextColor.GRAY));
		new BukkitRunnable() {
			int mTicks = 0;
			double mXOne = mOrbLocation.getX();
			double mXTwo = mOrbLocation.getX() - 5;

			@Override
			public void run() {
				if (mTicks == 0) {
					for (Player p : mSirius.getPlayersInArena(false)) {
						p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.3f, 0.6f);
						p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_3, SoundCategory.HOSTILE, 0.5f, 1.5f);
						p.playSound(p, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 0.5f, 0.4f);
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						for (Player p : mSirius.getPlayersInArena(false)) {
							p.playSound(p, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 2, 0.8f);
							p.playSound(p, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.4f, 0.7f);
						}
					}, 7);

				}
				for (Player p : mSirius.getPlayersInArena(false)) {
					if (LocationUtils.getVectorTo(p.getLocation(), mOrbLocation).length() <= mOrbRadius) {
						PassiveStarBlight.applyStarBlight(p);
					}
				}
				if (mTicks % 10 == 0) {
					ParticleUtils.drawSphere(mOrbLocation, 32, mOrbRadius, new ParticleUtils.ParametricParticle() {
						@Override
						public void run(Location loc, int t) {
							new PartialParticle(Particle.REDSTONE, loc).count(1).data(new Particle.DustOptions(STARBLIGHT, 1.25f)).spawnAsBoss();
						}
					});
					if (!mDone) {
						for (int rad = 0; rad < 32; rad++) {
							// outer circle
							new PartialParticle(Particle.REDSTONE, mPortalOne.clone().add(FastUtils.cos((rad * Math.PI) / 16) * RADIUS, FastUtils.sin((rad * Math.PI) / 16) * RADIUS, 0)).data(new Particle.DustOptions(STARBLIGHT, 2.5f)).count(2).spawnAsBoss();
							new PartialParticle(Particle.REDSTONE, mPortalTwo.clone().add(FastUtils.cos((rad * Math.PI) / 16) * RADIUS, FastUtils.sin((rad * Math.PI) / 16) * RADIUS, 0)).data(new Particle.DustOptions(STARBLIGHT, 2.5f)).count(2).spawnAsBoss();

						}
					}
				}
				if (mTicks % 40 == 0 && mTicks < Sirius.DAMAGE_PHASE_DURATION) {
					shrinkCircle(mPortalOne, mPortalTwo);
				}
				if (!mOldLoc.equals(mSirius.mBoss.getLocation())) {
					this.cancel();
					return;
				}
				if (mTicks >= Sirius.DAMAGE_PHASE_DURATION && !mDone) {
					if (mOldLoc.equals(mSirius.mBoss.getLocation())) {
						mDone = true;
					}
				}
				if (mTicks >= Sirius.DAMAGE_PHASE_DURATION) {
					mConverter.convertLine(mXOne, mXTwo, mOrbLocation.getZ(), 11, mOrbLocation);
					mXOne -= 2;
					mXTwo -= 2;
				}
				if (mTicks >= Sirius.DAMAGE_PHASE_DURATION + ANIMATIONDURATION) {
					if (mDone) {
						mSirius.changeHp(true, -5);
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

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void shrinkCircle(Location mPortalOne, Location mPortalTwo) {

		new BukkitRunnable() {
			int mTicks = 0;
			double mCurrentRadius = RADIUS;
			Location mBulletOne = mPortalOne.clone();
			Location mBulletTwo = mPortalTwo.clone();


			@Override
			public void run() {
				if (mTicks <= 2 * 20) {
					for (int rad = 0; rad < 32; rad++) {
						// outer circle
						new PartialParticle(Particle.REDSTONE, mPortalOne.clone().add(FastUtils.cos((rad * Math.PI) / 16) * mCurrentRadius, FastUtils.sin((rad * Math.PI) / 16) * mCurrentRadius, 0)).data(new Particle.DustOptions(STARBLIGHT, 1f)).count(2).spawnAsBoss();
						new PartialParticle(Particle.REDSTONE, mPortalTwo.clone().add(FastUtils.cos((rad * Math.PI) / 16) * mCurrentRadius, FastUtils.sin((rad * Math.PI) / 16) * mCurrentRadius, 0)).data(new Particle.DustOptions(STARBLIGHT, 1f)).count(2).spawnAsBoss();
					}
					mCurrentRadius -= RADIUSCHANGE;
				} else {
					new PPLine(Particle.REDSTONE, mBulletOne, mBulletOne.clone().add(0, 0, -1.25)).data(new Particle.DustOptions(STARBLIGHT, 1f)).count(5).spawnAsBoss();
					new PPLine(Particle.REDSTONE, mBulletTwo, mBulletTwo.clone().add(0, 0, 1.25)).data(new Particle.DustOptions(STARBLIGHT, 1f)).count(5).spawnAsBoss();
					mBulletOne.add(0, 0, -1.25);
					mBulletTwo.add(0, 0, 1.25);
					new PartialParticle(Particle.REDSTONE, mBulletOne).data(new Particle.DustOptions(STARBLIGHT, 2f)).count(1).spawnAsBoss();
					new PartialParticle(Particle.REDSTONE, mBulletTwo).data(new Particle.DustOptions(STARBLIGHT, 2f)).count(1).spawnAsBoss();
				}
				if (mTicks >= 3 * 20) {
					mOrbRadius += 0.25;
					this.cancel();
				}
				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}
}
