package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DefensiveLineCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DEFENSIVE_LINE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.CHAIN;
	}

	public void onCast(Plugin plugin, Player player, World world, Location loc, List<Player> affectedPlayers) {
		world.playSound(loc, Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.PLAYERS, 1.2f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.6f, 0.8f);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.2f, 1.4f);
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 2.0f, 1.2f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 35, 0.2, 0, 0.2, 0.25).spawnAsPlayerActive(player);

		for (Player affectedPlayer : affectedPlayers) {
			new PartialParticle(Particle.SPELL_INSTANT, affectedPlayer.getLocation().add(0, 1, 0), 35, 0.4, 0.4, 0.4, 0.25).spawnAsPlayerActive(player);
		}

		new BukkitRunnable() {
			final double mRadius = CharmManager.getRadius(player, DefensiveLine.CHARM_RANGE, 1.25);
			double mY = 0.15;

			@Override
			public void run() {
				mY += 0.2;

				Iterator<Player> iter = affectedPlayers.iterator();
				while (iter.hasNext()) {
					Player player = iter.next();

					if (player.isDead() || !player.isOnline()) {
						iter.remove();
					} else {
						Location playerLoc = player.getLocation().add(0, mY, 0);

						new PPCircle(Particle.CRIT_MAGIC, playerLoc, mRadius).count(60).delta(0.1).extra(0.125).spawnAsPlayerBuff(player);
						new PPCircle(Particle.SPELL_INSTANT, playerLoc, mRadius).count(20).spawnAsPlayerBuff(player);
					}
				}

				if (mY >= 1.8) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}
}
