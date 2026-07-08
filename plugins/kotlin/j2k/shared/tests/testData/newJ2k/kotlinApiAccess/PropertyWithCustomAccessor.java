// !ADD_KOTLIN_API
import kotlinApi.KotlinClassWithProperties;

class C {
    void foo(KotlinClassWithProperties obj) {
        obj.setSomeVar4(obj.getSomeVar4());
    }
}
