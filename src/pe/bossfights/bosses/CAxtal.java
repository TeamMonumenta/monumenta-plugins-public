package pe.bossfights.bosses;

import java.util.Arrays;
import java.util.List;

import mmbf.utils.SpellBossBar;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import pe.bossfights.SpellManager;
import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellAxtalDeathRay;
import pe.bossfights.spells.SpellAxtalMeleeMinions;
import pe.bossfights.spells.SpellAxtalSneakup;
import pe.bossfights.spells.SpellAxtalTntThrow;
import pe.bossfights.spells.SpellAxtalWitherAoe;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellConditionalTeleport;
import pe.bossfights.utils.Utils;

public class CAxtal extends Boss
{
	public static final String identityTag = "boss_caxtal";
	public static final int detectionRange = 110;

	Plugin plugin;
	LivingEntity boss;
	Location spawnLoc;
	Location endLoc;

	public CAxtal(Plugin pl, LivingEntity bossIn, Location spawnLocIn, Location endLocIn)
	{
		plugin = pl;
		boss = bossIn;
		spawnLoc = spawnLocIn;
		endLoc = endLocIn;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellAxtalWitherAoe(plugin, boss, 13, 4),
		                                                 new SpellAxtalMeleeMinions(plugin, boss, 10, 3, 3, 20, 12),
		                                                 new SpellAxtalSneakup(plugin, boss),
		                                                 new SpellAxtalTntThrow(plugin, boss, 5, 15),
		                                                 new SpellAxtalDeathRay(plugin, boss)
		                                             ));
		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellBlockBreak(boss),
		                                // Teleport the boss to spawnLoc if he gets too far away from where he spawned
		                                new SpellConditionalTeleport(boss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > detectionRange),
		                                // Teleport the boss to spawnLoc if he is stuck in bedrock
		                                new SpellConditionalTeleport(boss, spawnLoc, b -> ((b.getLocation().getBlock().getType() == Material.BEDROCK) ||
		                                        (b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK)))
		                            );

		SpellBossBar bossBar = new SpellBossBar(plugin);

		//create bossbar
		bossBar.spell(boss, detectionRange);
		//schedule hp messages
		Location loc = boss.getLocation();
		bossBar.setEvent(100, Utils.getExecuteCommandOnNearbyPlayers(loc, detectionRange, "tellraw @s [\"\",{\"text\":\"At last, the keys are collected. I can be free finally...\",\"color\":\"dark_red\"}]"));
		bossBar.setEvent(50,  Utils.getExecuteCommandOnNearbyPlayers(loc, detectionRange, "tellraw @s [\"\",{\"text\":\"PLEASE. KILL ME. KAUL HOLDS ONTO MY MIND, BUT I YEARN FOR FREEDOM.\",\"color\":\"dark_red\"}]"));
		bossBar.setEvent(25,  Utils.getExecuteCommandOnNearbyPlayers(loc, detectionRange, "tellraw @s [\"\",{\"text\":\"YOU ARE CLOSE. END THIS. END THE REVERIE!\",\"color\":\"dark_red\"}]"));
		bossBar.setEvent(10,  Utils.getExecuteCommandOnNearbyPlayers(loc, detectionRange, "tellraw @s [\"\",{\"text\":\"My servant is nearly dead. You dare to impose your will on the jungle?\",\"color\":\"dark_green\"}]"));

		super.constructBoss(pl, identityTag, boss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init()
	{
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(boss.getLocation(), detectionRange).size();
		int hp_del = 1024;
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

		//launch event related spawn commands
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detectionRange, "effect @s minecraft:blindness 2 2");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"C'Axtal\",\"color\":\"dark_red\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"The Soulspeaker\",\"color\":\"red\",\"bold\":true}]");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	@Override
	public void death()
	{
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		Utils.executeCommandOnNearbyPlayers(boss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"It ends at last... Is this what freedom feels like?..\",\"color\":\"dark_red\"}]");
		endLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
