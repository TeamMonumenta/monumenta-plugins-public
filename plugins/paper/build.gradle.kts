import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.errorprone.CheckSeverity

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.playmonumenta.plugins.java-conventions")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1" // Generates plugin.yml
    id("net.minecrell.plugin-yml.bungee") version "0.5.1" // Generates bungee.yml
    id("org.hidetake.ssh") version "2.10.1"
    id("java")
    id("net.ltgt.errorprone") version "2.0.2"
    id("net.ltgt.nullaway") version "1.3.0"
}

dependencies {
    implementation(project(":adapter_api"))
    implementation(project(":adapter_unsupported"))
    implementation(project(":adapter_v1_16_R3"))
    implementation(project(":adapter_v1_18_R1", "reobf"))
    implementation("org.openjdk.jmh:jmh-core:1.19")
    implementation("org.openjdk.jmh:jmh-generator-annprocess:1.19")
    implementation("com.github.LeonMangler:PremiumVanishAPI:2.6.3")
    implementation("net.kyori:adventure-text-minimessage:4.2-ab62718")
    implementation("com.opencsv:opencsv:5.5")
    implementation("dev.jaqobb:namemcapi:2.0.7")
    compileOnly("com.destroystokyo.paper:paper:1.16.5-R0.1-SNAPSHOT")
    compileOnly("dev.jorel.CommandAPI:commandapi-core:6.0.0")
    compileOnly("me.clip:placeholderapi:2.10.4")
    compileOnly("de.jeff_media:ChestSortAPI:12.0.0")
    compileOnly("net.luckperms:api:5.3")
    compileOnly("net.coreprotect:coreprotect:2.15.0")
    compileOnly("com.playmonumenta:scripted-quests:5.1")
    compileOnly("com.playmonumenta:redissync:3.6")
    compileOnly("com.playmonumenta:monumenta-network-relay:1.0")
    compileOnly("com.playmonumenta:structures:7.2")
    compileOnly("com.playmonumenta:worlds:1.6")
    compileOnly("com.playmonumenta:libraryofsouls:4.2")
    compileOnly("com.bergerkiller.bukkit:BKCommonLib:1.15.2-v2")
    compileOnly("com.goncalomb.bukkit:nbteditor:3.2")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.3.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    errorprone("com.google.errorprone:error_prone_core:2.10.0")
    errorprone("com.uber.nullaway:nullaway:0.9.5")

    // Bungeecord deps
    compileOnly("net.md-5:bungeecord-api:1.12-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.8.5")
    compileOnly("com.playmonumenta:monumenta-network-relay:1.0")
    compileOnly("com.vexsoftware:nuvotifier-universal:2.7.2")
}

group = "com.playmonumenta"
description = "Monumenta Main Plugin"
version = rootProject.version

// Configure plugin.yml generation
bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.playmonumenta.plugins.Plugin"
    apiVersion = "1.16"
    name = "Monumenta"
    authors = listOf("The Monumenta Team")
    depend = listOf("CommandAPI", "ScriptedQuests", "NBTAPI")
    softDepend = listOf("MonumentaRedisSync", "PlaceholderAPI", "ChestSort", "LuckPerms", "CoreProtect", "NBTEditor", "LibraryOfSouls", "BKCommonLib", "MonumentaNetworkRelay", "PremiumVanish", "ProtocolLib", "MonumentaStructureManagement", "MonumentaWorldManagement")
}

