package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.SanctifiedArmorHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;


public class SanctifiedArmor extends Ability {

	private static final double PERCENT_DAMAGE_RETURNED_1 = 1.5;
	private static final double PERCENT_DAMAGE_RETURNED_2 = 2.5;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.2;
	private static final int SLOWNESS_DURATION = 20 * 3;
	private static final float KNOCKBACK_SPEED = 0.4f;
	private static final String ENHANCEMENT_EFFECT_NAME = "SanctifiedArmorHealEffect";

	public static final String CHARM_DAMAGE = "Sanctified Armor Damage";
	public static final String CHARM_SLOW = "Sanctified Armor Slowness Amplifier";
	public static final String CHARM_DURATION = "Sanctified Armor Slowness Duration";

	private final double mPercentDamageReturned;

	private @Nullable UUID mLastAffectedMob = null;
	private double mLastDamage;

	private @Nullable Crusade mCrusade;

	public SanctifiedArmor(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Sanctified Armor");
		mInfo.mLinkedSpell = ClassAbility.SANCTIFIED_ARMOR;
		mInfo.mScoreboardId = "Sanctified";
		mInfo.mShorthandName = "Sa";
		mInfo.mDescriptions.add("Whenever you are damaged by melee or projectile hits from non-boss undead enemies, the enemy will take 1.5 times the damage you took, as magic damage.");
		mInfo.mDescriptions.add("Deal 2.5 times the final damage instead, and the undead enemy is also afflicted with 20% Slowness for 3 seconds.");
		mInfo.mDescriptions.add("If the most recently affected mob is killed by any means other than Sanctified Armor or Thorns damage, regain half the health lost from the last damage taken.");
		mPercentDamageReturned = isLevelOne() ? PERCENT_DAMAGE_RETURNED_1 : PERCENT_DAMAGE_RETURNED_2;
		mDisplayItem = new ItemStack(Material.IRON_CHESTPLATE, 1);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	@Override
	public double getPriorityAmount() {
		return 5000; // after all damage modifiers, but before lifelines, to get the proper final damage
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mPlayer != null
			    && source != null
			    && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)
			    && Crusade.enemyTriggersAbilities(source, mCrusade)
			    && !EntityUtils.isBoss(source)) {
			Location loc = source.getLocation();
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.FIREWORKS_SPARK, loc.add(0, source.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125).spawnAsPlayerPassive(mPlayer);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f);

			MovementUtils.knockAway(mPlayer, source, KNOCKBACK_SPEED, KNOCKBACK_SPEED, true);
			if (isLevelTwo()) {
				EntityUtils.applySlow(mPlugin, SLOWNESS_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION), SLOWNESS_AMPLIFIER_2 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW), source);
			}

			if (!event.isBlocked()) {
				double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mPercentDamageReturned * event.getFinalDamage(false));
				mLastAffectedMob = null;
				DamageUtils.damage(mPlayer, source, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true);

				if (isEnhanced() && source.isValid()) {
					Optional<SanctifiedArmorHeal> existingEffect = mPlugin.mEffectManager.getEffects(source, SanctifiedArmorHeal.class).stream().findFirst();
					if (existingEffect.isPresent()) {
						existingEffect.get().addPlayer(mPlayer);
					} else {
						mPlugin.mEffectManager.addEffect(source, ENHANCEMENT_EFFECT_NAME, new SanctifiedArmorHeal(mPlayer.getUniqueId()));
					}
					mLastAffectedMob = source.getUniqueId();
					mLastDamage = event.getFinalDamage(false);
				}
			}
		}
	}

	public void onMobKilled(LivingEntity entity) {
		if (isEnhanced()
			    && mPlayer != null
			    && mLastAffectedMob != null
			    && mLastAffectedMob.equals(entity.getUniqueId())
			    && entity.getLastDamageCause() != null
			    && entity.getLastDamageCause().getCause() != EntityDamageEvent.DamageCause.THORNS) {
			PlayerUtils.healPlayer(mPlugin, mPlayer, mLastDamage / 2.0);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.65f, 1.25f);
		}
	}
}
