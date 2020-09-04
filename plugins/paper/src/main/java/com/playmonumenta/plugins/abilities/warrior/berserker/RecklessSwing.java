package com.playmonumenta.plugins.abilities.warrior.berserker;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class RecklessSwing extends Ability {

	private static final double DAMAGE_PERCENT_PER_4_HEALTH_1 = 0.05;
	private static final double DAMAGE_PERCENT_PER_4_HEALTH_2 = 0.1;
	private static final double MAX_DAMAGE_PERCENT_1 = 0.25;
	private static final double MAX_DAMAGE_PERCENT_2 = 0.5;
	private static final int DAMAGE_INCREMENT_THRESHOLD_HEALTH = 4;
	private static final int SELF_DAMAGE = 4;
	private static final int DAMAGE_1 = 12;
	private static final int DAMAGE_2 = 24;
	private static final int RADIUS = 3;

	private final double mDamagePercentPer4Health;
	private final double mMaxDamagePercent;
	private final int mDamage;

	public RecklessSwing(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "RecklessSwing");
		mInfo.mLinkedSpell = Spells.RECKLESS_SWING;
		mInfo.mScoreboardId = "RecklessSwing";
		mInfo.mShorthandName = "RS";
		mInfo.mDescriptions.add("Passively, every 4 health you fall below your maximum health, gain +5% damage (capped at +25%) on sword and axe hits. Sneak left click with an axe or a sword without hitting a mob to deal 12 damage in a 3 block radius, while also taking 4 health of damage (not reduced by any kind of damage resistance, bypasses absorption).");
		mInfo.mDescriptions.add("Gain +10% damage (capped at +50%) instead. Damage from the active increased to 24.");
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK_AIR;
		mDamagePercentPer4Health = getAbilityScore() == 1 ? DAMAGE_PERCENT_PER_4_HEALTH_1 : DAMAGE_PERCENT_PER_4_HEALTH_2;
		mMaxDamagePercent = getAbilityScore() == 1 ? MAX_DAMAGE_PERCENT_1 : MAX_DAMAGE_PERCENT_2;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			int missingHealthChunks = (int)((mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - mPlayer.getHealth()) / DAMAGE_INCREMENT_THRESHOLD_HEALTH);
			event.setDamage(event.getDamage() * (1 + Math.min(mMaxDamagePercent, mDamagePercentPer4Health * missingHealthChunks)));
		}

		return true;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer.isSneaking()) {
			if (mPlayer.getHealth() <= SELF_DAMAGE) {
				mPlayer.damage(9001);
			} else {
				mPlayer.setHealth(mPlayer.getHealth() - SELF_DAMAGE);
				mPlayer.damage(0);
			}

			Location loc = mPlayer.getLocation();
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
			mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 25, 2, 0, 2, 0);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS)) {
				EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer);
			}
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		return InventoryUtils.isSwordItem(mainhand) || InventoryUtils.isAxeItem(mainhand);
	}

}
