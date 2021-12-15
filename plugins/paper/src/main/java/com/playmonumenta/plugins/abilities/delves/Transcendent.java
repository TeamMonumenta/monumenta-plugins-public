package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.Material;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CommanderBoss;
import com.playmonumenta.plugins.bosses.bosses.SpellSlingerBoss;
import com.playmonumenta.plugins.bosses.bosses.WrathBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Transcendent extends DelveModifier {

	private static final double[] ABILITY_CHANCE = {
			0.15,
			0.3,
			0.45,
			0.6,
			0.75,
			0.9
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
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[3] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[4] * 100) + "% chance to be Transcendent."
			}, {
				"Elites have a " + Math.round(ABILITY_CHANCE[5] * 100) + "% chance to be Transcendent."
			}
	};

	private final double mAbilityChance;

	public Transcendent(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.TRANSCENDENT);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.TRANSCENDENT);
			mAbilityChance = ABILITY_CHANCE[rank - 1];
		} else {
			mAbilityChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob) && FastUtils.RANDOM.nextDouble() < mAbilityChance) {
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
