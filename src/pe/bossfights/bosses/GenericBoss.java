package pe.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;

public class GenericBoss extends Boss
{
	public static final String identityTag = "boss_generic";
	public static final int detectionRange = 30;

	LivingEntity mBoss;

	public GenericBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		SpellBossBar bossBar = new SpellBossBar(plugin);
		bossBar.spell(mBoss, detectionRange);

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, bossBar);
	}
}
