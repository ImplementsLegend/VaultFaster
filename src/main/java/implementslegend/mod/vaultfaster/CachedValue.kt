package implementslegend.mod.vaultfaster

data class CachedValue<T>(var value:T)

inline fun <V> CachedValue<V>.cacheLoadOrUpdate(loadCondition: (V) -> Boolean, update: () -> V): V = value.takeIf(loadCondition)?:update().also { value=it }