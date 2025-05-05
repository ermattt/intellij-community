// !ADD_KOTLIN_API
import static kotlinApi.KotlinApiKt.extensionFunction;
import static kotlinApi.KotlinApiKt.getExtensionProperty;
import static kotlinApi.KotlinApiKt.setExtensionProperty;
import static kotlinApi.KotlinApiKt.GLOBAL_CONST;

class C {
    int foo() {
        extensionFunction(1)
        setExtensionProperty("a", GLOBAL_CONST);
        return getExtensionProperty("b");
    }
}
