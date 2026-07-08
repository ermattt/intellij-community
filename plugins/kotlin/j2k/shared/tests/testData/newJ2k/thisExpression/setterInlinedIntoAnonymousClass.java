package test;

public class Manager {
  private boolean mEnabled;

  public boolean getIsEnabled() {
    return mEnabled;
  }

  private void setIsEnabled(boolean enabled) {
    this.mEnabled = enabled;
  }

  interface Callback {
    void onSuccess(boolean b);

    void onFail();
  }

  void register(Callback c) {}

  void make() {
    register(
        new Callback() {
          @Override
          public void onSuccess(boolean b) {
            setIsEnabled(b);
          }

          @Override
          public void onFail() {
            setIsEnabled(false);
          }
        });
  }
}
