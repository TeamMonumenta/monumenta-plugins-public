package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Enhancement effect of Spellshock: Triggers an explosion when the enemy is killed.
 */
public class SpellShockExplosion extends Effect {

	private static final int DURATION = 20 * 60 * 5; // 5 minutes

	private final ItemStatManager.PlayerItemStats mPlayerItemStats;
	private final double mDamage;
	private final double mRadius;
	private final UUID mPlayerUuid; // store a UUID instead of a player to prevent memory leaks

	public SpellShockExplosion(ItemStatManager.PlayerItemStats playerItemStats, double damage, double radius, UUID playerUuid) {
		super(DURATION);
		mPlayerItemStats = playerItemStats;
		mDamage = damage;
		mRadius = radius;
		mPlayerUuid = playerUuid;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		Location loc = entity.getLocation();
		World world = loc.getWorld();
		Player player = Bukkit.getPlayer(mPlayerUuid);
		if (player == null) { // player logged off, don't do any damage
			return;
		}
		new PartialParticle(Particle.SPELL_WITCH, loc, 60, 1, 1, 1, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 45, 1, 1, 1, 0.25).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 1.5f);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(entity.getLocation(), mRadius, entity)) {
			DamageUtils.damage(player, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, ClassAbility.SPELLSHOCK, mPlayerItemStats), mDamage, true, false, false);
		}
	}

	@Override
	public String toString() {
		return String.format("SpellShockExplosion, player=%s", mPlayerUuid);
	}
}
