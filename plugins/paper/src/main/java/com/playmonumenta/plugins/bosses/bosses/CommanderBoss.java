package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellInspire;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class CommanderBoss extends BossAbilityGroup {

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

		super.constructBoss(SpellManager.EMPTY, passiveSpells, mParams.DETECTION, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!mSummonedReinforcements && mBoss.getHealth() < EntityUtils.getMaxHealth(mBoss) / 2) {
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
