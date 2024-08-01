package implementslegend.mod.vaultfaster

import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.template.StaticTemplate
import iskallia.vault.core.world.template.Template
import iskallia.vault.core.world.template.configured.ConfiguredTemplate
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ServerLevelAccessor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors


class SectionedTemplate(val base:ConfiguredTemplate) {

    val started = AtomicBoolean()
    val sections = CompletableFuture<HashMap<SectionPos,StaticTemplate>>()

    fun tryGenerate(){
        if(!started.plain && started.compareAndSet(false,true)){
            generate()
        }
    }

    private fun generate() {
        sections.completeAsync {

            val privateHashMap:LinkedHashMap<SectionPos,StaticTemplate> =
                    LinkedHashMap((base.parent as StreamedTemplate).getTileStream(Template.ALL_TILES, base.settings)
                        .collect(
                            TilesToTemplatesCollector
                        ).mapValues { (k, v) ->
                            StaticTemplate((v as ArrayList).apply { trimToSize() }, ArrayList(128))
                        })
                base.parent.getEntities(Template.ALL_ENTITIES).forEach {
                    val secpos = SectionPos.of(it.blockPos)
                    (privateHashMap.computeIfAbsent(secpos) {
                        StaticTemplate(
                            ArrayList(),
                            ArrayList(16)
                        )
                    }.entities as ArrayList) += it
                }
            privateHashMap
        }

    }

    fun place(world: ServerLevelAccessor, pos: ChunkPos) {
        tryGenerate()
        sections.thenAccept {
            (world.minSection..world.maxSection).toList().parallelStream().map { it1 ->
                val sectionPos = SectionPos.of(pos, it1)
                it[sectionPos]?.place(world, base.settings)
            }.collect(Collectors.toList())
        }.get()
    }
}


val TilesToTemplatesCollector = Collector.of(
    {
        LinkedHashMap<SectionPos,MutableList<PartialTile>>()
    },
    {
            a,b:PartialTile->
        val sp = SectionPos.of(b.pos)
        a.computeIfAbsent(sp){ ArrayList(4096) }.add(b)
    },
    {
        a,b->
        b.forEach { (sectionPos, partialTiles) ->
            a.computeIfAbsent(sectionPos){ ArrayList(4096) } += partialTiles
        }
        a
    }


)