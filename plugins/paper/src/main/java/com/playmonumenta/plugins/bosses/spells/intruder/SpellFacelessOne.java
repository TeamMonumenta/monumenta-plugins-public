package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellFacelessOne extends Spell {
	public static final int DURATION = 8 * 20;
	private final Plugin mPlugin;
	private final Mob mBoss;

	public static final String SEEN_PLAYER_TAG = "FacelessOneSeenPlayer";
	public static final int RANGE = 6;

	public SpellFacelessOne(Plugin plugin, Mob boss) {
		mPlugin = plugin;
		mBoss = boss;
		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead() || mBoss.getScoreboardTags().contains(SEEN_PLAYER_TAG)) {
					this.cancel();
					return;
				}

				Location bossLocation = mBoss.getLocation();
				new PPCircle(Particle.DUST_COLOR_TRANSITION, bossLocation, RANGE)
					.data(new Particle.DustTransition(Color.RED, Color.BLACK, 1.0f))
					.count(60)
					.spawnAsBoss();
				List<Player> players = PlayerUtils.playersInRange(bossLocation, RANGE, true, false);
				if (!players.isEmpty()) {
					Player player = players.get(0);

					player.playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, SoundCategory.HOSTILE, 5.0f, 2.0f, 3);
					player.showTitle(Title.title(Component.text(""), Component.text("THEY CAN HEAR YOU BREATHE", TextColor.color(0x6b0000), TextDecoration.BOLD), Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))));

					forceTargetNearbyEntities(player, bossLocation);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 2));
	}

	@Override
	public void run() {

	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		Location loc = mBoss.getLocation();

		if (!mBoss.getScoreboardTags().contains(SEEN_PLAYER_TAG) && loc.distance(source.getLocation()) > RANGE) {
			event.setCancelled(true);
			new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0)).count(15).delta(0)
				.extra(0.3).spawnAsEntityActive(mBoss);
			mBoss.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2.5f, 0.25f);
		}
	}

	private void forceTargetNearbyEntities(Player player, Location bossLocation) {
		forceTargetFaceless(mBoss, player);
		EntityUtils.getNearbyMobs(bossLocation, 100).forEach(entity -> {
			if (entity instanceof WitherSkeleton witherSkeleton && entity.getScoreboardTags().contains("boss_facelessone")) {
				forceTargetFaceless(witherSkeleton, player);
			} else if (entity instanceof Mob mob && mob.hasAI() && !mob.getScoreboardTags().contains("SourcelessGaze")) {
				genericForceTarget(mob, player);
			}
		});
	}

	private void forceTargetFaceless(Mob entity, Player target) {
		entity.getScoreboardTags().add(SEEN_PLAYER_TAG);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> entity.setAI(true), 20);
		genericForceTarget(entity, target);
	}

	private static void genericForceTarget(Mob entity, Player target) {
		entity.setTarget(target);
		double[] yawPitch = VectorUtils.vectorToRotation(target.getLocation().toVector().subtract(entity.getLocation().toVector()));

		entity.setRotation((float) yawPitch[0], (float) yawPitch[1]);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
