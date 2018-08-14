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

public class AuraLargeHungerBoss extends Boss
{
	public static final String identityTag = "HungerAura";
	public static final int detectionRange = 100;

	LivingEntity mBoss;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new AuraLargeHungerBoss(plugin, boss);
	}

	public AuraLargeHungerBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
				new SpellBaseAura(mBoss, 35, 20, 35, 50, Particle.FALLING_DUST, new MaterialData(Material.MELON_BLOCK),
					(Player player) -> {
						player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 0, false, false));
					})
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
