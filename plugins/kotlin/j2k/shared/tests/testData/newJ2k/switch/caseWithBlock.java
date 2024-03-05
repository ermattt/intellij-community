//method
void foo() {
  switch(a) {
    /* case 1 */ case 1: {
      int x = 1;
      System.out.println(x);
      break;
    }
    // case 2
    case 2: {
      int x = 2;
      System.out.println(x);
    }
    break;

    case 3: { // case 3 top
        System.out.println(3);
    } // case 3 bottom
  }
}