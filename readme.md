Vault Faster
=====
Minecraft Vault Hunters optimization mod with one goal: make vault world generation faster. \
More than 40x performance improvement.


List of changes:
- [batchsetblocks/*] optimised algorithm for placing blocks
- - skips heightmap updates
- - subchunks are processed in parallel
- - locks chunk only once per section instead of every block
- - skips iskallia.vault.core.event.CommonEvents.BLOCK_SET event
- [MixinBlockRegistryIndex.java, PartialBlockRegistryIndex.java, IndexedBlock.kt] replaced "string ids"/*ResourceLocation*s of blocks and *PartialTile*s with "numerical ids"/*registryIndex*
- [TileMapper.kt] added tile mapper; algorithm for minimizing number of applicable *iskallia.vault.core.world.processor.tile.TileProcessor*s by storing them in multimap indexed with numerical id of a block 
- cached some values
- [NoBiomeDecorations.java] removed biome decorations and custom structures; those aren't supposed to be there and can cause concurrency crashes
- [*StreamedTemplate] replaces Template.getTiles with StreamedTemplate.getTileStream(), which doesn't require custom iterators and it's parallel
- [MixinCascade.java, MixinCascadeProperties.java] optimizes cascading modifiers
- [VaultBypassMailbox.java] enables world generator to process chunks in parallel
- multiple fixes

New Events
===

ObjectiveTemplateEvent: for placing POIs of objectives (lodestone, scavenger table, braziers, etc...); this was previously done with BlockSetEvent, which was just too slow.
- **please, do not fork this repo just to add support for custom objectives**; registering this event is all you should need

TemplateConfigurationEvent: for additional configuration of templates before they're placed; this vas previously done with TemplateGenerationEvent which caused concurrency crashes and other unxepected behavior when multi-threading was introduced

Performance
===
tunnel_span=0 (vault without tunnels, this is much more demanding), allocated 18GB of ram:
- unmodified: 5 chunks per second
- modified: 390 chunks per second
- modified, no HT: 440 chunks per second
- ram speed also had a significant impact