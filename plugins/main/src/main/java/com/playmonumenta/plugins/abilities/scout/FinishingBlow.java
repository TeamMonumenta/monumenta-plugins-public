package com.playmonumenta.plugins.abilities.scout;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class FinishingBlow extends Ability {

	private static final int FINISHING_BLOW_1_DAMAGE = 3;
	private static final int FINISHING_BLOW_2_DAMAGE = 6;

	private static final Particle.DustOptions FINISHING_BLOW_COLOR = new Particle.DustOptions(Color.fromRGB(168, 0, 0), 1.0f);

	private static final String FINISHING_BLOW_RESET_METAKEY = "ResetFinishingBlowTimer";

	private ArrayList<Entity> marked = new ArrayList<Entity>();

	public FinishingBlow(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "FinishingBlow";
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (arrow.isCritical()) {
			if (marked.contains(damagee)) {
				damagee.setMetadata(FINISHING_BLOW_RESET_METAKEY, new FixedMetadataValue(mPlugin, null));
			} else {
				marked.add(damagee);
				new BukkitRunnable() {
					int t = 0;
					@Override
					public void run() {
						if (damagee.hasMetadata(FINISHING_BLOW_RESET_METAKEY)) {
							t = 0;
							damagee.removeMetadata(FINISHING_BLOW_RESET_METAKEY, mPlugin);
						}
						mWorld.spawnParticle(Particle.SMOKE_NORMAL, damagee.getLocation().clone().add(0, 1, 0), 1, 0.25, 0.5, 0.25, 0.02);
						if (t % 4 == 0) {
							mWorld.spawnParticle(Particle.CRIT_MAGIC, damagee.getLocation().clone().add(0, 1, 0), 1, 0.25, 0.5, 0.25, 0);
						}
						if (t >= 5 * 20 || !marked.contains(damagee) || damagee.isDead() || !damagee.isValid()) {
							marked.remove(damagee);
							this.cancel();
						}
						t += 2;
					}
				}.runTaskTimer(mPlugin, 0, 2);
			}
		}
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK
				&& !MetadataUtils.happenedThisTick(mPlugin, mPlayer, EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
			if (marked.contains(event.getEntity())) {
				LivingEntity ent = (LivingEntity) event.getEntity();

				double entMaxHp = ent.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double entHp = ent.getHealth();
				boolean belowHalf = entHp / entMaxHp <= 0.5;

				int damage = getAbilityScore() == 1 ? FINISHING_BLOW_1_DAMAGE : FINISHING_BLOW_2_DAMAGE;
				damage *= belowHalf ? 2 : 1;
				event.setDamage(event.getDamage() + damage);
				marked.remove(ent);

				Location loc = ent.getLocation();
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.25f);
				mWorld.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, 1f, 1.75f);
				mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1f, 1.25f);
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.65f);
				if (belowHalf) {
					mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 1.75f);
				}

				mWorld.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 10, 0.25, 0.5, 0.25, 0.4);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.1);
				mWorld.spawnParticle(Particle.BLOCK_CRACK, loc.clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.4, Bukkit.createBlockData("redstone_wire[power=8]"));
				if (belowHalf) {
					mWorld.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.4);
					mWorld.spawnParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 25, 0.35, 0.5, 0.35, 1.2, FINISHING_BLOW_COLOR);
				}
			}
		}

		return true;
	}
}
