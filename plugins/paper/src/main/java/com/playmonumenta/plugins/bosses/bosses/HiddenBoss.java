package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/*
 * Mob that is invisible until players are nearby
 * TODO: This boss and others that apply effects to mobs should be combined into one boss using the Boss Param system
 */
public final class HiddenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hidden";
	public static final PotionEffect potion = new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, false, false);

	@BossParam(help = "Applies invisibility to the boss if the player is outside the visible range")
	public static class Parameters extends BossParameters {
		@BossParam
		public int DETECTION_RANGE = 50;
		@BossParam(help = "The range in which the player must be within to see the boss")
		public int VISIBLE_RANGE = 5;
	}


	public HiddenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		HiddenBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new HiddenBoss.Parameters());

		// Immediately apply the effect, don't wait
		Spell invis = new SpellRunAction(() -> {
			if (PlayerUtils.playersInRange(mBoss.getLocation(), p.VISIBLE_RANGE, false).isEmpty()) {
				PotionUtils.applyPotion(null, mBoss, potion);
			}
		});
		invis.run();

		final List<Spell> passiveSpells = List.of(invis);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION_RANGE, null);
	}
}
