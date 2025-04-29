class C {
    private String name = "";

    public synchronized void start(String initialName) {
        name = initialName;
    }

    protected void touchName() {
        if (name != "") {
            System.out.println("Name isn't empty");
        }
    }

    public synchronized String getName() {
        return name;
    }
}