package intraprocedural.conditional;

public class Conditional5 {

    public static void main(String args[]) {
        int a = 1;
        int b = 2;
        // IF_ICMPNE L1
        if (a == 3) {
            a -= b;
            // IF_ICMPLE L2
            if (b > 2) {
                b -= b;
            }
            // GOTO L2
        } else {
            // L1
            a *= b;
        }
        // L2
        b += a;
    }

}
