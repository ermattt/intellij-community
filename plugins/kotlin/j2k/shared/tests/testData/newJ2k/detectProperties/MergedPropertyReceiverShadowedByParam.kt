internal interface Container {
    val item: Item
}

internal class Item {
    fun indexOf(o: Any?): Int {
        return 0
    }
}

internal class ContainerImpl(override val item: Item) : Container {
    fun locate(item: Any?): Int {
        return this.item.indexOf(item)
    }
}
