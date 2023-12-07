package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellVesperidysTeleport extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;
	private final double mDamageThreshold;

	public final ItemStack mHelmet;
	public final ItemStack mChestplate;
	public final ItemStack mLeggings;
	public final ItemStack mBoots;
	public final ItemStack mMainhand;
	public final ItemStack mOffhand;

	private double mDamage = 0;
	private int mTicks = 0;

	public boolean mTeleporting = false;

	public SpellVesperidysTeleport(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, double damageThreshold) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;
		mDamageThreshold = damageThreshold;

		mHelmet = mBoss.getEquipment().getHelmet();
		mChestplate = mBoss.getEquipment().getChestplate();
		mLeggings = mBoss.getEquipment().getLeggings();
		mBoots = mBoss.getEquipment().getBoots();
		mMainhand = mBoss.getEquipment().getItemInMainHand();
		mOffhand = mBoss.getEquipment().getItemInOffHand();
	}

	@Override
	public void run() {
		if (mVesperidys.mPhase == 0) {
			return;
		}

		mTicks += 5;

		if (((mTicks >= 25 * 20 && !mVesperidys.mAutoAttack.isRunning())
			 || (mVesperidys.mSpawnLoc.getY() - mBoss.getLocation().getY() > 5))
			&& !mTeleporting
			&& !mVesperidys.mInvincible
			&& !mVesperidys.mDefeated) {
			teleportRandom();
		}
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Player) {
			mDamage += event.getDamage();

			if (mDamage > mDamageThreshold && !mVesperidys.mAutoAttack.isRunning()) {
				teleportRandom();
			}
		}
	}

	public void teleportRandom() {
		List<Vesperidys.Platform> platforms = mVesperidys.mPlatformList.getShuffledPlatforms(null);
		Vesperidys.Platform selectedPlatform = null;

		// Prioritizes platforms which doesn't have adds on it.
		for (Vesperidys.Platform platform : platforms) {
			selectedPlatform = platform;
			if (platform.getMobsOnPlatform().size() <= 0 && platform.getPlayersOnPlatform().size() <= 0) {
				break;
			}
		}

		Location newLoc;
		if (selectedPlatform == null) {
			// Failsafe. Should not happen, ever.
			newLoc = mVesperidys.mSpawnLoc;
		} else {
			newLoc = selectedPlatform.getCenter();
		}

			teleport(newLoc, true);
	}

	public void teleportPlatform(int x, int y) {
		Vesperidys.Platform platform = mVesperidys.mPlatformList.getPlatform(x, y);
		Location newLoc = Objects.requireNonNull(platform).getCenter();

		teleport(newLoc, true);
	}

	public void teleport(Location newLoc, boolean gravity) {
		// Shouldn't happen but just in case.
		if (mTeleporting) {
			return;
		}

		mTicks = 0;
		mDamage = 0;

		vanish();
		BukkitRunnable runnableA = new BukkitRunnable() {
			@Override
			public synchronized void cancel() {
				super.cancel();
				reappear(gravity);
			}

			@Override
			public void run() {
				for (int attempts = 0; attempts < 4; attempts++) {
					if (mBoss.getLocation().distance(newLoc) < 1) {
						mBoss.teleport(newLoc.clone().add(0, 1, 0));

						if (gravity) {
							for (int i = -1; i <= 1; i++) {
								for (int j = -1; j <= 1; j++) {
									Location loc = newLoc.clone().add(i, 0, j);
									Block block = loc.getBlock();
									if (!block.isSolid()) {
										block.setType(Material.BLACKSTONE);
										mVesperidys.mAnticheese.mIgnored.add(block);

										Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
											mVesperidys.mAnticheese.mIgnored.remove(block);
										}, 10 * 20);
									}
								}
							}
						}
						this.cancel();
						return;
					}

					Vector dir = LocationUtils.getVectorTo(newLoc, mBoss.getLocation()).normalize().multiply(0.5);
					mBoss.teleport(mBoss.getLocation().add(dir));
					vanishParticles();

					BoundingBox box = BoundingBox.of(mBoss.getLocation(), 0.3, 0.3, 0.3);
					for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true)) {
						if (p.getBoundingBox().overlaps(box)) {
							BossUtils.blockableDamage(mBoss, p, DamageEvent.DamageType.MAGIC, 20);
							p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 2 * 20, 1));
						}
					}
				}
			}
		};
		runnableA.runTaskTimer(mPlugin, 0, 1);

		mActiveRunnables.add(runnableA);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void vanish() {
		mTeleporting = true;

		mBoss.setInvisible(true);
		mBoss.setInvulnerable(true);
		mBoss.setCollidable(false);
		mBoss.setGlowing(false);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.getEquipment().clear();

		mVesperidys.removeAllEyes();

		new PartialParticle(Particle.FLASH, mBoss.getLocation().add(0, 1.5, 0))
			.spawnAsBoss();
		new PartialParticle(Particle.REDSTONE, mBoss.getLocation().add(0, 1.5, 0), 20, 0.25, 1, 0.25)
			.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1f))
			.spawnAsBoss();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
	}

	private void vanishParticles() {
		new PartialParticle(Particle.REDSTONE, mBoss.getLocation().add(0, 1.5, 0))
			.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2f))
			.spawnAsBoss();
		new PartialParticle(Particle.REDSTONE, mBoss.getLocation().add(0, 1.5, 0))
			.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 0.5f))
			.spawnAsBoss();
	}

	public void reappear(boolean gravity) {
		mTeleporting = false;

		mBoss.setInvisible(false);
		mBoss.setInvulnerable(false);
		mBoss.setCollidable(true);
		mBoss.setAI(true);
		mBoss.setGlowing(true);
		mBoss.setGravity(gravity);
		mBoss.getEquipment().setHelmet(mHelmet, true);
		mBoss.getEquipment().setChestplate(mChestplate, true);
		mBoss.getEquipment().setLeggings(mLeggings, true);
		mBoss.getEquipment().setBoots(mBoots, true);
		mBoss.getEquipment().setItemInMainHand(mMainhand, true);
		mBoss.getEquipment().setItemInOffHand(mOffhand, true);

		mVesperidys.resummonAllEyes();

		new PartialParticle(Particle.FLASH, mBoss.getLocation().add(0, 1.5, 0))
			.spawnAsBoss();
		new PPExplosion(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1.5, 0))
			.extra(0.5)
			.count(20)
			.spawnAsBoss();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
	}
}
