package pe.bossfights.bosses;

import java.util.List;
import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.bossfights.Plugin;
import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellMobEffect;

public class InvisibleBoss extends Boss
{
	public static final String identityTag = "boss_invisible";
	public static final int detectionRange = 100;

	LivingEntity mBoss;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new InvisibleBoss(plugin, boss);
	}

	public InvisibleBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		// Immediately apply the effect, don't wait
		Spell invis = new SpellMobEffect(mBoss, new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false));
		invis.run();

		List<Spell> passiveSpells = Arrays.asList(invis);

		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
