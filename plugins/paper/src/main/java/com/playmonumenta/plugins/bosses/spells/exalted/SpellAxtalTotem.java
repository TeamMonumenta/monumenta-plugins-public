package com.playmonumenta.plugins.bosses.spells.exalted;

import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellThrowSummon;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellAxtalTotem extends Spell {
	Plugin mPlugin;
	LivingEntity mBoss;
	int mRange;

	public SpellAxtalTotem(Plugin plugin, LivingEntity boss, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.5f);
		new PartialParticle(Particle.PORTAL, mBoss.getLocation().add(0, 1, 0), 75, 0.0, 0.0, 0.0).spawnAsBoss();
		new PartialParticle(Particle.ENCHANTMENT_TABLE, mBoss.getLocation().add(0, 1, 0), 50, 0.0, 0.0, 0.0).spawnAsBoss();
		new BukkitRunnable() {

			@Override public void run() {
				List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);
				EntityTargets target = EntityTargets.GENERIC_ONE_PLAYER_TARGET.setOptional(false).setRange(mRange);
				int count = (int) Math.min(Math.ceil(players.size() / 2.0) + 1, 6);
				String summonPool = "~ExAxtalTotem";
				double distanceScalar = 0.95;

				// all input values not defined above are default values
				Spell spell = new SpellThrowSummon(mPlugin, mBoss, target, count, 0, summonPool, true, 20,
					0, 0.2f, 0, 0, distanceScalar, 10, 15,
					ParticlesList.fromString("[(FIRE,10,0.2,0.2,0.2,0.1)]"), SoundsList.fromString("[(ENTITY_SHULKER_SHOOT,1,1)]"));
				spell.run();
			}
		}.runTaskLater(mPlugin, 30);
	}

	@Override public int cooldownTicks() {
		return 20 * 7;
	}
}
