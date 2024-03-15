package implementslegend.mod.vaultfaster

interface IndexedBlock {
    val registryIndex:Int
    fun copyRegistryIndex(newIndex: Int)
}