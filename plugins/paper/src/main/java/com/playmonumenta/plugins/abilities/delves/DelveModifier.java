package com.playmonumenta.plugins.abilities.delves;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.projectiles.ProjectileSource;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;

public class DelveModifier extends Ability {

	/*
	 * TODO:
	 *
	 * On every single Delve Modifier, the stat arrays and description
	 * arrays should probably be indexed with rank 1 corresponding to
	 * index 1 instead of index 0. This removes the need to check that
	 * the rank is not 0 before grabbing values, because currently,
	 * if you try to grab a value with rank 0, it tries to access index
	 * -1 and crashes.
	 *
	 * Obviously it would be a pain in the ass to change everything and
	 * then also test to make sure there aren't any new edge case bugs.
	 */

	public static final String AVOID_MODIFIERS = "boss_delveimmune";

	private final Modifier mModifier;

	public DelveModifier(Plugin plugin, Player player, Modifier modifier) {
		super(plugin, player, null);

		mModifier = modifier;
	}

	public static boolean canUse(Player player, Modifier modifier) {
		return DelvesUtils.getDelveInfo(player).getRank(modifier) > 0;
	}

	@Override
	public boolean canUse(Player player) {
		return canUse(player, mModifier);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		Set<String> tags = event.getDamager().getScoreboardTags();
		if (tags == null || !tags.contains(AVOID_MODIFIERS)) {
			if (event.getCause() == DamageCause.CUSTOM) {
				playerTookCustomDamageEvent(event);
			} else {
				playerTookMeleeDamageEvent(event);
			}
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		ProjectileSource source = ((Projectile) event.getDamager()).getShooter();

		if (!(source instanceof Entity)) {
			return true;
		}

		Entity entity = (Entity) source;

		Set<String> tags = entity.getScoreboardTags();
		if (tags == null || !tags.contains(AVOID_MODIFIERS)) {
			playerTookProjectileDamageEvent(entity, event);
		}

		return true;
	}

	public void applyOnSpawnModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (EntityUtils.isHostileMob(mob)) {
			Set<String> tags = mob.getScoreboardTags();
			if (tags == null || !tags.contains(AVOID_MODIFIERS)) {
				applyModifiers(mob, event);

				if (event instanceof SpawnerSpawnEvent) {
					applyModifiers(mob, (SpawnerSpawnEvent) event);
				}
			}
		}
	}

	protected void applyModifiers(LivingEntity mob, EntitySpawnEvent event) { }

	protected void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) { }

	protected void playerTookCustomDamageEvent(EntityDamageByEntityEvent event) { }

	protected void playerTookMeleeDamageEvent(EntityDamageByEntityEvent event) { }

	protected void playerTookProjectileDamageEvent(Entity source, EntityDamageByEntityEvent event) { }

}
