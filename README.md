## Architecture

| Platform | Architecture       | Dispatch Strategy |
|----------|--------------------|-------------------|
| **Spigot / Paper** | Event Dispatcher   | Registered listeners are dispatched by `PluginManager`. |
| **Forge** | Event Bus          | Events are posted to one or more `IEventBus` instances. |
| **Fabric** | Event Callback API | Registered callbacks are invoked through `Event<T>` invokers. |

```
Observer Pattern
        │
        ▼
+------------------------+
| Minecraft Event Systems|
+------------------------+
      │       │       │
      ▼       ▼       ▼
 Spigot    Forge   Fabric
Dispatcher EventBus Callback API
```

## Ecosystem
For original diagram: [LucidChart Link](https://lucid.app/lucidchart/c92eac3b-899d-42aa-8241-5f5cc76dee75/edit?viewport_loc=8%2C-98%2C2958%2C1582%2C0_0&invitationId=inv_66e00f46-accd-48ba-8d1f-95ca54ad94a0)

![image](mc-ecosystem-overview.png)

## How it Works
Project itself can work independently of any Minecraft modding platform (framework-agnostic). 
Project intent was not designed to work with any specific Minecraft modding platform.
It is just a demonstration of how to implement a simple event system in Java, inspired by the event systems of popular Minecraft modding platforms.

## Download for Minecraft Modding Platforms
- [Spigot Buildtool](https://www.spigotmc.org/wiki/buildtools/)
- [Spigot Maven](https://www.spigotmc.org/wiki/spigot-maven/)
- [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/)
- [Forge-github](https://github.com/MinecraftForge)
- [Fabric](https://fabricmc.net/develop/)

## Minecraft Versions Min JDK Requirements (LTS - Long Term Support)
```
26.1 and later -> java 25
1.20.5 & 1.20.6 (1_20_R4) – 1.21.11 (R7) -> java 21
1.17 – 1.20.4 (1_20_R3) -> Java 17
1.13 – 1.16.5 -> java 11
1.8 - 1.12.2 -> java 8
```
## What is Mappings ?
The mappings are used to translate the obfuscated names of classes, methods, and fields in the Minecraft codebase into more readable names that developers can work with.
This allows developers to write plugins and mods for Minecraft without having to deal with the complexities of the obfuscated code.

On 29th October 2025, Mojang has announced that they will stop obfuscating Minecraft: Java Edition builds. This means that the game's code will now be shipped with readable names by default, largely eliminating the need for remapping tools and community-driven mapping projects like Yarn.
Given that there are no longer obfuscated names to translate from, the purpose of this site has effectively come to an end. It will stay online for archival purposes, but no further updates or maintenance will be performed.
[Mappings](https://mappings.dev/main.html)

## Minecraft Mappings
```
1.14.4 - Latest -> Mojang's official & Searge & Spigot & Intermediary & Yarn mappings
1.13.2 - 1.14.3 -> Searge & Spigot & Intermediary & Yarn mappings
1.8.8 - 1.13.2 -> Searge & Spigot mappings

// Yarn -> Fabric
// Searge -> Forge
```

## JADX JAR Paths for Spigot (OBC + NMS included) 
```
After building the desired Spigot version with BuildTools, you can find the jar file containing the
patched, Mojang-mapped (deobfuscated) NMS and OBC (CraftBukkit) sources in your local .m2 repository

- Spigot Server Bundler: ~/<optional-folder>/Buildtools/spigot-26.2.jar
// Includes Bukkit Source code

- Spigot Deobfuscated / Mojang-mapped Src: ~/.m2/repository/org/spigotmc/spigot/26.2-R0.1-SNAPSHOT/
// It only includes NMS+OBC source code, with readable Mojang nameplates. There are no third-party dependencies; it's intended for use in an IDE when developing plugins.

~ means /home/<user>
```

## MDK For Forge