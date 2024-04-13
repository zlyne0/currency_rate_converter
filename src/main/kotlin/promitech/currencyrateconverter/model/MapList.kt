package promitech.currencyrateconverter.model

class MapList<K, E> {
    val map = HashMap<K, ArrayList<E>>()

    fun put(key: K, element: E) {
        var list = map.get(key)
        if (list == null) {
            list = ArrayList<E>()
            map.put(key, list)
        }
        list.add(element)
    }

    fun getList(key: K): List<E>? {
        return map.get(key)
    }

    fun size(): Int {
        return map.size
    }

    fun entities(): MutableSet<MutableMap.MutableEntry<K, ArrayList<E>>> {
        return map.entries
    }

}