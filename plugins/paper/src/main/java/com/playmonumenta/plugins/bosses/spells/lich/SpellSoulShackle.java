package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSoulShackle extends Spell {
	private static final String SPELL_NAME = "Soul Shackle";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mRange;
	private final int mCeiling;
	private final ChargeUpManager mChargeUp;
	private final List<Player> mGotHit = new ArrayList<>();
	private final PartialParticle mPortal;
	private final PartialParticle mRod;
	private final PartialParticle mSpark;

	public SpellSoulShackle(Plugin plugin, LivingEntity boss, Location loc, double r, int ceil) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = r;
		mCeiling = ceil;
		mChargeUp = Lich.defaultChargeUp(mBoss, 20, "Charging " + SPELL_NAME + "...");
		mPortal = new PartialParticle(Particle.PORTAL, mBoss.getLocation(), 100, 0.1, 0.1, 0.1, 1.5);
		mRod = new PartialParticle(Particle.END_ROD, mBoss.getLocation(), 40, 1, 1, 1, 0);
		mSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 1, 0, 0, 0, 0);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 6.0f, 1.0f);
		mPortal.location(mBoss.getLocation().add(0, 5, 0)).spawnAsBoss();

		List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p) || p.getLocation().getY() >= mCenter.getY() + mCeiling);

		//summon all bullets after 0.5s
		BukkitRunnable run = new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
					world.playSound(mBoss.getLocation().add(0, 5, 0), Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.HOSTILE, 3.0f, 1.0f);
					Location cornerloc = mBoss.getLocation().add(-1, 5, -1);
					for (Player player : players) {
						Location loc = cornerloc.clone().add(FastUtils.RANDOM.nextInt(2), 0, FastUtils.RANDOM.nextInt(2));

						loc.getWorld().spawn(loc, ShulkerBullet.class, shulkerBullet -> {
							shulkerBullet.setTarget(player);
							shulkerBullet.setVelocity(new Vector(0, 0.5, 0));
							shulkerBullet.setShooter(mBoss);
							shulkerBullet.setTargetDelta(new Vector(0, -1, 0));
						});
					}

					mChargeUp.reset();
				}
			}

		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
	}

	// soul shackle player lock
	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof ShulkerBullet)) {
			return;
		}
		event.setCancelled(true);
		event.getEntity().remove();

		if (!(event.getHitEntity() instanceof Player p)) {
			return;
		}
		if (mGotHit.contains(p)) {
			return;
		}
		mGotHit.add(p);

		World world = mBoss.getWorld();
		Location pLoc = p.getLocation().add(0, 1.5, 0);
		p.sendMessage(Component.text("You got chained by Hekawt! Don't move outside of the ring!", NamedTextColor.DARK_AQUA));

		DamageUtils.damage(mBoss, p, DamageType.MAGIC, 27, null, false, true, SPELL_NAME);
		AbilityUtils.silencePlayer(p, 5 * 20);
		mRod.location(pLoc).spawnAsBoss();
		world.playSound(pLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.7f, 0.5f);
		BossBar bar = BossBar.bossBar(Component.text(SPELL_NAME + " Duration", NamedTextColor.RED), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
		p.showBossBar(bar);

		PPCircle indicator = new PPCircle(Particle.END_ROD, pLoc, 3).count(36);

		BukkitRunnable run = new BukkitRunnable() {

			int mINC = 0;

			@Override
			public void run() {
				//chain function
				mINC++;
				if (SpellDimensionDoor.getShadowed().contains(p)) {
					this.cancel();
					return;
				}

				if (mINC % 10 == 0) {
					p.removePotionEffect(PotionEffectType.LEVITATION);
					for (double n = -1; n < 2; n += 1) {
						Location mColumn = pLoc.clone().add(0, n, 0);
						mSpark.location(mColumn).spawnAsBoss();
					}

					// check HORIZONTAL distance to allow jump boost effects
					Location pGroundLoc = p.getLocation();
					Location pCheckLoc = pLoc.clone();
					pGroundLoc.setY(mCenter.getY());
					pCheckLoc.setY(mCenter.getY());
					if (pGroundLoc.distance(pCheckLoc) > 3) {
						p.sendMessage(Component.text("I shouldn't leave this ring.", NamedTextColor.DARK_AQUA));
						world.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2.0f, 1.0f);
						BossUtils.bossDamagePercent(mBoss, p, 0.15, SPELL_NAME);
						MovementUtils.knockAway(pCheckLoc, p, -0.75f, false);
					}

					Location endLoc = pLoc.clone();
					Vector baseVect = LocationUtils.getDirectionTo(p.getLocation(), pLoc);

					for (int inc = 0; inc < 100; inc++) {
						new PartialParticle(Particle.END_ROD, endLoc, 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
						endLoc.add(baseVect.clone().multiply(0.5));
						if (endLoc.distance(pLoc) > p.getLocation().distance(pLoc)) {
							break;
						}
					}

					indicator.location(pLoc).spawnAsBoss();
				}

				//boss bar
				float progress = 1.0f - mINC / (20.0f * 5.0f);
				if (progress > 0) {
					bar.progress(progress);
				}

				// cancel
				if (mINC >= 20 * 5 || p.isDead() || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
					p.hideBossBar(bar);
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				p.hideBossBar(bar);
			}
		};
		run.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

}
