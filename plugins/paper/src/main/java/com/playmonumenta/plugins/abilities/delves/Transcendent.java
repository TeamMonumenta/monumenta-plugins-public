package com.playmonumenta.plugins.abilities.delves;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CommanderBoss;
import com.playmonumenta.plugins.bosses.bosses.SpellSlingerBoss;
import com.playmonumenta.plugins.bosses.bosses.WrathBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Transcendent extends DelveModifier {

	private static final String TRANSCENDENT_TAG = "Transcendent";
	private static final String HEALTH_MODIFIER_NAME = "TranscendentHealthModifier";
	private static final double HEALTH_MULTIPLIER = 1.5;
	private static final double DAMAGE_MULTIPLIER = 1.25;

	private static final double[] ABILITY_CHANCE = {
			0.2,
			0.4,
			0.6
	};

	private static final String[] ABILITY_POOL_MELEE = {
		WrathBoss.identityTag,
		SpellSlingerBoss.identityTag,
		CommanderBoss.identityTag
	};

	private static final String[] ABILITY_POOL = {
		SpellSlingerBoss.identityTag,
		CommanderBoss.identityTag
	};

	public static final String DESCRIPTION = "Elites become greatly empowered.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Elites have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to be Transcendent."
			}
	};

	private final double mAbilityChance;

	public Transcendent(Plugin plugin, Player player) {
		super(plugin, player, Modifier.TRANSCENDENT);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.TRANSCENDENT);
		mAbilityChance = ABILITY_CHANCE[rank - 1];
	}

	@Override
	protected void playerTookCustomDamageEvent(EntityDamageByEntityEvent event) {
		modifyDamage(event.getDamager(), event);
	}

	@Override
	protected void playerTookMeleeDamageEvent(EntityDamageByEntityEvent event) {
		modifyDamage(event.getDamager(), event);
	}

	@Override
	protected void playerTookProjectileDamageEvent(Entity source, EntityDamageByEntityEvent event) {
		modifyDamage(source, event);
	}

	private void modifyDamage(Entity source, EntityDamageByEntityEvent event) {
		Set<String> tags = source.getScoreboardTags();
		if (tags != null && tags.contains(TRANSCENDENT_TAG)) {
			event.setDamage(EntityUtils.getDamageApproximation(event, DAMAGE_MULTIPLIER));
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		Set<String> tags = mob.getScoreboardTags();
		if (EntityUtils.isElite(mob)
				&& !tags.contains(StatMultiplier.DELVE_MOB_TAG)
				&& FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			if (mob instanceof Attributable) {
				double healthProportion = Math.min(1, mob.getHealth() / mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

				EntityUtils.addAttribute(mob, Attribute.GENERIC_MAX_HEALTH,
						new AttributeModifier(HEALTH_MODIFIER_NAME, HEALTH_MULTIPLIER - 1, Operation.MULTIPLY_SCALAR_1));

				mob.setHealth(healthProportion * mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
			}

			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(TRANSCENDENT_TAG);

			EntityEquipment equipment = mob.getEquipment();
			ItemStack mainhand = equipment == null ? null : equipment.getItemInMainHand();
			Material material = mainhand == null ? null : mainhand.getType();
			if (material == Material.BOW || material == Material.CROSSBOW || material == Material.TRIDENT
					|| mob instanceof Evoker) {
				mob.addScoreboardTag(ABILITY_POOL[FastUtils.RANDOM.nextInt(ABILITY_POOL.length)]);
			} else {
				mob.addScoreboardTag(ABILITY_POOL_MELEE[FastUtils.RANDOM.nextInt(ABILITY_POOL_MELEE.length)]);
			}
		}
	}

}
