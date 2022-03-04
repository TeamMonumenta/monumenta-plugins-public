package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobEffect;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InvisibleBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_invisible";
	public static final int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new InvisibleBoss(plugin, boss);
	}

	public InvisibleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Immediately apply the effect, don't wait
		Spell invis = new SpellMobEffect(boss, new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false));
		invis.run();

		List<Spell> passiveSpells = Arrays.asList(invis);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
