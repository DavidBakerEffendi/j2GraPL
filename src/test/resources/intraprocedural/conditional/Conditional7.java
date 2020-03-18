package intraprocedural.conditional;

public class Conditional7 {

    public static void main(String args[]) {
        int a = 1;
        int b = 2;
        // IF_ICMPNE L1
        if (a == 3) {
            a -= b;
            // GOTO L2
        } else {
            // L1
            // IF_ICMPLE L3
            if (b > 2) {
                b -= b;
                // GOTO L4
            } else {
                // L3
                b /= b;
            }
            // L4
            a *= b;
        }
        // L2
        b += a;
    }

}
