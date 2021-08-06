package com.playmonumenta.plugins.depths;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class DepthsDamageRunnable extends BukkitRunnable {

	public static final float PCT_DAMAGE = 0.1f;
	public static final List<Material> BAD_BLOCKS = Arrays.asList(Material.BLACK_STAINED_GLASS, Material.BLACK_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS);

	@Override
	public void run() {
		//Runs twice a second

		//Get players in depths system
		Set<UUID> playersOnShard = DepthsManager.getInstance().mPlayers.keySet();

		if (playersOnShard == null || playersOnShard.size() == 0) {
			return;
		}

		//Iterate through players and damage them if they are not null and standing on glass
		for (UUID id : playersOnShard) {
			Player p = Bukkit.getPlayer(id);
			if (p == null || p.isDead() || !p.getGameMode().equals(GameMode.SURVIVAL)) {
				continue;
			}
			Location loc = p.getLocation();
			World world = p.getWorld();
			if (BAD_BLOCKS.contains(loc.getBlock().getRelative(BlockFace.DOWN).getType())) {
				//Damage player
				Vector vel = p.getVelocity();
				EntityUtils.damageEntity(Plugin.getInstance(), p, 0.1, null, null, true, null, false, false, false, true);
				BossUtils.bossDamagePercent(p, p, PCT_DAMAGE);
				p.setVelocity(vel);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.3f, 1.5f);
			}
		}
	}

}
