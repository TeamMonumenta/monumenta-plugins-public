package mmbf.utils;

import org.bukkit.entity.Player;

import mmbf.main.Main;
import mmbf.main.MobSpell;

import org.bukkit.entity.Damageable;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;

public class SpellBossBar {
	
	Damageable mob = null;
	BossBar bar;
	int taskID = 0;
	Main plugin;
	MobSpell ms;
	String events[] = new String[101];
	int eventCursor = 100;
	
	public SpellBossBar(Main pl)
	{
		plugin = pl;
		ms = new MobSpell(pl);
		for (int i = 0; i < 101; i++)
			events[i] = "null";
	}
	
	public void spell(Damageable target, int range)
	{
		mob = target;
		double maxHP = ((Attributable) mob).getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		mob.setHealth(maxHP);
		bar = Bukkit.getServer().createBossBar(target.getCustomName(), BarColor.RED, BarStyle.SEGMENTED_10, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY, BarFlag.PLAY_BOSS_MUSIC);
		bar.setVisible(true);
        bar.setProgress(mob.getHealth() / maxHP);
        System.out.println(mob.getCustomName());
		for(Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(mob.getLocation()) < range)
			{
				bar.addPlayer(player);
			}
		}
	}
	
	public void update_bar(Damageable target, int range)
	{
		double maxHP = ((Attributable) target).getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		if (target.getHealth() <= 0)
		{
			bar.setVisible(false);
			while (eventCursor >= 0)
			{
				ms.spellCall((CommandSender)target, events[eventCursor].split(" "));
				eventCursor--;
			}
		}
		for(Player player : Bukkit.getServer().getOnlinePlayers())
		{
			if (player.getLocation().distance(target.getLocation()) < range)
			{
				bar.addPlayer(player);
			}
			else
				bar.removePlayer(player);
		}
		double progress = target.getHealth() / maxHP;
		
		while (eventCursor > (progress * 100))
		{
			ms.spellCall((CommandSender)target, events[eventCursor].split(" "));
			eventCursor--;
		}
		
		bar.setProgress(progress);
		bar.setTitle(target.getCustomName());
	}
	
	public void setEvent(int id, String str)
	{
		events[id] = str;
	}
	
	public void changeColor(BarColor color)
	{
		bar.setColor(color);
	}
	
	public void changeStyle(BarStyle sty)
	{
		bar.setStyle(sty);
	}
	
	public void remove()
	{
		bar.setVisible(false);
	}
}
