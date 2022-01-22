package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;


public class SanctifiedArmor extends Ability {

	private static final double PERCENT_DAMAGE_RETURNED_1 = 1.5;
	private static final double PERCENT_DAMAGE_RETURNED_2 = 2.5;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.2;
	private static final int SLOWNESS_DURATION = 20 * 3;
	private static final float KNOCKBACK_SPEED = 0.4f;

	private final double mPercentDamageReturned;

	private @Nullable Crusade mCrusade;

	public SanctifiedArmor(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Sanctified Armor");
		mInfo.mLinkedSpell = ClassAbility.SANCTIFIED_ARMOR;
		mInfo.mScoreboardId = "Sanctified";
		mInfo.mShorthandName = "Sa";
		mInfo.mDescriptions.add("Whenever you are damaged by melee or projectile hits from non-boss enemies, the enemy will take 1.5 times the damage you took, as magic damage.");
		mInfo.mDescriptions.add("Deal 2.5 times the final damage instead, and the undead enemy is also afflicted with 20% Slowness for 3 seconds.");
		mPercentDamageReturned = getAbilityScore() == 1 ? PERCENT_DAMAGE_RETURNED_1 : PERCENT_DAMAGE_RETURNED_2;
		mDisplayItem = new ItemStack(Material.IRON_CHESTPLATE, 1);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
			});
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (mPlayer != null && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE) && Crusade.enemyTriggersAbilities(source, mCrusade) && !EntityUtils.isBoss(source) && !event.isCancelled() && !event.isBlocked()) {
			Location loc = source.getLocation();
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.FIREWORKS_SPARK, loc.add(0, source.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f);

			double damage = mPercentDamageReturned * event.getDamage();
			MovementUtils.knockAway(mPlayer, source, KNOCKBACK_SPEED, KNOCKBACK_SPEED, true);
			DamageUtils.damage(mPlayer, source, DamageType.MAGIC, damage, mInfo.mLinkedSpell);

			if (getAbilityScore() > 1) {
				EntityUtils.applySlow(mPlugin, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER_2, source);
			}
		}
	}
}
