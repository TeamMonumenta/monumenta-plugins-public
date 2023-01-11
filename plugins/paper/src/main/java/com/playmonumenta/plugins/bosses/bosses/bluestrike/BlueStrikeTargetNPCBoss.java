package com.playmonumenta.plugins.bosses.bosses.bluestrike;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

// Mobs with this tag move towards closest DaggerCraftingBoss, and disrupts crafting if not killed.
public class BlueStrikeTargetNPCBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_bluestriketargetnpc";
	public @Nullable LivingEntity mTarget;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlueStrikeTargetNPCBoss(plugin, boss);
	}

	public BlueStrikeTargetNPCBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Team redTeam = ScoreboardUtils.getExistingTeamOrCreate("Red", NamedTextColor.RED);
		redTeam.addEntry(boss.getUniqueId().toString());
		mBoss.setGlowing(true);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100, EnumSet.of(EntityType.VILLAGER));
		Collections.shuffle(mobs);

		// Search for npc target that is the closest.
		double minDistance = 9999;
		for (LivingEntity mob : mobs) {
			if (mob.getScoreboardTags().contains(BlueStrikeDaggerCraftingBoss.identityTag)) {
				double distance = mBoss.getLocation().distance(mob.getLocation());
				if (distance < minDistance) {
					mTarget = mob;
					minDistance = distance;
				}
			}
		}

		if (mTarget == null) {
			MMLog.warning("TargetNPCBoss: Didn't find NPC! (Likely bug)");
		} else {
			Mob bossMob = (Mob) mBoss;
			bossMob.setTarget(mTarget);
		}

		List<Spell> passiveList = List.of(
			new SpellRunAction(() -> {
				if (mTarget != null && mBoss.getLocation().distance(mTarget.getLocation()) < 2) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.8f);
					PartialParticle particles = new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 10, 1, 1, 1);
					particles.spawnAsEnemy();
					BlueStrikeDaggerCraftingBoss targetAbility = BossUtils.getBossOfClass(mTarget, BlueStrikeDaggerCraftingBoss.class);

					if (targetAbility != null) {
						targetAbility.takeDamage();
					}
					mBoss.remove();
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveList, 100, null);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (event.getTarget() != mTarget) {
			// Ignore everything other than target
			event.setTarget(mTarget);
		}
	}
}
