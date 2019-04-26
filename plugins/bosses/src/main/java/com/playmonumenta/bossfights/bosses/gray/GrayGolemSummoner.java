package com.playmonumenta.bossfights.bosses.gray;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;

public class GrayGolemSummoner extends GrayStrongSummonerBase {
	public static final String identityTag = "boss_gray_summ_golem";
	public static final int detectionRange = 35;
	private static final String nbt = "{HurtByTimestamp:0,Leashed:0b,Health:60.0f,Bukkit.updateLevel:2,Attributes:[{Base:60.0d,Name:\"generic.maxHealth\"}],OnGround:1b,Dimension:0,PortalCooldown:0,AbsorptionAmount:0.0f,HandItems:[{id:\"minecraft:stone_axe\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§6§lSoulcrusher\\\"}\"},Damage:0,AttributeModifiers:[{UUIDMost:-4664142119989130575L,UUIDLeast:-7207479895603301059L,Amount:1.0d,Slot:\"mainhand\",AttributeName:\"generic.knockbackResistance\",Operation:0,Name:\"Modifier\"},{UUIDMost:-9164396771939824792L,UUIDLeast:-6052235485526131778L,Amount:14.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"},{UUIDMost:-433896928173994138L,UUIDLeast:-8859177146029034537L,Amount:0.05d,Slot:\"mainhand\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}},{id:\"minecraft:redstone\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§4§lCorrupted Malevolence\\\"}\"}}}],CustomName:\"{\\\"text\\\":\\\"Conjured Golem\\\"}\",WorldUUIDMost:-1041596277173696703L,ArmorItems:[{id:\"minecraft:chainmail_boots\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§bSummonerBoots\\\"}\"},Damage:0}},{id:\"minecraft:chainmail_leggings\",Count:1b,tag:{display:{Name:\"{\\\"text\\\":\\\"§9§lIronscale Leggings\\\"}\"}}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:11546150,Name:\"{\\\"text\\\":\\\"§bSummoner2Chest\\\"}\"},Damage:0}},{id:\"minecraft:leather_helmet\",Count:1b,tag:{display:{color:9468004,Name:\"{\\\"text\\\":\\\"§c§lSacrifice\\\\u0027s Scalp\\\"}\"},Damage:0}}],Spigot.ticksLived:92,WorldUUIDLeast:-7560693509725274339L,Tags:[\"boss_gray_summoned\"]}";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GrayGolemSummoner(plugin, boss);
	}

	public GrayGolemSummoner(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, boss, identityTag, detectionRange, EntityType.HUSK, nbt);
	}
}
