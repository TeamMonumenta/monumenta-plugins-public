package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scoreboard.Team;

public class TemporalInstability extends Spell {
	private static final double RADIUS = 11;
	private static final String INVULNERABLE_TEAM = "TealSpiritInvulnerable";

	private final LivingEntity mBoss;
	private final Location mCenter;

	private final Team mVulnerableTeam;
	private final Team mInvulnerableTeam;

	private final PPCircle mCircle;

	public TemporalInstability(LivingEntity boss, Location center, Team team) {
		mBoss = boss;
		mCenter = center;

		mVulnerableTeam = team;
		mInvulnerableTeam = ScoreboardUtils.getExistingTeamOrCreate(INVULNERABLE_TEAM, NamedTextColor.WHITE);

		mCircle = new PPCircle(Particle.REDSTONE, mCenter, RADIUS).data(new Particle.DustOptions(Color.AQUA, 2)).count(50).ringMode(true);
	}

	@Override
	public void run() {
		Team team = ScoreboardUtils.getEntityTeam(mBoss);
		if (isVulnerable()) {
			mVulnerableTeam.addEntity(mBoss);
		} else if (team != null && team.color().equals(NamedTextColor.AQUA)) {
			mInvulnerableTeam.addEntity(mBoss);
		}
		mCircle.spawnAsBoss();
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!isVulnerable()) {
			// Let /kill commands work
			if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
				return;
			}

			event.setCancelled(true);

			if (event.getSource() instanceof Player player && event.getType() != DamageEvent.DamageType.AILMENT) {
				Location loc = mBoss.getLocation();
				player.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.5f);
				player.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
				new PartialParticle(Particle.SOUL, loc.clone().add(0, 1.5, 0), 5, 0.5, 2, 0.5, 0).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.END_ROD, loc.clone().add(0, 1.5, 0), 5, 0.5, 2, 0.5, 0).spawnAsEntityActive(mBoss);
			}
		}
	}

	private boolean isVulnerable() {
		return LocationUtils.xzDistance(mBoss.getLocation(), mCenter) <= RADIUS;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
