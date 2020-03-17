package intraprocedural.conditional;

public class Conditional4 {

    public static void main(String args[]) {
        int a = 1;
        int b = 2;
        if (a == 3) {
            a -= b;
            b -= b;
        } else {
            a *= b;
        }
        b += a;
    }

}
