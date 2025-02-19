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
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.RestlessSoulsCS;
import com.playmonumenta.plugins.effects.CholericFlamesAntiHeal;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class RestlessSoulsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_restlesssouls";
	public static final int detectionRange = 64;

	private final com.playmonumenta.plugins.Plugin mMonPlugin = com.playmonumenta.plugins.Plugin.getInstance();
	private @Nullable Player mPlayer = null;
	private double mDamage = 0;
	private double mRange = 0;
	private int mSilenceTime = 0;
	private int mDuration = 0;
	private boolean mLevelOne;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;

	private Ability[] mAbilities = {};
	private static final String DOT_EFFECT_NAME = "RestlessSoulsDamageOverTimeEffect";
	private @Nullable RestlessSoulsCS mCosmetic = null;

	public RestlessSoulsBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		boss.setInvulnerable(true);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, double range, int silenceTime, int duration, boolean levelone, ItemStatManager.PlayerItemStats playerItemStats, RestlessSoulsCS cosmetic) {
		mPlayer = player;
		mDamage = damage;
		mRange = range;
		mSilenceTime = silenceTime;
		mDuration = duration;
		mLevelOne = levelone;
		mPlayerItemStats = playerItemStats;
		mCosmetic = cosmetic;

		if (player != null) {
			Bukkit.getScheduler().runTask(mMonPlugin, () -> {
				mAbilities = Stream.of(CholericFlames.class, GraspingClaws.class,
						MelancholicLament.class, HauntingShades.class, WitheringGaze.class)
					             .map(c -> AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, c)).toArray(Ability[]::new);
			});
		}
		cosmetic.createTeam().addEntity(mBoss);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setCancelled(true);
		attack(damagee);
	}

	public void attack(LivingEntity damagee) {
		if (mPlayer == null || mCosmetic == null) {
			return;
		}

		mCosmetic.vexAttack(mPlayer, mBoss, damagee, mRange);

		// tag mob to prevent it from spawning more stuff
		damagee.addScoreboardTag("TeneGhost");

		DamageUtils.damage(mPlayer, damagee, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.RESTLESS_SOULS, mPlayerItemStats), mDamage, true, true, false);

		// remove tag if mob is not dead
		if (!damagee.isDead()) {
			damagee.removeScoreboardTag("TeneGhost");
		}
		// debuff
		for (LivingEntity e : EntityUtils.getNearbyMobs(damagee.getLocation(), mRange)) {
			if (!EntityUtils.isBoss(e)) {
				EntityUtils.applySilence(mMonPlugin, mSilenceTime, e);
			}
			if (!mLevelOne) {
				for (Ability ability : mAbilities) {
					if (ability != null && mMonPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ability.getInfo().getLinkedSpell())) {
						if (ability.getInfo().getLinkedSpell() == ClassAbility.CHOLERIC_FLAMES) {
							EntityUtils.applyFire(mMonPlugin, mDuration, e, mPlayer, mPlayerItemStats);
							if (ability.isLevelTwo()) {
								mMonPlugin.mEffectManager.addEffect(e, CholericFlames.ANTIHEAL_EFFECT, new CholericFlamesAntiHeal(mDuration));
							}
						} else if (ability.getInfo().getLinkedSpell() == ClassAbility.GRASPING_CLAWS) {
							EntityUtils.applySlow(mMonPlugin, mDuration, 0.1, e);
						} else if (ability.getInfo().getLinkedSpell() == ClassAbility.MELANCHOLIC_LAMENT) {
							EntityUtils.applyWeaken(mMonPlugin, mDuration, 0.1, e);
						} else if (ability.getInfo().getLinkedSpell() == ClassAbility.HAUNTING_SHADES) {
							EntityUtils.applyVulnerability(mMonPlugin, mDuration, 0.1, e);
						} else if (ability.getInfo().getLinkedSpell() == ClassAbility.WITHERING_GAZE) {
							mMonPlugin.mEffectManager.addEffect(e, DOT_EFFECT_NAME, new CustomDamageOverTime(mDuration, 1, 40, mPlayer, mPlayerItemStats, ClassAbility.RESTLESS_SOULS, DamageType.AILMENT));
						}
					}
				}
			}
		}

		// kill vex
		mBoss.remove();
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (target != null) {
			Set<String> tags = target.getScoreboardTags();
			if (!EntityUtils.isHostileMob(target) || tags.contains(AbilityUtils.IGNORE_TAG) || (target instanceof LivingEntity le && DamageUtils.isImmuneToDamage(le, DamageType.MAGIC))) {
				event.setCancelled(true);
			}
		}
	}
}
