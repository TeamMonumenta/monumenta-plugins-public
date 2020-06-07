package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.abilities.delves.cursed.Spectral;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * DREADFUL: You take x2 damage.
 * Mobs turn into Spectres on death 25% of the time.
 * Elites turn into Dreadnaughts on death 50% of the time.
 */

public class Dreadful extends StatMultiplier {

	public static final String DREADFUL_DREADNAUGHT_TAG = "boss_dreadnaughtparticle";
	public static final String DREADFUL_DREADLING_TAG = "boss_dreadling";

	private static final String DREADFUL_SUMMON_COMMAND = "summon minecraft:zombie ";
	private static final String DREADFUL_1_SUMMON_COMMAND_DATA = " {Attributes:[{Base:100.0d,Name:\"generic.maxHealth\"},{Base:1.0d,Name:\"generic.knockbackResistance\"},{Base:0.28d,Name:\"generic.movementSpeed\"},{Base:0.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:64.0d,Name:\"generic.followRange\"},{Base:3.0d,Name:\"generic.attackDamage\"},{Base:0.0d,Name:\"zombie.spawnReinforcements\"}],HandDropChances:[0.0f,0.0f],PersistenceRequired:0b,Tags:[\"Elite\",\"boss_ccimmune\",\"boss_dreadnaughtparticle\",\"boss_charger\",\"boss_projdeflect\"],Health:100.0f,HandItems:[{id:\"minecraft:stone_axe\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§5§lErebus\\\"}\"},AttributeModifiers:[{UUIDMost:-9128757219735418705L,UUIDLeast:-7448010627679965268L,Amount:11.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:shield\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:10},{Pattern:\"gra\",Color:15}],Base:15}}}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],CustomName:\"{\\\"text\\\":\\\"§5§lDreadnaught\\\"}\",ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:1908001,Name:\"{\\\"text\\\":\\\"§1§lShadowborn Boots\\\"}\"}}},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§9§lAntimatter Chestplate\\\"}\"}}},{}],CanPickUpLoot:0b,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}],DrownedConversionTime:-1}";
	private static final String DREADFUL_2_SUMMON_COMMAND_DATA = " {Attributes:[{Base:200.0d,Name:\"generic.maxHealth\"},{Base:1.0d,Name:\"generic.knockbackResistance\"},{Base:0.28d,Name:\"generic.movementSpeed\"},{Base:0.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:64.0d,Name:\"generic.followRange\"},{Base:3.0d,Name:\"generic.attackDamage\"},{Base:0.0d,Name:\"zombie.spawnReinforcements\"}],HandDropChances:[0.0f,0.0f],PersistenceRequired:0b,Tags:[\"Elite\",\"boss_ccimmune\",\"boss_dreadnaughtparticle\",\"boss_chargerstrong\",\"boss_projdeflect\"],Health:200.0f,HandItems:[{id:\"minecraft:stone_axe\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§5§lErebus\\\"}\"},AttributeModifiers:[{UUIDMost:-9128757219735418705L,UUIDLeast:-7448010627679965268L,Amount:22.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:shield\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:10},{Pattern:\"gra\",Color:15}],Base:15}}}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],CustomName:\"{\\\"text\\\":\\\"§5§lDreadnaught\\\"}\",ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:1908001,Name:\"{\\\"text\\\":\\\"§1§lShadowborn Boots\\\"}\"}}},{},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§9§lAntimatter Chestplate\\\"}\"}}},{}],CanPickUpLoot:0b,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}],DrownedConversionTime:-1}";

	private static final int DREADFUL_CHALLENGE_SCORE = 24;
	private static final int DREADFUL_SPAWN_COUNTER_SPAWN = 10;
	private static final double DREADFUL_DAMAGE_TAKEN_MULTIPLIER = 2;

	private final String mSpectralSummonCommandData;
	private final String mDreadfulSummonCommandData;

	private int mSpawnCounter = 0;
	private int mEliteSpawnCounter = 0;

	public Dreadful(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "The air reeks of death, heralding a " + ChatColor.DARK_RED + ChatColor.BOLD + "DREADFUL" + ChatColor.GRAY + " fate for the fallen.",
				1, DREADFUL_DAMAGE_TAKEN_MULTIPLIER, DREADFUL_DAMAGE_TAKEN_MULTIPLIER);
		mSpectralSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? Spectral.SPECTRAL_2_SUMMON_COMMAND_DATA : Spectral.SPECTRAL_1_SUMMON_COMMAND_DATA;
		mDreadfulSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? DREADFUL_2_SUMMON_COMMAND_DATA : DREADFUL_1_SUMMON_COMMAND_DATA;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, DREADFUL_CHALLENGE_SCORE);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity mob = event.getEntity();

		if (!mob.getScoreboardTags().contains(DREADFUL_DREADNAUGHT_TAG)
				&& !mob.getScoreboardTags().contains(Spectral.SPECTRAL_SPECTRE_TAG)
				&& !mob.getScoreboardTags().contains(DREADFUL_DREADLING_TAG)) {
			if (EntityUtils.isElite(mob)) {
				mEliteSpawnCounter += (4 + FastUtils.RANDOM.nextInt(3));

				if (mEliteSpawnCounter >= DREADFUL_SPAWN_COUNTER_SPAWN) {
					mEliteSpawnCounter -= DREADFUL_SPAWN_COUNTER_SPAWN;
					Location loc = mob.getLocation();
					String command = DREADFUL_SUMMON_COMMAND + loc.getX() + " " + loc.getY() + " " + loc.getZ() + mDreadfulSummonCommandData;
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

					loc.add(0, 1, 0);
					mWorld.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.1);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
				}
			} else {
				mSpawnCounter += (2 + FastUtils.RANDOM.nextInt(2));

				if (mSpawnCounter >= DREADFUL_SPAWN_COUNTER_SPAWN) {
					mSpawnCounter -= DREADFUL_SPAWN_COUNTER_SPAWN;
					Location loc = mob.getLocation();
					String command = Spectral.SPECTRAL_SUMMON_COMMAND + loc.getX() + " " + loc.getY() + " " + loc.getZ() + mSpectralSummonCommandData;
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

					loc.add(0, 1, 0);
					mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
				}
			}
		}
	}

}
