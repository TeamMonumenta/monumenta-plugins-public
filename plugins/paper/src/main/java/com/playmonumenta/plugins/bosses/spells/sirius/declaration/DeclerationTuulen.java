package com.playmonumenta.plugins.bosses.spells.sirius.declaration;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.SpellSummonTheStars;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.SiriusSetTargetEffect;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.*;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class DeclerationTuulen extends Spell {
	private final SpellSummonTheStars mSpawner;
	private final Sirius mSirius;
	private final Plugin mPlugin;
	private List<Mob> mSpawnedMobs;
	private LivingEntity mTarget;
	private static final float MOBSPERPLAYERPERTICK = 0.035f;
	private static final int RADIUS = 7;
	private static final int DURATION = 12 * 20;
	private static final int SWINGRADIUS = 4;
	//spawned mobs for this
	private static final List<String> MOBNAMES = List.of(
		"StarblightSkulker",
		"BlightpodShambler",
		"StarhungrySlasher"
	);

	private static final List<ItemStack> SILVERKNIGHTWEAPONS = List.of(
		DisplayEntityUtils.generateRPItem(Material.STONE_SWORD, "Blade de Vie"),
		DisplayEntityUtils.generateRPItem(Material.IRON_SWORD, "Silver Knight's Failure"),
		DisplayEntityUtils.generateRPItem(Material.IRON_SWORD, "Celsian Sarissa"),
		DisplayEntityUtils.generateRPItem(Material.TRIDENT, "Twisted Pike")
	);

	private final List<Location> SILVERKNIGHTLOCATIONS;

	public DeclerationTuulen(SpellSummonTheStars spawner, Sirius sirius, Plugin plugin) {
		mSpawner = spawner;
		mSirius = sirius;
		mPlugin = plugin;
		mTarget = mSirius.mTuulen;
		mSpawnedMobs = new ArrayList<>();
		SILVERKNIGHTLOCATIONS = new ArrayList<>();
		SILVERKNIGHTLOCATIONS.addAll(List.of(mSirius.mBoss.getLocation().add(-19, 4, -6),
			mSirius.mBoss.getLocation().add(-19, 7, -6),
			mSirius.mBoss.getLocation().add(-21, 6, -3),
			mSirius.mBoss.getLocation().add(-23, 4, 0),
			mSirius.mBoss.getLocation().add(-23, 7, 0),
			mSirius.mBoss.getLocation().add(-19, 4, 6),
			mSirius.mBoss.getLocation().add(-19, 7, 6),
			mSirius.mBoss.getLocation().add(-21, 6, 3)));
	}

	@Override
	public void run() {
		mSpawnedMobs = new ArrayList<>();
		List<DisplayEntityUtils.DisplayAnimation> mDisplays = new ArrayList<>();
		Team mRed = ScoreboardUtils.getExistingTeamOrCreate("Red", NamedTextColor.RED);
		mTarget.setGlowing(true);
		for (Player p : mSirius.getPlayersInArena(false)) {
			MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text("Sirius has turned his eye to me. Protect my position as I gather my strength!", NamedTextColor.GRAY, TextDecoration.BOLD));
		}
		float mThrowRate = (DURATION - 20) / (SILVERKNIGHTLOCATIONS.size() - 1f);
		Collections.shuffle(SILVERKNIGHTLOCATIONS);
		mSirius.mAnimationLock = true;

		new BukkitRunnable() {
			int mTicks = 0;
			float mMobCount = 0;
			int mThrown = 0;
			int mHitsRemaining = 2;
			ChargeUpManager mBar = new ChargeUpManager(mSirius.mBoss, DURATION, Component.text("Defend Tuulen!", NamedTextColor.GRAY), BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, 75);

			@Override
			public void run() {
				mBar.nextTick();
				List<Mob> toRemove = new ArrayList<>();
				mMobCount += Math.min(mSirius.getPlayersInArena(false).size() * MOBSPERPLAYERPERTICK, MOBSPERPLAYERPERTICK * 5);
				List<Mob> mobs = new ArrayList<>();
				while (mMobCount >= 1) {
					mMobCount--;
					Location loc = findSpawnLocation(0);
					if (!loc.equals(mSirius.mTuulenLocation)) {
						String name = FastUtils.getRandomElement(MOBNAMES);
						loc = LocationUtils.fallToGround(loc, mSirius.mTuulenLocation.getY());
						Mob spawn = (Mob) LibraryOfSoulsIntegration.summon(loc.subtract(0, 1, 0), name);
						if (spawn != null) {
							spawn.getWorld().playSound(loc, Sound.ENTITY_WARDEN_DIG, SoundCategory.HOSTILE, 1, 1);
							spawn.addScoreboardTag(Sirius.MOB_TAG);
							mRed.addEntity(spawn);
							//Scale the entities for people helping fight them
							mSpawner.scaleMob(spawn, Math.max(PlayerUtils.playersInRange(mSirius.mTuulenLocation, 20, true, true).size(), 1));
							mobs.add(spawn);
							mSpawnedMobs.add(spawn);
							spawn.setGlowing(true);
							spawn.setTarget(mTarget);
							EffectManager.getInstance().addEffect(spawn, mSirius.getIdentityTag(), new SiriusSetTargetEffect(DURATION - mTicks + 5, mSirius, mTarget));
						}
					}
				}

				for (Mob mob : mSpawnedMobs) {
					if (LocationUtils.getVectorTo(mTarget.getLocation(), mob.getLocation()).length() <= 1.5) {
						mob.remove();
						toRemove.add(mob);
						mHitsRemaining--;
						List<LivingEntity> entities = EntityUtils.getNearbyMobs(mSirius.mTuulenLocation, 5);
						mSirius.mBoss.getWorld().playSound(mSirius.mTuulenLocation, Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1, 1);
						for (LivingEntity entity : entities) {
							MovementUtils.knockAway(mTarget, entity, 0.5f, 0.5f, false);
						}
						for (Player p : mSirius.getPlayersInArena(false)) {
							MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text("My concentration wanes. Stop them!", NamedTextColor.GRAY));
						}
						if (mHitsRemaining <= 0) {
							swing();
							for (Player p : mSirius.getPlayersInArena(false)) {
								MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text(" I hath lost the magic. Protect me better, woolbearer", NamedTextColor.GRAY));
							}
							mBar.remove();
							Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
								for (DisplayEntityUtils.DisplayAnimation display : mDisplays) {
									display.cancel();
								}
							}, 30);
							mSirius.changeHp(true, -1);
							this.cancel();
							return;
						}
					}
				}
				mSpawner.mobSpawnAnimation(mobs);

				if (mSirius.mBoss.isDead()) {
					this.cancel();
					mSpawnedMobs.removeAll(toRemove);
					return;
				}
				if (mTicks == DURATION) {
					swing();
					for (Player p : mSirius.getPlayersInArena(false)) {
						MessagingUtils.sendNPCMessage(p, "Tuulen", Component.text("Unleash thy fury on the Hearld!", NamedTextColor.GRAY, TextDecoration.BOLD));
					}
					for (Location loc : SILVERKNIGHTLOCATIONS) {
						swordflysound(loc);
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						//Delay so swords impact properly
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mSirius.startDamagePhase(
							"Tuulen",
							Component.text("Thy rage is true! Sirius has fallen back again!", NamedTextColor.GRAY),
							"Tuulen",
							Component.text("Your attacks must be true and they were not.", NamedTextColor.GRAY)
						), 10);
						World world = mSirius.mBoss.getWorld();
						Location loc = mSirius.mBoss.getLocation();
						world.playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 1f, 1.2f);
						world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 0.8f, 0.8f);
						world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.HOSTILE, 0.4f, 0.4f);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.2f, 1.4f);
						mSirius.changeHp(true, 2);
					}, 40);
					mBar.remove();
					this.cancel();
				}
				mSpawnedMobs.removeAll(toRemove);
				if (mTicks >= mThrowRate * mThrown) {
					swordflysound(mSirius.mTuulenLocation);
					ItemDisplay display = mSirius.mBoss.getWorld().spawn(mSirius.mTuulenLocation.clone().add(0.5, 1, 0), ItemDisplay.class);
					display.setItemStack(FastUtils.getRandomElement(SILVERKNIGHTWEAPONS));
					display.setDisplayWidth(2);
					display.setDisplayHeight(2);
					display.setGlowing(true);
					display.addScoreboardTag("SiriusDisplay");
					DisplayEntityUtils.DisplayAnimation animation = new DisplayEntityUtils.DisplayAnimation(display);
					Vector vec = LocationUtils.getVectorTo(SILVERKNIGHTLOCATIONS.get(mThrown), mSirius.mTuulenLocation);
					Vector nVec = vec.clone().normalize();
					mTarget.setRotation((float) FastMath.atan2(nVec.getZ(), nVec.getX()), (float) FastMath.asin(nVec.getY()));
					display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) -FastMath.atan2(nVec.getZ(), nVec.getY()), 0, 1, 0), new Vector3f(2f), new AxisAngle4f((float) FastMath.asin(nVec.getY() - FastMath.PI / 4.0f), 0, 0, 1)));
					animation = animation.addKeyframe(new Transformation(new Vector3f((float) vec.getX(), (float) vec.getY(), (float) vec.getZ()), new AxisAngle4f((float) -FastMath.atan2(nVec.getZ(), nVec.getY()), 0, 1, 0), new Vector3f(2f), new AxisAngle4f((float) FastMath.asin(nVec.getY()), 0, 0, 1)), 20);
					Vector vec2 = LocationUtils.getVectorTo(mSirius.mBoss.getLocation(), SILVERKNIGHTLOCATIONS.get(mThrown));
					vec2.normalize();
					Vector vecToBoss = LocationUtils.getVectorTo(mSirius.mBoss.getLocation(), SILVERKNIGHTLOCATIONS.get(mThrown));
					Location fallVec = LocationUtils.fallToGround(SILVERKNIGHTLOCATIONS.get(mThrown), 0).subtract(SILVERKNIGHTLOCATIONS.get(mThrown));
					animation = animation.addKeyframe(new Transformation(new Vector3f(), new AxisAngle4f((float) -FastMath.atan2(vec2.getZ(), vec2.getX()), 0, 1, 0), new Vector3f(2f), new AxisAngle4f((float) (FastMath.asin(vec2.getY()) - FastMath.PI / 4.0f), 0, 0, 1)), 10);
					animation = animation.addDelay(DURATION - mTicks);
					animation.addKeyframe(new Transformation(new Vector3f((float) vecToBoss.getX(), (float) vecToBoss.getY(), (float) vecToBoss.getZ()), new AxisAngle4f(), new Vector3f(2f), new AxisAngle4f()), 5);
					animation = animation.removeDisplaysAfterwards();
					animation.addCancelFrame(new Transformation(new Vector3f((float) fallVec.x(), (float) fallVec.y(), (float) fallVec.z()), new AxisAngle4f((float) (-Math.PI / 2 - Math.PI / 4.0f), 0, 0, 1), new Vector3f(2f), new AxisAngle4f()), 10);
					animation.addCancelDelay(1 * 20);
					//Delayed to allow for the size and rotation to be constant
					DisplayEntityUtils.DisplayAnimation finalAnimation = animation;
					Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), finalAnimation::play, 1);
					mDisplays.add(finalAnimation);
					mThrown++;
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				//Delayed so the swords impact
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> mSirius.mAnimationLock = false, 55);
				endBehavior(mSpawnedMobs);
				for (Mob mob : mSpawnedMobs) {
					mRed.removeEntity(mob);
				}
				mTarget.setGlowing(false);
				mActiveRunnables.remove(this);

			}
		}.runTaskTimer(mPlugin, 0, 1);
		mTarget.setGlowing(false);
	}

	private void swordflysound(Location loc) {
		World world = mSirius.mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 1f, 0.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 0.4f, 0.8f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.HOSTILE, 0.6f, 0.4f);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public Location findSpawnLocation(int attempt) {
		if (attempt >= 20) {
			return mSirius.mTuulenLocation;
		}
		//Generate x between -radius and radius then random between negative or positive of the square root.
		Double x = FastUtils.randomDoubleInRange(-RADIUS, RADIUS);
		Double radiusRemaining = RADIUS * RADIUS - (x * x);
		Double z = Math.sqrt(radiusRemaining);
		if (FastUtils.randomIntInRange(0, 1) == 0) {
			z *= -1;
		}
		Location potential = mSirius.mTuulenLocation.clone().add(x, 0, z);
		if (LocationUtils.getVectorTo(mSirius.mTuulenLocation, potential).length() >= RADIUS) {
			return potential.add(0, 5, 0);
		} else {
			attempt++;
			return findSpawnLocation(attempt);
		}
	}

	private void endBehavior(List<Mob> mobs) {
		for (Mob mob : mobs) {
			mob.setGlowing(false);
		}
	}

	private void swing() {
		ItemDisplay mDisplay = mSirius.mTuulenLocation.getWorld().spawn(mSirius.mTuulenLocation, ItemDisplay.class);
		mDisplay.setItemStack(DisplayEntityUtils.generateRPItem(Material.IRON_SWORD, "Silver Knight's Failure"));
		mDisplay.setTransformation(new Transformation(new Vector3f(0, 0.25f, 1.5f), new AxisAngle4f((float) (-FastMath.PI / 4.0), 0, 1, 0), new Vector3f(SWINGRADIUS), new AxisAngle4f((float) (FastMath.PI / 2.0), 1, 0, 0)));
		mDisplay.setInterpolationDuration(2);
		new BukkitRunnable() {
			int mTicks = 0;
			float mAngle = 0;
			final float mIncriment = (float) ((2 * FastMath.PI) / 10f);

			@Override
			public void run() {
				mSirius.mTuulenLocation.getWorld().playSound(mSirius.mTuulenLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1, 1);
				mDisplay.setInterpolationDelay(-1);
				mDisplay.setTransformation(new Transformation(
					new Vector3f((float) (-1.5 * FastMath.sin(mAngle)), 0.25f, (float) (1.5 * FastMath.cos(mAngle))),
					new AxisAngle4f((float) (-FastMath.PI / 4.0 - mAngle), 0, 1, 0), //spin
					new Vector3f(SWINGRADIUS),
					new AxisAngle4f((float) (FastMath.PI / 2.0), 1, 0, 0))); //flat
				mDisplay.setInterpolationDelay(-1);
				if (mTarget != null) {
					mTarget.setRotation((float) FastMath.atan2((1.5 * FastMath.cos(mAngle)), (-1.5 * FastMath.sin(mAngle))), 0);
				}
				if (mTicks > 11) {
					//Kill things in slash
					for (LivingEntity e : EntityUtils.getNearbyMobs(mSirius.mTuulenLocation, SWINGRADIUS)) {
						if (EntityUtils.isHostileMob(e)) {
							e.damage(10000);
						}
					}
					mDisplay.remove();
					this.cancel();
				}
				mAngle += mIncriment;
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

}
