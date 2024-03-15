Vault Faster
=====
Minecraft Vault Hunters optimization mod with one goal: make vault world generation faster. \
More than 4x performance improvement.


List of changes:
- [batchSetBlocks.kt] blocks are placed with optimised algorithm
- - skips heightmap updates
- - locks chunk only once per section instead of every block
- - skips iskallia.vault.core.event.CommonEvents.BLOCK_SET event
- [MixinBlockRegistryIndex.java, PartialBlockRegistryIndex.java, IndexedBlock.kt] replaced "string ids"/*ResourceLocation*s of blocks and *PartialTile*s with "numerical ids"/*registryIndex*
- [TileMapper.kt] added tile mapper; algorithm for minimizing number of applicable *iskallia.vault.core.world.processor.tile.TileProcessor*s by storing them in multimap indexed with numerical id of a block 
- cached some values
- [NoBiomeDecorations.java] removed biome decorations

Performance on my system (# of chunks generated in 50 seconds):
- unmodified, 5 cpu cores: 1330
- unmodified, 2 cpu cores:  430
- modified, 5 cpu cores:   5500
- modified, 2 cpu cores:   2000