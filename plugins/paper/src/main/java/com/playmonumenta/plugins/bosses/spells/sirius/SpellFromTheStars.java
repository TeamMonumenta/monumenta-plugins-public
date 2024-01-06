package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class SpellFromTheStars extends Spell {

	private Sirius mSirius;
	private boolean mOnCooldown;
	private Plugin mPlugin;
	private boolean mPrimed;
	private final PassiveTentacleManager mTentancleManager;
	private final PassiveDeclaration mDeclerations;

	private static final int COOLDOWN = 10 * 20;
	private static final int RADIUS = 15;
	private static final int JUMPHEIGHT = 15;
	private static final int DAMAGE = 50;


	public SpellFromTheStars(Sirius sirius, Plugin plugin, PassiveTentacleManager tentacles, PassiveDeclaration declaration) {
		mSirius = sirius;
		mOnCooldown = false;
		mPlugin = plugin;
		mTentancleManager = tentacles;
		mPrimed = false;
		mDeclerations = declaration;
	}


	@Override
	public void run() {
		mPrimed = true;
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!mSirius.mDamagePhase && !mSirius.mAnimationLock) {
					slam();
					mPrimed = false;
					this.cancel();
				}
				if (mTicks >= 20 * 20) {
					mPrimed = false;
					this.cancel();
				}
				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void slam() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		//DELAY till sirius slams down. Probably goes up for 2 seconds and slams down in 0.5 second.
		List<Vector3f> mTranslations = new ArrayList<>();
		for (Display display : mSirius.mDisplays) {
			mTranslations.add(display.getTransformation().getTranslation());
		}
		mSirius.stopCollision();
		mTentancleManager.mCancelMovements = true;
		new BukkitRunnable() {
			int mTicks = 0;
			int mRadius = RADIUS;
			Location mCircleLoc = LocationUtils.fallToGround(mSirius.mBoss.getLocation(), mSirius.mBoss.getLocation().subtract(0, 10, 0).getY());
			ChargeUpManager mManager = new ChargeUpManager(mSirius.mBoss, 50, Component.text("Preparing to Slam", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 50);

			@Override
			public void run() {
				mManager.nextTick();
				if (mTicks == 0) {
					World world = mSirius.mBoss.getWorld();
					Location loc = mSirius.mBoss.getLocation();
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.4f, 1.2f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.4f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 2f);
					world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.4f, 1.4f);
					world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 1.4f);
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2f, 0.1f);
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 0.6f, 0.6f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.8f);

					if (mSirius.mBlocks <= 10) {
						mRadius += 10;
					}
					mSirius.mBoss.setVisibleByDefault(false); //hide it so it doesnt have to move with interpolation
					for (int i = 0; i < mSirius.mDisplays.size(); i++) {
						mSirius.mDisplays.get(i).setInterpolationDuration(39);
						Transformation trans = mSirius.mDisplays.get(i).getTransformation();
						mSirius.mDisplays.get(i).setTransformation(new Transformation(new Vector3f(0, JUMPHEIGHT, 0).add(mTranslations.get(i)), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						mSirius.mDisplays.get(i).setInterpolationDelay(-1);
					}
				}
				if (mTicks < 50 && mTicks % 5 == 0) {
					new PPCircle(Particle.SCRAPE, mCircleLoc, mRadius).ringMode(true).count(50).spawnAsBoss();
					new PPPillar(Particle.END_ROD, mSirius.mBoss.getLocation(), JUMPHEIGHT - mTicks / 39.0).delta(5, 0, 5).spawnAsBoss();
				}
				if (mTicks == 45) {
					for (int i = 0; i < mSirius.mDisplays.size(); i++) {
						mSirius.mDisplays.get(i).setInterpolationDuration(6);
						Transformation trans = mSirius.mDisplays.get(i).getTransformation();
						mSirius.mDisplays.get(i).setTransformation(new Transformation(mTranslations.get(i), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
						mSirius.mDisplays.get(i).setInterpolationDelay(-1);
					}
				}
				if (mTicks == 50) {
					World world = mSirius.mBoss.getWorld();
					Location loc = mSirius.mBoss.getLocation();
					world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1.0f, 0.1f);
					world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 2.0f);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.1f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.1f);
					mManager.remove();
					DisplayEntityUtils.groundBlockQuake(mSirius.mBoss.getLocation().subtract(0, 2, 0), mRadius,
						List.of(Material.WARPED_WART_BLOCK, Material.STRIPPED_WARPED_HYPHAE), new Display.Brightness(8, 8), 0.015);
					for (Player p : PlayerUtils.playersInRange(mCircleLoc, mRadius, false, true)) {
						DamageUtils.damage(mSirius.mBoss, p, DamageEvent.DamageType.MELEE, DAMAGE, null, false, true, "corrupting blight.");
					}
					mSirius.mBoss.setVisibleByDefault(true); //unhide it
					mSirius.startCollision();
					mTentancleManager.mCancelMovements = false;
					this.cancel();
					return;
				}
				mTicks++;

			}
		}.runTaskTimer(mPlugin, 5, 1);

	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mPrimed && !mDeclerations.mSwapping;
	}
}
