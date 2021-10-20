package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;

public class SpellMiasma extends Spell {

	private int mT = 0;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mDepth;
	private final double mRange;
	private List<Player> mWarnedPlayers = new ArrayList<Player>();


	public SpellMiasma(LivingEntity boss, Location loc, double j, double r) {
		mBoss = boss;
		mCenter = loc;
		mDepth = j;
		mRange = r;
	}

	@Override
	public void run() {
		mT--;
		if (mT <= 0) {
			mT = 2;
			for (Player player : Lich.playersInRange(mCenter, mRange, true)) {
				if (player.getLocation().getY() > mDepth + 10 && player.getGameMode() == GameMode.SURVIVAL) {
					World world = player.getWorld();
					Location l = player.getEyeLocation();
					world.spawnParticle(Particle.SQUID_INK, l, 10, 0.1, 0.1, 0.1, 0.25);

					BossUtils.bossDamage(mBoss, player, 20, (Location)null, "Miasma");
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 0));
					player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 6 * 20, 1));
					if (!mWarnedPlayers.contains(player)) {
						mWarnedPlayers.add(player);
						player.sendMessage(ChatColor.DARK_PURPLE + "BEGONE THEN! FLY AWAY LITTLE BIRD!");
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