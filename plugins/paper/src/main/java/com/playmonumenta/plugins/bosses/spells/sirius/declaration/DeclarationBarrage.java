package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.*;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DeclarationBarrage extends Spell {
	private Sirius mSirius;
	private Plugin mPlugin;
	private static final int DAMAGE = 50;
	private static final int DURATION = 10 * 20;
	private static final int RADIUS = 3;
	private static final int SPREAD = 5;
	private static final int METEORDURATION = 2 * 20;
	private static final int HEIGHT = 7;
	private int mCount;

	public DeclarationBarrage(Plugin plugin, Sirius sirius) {
		mPlugin = plugin;
		mSirius = sirius;
	}


	@Override
	public void run() {
		for (Player p : mSirius.getPlayersInArena(false)) {
			p.playSound(p, Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1f, 2f);
			MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("The stars whisper their truth to us. Let the comets tell us what they must.", NamedTextColor.AQUA, TextDecoration.BOLD));
		}
		mCount = mSirius.getValidDeclarationPlayersInArena(false).size();

		new BukkitRunnable() {
			List<Location> mTargetLocs = new ArrayList<>();
			ChargeUpManager mBar = new ChargeUpManager(mSirius.mBoss, DURATION, Component.text("Impaling Doom", NamedTextColor.RED), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);
			int mTicks = 0;
			float mMeteorcount = 0;

			@Override
			public void run() {
				mBar.nextTick();
				mMeteorcount += 0.1f;
				while (mMeteorcount >= 1) {
					mMeteorcount--;
					//stop more than 10 players getting meteored per time to prevent lag
					//also players will bunch up more.
					int mPlayerCount = 10;
					List<Player> mPList = mSirius.getPlayersInArena(false);
					Collections.shuffle(mPList);
					for (Player p : mPList) {
						if (mPlayerCount > 0) {
							boolean valid = true;
							int meteorsAround = 0;
							mPlayerCount--;
							for (Location loc : mTargetLocs) {
								//prevents 25 meteors spawning ontop of someone
								if (LocationUtils.getVectorTo(loc, p.getLocation()).length() < ((SPREAD + 1) * (SPREAD + 1))) {
									meteorsAround++;
								}
								if (meteorsAround >= 8) {
									valid = false;
									break;
								}
							}
							if (valid) {
								Location loc = LocationUtils.fallToGround(p.getLocation().clone().add(FastUtils.randomDoubleInRange(-SPREAD, SPREAD), 1, FastUtils.randomDoubleInRange(-SPREAD, SPREAD)), p.getLocation().getY() - 7);
								if (!loc.getBlock().isSolid() && !loc.clone().subtract(0, 1, 0).getBlock().getType().equals(Material.AIR)) {
									mTargetLocs.add(loc);
									meteor(loc);
									Bukkit.getScheduler().runTaskLater(mPlugin, () -> mTargetLocs.remove(loc), METEORDURATION);
								} else if (!p.getLocation().getBlock().isSolid()) {
									mTargetLocs.add(p.getLocation());
									meteor(LocationUtils.fallToGround(p.getLocation(), mSirius.mBoss.getLocation().getY() - 10));
									Bukkit.getScheduler().runTaskLater(mPlugin, () -> mTargetLocs.remove(loc), METEORDURATION);
								}
							}
						}
					}
				}
				if (mSirius.mBoss.isDead()) {
					this.cancel();
				}
				if (mTicks >= DURATION) {
					if (mCount * 0.75 <= mSirius.getPlayersInArena(false).size()) {
						for (Player p : mSirius.getPlayersInArena(false)) {
							MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("No! Your flesh must bind with their blood! Do not escape!", NamedTextColor.AQUA));
						}
						mSirius.changeHp(false, 1);
					} else {
						for (Player p : mSirius.getPlayersInArena(false)) {
							MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("Yes, I can feel their blood take hold... One step closer...", NamedTextColor.AQUA));
						}
						mSirius.changeHp(false, -1);
					}
					mBar.remove();
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void meteor(Location loc) {
		List<BlockDisplay> mDisplays = createSpike(loc);
		new BukkitRunnable() {
			int mTicks = 0;
			World mWorld = loc.getWorld();

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					new PPCircle(Particle.SCULK_CHARGE_POP, loc.clone().add(0, 0.2, 0), RADIUS).ringMode(true).count(35).spawnAsBoss();
				}
				if (mTicks == 1) {
					for (BlockDisplay dis : mDisplays) {
						Transformation trans = dis.getTransformation();
						dis.setInterpolationDuration(34);
						dis.setInterpolationDelay(-1);
						dis.setTransformation(new Transformation(new Vector3f(0, 2, 0).add(trans.getTranslation()), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
					}
					mWorld.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 2f, 2f);
					mWorld.playSound(loc, Sound.ENTITY_WARDEN_ANGRY, SoundCategory.HOSTILE, 1f, 2f);
					mWorld.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 0.4f, 1f);

				}
				if (mTicks == 37) {
					for (BlockDisplay dis : mDisplays) {
						Transformation trans = dis.getTransformation();
						dis.setInterpolationDuration(5);
						dis.setInterpolationDelay(-1);
						dis.setTransformation(new Transformation(new Vector3f(0, 7, 0).add(trans.getTranslation()).sub(0, 4, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
					}
				}
				if (mTicks >= METEORDURATION) {
					mWorld.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 1f, 0.1f);
					mWorld.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 0.2f, 2f);
					mWorld.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.6f, 2f);
					mWorld.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 0.9f, 0.4f);


					new PPExplosion(Particle.END_ROD, loc.add(0, 0.5, 0)).delta(RADIUS).count(15).spawnAsBoss();
					mSirius.mBoss.getWorld().playSound(loc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 1, 2);
					Hitbox hitBox = new Hitbox.UprightCylinderHitbox(loc, HEIGHT, RADIUS);
					for (Player p : new ArrayList<>(hitBox.getHitPlayers(true))) {
						p.playSound(p, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 2f, 0.2f);
						p.playSound(p, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 0.4f, 0.8f);
						DamageUtils.damage(null, p, DamageEvent.DamageType.MELEE, DAMAGE, null, false, false, "impaled on starblight spike");
						MovementUtils.knockAway(loc, p, 0.25f, 0.8f);
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						for (BlockDisplay dis : mDisplays) {
							dis.remove();
						}
					}, 5);
					this.cancel();
				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private BlockDisplay createBlockDisplay(BlockData data, Matrix4f matrix, Location target, List<BlockDisplay> mDisplays) {
		BlockDisplay display = mSirius.mBoss.getWorld().spawn(target.clone().subtract(0, 7, 0), BlockDisplay.class);
		display.setBlock(data);
		display.setInterpolationDelay(-1);
		display.setTransformationMatrix(matrix);
		display.setBrightness(new Display.Brightness(15, 15));
		mDisplays.add(display);
		display.addScoreboardTag("SiriusDisplay");
		return display;
	}

	//Display visuals
	private List<BlockDisplay> createSpike(Location target) {
		List<BlockDisplay> mDisplays = new ArrayList<>();
		createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(3.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 3.0000f, 0.0000f, -1.5000f, 0.0000f, -1.5000f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.STRIPPED_WARPED_HYPHAE),
			new Matrix4f(0.7500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 4.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7500f, 0.0000f, -0.3750f, 0.9000f, -0.3750f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.1875f, 1.0000f, 0.1250f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.STRIPPED_WARPED_HYPHAE),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.2000f, 0.0000f, 0.1875f, 0.9000f, -1.0625f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.STRIPPED_WARPED_HYPHAE),
			new Matrix4f(0.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.2500f, 0.0000f, -0.1250f, 4.1250f, -0.1250f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.8000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.7500f, 1.7500f, -0.5625f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.8125f, 0.8125f, 0.0625f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.STRIPPED_WARPED_HYPHAE),
			new Matrix4f(0.0000f, 0.0000f, 1.2000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, -1.2000f, 0.0000f, 0.0000f, 0.0000f, 0.1875f, 0.9000f, -1.0625f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.1875f, 1.5625f, -0.7500f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.WARPED_WART_BLOCK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 2.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.5625f, 0.3750f, -1.3125f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.STRIPPED_WARPED_HYPHAE),
			new Matrix4f(0.0000f, 0.0000f, 1.2000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, -1.2000f, 0.0000f, 0.0000f, 0.0000f, 0.8125f, 0.9000f, 0.1875f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.9000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 1.1875f, 0.0000f, -0.5000f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.9000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.5000f, 0.0000f, -2.0625f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.9000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.5000f, 0.0000f, 1.0625f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.9000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.9375f, 0.0000f, -0.5000f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.9000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -0.2500f, 1.5625f, 0.2500f, 1.0000f),
			target,
			mDisplays);
		createBlockDisplay(
			Bukkit.createBlockData(Material.SCULK),
			new Matrix4f(1.8000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.0000f, 0.0000f, -1.3750f, 0.8125f, -0.1250f, 1.0000f),
			target,
			mDisplays);
		return mDisplays;

	}
}
