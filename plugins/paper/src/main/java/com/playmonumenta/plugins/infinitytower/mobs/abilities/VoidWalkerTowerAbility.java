package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

//Clone ability of Orb of Bones, just with a different name since the original is bad.

public class VoidWalkerTowerAbility extends TowerAbility {
	public VoidWalkerTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);


		Spell spell = new SpellBaseSeekingProjectile(
			plugin,
			boss,
			true,
			160,
			20,
			0.2,
			Math.PI / 45,
			20 * 6,
			2,
			false,
			true,
			10,
			false,
			() -> {
				List<LivingEntity> list = mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs();
				Collections.shuffle(list);
				if (list.size() > 0) {
					return list.subList(0, 1);
				} else {
					return list;
				}
			},
			(World world, Location loc, int ticks) -> {

				if (ticks % 2 == 0) {
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2);
					world.spawnParticle(Particle.FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2);
				}
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 3, 0.5f);
				world.playSound(loc, Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 5, 1.5f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {

				world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 6, 0.5, 0.5, 0.5, 0.1);
				world.spawnParticle(Particle.FLAME, loc, 8, 1, 1, 1, 0.1);
				world.spawnParticle(Particle.CLOUD, loc, 6, 0.5, 0.5, 0.5, 0);
			},
			// Hit Action
			(World world, LivingEntity target, Location loc) -> {
				world.spawnParticle(Particle.FLAME, loc, 80, 2, 2, 2, 0.5);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 80, 2, 2, 2, 0.5);
				world.spawnParticle(Particle.CLOUD, loc, 40, 1, 1, 1, 0.5);
				if (target != null) {
					world.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 1);
					BossUtils.bossDamagePercent(mBoss, target, 0.3);
				}
			}
		);

		SpellManager manager = new SpellManager(List.of(spell));

		super.constructBoss(manager, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 100) + 20);
	}
}
