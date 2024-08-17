package implementslegend.mod.vaultfaster

import implementslegend.mod.vaultfaster.interfaces.StreamedTemplate
import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.template.StaticTemplate
import iskallia.vault.core.world.template.Template
import iskallia.vault.core.world.template.configured.ConfiguredTemplate
import net.minecraft.Util
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ServerLevelAccessor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collector
import java.util.stream.Collectors

val GENERATOR_THREAD_POOL = Executors.newCachedThreadPool{
    Thread(it).apply {
        name="Vault-Generator-$name"
    }
}

class SectionedTemplate(val base:ConfiguredTemplate) {

    val started = AtomicBoolean()
    val sections = CompletableFuture<HashMap<SectionPos,StaticTemplate>>()

    fun tryGenerate(){
        if(!started.plain && started.compareAndSet(false,true)){
            generate()
        }
    }

    private fun generate() {
        sections.completeAsync ({

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
            privateHashMap.forEach{
                    (_,template)->
                (template.entities as ArrayList).trimToSize()
            }
            privateHashMap
        }, GENERATOR_THREAD_POOL)

    }

    fun place(world: ServerLevelAccessor, pos: ChunkPos) {
        tryGenerate()
        (world.minSection..world.maxSection).map { it1->
            sections.thenAcceptAsync({
                val sectionPos = SectionPos.of(pos, it1)
                it[sectionPos]?.place(world, base.settings)//?: println("no section at $sectionPos")
            }, GENERATOR_THREAD_POOL)
        }.forEach {
            it.get()
        }
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