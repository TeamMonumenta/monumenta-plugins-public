package com.playmonumenta.plugins.specializations;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Random;

import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.World;

public class AssassinSpecialization extends BaseSpecialization {
	private World mWorld;

	public AssassinSpecialization(Plugin plugin, Random random, World world) {
		super(plugin, random);
		mWorld = world;
	}

	public static final String SWIFT_COMBOS_ACTIVE_METAKEY = "SwiftCombosStacks";

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		int perfectKill = ScoreboardUtils.getScoreboardValue(player, "PerfectKill");
		LivingEntity e = (LivingEntity) event.getEntity();

		/*
		 * Perfect Kill: On a critical, instantly kill a non-elite, non-boss mob.
		 * (Cooldown: 24 / 12 seconds)
		 */
		if (perfectKill > 0) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PERFECT_KILL)) {
				if (PlayerUtils.isCritical(player) && !EntityUtils.isBoss(e) && !EntityUtils.isElite(e)) {
					e.setHealth(0);
					e.getWorld().playSound(e.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1, 1.75f);
					mWorld.spawnParticle(Particle.SPELL_WITCH, e.getLocation().add(0, 1.15, 0), 50, 0.3, 0.35, 0.3, 1);
					mWorld.spawnParticle(Particle.SPELL_MOB, e.getLocation().add(0, 1.15, 0), 50, 0.2, 0.35, 0.2, 0);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, e.getLocation().add(0, 1.15, 0), 5, 0.3, 0.35, 0.3, 0);

					int cooldown = perfectKill == 1 ? 24 * 20 : 12 * 20;

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PERFECT_KILL, cooldown);
				}
			}
		}

		int backstab = ScoreboardUtils.getScoreboardValue(player, "Backstab");
		/*
		 * Backstab: When you damage an enemy who is not targeting you,
		 * deal +6 damage and they take Weakness I for 6 seconds. At
		 * level 2, instead deal +12 damage.
		 */
		if (backstab > 0) {
			if (EntityUtils.isHostileMob(e)) {
				Creature c = (Creature) e;
				if (c.getTarget() != null && !c.getTarget().equals(player)) {
					int damage = backstab == 1 ? 6 : 12;

					event.setDamage(event.getDamage() + damage);
					e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 0, true, false));
				}
			}
		}

		int swiftCombos = ScoreboardUtils.getScoreboardValue(player, "SwiftCombos");
		/*
		 * Swift Combos: Whenever you hit an enemy with a sword,
		 * you gain Swift Combos for 3 seconds. Every stack of Swift Combos
		 * you have increases your damage by 1. Hitting an enemy refreshes
		 * your Swift Combo duration and increases its level by 1 (max of 5 levels).
		 * At level 2, increases the max amount of stacks to 8, causes sweep
		 * attacks to also increase levels of Swift Combos per target hit,
		 * and applies the bonus damage to Sweep attack targets.
		 */
		if (swiftCombos > 0) {
			if (!player.hasMetadata(SWIFT_COMBOS_ACTIVE_METAKEY)) {
				player.setMetadata(SWIFT_COMBOS_ACTIVE_METAKEY, new FixedMetadataValue(mPlugin, 1));
				MessagingUtils.sendActionBarMessage(mPlugin, player, "You have begun to stack Swift Combos");
				new BukkitRunnable() {
					int t = 0;
					int stacks = player.getMetadata(SWIFT_COMBOS_ACTIVE_METAKEY).get(0).asInt();
					@Override
					public void run() {
						t++;
						if (stacks != player.getMetadata(SWIFT_COMBOS_ACTIVE_METAKEY).get(0).asInt()) {
							t = 0;
							stacks = player.getMetadata(SWIFT_COMBOS_ACTIVE_METAKEY).get(0).asInt();
						}

						if (t >= 20 * 3) {
							this.cancel();
							player.removeMetadata(SWIFT_COMBOS_ACTIVE_METAKEY, mPlugin);
							MessagingUtils.sendActionBarMessage(mPlugin, player, "Your Swift Combo Stacks have been reset to 0");
						}

					}

				}.runTaskTimer(mPlugin, 0, 1);
			} else {
				int stacks = player.getMetadata(SWIFT_COMBOS_ACTIVE_METAKEY).get(0).asInt();
				event.setDamage(event.getDamage() + stacks);
				player.setMetadata(SWIFT_COMBOS_ACTIVE_METAKEY, new FixedMetadataValue(mPlugin, stacks + 1));
			}
		}
		return true;
	}

}
