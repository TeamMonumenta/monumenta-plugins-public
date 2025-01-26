package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.CustomTimerEffect;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSummonTheStars extends Spell {
	private List<Entity> mActiveMobs;
	private boolean mOnCooldown = false;
	private int mMobLimit;
	private static final int COOLDOWN = 15 * 20;
	private final Sirius mSirius;
	private int mMobsAlive;
	private final Plugin mPlugin;
	private static final Color STARBLIGHT = Color.fromRGB(0, 128, 128);
	private final LoSPool mMobPool;

	public SpellSummonTheStars(Plugin plugin, Sirius sirius) {
		mSirius = sirius;
		mPlugin = plugin;
		mMobPool = LoSPool.fromString("~SiriusMobs");
		mActiveMobs = new ArrayList<>();
	}


	@Override
	public void run() {
		mOnCooldown = true;
		int mPlayerCount = mSirius.getPlayers().size();
		mMobLimit = mPlayerCount * 10 + 10;
		double scaleAmount = 1;
		if (mMobLimit >= 30) {
			mMobLimit = 30;
			scaleAmount = getScale(mPlayerCount);
		}
		int mobCount = 0;
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mSirius.mBoss.getLocation(), 100)) {
			if (mob.getScoreboardTags().contains(Sirius.MOB_TAG)) {
				mobCount++;
			}
		}

		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, cooldownTicks() + 20);
		raiseMobs((float) Math.min(mMobLimit - mobCount, mPlayerCount / 3.0 + 1.5), true, scaleAmount);
	}

	public void raiseMobs(float count, boolean inBlight, double scaleAmount) {
		List<LivingEntity> summoned = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Location loc = findSpawnLocation(0, inBlight);
			if (!mSirius.mBoss.getLocation().equals(loc)) {
				//sink them
				loc.subtract(0, 0.5, 0);
				Mob spawn = (Mob) mMobPool.spawn(loc);
				//Check that it didnt failed to find.
				if (spawn != null) {
					spawn.addScoreboardTag(Sirius.MOB_TAG);
					summoned.add(spawn);
				}
			}
		}
		//scaleMobs(summoned, scaleAmount);
		mobSpawnAnimation(summoned);

	}

	private Location findSpawnLocation(int attempts, boolean inBlight) {
		if (attempts > 20) {

			return mSirius.mBoss.getLocation();
		}
		Location loc;
		if (inBlight) {
			//just in blight.
			loc = mSirius.mBoss.getLocation().add(
				//x
				FastUtils.randomDoubleInRange(0, Math.min(Math.abs(mSirius.mBoss.getLocation().getX() - mSirius.mSpawnCornerOne.getX()), 5)),
				//y
				FastUtils.randomDoubleInRange(0, 3),
				//z
				FastUtils.randomDoubleInRange(-Math.abs(mSirius.mSpawnCornerOne.getZ() - mSirius.mSpawnCornerTwo.getZ()) / 2.0, Math.abs(mSirius.mSpawnCornerOne.getZ() - mSirius.mSpawnCornerTwo.getZ()) / 2.0));
		} else {
			loc = mSirius.mBoss.getLocation().add(
				//x
				FastUtils.randomDoubleInRange(-Math.abs(mSirius.mBoss.getLocation().getX() - mSirius.mSpawnCornerTwo.getX()), 1),
				//y
				FastUtils.randomDoubleInRange(0, 3),
				//z
				FastUtils.randomDoubleInRange(-Math.abs(mSirius.mSpawnCornerOne.getZ() - mSirius.mSpawnCornerTwo.getZ()) / 2.0, Math.abs(mSirius.mSpawnCornerOne.getZ() - mSirius.mSpawnCornerTwo.getZ()) / 2.0));
		}

		loc = LocationUtils.fallToGround(loc, mSirius.mBoss.getLocation().getY() - 10);

		if (loc.getBlock().isSolid() || (loc.getY() == mSirius.mBoss.getLocation().getY() - 10 || loc.clone().subtract(0, 1, 0).getBlock().getType().equals(Material.BARRIER))) {
			return findSpawnLocation(attempts + 1, inBlight);
		} else {
			return loc;
		}
	}

	public void mobSpawnAnimation(List<LivingEntity> spawns) {
		for (LivingEntity spawn : spawns) {
			spawn.setAI(false);
			spawn.setInvulnerable(false);
		}
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= 40) {
					for (LivingEntity spawn : spawns) {
						if (spawn != null) {
							spawn.setAI(true);
							new PPExplosion(Particle.REDSTONE, spawn.getEyeLocation()).count(5).data(new Particle.DustOptions(STARBLIGHT, 0.5f)).spawnAsBoss();
							//Put blighty sound here
						}
					}
					this.cancel();
					return;
				}
				for (LivingEntity spawn : spawns) {
					Location summonLoc = spawn.getLocation().add(0, 0.05, 0);
					spawn.teleport(summonLoc);
					new PPPillar(Particle.REDSTONE, summonLoc, spawn.getHeight()).data(new Particle.DustOptions(STARBLIGHT, 0.75f)).count(10).delta(0.5, 0, 0.5).spawnAsBoss();
					spawn.getWorld().playSound(summonLoc, Sound.BLOCK_CALCITE_BREAK, SoundCategory.HOSTILE, 0.5f, 1.5f);
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	public void mobDeclaration(int duration, double summonsPerPlayer) {
		mActiveMobs = new ArrayList<>();
		List<Player> pList = mSirius.getPlayers();
		mMobsAlive = (int) (pList.size() * summonsPerPlayer + 1.5);
		Collections.shuffle(pList);
		mMobsAlive = Math.min(21, mMobsAlive);
		int spawned = 0;
		List<LivingEntity> summoned = new ArrayList<>();
		for (Player p : pList) {
			for (int i = 0; i < 3; i++) {
				if (spawned >= mMobsAlive) {
					break;
				}
				Location loc = getNearbyLoc(p, 0);
				if (!p.getLocation().equals(loc)) {
					//sink them
					loc.subtract(0, 0.5, 0);
					LivingEntity spawn;
					if (spawned == 0 || spawned % 7 != 0) {
						spawn = (LivingEntity) mMobPool.spawn(loc);
					} else {
						spawn = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, FastUtils.getRandomElement(SpellBlightedPods.MINIBOSSES));
						if (spawn != null) {
							int maxHealth = (int) (SpellBlightedPods.BASEHEALTH * BossUtils.healthScalingCoef(+mSirius.getPlayers().size(), SpellBlightedPods.SCALING_X, SpellBlightedPods.SCALING_Y));
							Objects.requireNonNull(spawn.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
							spawn.setHealth(maxHealth); //half of the hp is effected by damage done to the pod
						}
					}
					//Check that it didnt failed to find.
					if (spawn != null) {
						spawn.addScoreboardTag(Sirius.MOB_TAG);
						mActiveMobs.add(spawn);
						summoned.add(spawn);
						// make sure player abilities don't override the mobs' glow colour
						GlowingManager.startGlowing(spawn, NamedTextColor.AQUA, duration, GlowingManager.BOSS_SPELL_PRIORITY - 1);
						spawned++;
					}
				}
			}
			if (spawned >= mMobsAlive) {
				break;
			}
		}
		mobSpawnAnimation(summoned);
		for (Entity mob : mActiveMobs) {
			mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_WARDEN_DIG, SoundCategory.HOSTILE, 1, 1);
		}
		new BukkitRunnable() {
			int mTicks = 0;
			final ChargeUpManager mManager = new ChargeUpManager(mSirius.mBoss, duration, Component.text(mMobsAlive + " Remaining Crowned Blight!", NamedTextColor.AQUA), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, 75);

			@Override
			public void run() {
				mManager.nextTick(5);
				List<Entity> mDead = new ArrayList<>();
				for (Entity entity : mActiveMobs) {
					if (entity.isDead()) {
						LivingEntity livingEntity = ((LivingEntity) entity).getKiller();
						if (livingEntity != null) {
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(livingEntity, Sirius.PARTICIPATION_TAG, new CustomTimerEffect(duration, "Participated").displays(false));
						}
						mDead.add(entity);
					}
					if (entity.getLocation().getY() < mSirius.mBoss.getLocation().getY() - 10) {
						mDead.add(entity);
						entity.remove();
					}
				}
				mActiveMobs.removeAll(mDead);
				mManager.setTitle(Component.text(mActiveMobs.size() + " Remaining Crowned Blight!", NamedTextColor.AQUA));
				for (Entity e : mActiveMobs) {
					new PPCircle(Particle.NAUTILUS, e.getLocation().clone().add(0, e.getHeight() + 0.5, 0), 0.5).ringMode(true).count(5).spawnAsBoss();
				}
				if (mMobsAlive == 0 || mActiveMobs.isEmpty()) {
					mSirius.changeHp(true, 1);
					for (Player p : mSirius.getPlayers()) {
						MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("Their bearers, shattered in nothingness... They will not be pleased.", NamedTextColor.AQUA));
					}
					mManager.remove();
					this.cancel();
					return;
				}
				if (mTicks >= duration) {
					mSirius.changeHp(true, -5);
					for (Player p : mSirius.getPlayers()) {
						MessagingUtils.sendNPCMessage(p, "Sirius", Component.text("The starlight will shine brighter while they live. You have failed, conquerers.", NamedTextColor.AQUA));
					}
					mManager.remove();
					this.cancel();
				}
				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}


	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}


	@Override
	public int cooldownTicks() {
		if (mSirius.mBlocks <= 10) {
			return COOLDOWN - 5 * 20;
		} else {
			return COOLDOWN;
		}
	}

	public void wipeMobs() {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mSirius.mBoss.getLocation(), 100)) {
			if (mob.getScoreboardTags().contains(Sirius.MOB_TAG)) {
				mob.remove();
			}
		}
	}

	public double getScale(int pCount) {
		return Math.max(1.1 * (pCount - 5), 1);
	}

	//being left incase its needed in the future

	public void scaleMobs(List<Mob> mMobs, double scaleAmount) {
		for (LivingEntity e : mMobs) {
			scaleMob(e, scaleAmount);
		}
	}

	public void scaleMob(LivingEntity e, double scaleAmount) {
		/*AttributeInstance maxHp = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		AttributeInstance damage = e.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
		if (maxHp != null) {
			Objects.requireNonNull(e.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHp.getBaseValue() * scaleAmount);
			e.setHealth(maxHp.getValue());
		}
		if (damage != null) {
			damage.setBaseValue(damage.getValue() * scaleAmount);
		}*/
	}

	private Location getNearbyLoc(Player p, int attempt) {
		if (attempt > 20) {
			return p.getLocation();
		}
		Location loc = p.getLocation();
		loc = LocationUtils.fallToGround(loc, mSirius.mBoss.getLocation().getY() - 10);
		loc.add(
			FastUtils.randomDoubleInRange(-10, 10),
			FastUtils.randomDoubleInRange(0, 3),
			FastUtils.randomDoubleInRange(-10, 10)
		);
		loc = LocationUtils.fallToGround(loc, mSirius.mBoss.getLocation().getY() - 10);
		if (loc.getX() < mSirius.mSpawnCornerTwo.getX() || loc.getX() > mSirius.mSpawnCornerOne.getX()) {
			return getNearbyLoc(p, attempt + 1);
		}
		if (loc.getZ() < mSirius.mSpawnCornerTwo.getZ() || loc.getZ() > mSirius.mSpawnCornerOne.getZ()) {
			return getNearbyLoc(p, attempt + 1);
		}
		if (loc.getBlock().isSolid() || loc.getY() == mSirius.mBoss.getLocation().getY() - 10 || loc.clone().subtract(0, 1, 0).getBlock().getType().equals(Material.BARRIER)) {
			return getNearbyLoc(p, attempt + 1);
		} else {
			return loc;
		}
	}
}
