package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.MelancholicLament;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class RestlessSoulsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_restlesssouls";
	public static final int detectionRange = 64;

	private final com.playmonumenta.plugins.Plugin mMonPlugin = com.playmonumenta.plugins.Plugin.getInstance();
	private @Nullable Player mPlayer;
	private double mDamage = 0;
	private int mSilenceTime = 0;
	private int mDuration = 0;
	private boolean mLevelOne;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;

	private Ability[] mAbilities = {};
	private static final String DOT_EFFECT_NAME = "RestlessSoulsDamageOverTimeEffect";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RestlessSoulsBoss(plugin, boss);
	}

	public RestlessSoulsBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		boss.setInvulnerable(true);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, int silenceTime, int duration, boolean levelone, ItemStatManager.PlayerItemStats playerItemStats) {
		mPlayer = player;
		mDamage = damage;
		mSilenceTime = silenceTime;
		mDuration = duration;
		mLevelOne = levelone;
		mPlayerItemStats = playerItemStats;

		if (player != null) {
			Bukkit.getScheduler().runTask(mMonPlugin, () -> {
				mAbilities = Stream.of(CholericFlames.class, GraspingClaws.class,
						MelancholicLament.class, HauntingShades.class, WitheringGaze.class)
					.map(c -> AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, c)).toArray(Ability[]::new);
			});
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setCancelled(true);
		attack(mMonPlugin, mPlayer, mPlayerItemStats, mBoss, damagee, mLevelOne, mDamage, mSilenceTime, mAbilities, mDuration);
	}

	public static void attack(com.playmonumenta.plugins.Plugin plugin, Player p, ItemStatManager.PlayerItemStats playerItemStats,
							  LivingEntity boss, LivingEntity damagee, boolean levelOne, double damage, int silenceTime,
							  Ability[] abilities, int duration) {
		if (p != null || playerItemStats != null) {
			boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.5f, 1.0f);

			// tag mob to prevent it from spawning more stuff
			damagee.addScoreboardTag("TeneGhost");

			DamageUtils.damage(p, damagee, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.RESTLESS_SOULS, playerItemStats), damage, true, true, false);

			// remove tag if mob is not dead
			if (!damagee.isDead()) {
				damagee.removeScoreboardTag("TeneGhost");
			}
			// debuff
			if (!EntityUtils.isBoss(damagee)) {
				EntityUtils.applySilence(plugin, silenceTime, damagee);
			}
			if (!levelOne) {
				for (Ability ability : abilities) {
					if (ability != null && plugin.mTimers.isAbilityOnCooldown(p.getUniqueId(), ability.getInfo().mLinkedSpell)) {
						if (ability.getInfo().mLinkedSpell == ClassAbility.CHOLERIC_FLAMES) {
							damagee.setFireTicks(duration);
							if (ability.isLevelTwo()) {
								PotionUtils.applyPotion(p, damagee, new PotionEffect(PotionEffectType.HUNGER, duration, 0, false, true));
							}
						} else if (ability.getInfo().mLinkedSpell == ClassAbility.GRASPING_CLAWS) {
							EntityUtils.applySlow(plugin, duration, 0.1, damagee);
						} else if (ability.getInfo().mLinkedSpell == ClassAbility.MELANCHOLIC_LAMENT) {
							EntityUtils.applyWeaken(plugin, duration, 0.1, damagee);
						} else if (ability.getInfo().mLinkedSpell == ClassAbility.HAUNTING_SHADES) {
							EntityUtils.applyVulnerability(plugin, duration, 0.1, damagee);
						} else if (ability.getInfo().mLinkedSpell == ClassAbility.WITHERING_GAZE) {
							plugin.mEffectManager.addEffect(damagee, DOT_EFFECT_NAME, new CustomDamageOverTime(duration, 1, 40, p, null, Particle.SQUID_INK));
						}
					}
				}
			}
			// kill vex
			boss.remove();
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (target != null) {
			Set<String> tags = target.getScoreboardTags();
			if (!EntityUtils.isHostileMob(target) || (tags != null && tags.contains(AbilityUtils.IGNORE_TAG))) {
				event.setCancelled(true);
			}
		}
	}
}
