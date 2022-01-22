package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.utils.BossUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/*
 * Mob that is invisible until players are nearby
 */
public final class HiddenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_hidden";
	public static final int detectionRange = 50;
	public static final int visibleRange = 5;
	public static final PotionEffect potion = new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, false, false);

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HiddenBoss(plugin, boss);
	}

	public HiddenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Immediately apply the effect, don't wait
		Spell invis = new SpellRunAction(() -> {
			if (BossUtils.getPlayersInRangeForHealthScaling(boss, visibleRange) < 1) {
				boss.addPotionEffect(potion);
			}
		});
		invis.run();

		List<Spell> passiveSpells = Arrays.asList(invis);

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
