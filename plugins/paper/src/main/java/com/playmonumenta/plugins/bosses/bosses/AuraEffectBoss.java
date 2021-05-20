package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAura;
import com.playmonumenta.plugins.utils.BossUtils;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;

public class AuraEffectBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_auraeffect";

	public static class Parameters {
		public int RADIUS = 35;
		public int HEIGHT = 20;
		public int DETECTION = 45;
		public int EFFECT_LEVEL = 1;
		public float DUST_SIZE = 2f;
		public int PARTICEL_NUMBER = 20;
		public int EFFECT_DURATION = 3 * 20;
		public boolean EFFECT_AMBIENT = true;
		public Color DUST_COLOR = Color.WHITE;
		public boolean EFFECT_PARTICLE = false;
		public PotionEffectType EFFECT = PotionEffectType.LUCK;
	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraEffectBoss(plugin, boss);
	}

	public AuraEffectBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());
		boss.getWorld().sendMessage(Component.text(p.DUST_COLOR.toString()));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, p.RADIUS, p.HEIGHT, p.RADIUS, p.PARTICEL_NUMBER, Particle.REDSTONE, new Particle.DustOptions(p.DUST_COLOR, 2f),
			                  (Player player) -> {
								  if (p.EFFECT != null) {
									  player.addPotionEffect(new PotionEffect(p.EFFECT, p.EFFECT_DURATION, p.EFFECT_LEVEL, p.EFFECT_AMBIENT, p.EFFECT_PARTICLE));
								  }
								})
		);
		super.constructBoss(null, passiveSpells, p.DETECTION, null);

	}

}
