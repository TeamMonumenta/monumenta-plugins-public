package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlight;
import com.playmonumenta.plugins.effects.CustomTimerEffect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class DeclarationTp extends Spell {
	private static final int RADIUS = 7;
	private static final int PORTALHEIGHT = 1;
	private static final int DURATION = 12 * 20;
	private final Location mPortalOneLoc;
	private final Location mPortalTwoLoc;
	private final Plugin mPlugin;
	private final Sirius mSirius;


	public DeclarationTp(Plugin plugin, Sirius sirius) {
		mSirius = sirius;
		mPlugin = plugin;
		mPortalOneLoc = mSirius.mBoss.getLocation().clone().add(32, PORTALHEIGHT, 30);
		mPortalOneLoc.setYaw(90);
		mPortalTwoLoc = mSirius.mBoss.getLocation().clone().add(32, PORTALHEIGHT, -30);
		mPortalTwoLoc.setYaw(90);
	}

	@Override
	public void run() {
		Location mMidPoint = new Location(mSirius.mBoss.getWorld(), (mSirius.mTuulenLocation.getX() + mSirius.mAuroraLocation.getX()) / 2 - 10, (mSirius.mTuulenLocation.getY() + mSirius.mAuroraLocation.getY()) / 2, (mSirius.mTuulenLocation.getZ() + mSirius.mAuroraLocation.getZ()) / 2);
		new BukkitRunnable() {
			int mTicks = 0;
			double mRadius = 0;
			final ChargeUpManager mManager = new ChargeUpManager(mSirius.mBoss, 20, Component.text("Distorting Reality", NamedTextColor.AQUA), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);
			final ItemDisplay mTuulenSword = mSirius.mBoss.getWorld().spawn(mSirius.mTuulenLocation.clone().add(-0.5, 0.75, 0.5), ItemDisplay.class);
			final double mMaxDistance = LocationUtils.getVectorTo(mMidPoint.clone(), mSirius.mAuroraLocation.clone()).length();

			@Override
			public void run() {
				mManager.nextTick(5);
				if (mTicks == 0) {
					new BukkitRunnable() {
						int mTicks = 0;

						@Override
						public void run() {
							for (Player p : mSirius.getPlayers()) {
								p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.4f + mTicks / 10.0f);
							}
							if (mTicks >= 10) {
								this.cancel();
							}
							mTicks++;
						}
					}.runTaskTimer(mPlugin, 0, 2);
					mTuulenSword.setItemStack(DisplayEntityUtils.generateRPItem(Material.IRON_SWORD, "Silver Knight's Failure"));
					DisplayEntityUtils.rotateToPointAtLoc(mTuulenSword, LocationUtils.getVectorTo(mMidPoint, mSirius.mTuulenLocation.clone().add(0, 1.25, 0)), 0, -3*Math.PI/4.0f, new Vector3f(2f));
					mTuulenSword.addScoreboardTag("SiriusDisplay");
					for (Player p : mSirius.getPlayers()) {
						MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("You... come join our power...", NamedTextColor.AQUA, TextDecoration.BOLD));
						p.playSound(p, Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, 1f, 0.6f);
					}
					World world = mSirius.mBoss.getWorld();
					world.playSound(mSirius.mTuulenLocation, Sound.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 1, 0.7f);
					world.playSound(mSirius.mTuulenLocation, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 0.4f, 0.8f);
					//TODO add aurora rotating staff doing a similiar animation to refraction. Then add some more flair to the beam.
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						Transformation trans = mTuulenSword.getTransformation();
						Vector vec = LocationUtils.getVectorTo(mMidPoint, mTuulenSword.getLocation());
						mTuulenSword.setInterpolationDelay(-1);
						mTuulenSword.setInterpolationDuration(19);
						mTuulenSword.setTransformation(new Transformation(
							trans.getTranslation().add((float) vec.getX(), (float) vec.getY() + 0.6f, (float) vec.getZ()),
							trans.getLeftRotation(),
							trans.getScale(),
							trans.getRightRotation()));
					}, 1);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						Location mMidPointClone = mMidPoint.clone();
						mMidPointClone.setY(mTuulenSword.getLocation().getY() - 1);
						Vector vec = LocationUtils.getVectorTo(mMidPointClone, mTuulenSword.getLocation());
						mTuulenSword.setInterpolationDelay(-1);
						mTuulenSword.setInterpolationDuration(1);
						mTuulenSword.setTransformation(DisplayEntityUtils.rotateToPointAtLoc(mTuulenSword, vec, -3*Math.PI/4.0f));
						world.playSound(mMidPointClone, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 1, 0.7f);
						world.playSound(mMidPointClone, Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 1, 2);
						world.playSound(mMidPointClone, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 0.9f, 0.8f);
					}, 20);
				}
				if (mTicks == 20) {
					for (Player p : mSirius.getPlayers()) {
						p.playSound(p, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 2f, 0.1f);
						p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.4f, 0.8f);
					}
					mSirius.mStarBlightConverter.restoreFullCircle(mMidPoint, RADIUS + 1);
					mManager.setTime(0);
					mManager.setTitle(Component.text("Channeling Power Behind the Tomb", NamedTextColor.DARK_PURPLE));
					mManager.setChargeTime(DURATION - 20);
					mManager.update();
					List<Player> pList = mSirius.getPlayersInArena(false);
					for (Player p : pList) {
						MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text("There is no time, woolbearer! Come behind the tomb and be cleansed!", NamedTextColor.GRAY, TextDecoration.BOLD));
					}
					for (Player p : pList) {
						if (FastUtils.randomIntInRange(0, 1) == 0) {
							new PPExplosion(Particle.SPELL_WITCH, p.getLocation()).count(10).delta(0.25).spawnAsBoss();
							p.playSound(p, Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 1, 1);
							p.teleport(mPortalOneLoc);
						} else {
							new PPExplosion(Particle.SPELL_WITCH, p.getLocation()).count(10).delta(0.25).spawnAsBoss();
							p.playSound(p, Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 1, 1);
							p.teleport(mPortalTwoLoc);
						}
					}
					new BukkitRunnable() {
						int mTicks = 20;

						@Override
						public void run() {
							World world = mSirius.mAuroraLocation.getWorld();
							world.playSound(mSirius.mAuroraLocation, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.5f, 1.7f);
							world.playSound(mMidPoint, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.5f, 1.7f);
							if (mTicks >= DURATION) {
								this.cancel();
							}
							mTicks++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
				}
				if (mTicks >= 20) {
					//ring
					if (mTicks <= DURATION - 40) {
						mRadius += 5 * (RADIUS / (DURATION - 40.0));
					}
					new PPCircle(Particle.END_ROD, mMidPoint.clone().add(0, 0.1, 0), mRadius).count(20).ringMode(true).spawnAsBoss();
					//Aurora
					createBeam(mSirius.mAuroraLocation.clone().add(0, 0.5, 0), mMidPoint.clone().add(0, 0.60, 0), mMaxDistance);
				}
				if (mTicks >= DURATION) {
					int passers = 0;
					//cleanse all people in radius
					List<Player> pList = mSirius.getValidDeclarationPlayersInArena();
					for (Player p : PlayerUtils.playersInRange(pList, mMidPoint, RADIUS, true, true)) {
						passers++;
					}
					//make sure everyone is tagged with participation if they tried also
					for (Player p : PlayerUtils.playersInRange(mSirius.getPlayers(), mMidPoint, RADIUS + 3, true, true)) {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, Sirius.PARTICIPATION_TAG, new CustomTimerEffect(DURATION, "Participated").displays(false));
						EffectManager.getInstance().clearEffects(p, PassiveStarBlight.STARBLIGHTAG);
						p.playSound(p, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 0.7f, 1.2f);
						p.playSound(p, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 1f);
						p.playSound(p, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 0.7f, 1.2f);
						p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.HOSTILE, 0.4f, 0.9f);
						new PPExplosion(Particle.CLOUD, p.getLocation()).delta(0.5).count(10).spawnAsBoss();
					}
					if (passers >= pList.size() / 2.0 + 0.5) {
						mSirius.changeHp(true, 1);
						for (Player p : mSirius.getPlayers()) {
							MessagingUtils.sendNPCMessage(p, "Aurora", Component.text("You have bathed in the blood of the Stars and lived. Rejoin the battle!", NamedTextColor.DARK_PURPLE));
						}
					} else {
						mSirius.changeHp(true, -5);
						for (Player p : mSirius.getPlayers()) {
							MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("There is no escape...", NamedTextColor.AQUA));
						}
					}
					//fall through the ground then delete it
					Transformation trans = mTuulenSword.getTransformation();
					mTuulenSword.setInterpolationDelay(-1);
					mTuulenSword.setInterpolationDuration(10);
					mTuulenSword.setTransformation(new Transformation(trans.getTranslation().sub(0, 5, 0), trans.getLeftRotation(), trans.getScale(), trans.getRightRotation()));
					Bukkit.getScheduler().runTaskLater(mPlugin, mTuulenSword::remove, 10);

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

	private void createBeam(Location start, Location end, double maxDistance) {
		Vector vec = LocationUtils.getDirectionTo(end, start);
		Location current = start.clone();
		for (int i = 0; i < maxDistance; i++) {
			new PartialParticle(Particle.REDSTONE, current, 5).delta(0.25).data(calculateColorProgress(i, (int) maxDistance)).spawnAsBoss();
			current.add(vec);
		}
	}


	private Particle.DustOptions calculateColorProgress(int distance, int maxDistance) {
		Particle.DustOptions data;
		int halfDistance = maxDistance / 2;
		if (distance < halfDistance) {
			// Transition from start to mid
			data = new Particle.DustOptions(
				ParticleUtils.getTransition(Color.fromRGB(162, 0, 211), Color.fromRGB(208, 96, 213), Math.min(distance / (double) halfDistance, 1)),
				1f
			);
		} else {
			// Transition from mid to end
			data = new Particle.DustOptions(
				ParticleUtils.getTransition(Color.fromRGB(208, 96, 213), Color.fromRGB(211, 211, 211), Math.min((distance - halfDistance) / (double) halfDistance, 1)),
				1f
			);
		}
		return data;
	}
}
