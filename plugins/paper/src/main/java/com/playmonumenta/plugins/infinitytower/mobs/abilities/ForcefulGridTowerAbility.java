package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

public class ForcefulGridTowerAbility extends TowerAbility {
	public ForcefulGridTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		EntityTargets targets;

		if (isPlayerMob) {
			targets = new EntityTargets(EntityTargets.TARGETS.MOB, 100, false, new EntityTargets.Limit(1, EntityTargets.Limit.SORTING.FARTHER), List.of(), new EntityTargets.TagsListFiter(new HashSet<>(List.of(TowerConstants.MOB_TAG_FLOOR_TEAM))));
		} else {
			targets = new EntityTargets(EntityTargets.TARGETS.MOB, 100, false, new EntityTargets.Limit(1, EntityTargets.Limit.SORTING.FARTHER), List.of(), new EntityTargets.TagsListFiter(new HashSet<>(List.of(TowerConstants.MOB_TAG_PLAYER_TEAM))));
		}


		Spell spell = new SpellBaseSeekingProjectile(
			plugin,
			boss,
			true,
			160,
			20,
			0.9,
			0,
			45,
			0.7,
			false,
			true,
			0,
			false,
			() -> {
				List<? extends LivingEntity> list = targets.getTargetsList(mBoss);
				list.removeIf(target -> target.getScoreboardTags().contains(TowerConstants.MOB_TAG_UNTARGETABLE));
				return list;
			},
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 2f, 0.5f);
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
				world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 2f, 0.5f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.CRIT, loc, 3, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 4, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
				if (ticks % 40 == 0) {
					world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 2f, 0.2f);
				}
			},
			// Hit Action
			(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
				world.playSound(loc, Sound.ENTITY_ARMOR_STAND_BREAK, 1f, 0.5f);
				new PartialParticle(Particle.CRIT, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
				if (target != null) {
					DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, 15);
					MovementUtils.pullTowards(boss, target, 1);
					EntityUtils.applySilence(Plugin.getInstance(), 20 * 5, target);
					if (mBoss instanceof Mob aiMob && BossManager.getInstance() != null) {
						GenericTowerMob towerMob = BossManager.getInstance().getBoss(mBoss, GenericTowerMob.class);
						if (towerMob != null) {
							//this should always be true.
							towerMob.mLastTarget = target;
							towerMob.mCanChangeTarget = false;
						}
						aiMob.setTarget(mBoss);
					}
				}
			}
		);

		SpellManager activeSpells = new SpellManager(List.of(spell));

		super.constructBoss(activeSpells, Collections.emptyList(), -1, null, 40);

	}
}
