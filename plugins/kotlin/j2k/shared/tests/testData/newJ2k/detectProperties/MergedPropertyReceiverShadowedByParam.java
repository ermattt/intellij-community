// IGNORE_K2
interface Container {
    Item getItem();
}

class Item {
    int indexOf(Object o) {
        return 0;
    }
}

class ContainerImpl implements Container {
    private final Item mItem;

    ContainerImpl(Item item) {
        mItem = item;
    }

    @Override
    public Item getItem() {
        return mItem;
    }

    public int locate(Object item) {
        return mItem.indexOf(item);
    }
}