// Configure bungee.yml generation
bungee {
    name = "Monumenta-Bungee"
    main = "com.playmonumenta.bungeecord.Main"
    author = "The Monumenta Team"
    softDepends = setOf("MonumentaNetworkRelay", "MonumentaRedisSync", "Votifier", "SuperVanish", "PremiumVanish", "BungeeTabListPlus", "LuckPerms")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xmaxwarns")
    options.compilerArgs.add("10000")

    // TODO: Also need to re-enable these deprecation warnings
    //options.compilerArgs.add("-Xlint:deprecation")

    options.errorprone {
        // TODO This must be turned back on as soon as some of the other warnings are under control
        option("NullAway:AnnotatedPackages", "com.playmonumenta.DISABLE")

        allErrorsAsWarnings.set(true)

        /*** Disabled checks ***/
        // These we almost certainly don't want
        check("CatchAndPrintStackTrace", CheckSeverity.OFF) // This is the primary way a lot of exceptions are handled
        check("FutureReturnValueIgnored", CheckSeverity.OFF) // This one is dumb and doesn't let you check return values with .whenComplete()
        check("ImmutableEnumChecker", CheckSeverity.OFF) // Would like to turn this on but we'd have to annotate a bunch of base classes
        check("LockNotBeforeTry", CheckSeverity.OFF) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
        check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
        check("StringSplitter", CheckSeverity.OFF) // We have a lot of string splits too which are fine for this use
        check("MutablePublicArray", CheckSeverity.OFF) // These are bad practice but annoying to refactor and low risk of actual bugs
    }
}

val basicssh = remotes.create("basicssh") {
    host = "admin-eu.playmonumenta.com"
    port = 8822
    user = "epic"
    agent = true
    knownHosts = allowAnyHosts
}

val adminssh = remotes.create("adminssh") {
    host = "admin-eu.playmonumenta.com"
    port = 9922
    user = "epic"
    agent = true
    knownHosts = allowAnyHosts
}

tasks.create("dev1-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                execute("cd /home/epic/dev1_shard_plugins && rm -f Monumenta*.jar")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/dev1_shard_plugins")
            }
        }
    }
}

tasks.create("dev2-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                execute("cd /home/epic/dev2_shard_plugins && rm -f Monumenta*.jar")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/dev2_shard_plugins")
            }
        }
    }
}

tasks.create("dev3-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                execute("cd /home/epic/dev3_shard_plugins && rm -f Monumenta*.jar")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/dev3_shard_plugins")
            }
        }
    }
}

tasks.create("dev4-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                execute("cd /home/epic/dev4_shard_plugins && rm -f Monumenta*.jar")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/dev4_shard_plugins")
            }
        }
    }
}

tasks.create("futurama-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                execute("cd /home/epic/futurama_shard_plugins && rm -f Monumenta*.jar")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/futurama_shard_plugins")
            }
        }
    }
}

tasks.create("mobs-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                execute("cd /home/epic/mob_shard_plugins && rm -f Monumenta*.jar")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/mob_shard_plugins")
            }
        }
    }
}

tasks.create("stage-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(basicssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/stage/m12/server_config/plugins")
                execute("cd /home/epic/stage/m12/server_config/plugins && rm -f Monumenta.jar && ln -s " + shadowJar.archiveFileName.get() + " Monumenta.jar")
            }
        }
    }
}

tasks.create("build-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(adminssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/project_epic/server_config/plugins")
                execute("cd /home/epic/project_epic/server_config/plugins && rm -f Monumenta.jar && ln -s " + shadowJar.archiveFileName.get() + " Monumenta.jar")
                execute("cd /home/epic/project_epic/mobs/plugins && rm -f Monumenta.jar && ln -s ../../server_config/plugins/Monumenta.jar")
            }
        }
    }
}

tasks.create("play-deploy") {
    val shadowJar by tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    doLast {
        ssh.runSessions {
            session(adminssh) {
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m8/server_config/plugins")
                put(shadowJar.archiveFile.get().getAsFile(), "/home/epic/play/m11/server_config/plugins")
                execute("cd /home/epic/play/m8/server_config/plugins && rm -f Monumenta.jar && ln -s " + shadowJar.archiveFileName.get() + " Monumenta.jar")
                execute("cd /home/epic/play/m11/server_config/plugins && rm -f Monumenta.jar && ln -s " + shadowJar.archiveFileName.get() + " Monumenta.jar")
            }
        }
    }
}

fun Service.runSessions(action: RunHandler.() -> Unit) =
    run(delegateClosureOf(action))

fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
    session(*remotes, delegateClosureOf(action))

fun SessionHandler.put(from: Any, into: Any) =
    put(hashMapOf("from" to from, "into" to into))
