package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
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
	public static final int detectionRange = 50;
	public static final int visibleRange = 5;
	public static final PotionEffect potion = new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, false, false);

	public HiddenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Immediately apply the effect, don't wait
		Spell invis = new SpellRunAction(() -> {
			if (PlayerUtils.playersInRange(mBoss.getLocation(), visibleRange, false).isEmpty()) {
				PotionUtils.applyPotion(null, mBoss, potion);
			}
		});
		invis.run();

		final List<Spell> passiveSpells = List.of(invis);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
