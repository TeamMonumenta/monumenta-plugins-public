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

public class AuraSmallWeaknessBoss extends BossAbilityGroup
{
	public static final String identityTag = "aura_weakness";
	public static final int detectionRange = 40;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new AuraSmallWeaknessBoss(plugin, boss);
	}

	public AuraSmallWeaknessBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
				new SpellBaseAura(mBoss, 8, 5, 8, 10, Particle.FALLING_DUST, new MaterialData(Material.REDSTONE_BLOCK),
					(Player player) -> {
						player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, true, true));
					})
		);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
