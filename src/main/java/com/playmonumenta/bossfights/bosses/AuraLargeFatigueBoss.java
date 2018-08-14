package com.playmonumenta.bossfights.bosses;

import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellBaseAura;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AuraLargeFatigueBoss extends Boss
{
	public static final String identityTag = "FatigueAura";
	public static final int detectionRange = 50;

	LivingEntity mBoss;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new AuraLargeFatigueBoss(plugin, boss);
	}

	public AuraLargeFatigueBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
				new SpellBaseAura(mBoss, 35, 20, 35, 50, Particle.FALLING_DUST, new MaterialData(Material.SAND),
					(Player player) -> {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 1, true, true));
					})
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
