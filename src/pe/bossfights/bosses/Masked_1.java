package pe.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import pe.bossfights.SpellManager;
import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellConditionalTeleport;
import pe.bossfights.spells.SpellMaskedEldritchBeam;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.spells.SpellPushPlayersAway;
import pe.bossfights.utils.Utils;

public class Masked_1 extends Boss
{
	public static final String identityTag = "boss_masked_1";
	public static final int detectionRange = 50;

	Plugin plugin;
	LivingEntity boss;
	Location spawnLoc;
	Location endLoc;

	public Masked_1(Plugin pl, LivingEntity bossIn, Location spawnLocIn, Location endLocIn)
	{
		plugin = pl;
		boss = bossIn;
		spawnLoc = spawnLocIn;
		endLoc = endLocIn;

		SpellBossBar bossBar = new SpellBossBar(plugin);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellMaskedEldritchBeam(plugin, boss),
		                                                 new SpellMaskedShadowGlade(plugin, boss.getLocation(), 2),
		                                                 new SpellMaskedSummonBlazes(plugin, boss)
		                                             ));
		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellBlockBreak(boss),
		                                new SpellPushPlayersAway(boss, 7, 15),
		                                // Teleport the boss to spawnLoc whenever "true" (always)
		                                new SpellConditionalTeleport(boss, spawnLoc, b -> true)
		                            );

		bossBar.spell(boss, detectionRange);
		bossBar.changeColor(BarColor.WHITE);
		bossBar.changeStyle(BarStyle.SOLID);

		super.constructBoss(pl, identityTag, boss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init()
	{
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(boss.getLocation(), detectionRange).size();
		int hp_del = 256;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		boss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		boss.setHealth(bossTargetHp);
	}

	@Override
	public void death()
	{
		endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		boss.teleport(new Location(spawnLoc.getWorld(), 0, -60, 0));
	}
}
