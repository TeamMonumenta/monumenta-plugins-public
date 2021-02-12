package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellInspire;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class CommanderBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_commander";
	public static final int detectionRange = 24;

	private static final int RANGE = 8;

	boolean mSummonedReinforcements = false;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CommanderBoss(plugin, boss);
	}

	public CommanderBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellInspire(com.playmonumenta.plugins.Plugin.getInstance(), boss, RANGE)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (!mSummonedReinforcements && mBoss.getHealth() < mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {
			mSummonedReinforcements = true;

			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();
			world.playSound(loc, Sound.ENTITY_HORSE_ANGRY, 1f, 2f);
			world.playSound(loc, Sound.ENTITY_HORSE_DEATH, 1f, 0.5f);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), RANGE, mBoss)) {
				if (!EntityUtils.isBoss(mob)) {
					DelvesUtils.duplicateLibraryOfSoulsMob(mob);
				}
			}
		}
	}

}
