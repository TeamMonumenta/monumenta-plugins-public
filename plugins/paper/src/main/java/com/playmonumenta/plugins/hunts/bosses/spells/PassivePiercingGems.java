package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.AbilityCooldownIncrease;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PassivePiercingGems extends Spell {
	private static final int DURATION = 20 * 20;
	private static final double HEALING = -0.5;
	private static final double VULN = 0.2;
	private static final double COOLDOWN_INCREASE = 0.5;
	private static final double WEAKNESS = -0.2;
	private static final String HEALING_SOURCE = "PiercingGemsHealingEffect";
	private static final String VULN_SOURCE = "PiercingGemsVulnEffect";
	private static final String CDR_SOURCE = "PiercingGemsCDREffect";
	private static final String WEAKNESS_SOURCE = "PiercingGemsWeaknessEffect";
	private static final int ATTACK_DAMAGE = 55;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final int mRange;

	private int mTicks = 0;
	private final List<ShulkerBullet> mBullets = new ArrayList<>();

	public PassivePiercingGems(Plugin plugin, LivingEntity boss, Location spawnLoc, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mRange = range;
	}

	@Override
	public void run() {
		if (mTicks >= 12 * 20) {
			mTicks = 0;
			List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, mRange, true);
			if (players.isEmpty()) {
				return;
			}
			// Many will end up in blocks or far away, we just put a higher number than desired
			int count = 2 + (int) Math.pow(players.size(), 0.8);
			for (int i = 0; i < count; i++) {
				spawnBullet(players);
			}

		}
		mTicks += 1;
	}

	private void spawnBullet(List<Player> players) {
		Location loc = mSpawnLoc.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), FastUtils.randomIntInRange(-mRange / 3, mRange / 3), FastUtils.randomDoubleInRange(-mRange, mRange));
		if (loc.getBlock().isSolid()) {
			return;
		}
		World world = loc.getWorld();
		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (loc.getBlock().isSolid()) {
					this.cancel();
					return;
				}

				if (mT >= 20) {
					Player target = EntityUtils.getNearestPlayer(loc, players);
					if (target == null) {
						// Should never happen, players is nonempty
						return;
					}
					world.spawn(loc, ShulkerBullet.class, bullet -> {
						bullet.setTarget(target);
						bullet.setShooter(mBoss);
						EntityUtils.setRemoveEntityOnUnload(bullet);
						mBullets.add(bullet);
					});
					world.playSound(loc, Sound.BLOCK_BEEHIVE_EXIT, 1.0f, 0.6f);
					this.cancel();
					return;
				}

				PPCircle circle = new PPCircle(Particle.PORTAL, loc, 0.2).count(6).delta(0.02).ringMode(true);
				circle.axes(new Vector(1, 0, 0), new Vector(0, 1, 0)).spawnAsBoss();
				circle.axes(new Vector(1, 0, 0), new Vector(0, 0, 1)).spawnAsBoss();
				circle.axes(new Vector(0, 1, 0), new Vector(0, 0, 1)).spawnAsBoss();
				world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.75f, 0.2f + mT * 0.01f);

				mT += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	public void hit(DamageEvent event, Player player) {
		event.setCancelled(true);
		if (event.isBlockedByShield()) {
			return;
		}
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, ATTACK_DAMAGE, null, true, false, "Piercing Gems");
		player.removePotionEffect(PotionEffectType.LEVITATION);
		if (FastUtils.RANDOM.nextBoolean()) {
			EffectManager.getInstance().addEffect(player, HEALING_SOURCE, new PercentHeal(DURATION, HEALING));
		} else {
			EffectManager.getInstance().addEffect(player, VULN_SOURCE, new PercentDamageReceived(DURATION, VULN));
		}
		if (FastUtils.RANDOM.nextBoolean()) {
			EffectManager.getInstance().addEffect(player, CDR_SOURCE, new AbilityCooldownIncrease(DURATION, COOLDOWN_INCREASE));
		} else {
			EffectManager.getInstance().addEffect(player, WEAKNESS_SOURCE, new PercentDamageDealt(DURATION, WEAKNESS));
		}
	}

	public void clearBullets() {
		for (ShulkerBullet bullet : mBullets) {
			if (bullet.isValid()) {
				bullet.remove();
			}
		}
		mBullets.clear();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
