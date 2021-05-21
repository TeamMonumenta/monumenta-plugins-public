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
		public Color COLOR = Color.WHITE;
		//to customize the colors use a number between (16777216 - 0)
		public Particle PARTICLE = Particle.REDSTONE;
		//use the same name of the spigot api to change the value
		//if you change the particles to something other than redstone then you will not use COLOR
		public boolean EFFECT_PARTICLE = false;
		public PotionEffectType EFFECT = PotionEffectType.BLINDNESS;
		//use the same name of the spigot api to change the value
	}


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AuraEffectBoss(plugin, boss);
	}

	public AuraEffectBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBaseAura(boss, p.RADIUS, p.HEIGHT, p.RADIUS, p.PARTICEL_NUMBER, p.PARTICLE, new Particle.DustOptions(p.COLOR, p.DUST_SIZE),
			                  (Player player) -> {
								  if (p.EFFECT != null) {
									  player.addPotionEffect(new PotionEffect(p.EFFECT, p.EFFECT_DURATION, p.EFFECT_LEVEL, p.EFFECT_AMBIENT, p.EFFECT_PARTICLE));
								  }
								})
		);
		super.constructBoss(null, passiveSpells, p.DETECTION, null);

	}

}
