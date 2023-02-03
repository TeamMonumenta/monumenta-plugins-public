package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.SanctifiedArmorCS;
import com.playmonumenta.plugins.effects.SanctifiedArmorHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;


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

	public static final AbilityInfo<SanctifiedArmor> INFO =
		new AbilityInfo<>(SanctifiedArmor.class, "Sanctified Armor", SanctifiedArmor::new)
			.linkedSpell(ClassAbility.SANCTIFIED_ARMOR)
			.scoreboardId("Sanctified")
			.shorthandName("Sa")
			.descriptions(
				"Whenever you are damaged by melee or projectile hits from non-boss undead enemies, the enemy will take 1.5 times the damage you took, as magic damage.",
				"Deal 2.5 times the final damage instead, and the undead enemy is also afflicted with 20% Slowness for 3 seconds.",
				"If the most recently affected mob is killed by any means other than Sanctified Armor or Thorns damage, regain half the health lost from the last damage taken.")
			.displayItem(new ItemStack(Material.IRON_CHESTPLATE, 1))
			.priorityAmount(5000); // after all damage modifiers, but before lifelines, to get the proper final damage

	private final double mPercentDamageReturned;

	private @Nullable UUID mLastAffectedMob = null;
	private double mLastDamage;

	private final SanctifiedArmorCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public SanctifiedArmor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageReturned = isLevelOne() ? PERCENT_DAMAGE_RETURNED_1 : PERCENT_DAMAGE_RETURNED_2;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SanctifiedArmorCS(), SanctifiedArmorCS.SKIN_LIST);

		Bukkit.getScheduler().runTask(plugin, () -> mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null
			    && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE)
			    && Crusade.enemyTriggersAbilities(source, mCrusade)
			    && !EntityUtils.isBoss(source)) {
			Location loc = source.getLocation();
			World world = mPlayer.getWorld();

			MovementUtils.knockAway(mPlayer, source, KNOCKBACK_SPEED, KNOCKBACK_SPEED, true);
			if (isLevelTwo()) {
				EntityUtils.applySlow(mPlugin, CharmManager.getDuration(mPlayer, CHARM_DURATION, SLOWNESS_DURATION), SLOWNESS_AMPLIFIER_2 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW), source);
				mCosmetic.sanctOnTrigger2(world, mPlayer, loc, source);
			} else {
				mCosmetic.sanctOnTrigger1(world, mPlayer, loc, source);
			}

			if (!event.isBlocked()) {
				double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mPercentDamageReturned * event.getFinalDamage(false));
				mLastAffectedMob = null;
				DamageUtils.damage(mPlayer, source, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);

				if (isEnhanced() && source.isValid()) {
					Optional<SanctifiedArmorHeal> existingEffect = mPlugin.mEffectManager.getEffects(source, SanctifiedArmorHeal.class).stream().findFirst();
					if (existingEffect.isPresent()) {
						existingEffect.get().addPlayer(mPlayer);
					} else {
						mPlugin.mEffectManager.addEffect(source, ENHANCEMENT_EFFECT_NAME, new SanctifiedArmorHeal(mPlayer.getUniqueId()).displaysTime(false));
					}
					mLastAffectedMob = source.getUniqueId();
					mLastDamage = event.getFinalDamage(false);
				}
			}
		}
	}

	public void onMobKilled(LivingEntity entity) {
		if (isEnhanced()
			    && mLastAffectedMob != null
			    && mLastAffectedMob.equals(entity.getUniqueId())
			    && entity.getLastDamageCause() != null
			    && entity.getLastDamageCause().getCause() != EntityDamageEvent.DamageCause.THORNS) {
			PlayerUtils.healPlayer(mPlugin, mPlayer, mLastDamage / 2.0);
			mCosmetic.sanctOnHeal(mPlayer, entity);
		}
	}
}
