# Guidebook Tome

NeoForge add-on for Minecraft 1.21.1. The first time you join a world or server, you get one Akashic Tome filled with the mod guidebooks that are installed.

This mod does not add its own tome. It uses the Akashic Tome you already have, or creates one with Akashic Tome's item if you do not have one yet.

## Install

1. Install NeoForge for Minecraft 1.21.1 (NeoForge 21.1.1 or newer).
2. Install Akashic Tome for NeoForge 1.21.1. Guidebook Tome will not load without it.
3. Put `guidebook-tome.jar` in the instance `mods` folder.
4. Patchouli and Modonomicon are optional. When they are installed, their loaded books are placed in the tome. If either mod is missing, it is skipped.

Other guidebook items are picked up when they implement Akashic Tome's modded-book marker, or when they are in one of these item tags:

- `guidebooktome:guidebooks`
- `c:guidebooks`
- `c:books/guidebooks`

The list is built from the mods and datapacks that are loaded. It is not a fixed book list.

You only receive a tome once. Joining again does not give a second one. If you already have an Akashic Tome on that first join, that tome is filled instead.

## Config

`config/guidebooktome-common.toml`

- `enable` — give the filled tome on first join. Default `true`. Set this to `false` to turn the gift off.
