package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.BossUtils;

/*
 * Mob that is invisible until players are nearby
 */
public class HiddenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hidden";
	public static final int detectionRange = 50;
	public static final int visibleRange = 5;
	public static final PotionEffect potion = new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, false, false);

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HiddenBoss(plugin, boss);
	}

	public HiddenBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		// Immediately apply the effect, don't wait
		Spell invis = new SpellRunAction(() -> {
			if (BossUtils.getPlayersInRangeForHealthScaling(mBoss, visibleRange) < 1) {
				mBoss.addPotionEffect(potion);
			}
		});
		invis.run();

		List<Spell> passiveSpells = Arrays.asList(invis);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
