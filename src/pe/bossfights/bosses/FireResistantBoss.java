package pe.bossfights.bosses;

import java.util.List;
import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.bossfights.Plugin;
import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellMobEffect;

public class FireResistantBoss extends Boss
{
	public static final String identityTag = "boss_fireresist";
	public static final int detectionRange = 100;

	LivingEntity mBoss;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new FireResistantBoss(plugin, boss);
	}

	public FireResistantBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		// Immediately apply the effect, don't wait
		Spell fireresist = new SpellMobEffect(mBoss, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0, false, false));
		fireresist.run();

		List<Spell> passiveSpells = Arrays.asList(fireresist);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
