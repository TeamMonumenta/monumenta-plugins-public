package pe.bossfights.bosses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;

import pe.bossfights.BossBarManager;
import pe.bossfights.Plugin;
import pe.bossfights.SpellManager;
import pe.bossfights.spells.Spell;
import pe.bossfights.spells.SpellBlockBreak;
import pe.bossfights.spells.SpellConditionalTeleport;
import pe.bossfights.spells.SpellMaskedFrostNova;
import pe.bossfights.spells.SpellMaskedShadowGlade;
import pe.bossfights.spells.SpellMaskedSummonBlazes;
import pe.bossfights.utils.SerializationUtils;
import pe.bossfights.utils.Utils;

public class Masked_2 extends Boss
{
	public static final String identityTag = "boss_masked_2";
	public static final int detectionRange = 50;

	LivingEntity mBoss;
	Location mSpawnLoc;
	Location mEndLoc;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		String content = SerializationUtils.retrieveDataFromEntity(boss);

		if (content == null || content.isEmpty())
			throw new Exception("Can't instantiate " + identityTag + " with no serialized data");

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(content, JsonObject.class);

		if (!(object.has("spawnX") && object.has("spawnY") && object.has("spawnZ") &&
		      object.has("endX") && object.has("endY") && object.has("endZ")))
			throw new Exception("Failed to instantiate " + identityTag + ": missing required data element");

		Location spawnLoc = new Location(boss.getWorld(), object.get("spawnX").getAsDouble(),
		                                 object.get("spawnY").getAsDouble(), object.get("spawnZ").getAsDouble());
		Location endLoc = new Location(boss.getWorld(), object.get("endX").getAsDouble(),
		                               object.get("endY").getAsDouble(), object.get("endZ").getAsDouble());

		return new Masked_2(plugin, boss, spawnLoc, endLoc);
	}

	public Masked_2(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc)
	{
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
		                                                 new SpellMaskedFrostNova(plugin, mBoss, 9, 70),
		                                                 new SpellMaskedShadowGlade(plugin, spawnLoc, 2),
		                                                 new SpellMaskedSummonBlazes(plugin, mBoss)
		                                             ));
		List<Spell> passiveSpells = Arrays.asList(
		                                new SpellBlockBreak(mBoss),
		                                // Teleport the boss to spawnLoc whenever condition is true
		                                new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getY() < 157)
		                            );

		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BarColor.RED, BarStyle.SOLID, null);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init()
	{
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hp_del = 256;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0)
		{
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);
	}

	@Override
	public void death()
	{
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public String serialize()
	{
		Gson gson = new GsonBuilder().create();
		JsonObject root = new JsonObject();

		root.addProperty("spawnX", mSpawnLoc.getX());
		root.addProperty("spawnY", mSpawnLoc.getY());
		root.addProperty("spawnZ", mSpawnLoc.getZ());
		root.addProperty("endX", mEndLoc.getX());
		root.addProperty("endY", mEndLoc.getY());
		root.addProperty("endZ", mEndLoc.getZ());

		return gson.toJson(root);
	}
}
