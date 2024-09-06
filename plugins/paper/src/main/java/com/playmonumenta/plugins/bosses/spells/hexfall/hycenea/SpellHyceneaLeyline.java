package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellHyceneaLeyline extends Spell {

	private static final String ABILITY_NAME = "The Leyline";
	private static final double mLeniency = 0.65;
	private final Location mSpawnLoc;
	private final LivingEntity mBoss;
	private final int mRange;

	public SpellHyceneaLeyline(Location spawnLoc, LivingEntity boss, int range) {
		mSpawnLoc = spawnLoc;
		mRange = range;
		mBoss = boss;
	}

	@Override
	public void run() {
		for (LivingEntity entity : EntityUtils.getNearbyMobs(mSpawnLoc, mRange)) {
			if ((EntityUtils.isHostileMob(entity) || entity instanceof Wolf) && entity.getLocation().getY() <= (mSpawnLoc.getY() - mLeniency) && !entity.getScoreboardTags().contains("Boss")) {
				DamageUtils.damage(null, entity, DamageEvent.DamageType.TRUE, 50000, null, false, false, ABILITY_NAME);

				new PPExplosion(Particle.SPELL_WITCH, entity.getLocation())
					.count(10)
					.delta(1)
					.spawnAsBoss();
			}
		}

		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			if (player.getLocation().getY() <= (mSpawnLoc.getY() - mLeniency)) {

				PlayerUtils.killPlayer(player, mBoss, ABILITY_NAME);

				if (player.getLocation().getY() >= (mSpawnLoc.getY() - 4)) {
					PlayerUtils.playerTeleport(player, player.getLocation().clone().add(0, 8, 0));
					player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 5, 0, false, false, false));
					player.playSound(player, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
					new PartialParticle(Particle.FIREWORKS_SPARK, player.getLocation().clone().add(0, 1, 0), 5, 0.25, 0.45, 0.25, 0.15).spawnAsBoss();
				}

				new PPExplosion(Particle.SPELL_WITCH, player.getLocation())
					.count(10)
					.delta(1)
					.spawnAsBoss();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}


