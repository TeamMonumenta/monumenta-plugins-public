package com.playmonumenta.bossfights.bosses;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellRunAction;
import com.playmonumenta.bossfights.utils.Utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
			if (Utils.playersInRange(mBoss.getLocation(), visibleRange).size() == 0) {
				mBoss.addPotionEffect(potion, true);
			}
		});
		invis.run();

		List<Spell> passiveSpells = Arrays.asList(invis);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
