import androidx.fragment.app.CustomFragment;
import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.Nullable;
import com.analytics.logger.events.MsgMediaPickerDidShow;
import com.foo.FooUtil;
import com.foo.FooUtil.create;
import com.foo.R;
import com.something.PostBaseFragmentViewModel;
import java.util.ArrayList;

public class Test extends CustomFragment {
  List<String> xs;
  @Nullable List<String> strings;

  // It would be even better if this were lateinit
  private PostBaseFragmentViewModel mFragmentViewModel;

  @Override
  public void onFragmentCreate(@Nullable Bundle savedInstanceState) {
      super.onFragmentCreate(savedInstanceState);

      mFragmentViewModel = new ViewModelProvider();
      mFragmentViewModel.toString();
  }

  public void typeParameterWeirdness() {
      xs = strings != null ? new ArrayList<String>() : new ArrayList<>(strings);
  }

  public void resourceReference() {
      // We often see imports of `android.R` getting added, even when a conflicting import, like `com.foo.R`, already exist. Then the
      // reference to `android.R.attr.string.label` below would get shortened to `R.attr.string.label`. Unfortunately, I can't reproduce it
      // in this test.
      String text1 = R.string.error_state_accessibility_label;
      String text2 = android.R.attr.string.label;
  }
}