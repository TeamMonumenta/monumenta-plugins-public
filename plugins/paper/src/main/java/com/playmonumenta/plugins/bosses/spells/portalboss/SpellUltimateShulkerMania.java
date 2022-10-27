package com.playmonumenta.plugins.bosses.spells.portalboss;

import com.playmonumenta.plugins.bosses.bosses.PortalBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellUltimateShulkerMania extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private int mCooldown;

	public static final int ARENA_SIZE = 18;
	public static final int DAMAGE = 40;

	public SpellUltimateShulkerMania(Plugin plugin, LivingEntity boss, Location loc, int cooldown) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mCooldown = cooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 6.0f, 1.0f);

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 50, true);
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), PortalBoss.detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\", \"color\":\"gold\"},{\"text\":\" TARGETS LOCKED. BEGINNING BARRAGE.\",\"color\":\"red\"}]");


		//Summon bullet every 10 ticks
		BukkitRunnable run = new BukkitRunnable() {

			public int mTicks;

			@Override
			public void run() {

				mTicks += 10;
				if (mTicks >= 200) {
					this.cancel();
					return;
				}

				world.playSound(mBoss.getLocation().add(0, 5, 0), Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.HOSTILE, 3.0f, 1.0f);
				Location l = null;
				double checkCircle = -1;
				int attempts = 0;
				while ((l == null || checkCircle <= 0 || l.getWorld().getBlockAt(l).isSolid()) && attempts < 100) {
					double x = FastUtils.randomDoubleInRange(-ARENA_SIZE, ARENA_SIZE);
					double z = FastUtils.randomDoubleInRange(-ARENA_SIZE, ARENA_SIZE);
					l = new Location(mCenter.getWorld(), mCenter.getX() + x, mCenter.getY() - 2, mCenter.getZ() + z);
					checkCircle = Math.pow(ARENA_SIZE, 2) - (Math.pow(x, 2) + Math.pow(z, 2));
					attempts++;
				}
				if (l == null) {
					//Try again next time
					return;
				}
				ShulkerBullet bullet = null;

				for (int i = 0; i < players.size(); i++) {
					try {
						bullet = (ShulkerBullet) EntityUtils.getSummonEntityAt(l, EntityType.SHULKER_BULLET, "{Target:[I;1234,1234,1234,1234],Motion:[0.0,0.5,0.0],TYD:-1d}");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				world.playSound(mBoss.getLocation(), Sound.BLOCK_LAVA_POP, 6.0f, 1.0f);

				//set target of bullet
				Collections.shuffle(players);
				if (bullet != null) {
					bullet.setTarget(players.get(0));
					bullet.setShooter(mBoss);
					bullet.setVelocity(new Vector(0.0, 1.0, 0.0));
				}
			}

		};
		run.runTaskTimer(mPlugin, 0, 10);
		mActiveRunnables.add(run);
	}

	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getEntity() instanceof ShulkerBullet && event.getHitEntity() instanceof Player) {
			event.setCancelled(true);
			event.getEntity().remove();

			Player p = (Player) event.getHitEntity();
			if (p == null) {
				return;
			}
			if (com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.hasEffect(p, "PortalShulkerSlow")) {
				DamageUtils.damage(mBoss, p, DamageType.MAGIC, DAMAGE * 0.5, null, false, true, "Byte Barrage");
			} else {
				DamageUtils.damage(mBoss, p, DamageType.MAGIC, DAMAGE, null, false, true, "Byte Barrage");
			}
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "PortalShulkerSlow", new PercentSpeed(2 * 20, -.40, "PortalShulkerSlow"));
			//Set all nearby mobs to target them
			for (LivingEntity le : EntityUtils.getNearbyMobs(mBoss.getLocation(), 10.0)) {
				if (le instanceof Mob mob) {
					mob.setTarget(p);
				}
			}
			//Let the players know something happened
			p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 0.9f);
			p.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, p.getLocation(), 25, 1.5, 1.5, 1.5);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

}
