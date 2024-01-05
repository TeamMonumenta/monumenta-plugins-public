package com.playmonumenta.plugins.bosses.spells.sirius.miniboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.*;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellStarblightCharge extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private PassiveStarBlightConversion mConverter;
	private boolean mOnCooldown;
	private static final int COOLDOWN = 10 * 20;
	private static final int CHARGERANGE = 15;
	private static final int CHARGEUPTIME = 5 * 20;
	private static final int DAMAGE = 40;

	public SpellStarblightCharge(Plugin plugin, LivingEntity boss, PassiveStarBlightConversion converter) {
		mPlugin = plugin;
		mBoss = boss;
		mConverter = converter;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		List<Player> pList = PlayerUtils.playersInRange(mBoss.getLocation(), CHARGERANGE, false, false);
		Location mTargetLoc;
		if (pList.isEmpty()) {
			mTargetLoc = mBoss.getLocation().clone().add(-CHARGERANGE, 0, 0);
		} else {
			mTargetLoc = FastUtils.getRandomElement(pList).getLocation();
		}
		//could maybe check the entire line is air
		EntityUtils.selfRoot(mBoss, CHARGEUPTIME);
		new BukkitRunnable() {
			int mTick = 0;

			@Override
			public void run() {
				if (mTick == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.4f, 1.2f);
				}
				if (mTick <= CHARGEUPTIME) {
					new PPLine(Particle.VILLAGER_ANGRY, mBoss.getLocation().add(0, 1, 0), mTargetLoc).countPerMeter(5).delta(0.75).spawnAsBoss();
				}
				if (CHARGEUPTIME > mTick) {
					World world = mBoss.getWorld();
					Location loc = mBoss.getLocation();
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 1f, 0.4f);
					world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.8f, 0.9f);
					world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 0.4f, 0.8f);
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 0.4f, 0.8f);

					boolean teleported = false;
					List<Player> mPList = PlayerUtils.playersInRange(mBoss.getLocation(), 10, true, true);
					Vector vec = LocationUtils.getVectorTo(mTargetLoc, mBoss.getLocation()).multiply(1 / 7.0f);
					for (int i = 0; i < vec.clone().multiply(7.0f).length(); i++) {
						Location temp = LocationUtils.fallToGround(mBoss.getLocation().add(vec.getX() * i, 0, vec.getZ() * i), 0);
						mConverter.convertColumn(temp.getX(), temp.getZ());
						mConverter.convertColumn(temp.getX() + 1, temp.getZ());
						mConverter.convertColumn(temp.getX() - 1, temp.getZ());
						mConverter.convertColumn(temp.getX(), temp.getZ() + 1);
						mConverter.convertColumn(temp.getX(), temp.getZ() - 1);
						BoundingBox box = BoundingBox.of(temp, 1.5, 3, 1.5);
						for (Player p : mPList) {
							if (box.contains(p.getBoundingBox())) {
								p.playSound(p, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.HOSTILE, 1, 1);
								DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, DAMAGE, null, false, true, "Starblight Charge");
								p.playSound(p, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.HOSTILE, 1f, 0.4f);
								p.playSound(p, Sound.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.HOSTILE, 1f, 0.8f);
							}
						}
						if (temp.clone().add(0, 2, 0).getBlock().isSolid()) {
							teleported = true;
							break;
						}
					}
					if (!teleported) {
						mBoss.teleport(LocationUtils.fallToGround(mTargetLoc.add(0, 5, 0), 0));
					}
					this.cancel();
				}
				mTick++;
			}
		}.runTaskTimer(mPlugin, 0, 5);

	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}
}
