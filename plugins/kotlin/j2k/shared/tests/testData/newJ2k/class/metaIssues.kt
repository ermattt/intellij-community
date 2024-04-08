import android.os.Bundle
import androidx.fragment.app.CustomFragment
import androidx.lifecycle.ViewModelProvider
import com.foo.R
import com.something.PostBaseFragmentViewModel

class Test : CustomFragment() {
    var xs: List<String>? = null
    var strings: List<String>? = null

    // It would be even better if this were lateinit
    private var mFragmentViewModel: PostBaseFragmentViewModel? = null

    override fun onFragmentCreate(savedInstanceState: Bundle?) {
        super.onFragmentCreate(savedInstanceState)

        mFragmentViewModel = ViewModelProvider()
        mFragmentViewModel!!.toString()
    }

    fun typeParameterWeirdness() {
        xs = if (strings != null) ArrayList() else ArrayList(strings!!)
    }

    fun resourceReference() {
        // We often see imports of `android.R` getting added, even when a conflicting import, like `com.foo.R`, already exist. Then the
        // reference to `android.R.attr.string.label` below would get shortened to `R.attr.string.label`. Unfortunately, I can't reproduce it
        // in this test.
        val text1: String = R.string.error_state_accessibility_label
        val text2: String = android.R.attr.string.label
    }
}
