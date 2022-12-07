package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellDominion extends Spell {

	private int mT = 0;

	Plugin mPlugin;
	LivingEntity mBoss;
	Location mSpawnLoc;
	private List<Player> mWarnedPlayers = new ArrayList<Player>();

	public SpellDominion(Plugin plugin, LivingEntity boss, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		mT--;

		if (mT <= 0) {
			mT = 3;
			List<Player> players = EntityUtils.getNearestPlayers(mSpawnLoc, 100);

			for (Player player : players) {
				if (player.getGameMode() == GameMode.ADVENTURE
					    && (Math.abs(player.getLocation().getY() - mSpawnLoc.getY()) > 4
						        || player.getLocation().distance(mSpawnLoc) > 30)
					    && player.getLocation().add(0, -1, 0).getBlock().getType() != Material.AIR) {
					Location l = player.getEyeLocation();
					new PartialParticle(Particle.SQUID_INK, l, 10, 0.1, 0.1, 0.1, 0.25).spawnAsEntityActive(mBoss);
					BossUtils.bossDamagePercent(mBoss, player, 0.5, "Dominion");
					player.teleport(mSpawnLoc);
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 0));
					player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 6 * 20, 1));
					if (!mWarnedPlayers.contains(player)) {
						mWarnedPlayers.add(player);
						MessagingUtils.sendNPCMessage(player, "Samwell", "&cRunning away? I'm afraid that wouldn't work. This place is my domain!");
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
