package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

public class Spellshock extends Ability {

	private static final int SPELL_SHOCK_DURATION = 6 * 20;
	private static final int SPELL_SHOCK_TEST_PERIOD = 2;
	private static final int SPELL_SHOCK_DEATH_RADIUS = 3;
	private static final int SPELL_SHOCK_DEATH_DAMAGE = 3;
	private static final int SPELL_SHOCK_SPELL_RADIUS = 4;
	private static final int SPELL_SHOCK_SPELL_DAMAGE = 3;
	private static final int SPELL_SHOCK_REGEN_DURATION = 51;
	private static final int SPELL_SHOCK_REGEN_AMPLIFIER = 1;
	private static final int SPELL_SHOCK_SPEED_DURATION = 120;
	private static final int SPELL_SHOCK_SPEED_AMPLIFIER = 0;
	private static final int SPELL_SHOCK_STAGGER_DURATION = (int)(0.6 * 20);
	private static final int SPELL_SHOCK_VULN_DURATION = 4 * 20;
	private static final int SPELL_SHOCK_VULN_AMPLIFIER = 3; // 20%
	private static final Particle.DustOptions SPELL_SHOCK_COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	public List<LivingEntity> shocked = new ArrayList<LivingEntity>();

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity hit = (LivingEntity) event.getEntity();
			if (!shocked.contains(hit)) {
				shocked.add(hit);
				new BukkitRunnable() {
					int t = 0;
					@Override
					public void run() {
						t += 2;
						Location loc = hit.getLocation().add(0, 1, 0);
						mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.2, 0.6, 0.2, 1);
						mWorld.spawnParticle(Particle.REDSTONE, loc, 4, 0.3, 0.6, 0.3, SPELL_SHOCK_COLOR);

						if (hit.isDead() || hit.getHealth() <= 0) {
							// Mob has died - trigger effects
							mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 50, 1, 1, 1, 0.001);
							mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 100, 1, 1, 1, 0.25);
							mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.0f);
							for (LivingEntity nearbyMob : EntityUtils.getNearbyMobs(hit.getLocation(), SPELL_SHOCK_DEATH_RADIUS)) {
								EntityUtils.damageEntity(mPlugin, nearbyMob, SPELL_SHOCK_DEATH_DAMAGE, player);
								nearbyMob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
								                                           SPELL_SHOCK_VULN_AMPLIFIER, false, true));
							}

							shocked.remove(hit);
						}

						if (t >= SPELL_SHOCK_DURATION) {
							this.cancel();
							shocked.remove(hit);
						}
					}

				}.runTaskTimer(mPlugin, 0, SPELL_SHOCK_TEST_PERIOD);
			}
		}
		return true;
	}

	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		info.scoreboardId = "SpellShock";
		return info;
	}

	@Override
	public boolean runCheck(Player player) {
		return InventoryUtils.isWandItem(player.getInventory().getItemInMainHand());
	}

}
