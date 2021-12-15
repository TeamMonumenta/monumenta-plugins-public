package com.playmonumenta.plugins.bosses.bosses;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.EntityUtils;

public class TrainingDummyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_training_dummy";
	public static final int detectionRange = 25;
	private static final DecimalFormat cutoffDigits = new DecimalFormat("#.#####"); // number of #s determines maximum digits shown

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TrainingDummyBoss(plugin, boss);
	}

	public TrainingDummyBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellRunAction(() -> {
				boss.setHealth(EntityUtils.getMaxHealth(boss));
			}, 60 * 20)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
		boss.setRemoveWhenFarAway(false);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		double damage = event.getFinalDamage();

		// Damage smaller than this is only meant to tag the mob as damaged by a player
		if (damage < 0.01) {
			return;
		}

		String damageString = cutoffDigits.format(damage);
		if (!damageString.contains(".")) {
			damageString += ".0"; // DecimalFormat would take 1.0 to "1", but the ".0" is desired
		}

		if (damager instanceof Player player) {
			player.sendMessage(ChatColor.GOLD + "Damage: " + ChatColor.RED + damageString);
		} else if (damager instanceof Projectile projectile) {
			if (projectile.getShooter() instanceof Player player) {
				player.sendMessage(ChatColor.GOLD + "Damage: " + ChatColor.RED + damageString);
			}
		}
	}
}
