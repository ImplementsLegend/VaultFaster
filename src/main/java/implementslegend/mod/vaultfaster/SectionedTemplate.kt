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
        if(!started.plain && !started.getAndSet(true)){
            generate()
        }
    }

    private fun generate() {
        val privateHashMap = LinkedHashMap((base.parent as StreamedTemplate).getTileStream(Template.ALL_TILES,base.settings).parallel().collect(
            TilesToTemplatesCollector).mapValues {
            (k,v)->StaticTemplate((v as ArrayList).apply { trimToSize() },ArrayList(128))
        })
        base.parent.getEntities(Template.ALL_ENTITIES).forEach {
            val secpos = SectionPos.of(it.blockPos)
            (privateHashMap.computeIfAbsent(secpos){StaticTemplate(ArrayList(),ArrayList(16))}.entities as ArrayList)+=it
        }

        sections.complete(privateHashMap)
    }

    fun place(world: ServerLevelAccessor, pos: ChunkPos) {
        tryGenerate()

        (world.minSection..world.maxSection).toList().parallelStream().forEach {
            val sectionPos = SectionPos.of(pos,it)
            sections.thenAcceptAsync({
                it[sectionPos]?.place(world,base.settings)
            }, VAULT_GENERATION_EXECUTOR)
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
        a.apply {
            a.forEach { sectionPos, partialTiles ->
                this.computeIfAbsent(sectionPos){ ArrayList(4096) } += partialTiles
            }
        }
    }


)