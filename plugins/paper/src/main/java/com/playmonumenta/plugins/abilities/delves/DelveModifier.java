package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

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

	private final @Nullable Modifier mModifier;

	public DelveModifier(Plugin plugin, @Nullable Player player, @Nullable Modifier modifier) {
		super(plugin, player, null);

		mModifier = modifier;
	}

	public static boolean canUse(Player player, @Nullable Modifier modifier) {
		return player != null && DelvesUtils.getDelveInfo(player).getRank(modifier) > 0;
	}

	@Override
	public boolean canUse(Player player) {
		return canUse(player, mModifier);
	}

	private boolean shouldApplyModifiers(Entity mob) {
		Set<String> tags = mob.getScoreboardTags();
		return !tags.contains(AVOID_MODIFIERS);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (shouldApplyModifiers(event.getEntity())) {
			entityDeathEvent(event);
		}
	}

	/* This version is only called when the event should apply modifiers! */
	protected void entityDeathEvent(EntityDeathEvent event) {
	}

	public void applyOnSpawnModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (EntityUtils.isHostileMob(mob)) {
			Set<String> tags = mob.getScoreboardTags();
			if (!tags.contains(AVOID_MODIFIERS)) {
				PotionEffect resistance = mob.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				if (resistance == null || resistance.getAmplifier() < 4 || resistance.getDuration() < 20 * 8) {
					applyModifiers(mob, event);

					if (event instanceof SpawnerSpawnEvent) {
						applyModifiers(mob, (SpawnerSpawnEvent) event);
					}
				}
			}
		}
	}

	protected void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {

	}

	protected void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {

	}

}
