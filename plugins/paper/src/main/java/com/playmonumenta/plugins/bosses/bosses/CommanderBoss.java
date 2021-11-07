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
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class CommanderBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_commander";

	public static class Parameters extends BossParameters {
		public int DETECTION = 24;
		private int RANGE = 8;
	}

	private final Parameters mParams;
	boolean mSummonedReinforcements = false;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CommanderBoss(plugin, boss);
	}

	public CommanderBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		List<Spell> passiveSpells = Arrays.asList(
			new SpellInspire(com.playmonumenta.plugins.Plugin.getInstance(), boss, mParams.RANGE)
		);

		super.constructBoss(null, passiveSpells, mParams.DETECTION, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (!mSummonedReinforcements && mBoss.getHealth() < mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {
			mSummonedReinforcements = true;

			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();
			world.playSound(loc, Sound.ENTITY_HORSE_ANGRY, 1f, 2f);
			world.playSound(loc, Sound.ENTITY_HORSE_DEATH, 1f, 0.5f);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), mParams.RANGE, mBoss)) {
				if (!EntityUtils.isBoss(mob)) {
					DelvesUtils.duplicateLibraryOfSoulsMob(mob);
				}
			}
		}
	}

}
