package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;

public class DivineJustice extends Ability {

	private static final int CRITICAL_UNDEAD_DAMAGE = 4;
	private static final double CRITICAL_SCALING = 0.15;
	private static final int RADIUS = 12;
	private static final double LUMINOUS_BONUS = 4;
	
	private final double mCriticalScaling;
	
	private Crusade mCrusade;
	private LuminousInfusion mLuminous;

	public DivineJustice(Plugin plugin, Player player) {
		super(plugin, player, "Divine Justice");
		mInfo.mScoreboardId = "DivineJustice";
		mInfo.mShorthandName = "DJ";
		mInfo.mDescriptions.add("Your critical strikes deal +4 damage to undead enemies.");
		mInfo.mDescriptions.add("Additionally, your critical strikes deal +15% damage to undead enemies, calculated from the final damage of the critical strike. Additionally, heal 10% of your max health and 5% of other players' max health within 12 blocks whenever you kill an undead enemy.");
		mCriticalScaling = getAbilityScore() == 1 ? 0 : CRITICAL_SCALING;
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
				mLuminous = AbilityManager.getManager().getPlayerAbility(mPlayer, LuminousInfusion.class);
			}
		});
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && PlayerUtils.isCritical(mPlayer)) {
			LivingEntity damagee = (LivingEntity) event.getEntity();
			if (EntityUtils.isUndead(damagee) || (mCrusade.getAbilityScore() == 2 && EntityUtils.isHumanoid(damagee))) {
				Location loc = damagee.getLocation().add(0, damagee.getHeight() / 2, 0);
				double xz = damagee.getWidth() / 2 + 0.1;
				double y = damagee.getHeight() / 3;
				World world = mPlayer.getWorld();
				world.spawnParticle(Particle.END_ROD, loc, 5, xz, y, xz, 0.065);
				world.spawnParticle(Particle.FLAME, loc, 6, xz, y, xz, 0.05);
				world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.15f, 1.5f);

				double bonusDamage = 0.0;
				double bonusScaling = 0.0;
				double baseDamage = CRITICAL_UNDEAD_DAMAGE;
				if (mLuminous != null) {
					if (mLuminous.getAbilityScore() == 2) {
						baseDamage += LUMINOUS_BONUS;
					}
				}
				if (mCrusade != null) {
					if (mCrusade.getAbilityScore() > 0) {
						bonusDamage = baseDamage * 0.33;
						bonusScaling = mCriticalScaling * 0.33;
						world.spawnParticle(Particle.CRIT_MAGIC, damagee.getEyeLocation(), 10, 0.25, 0.5, 0.25, 0);
					}
				}
				
				event.setDamage((event.getDamage() + baseDamage + bonusDamage) * (1 + mCriticalScaling + bonusScaling));
			}
		}
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity mob = (LivingEntity) event.getEntity();
		if (EntityUtils.isUndead(mob) || (mCrusade.getAbilityScore() == 2 && EntityUtils.isHumanoid(mob))) {
			double percentMaxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.1;
			PlayerUtils.healPlayer(mPlayer, percentMaxHealth);
			for (Player p : PlayerUtils.playersInRange(mPlayer, RADIUS, true)) {
				percentMaxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.1;
				PlayerUtils.healPlayer(p, percentMaxHealth);
			}
		}
	}

}
