package com.playmonumenta.plugins.abilities.delves.cursed;

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
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * SPECTRAL: You take x1.6 damage.
 * Mobs turn into Spectres on death 15% of the time.
 */

public class Spectral extends StatMultiplier {

	public static final String SPECTRAL_SPECTRE_TAG = "boss_spectreparticle";

	public static final String SPECTRAL_SUMMON_COMMAND = "summon minecraft:skeleton ";
	public static final String SPECTRAL_1_SUMMON_COMMAND_DATA = " {Attributes:[{Base:60.0d,Name:\"generic.maxHealth\"},{Base:1.0d,Name:\"generic.knockbackResistance\"},{Base:0.28d,Name:\"generic.movementSpeed\"},{Base:0.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:64.0d,Name:\"generic.followRange\"},{Base:2.0d,Name:\"generic.attackDamage\"}],FallFlying:1b,HandDropChances:[0.0f,0.0f],PersistenceRequired:0b,Tags:[\"boss_ccimmune\",\"boss_punchresist\",\"boss_tpbehindtargeted\",\"boss_spectreparticle\"],Motion:[0.0d,0.0d,0.0d],Health:60.0f,Silent:1b,HandItems:[{id:\"minecraft:golden_hoe\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§8§lSoulreaper\\\"}\"},AttributeModifiers:[{UUIDMost:6652092716411865999L,UUIDLeast:-7702247990190916129L,Amount:6.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],CustomName:\"{\\\"text\\\":\\\"§8§lSpectre\\\"}\",ArmorItems:[{},{},{},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"125712ee-79e3-4de7-b2b3-0471dc268dfc\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZiODVlZjFlMTk4NmM3NTZhMTE0MDY1YThhZGY1OTE2ZTQzNjY0MTFhOTA4OTQ0YWU0YjNiMTI4ZjNkYmIifX19\"}]}},display:{Name:\"{\\\"text\\\":\\\"Ghost\\\"}\"}}}],CanPickUpLoot:0b,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}],CustomNameVisible:0b}";
	public static final String SPECTRAL_2_SUMMON_COMMAND_DATA = " {Attributes:[{Base:120.0d,Name:\"generic.maxHealth\"},{Base:1.0d,Name:\"generic.knockbackResistance\"},{Base:0.28d,Name:\"generic.movementSpeed\"},{Base:0.0d,Name:\"generic.armor\"},{Base:0.0d,Name:\"generic.armorToughness\"},{Base:64.0d,Name:\"generic.followRange\"},{Base:2.0d,Name:\"generic.attackDamage\"}],FallFlying:1b,HandDropChances:[0.0f,0.0f],PersistenceRequired:0b,Tags:[\"boss_ccimmune\",\"boss_punchresist\",\"boss_tpbehindtargeted\",\"boss_spectreparticle\"],Motion:[0.0d,0.0d,0.0d],Health:120.0f,Silent:1b,HandItems:[{id:\"minecraft:golden_hoe\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§8§lSoulreaper\\\"}\"},AttributeModifiers:[{UUIDMost:6652092716411865999L,UUIDLeast:-7702247990190916129L,Amount:13.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{}],ArmorDropChances:[0.0f,0.0f,0.0f,0.0f],CustomName:\"{\\\"text\\\":\\\"§8§lSpectre\\\"}\",ArmorItems:[{},{},{},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"125712ee-79e3-4de7-b2b3-0471dc268dfc\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWZiODVlZjFlMTk4NmM3NTZhMTE0MDY1YThhZGY1OTE2ZTQzNjY0MTFhOTA4OTQ0YWU0YjNiMTI4ZjNkYmIifX19\"}]}},display:{Name:\"{\\\"text\\\":\\\"Ghost\\\"}\"}}}],CanPickUpLoot:0b,ActiveEffects:[{Ambient:1b,ShowIcon:1b,ShowParticles:1b,Duration:1000000,Id:14b,Amplifier:0b}],CustomNameVisible:0b}";

	private static final int SPECTRAL_CHALLENGE_SCORE = 14;
	private static final int SPECTRAL_SPAWN_COUNTER_SPAWN = 20;
	private static final double SPECTRAL_DAMAGE_TAKEN_MULTIPLIER = 1.6;

	private final String mSpectralSummonCommandData;

	private int mSpawnCounter = 0;

	public Spectral(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "The air reeks of death, heralding a " + ChatColor.RED + ChatColor.BOLD + "SPECTRAL" + ChatColor.GRAY + " fate for the fallen.",
				SPECTRAL_DAMAGE_TAKEN_MULTIPLIER, SPECTRAL_DAMAGE_TAKEN_MULTIPLIER, 1);
		mSpectralSummonCommandData = ServerProperties.getClassSpecializationsEnabled() ? SPECTRAL_2_SUMMON_COMMAND_DATA : SPECTRAL_1_SUMMON_COMMAND_DATA;
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SPECTRAL_CHALLENGE_SCORE);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity mob = event.getEntity();

		if (!mob.getScoreboardTags().contains(SPECTRAL_SPECTRE_TAG)) {
			mSpawnCounter += (2 + FastUtils.RANDOM.nextInt(3));

			if (mSpawnCounter >= SPECTRAL_SPAWN_COUNTER_SPAWN) {
				mSpawnCounter -= SPECTRAL_SPAWN_COUNTER_SPAWN;

				Location loc = mob.getLocation();
				String command = SPECTRAL_SUMMON_COMMAND + loc.getX() + " " + loc.getY() + " " + loc.getZ() + mSpectralSummonCommandData;
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

				loc.add(0, 1, 0);
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5);
				mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
			}
		}
	}

}
