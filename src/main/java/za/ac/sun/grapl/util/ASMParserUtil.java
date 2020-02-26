package za.ac.sun.grapl.util;

import za.ac.sun.grapl.domain.enums.EvaluationStrategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public class ASMParserUtil {

    private static final HashSet<Character> PRIMITIVES = new HashSet<>(Arrays.asList('Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D'));

    /**
     * Given a method signature, returns a list of all the parameters separated into a list.
     *
     * @param signature the raw method signature from ASM5
     * @return a list of the parameters
     */
    public static List<String> obtainParameters(String signature) {
        final List<String> parameters = new ArrayList<>();
        final char[] sigArr = signature.substring(1, signature.indexOf(')')).toCharArray();
        final StringBuilder sb = new StringBuilder();
        IntStream.range(0, sigArr.length).mapToObj(i -> sigArr[i]).forEach(c -> {
            if (c == ';') {
                parameters.add(sb.toString());
                sb.delete(0, sb.length());
            } else if (isPrimitive(c) && sb.indexOf("L") == -1) {
                parameters.add(sb.append(c).toString());
                sb.delete(0, sb.length());
            } else if (isObject(c)) {
                sb.append(c);
            } else if (isArray(c)) sb.append(c);
            else sb.append(c);
        });
        return parameters;
    }

    /**
     * Given a method signature, returns the return type.
     *
     * @param signature the raw method signature from ASM5
     * @return a list of the parameters
     */
    public static String obtainReturnType(String signature) {
        return signature.substring(signature.lastIndexOf(')'));
    }

    /**
     * Given a parameter signature and context of the parameter, determines the evaluation strategy used.
     * TODO: Confirm if these assumptions are true
     *
     * @param paramType      the parameter signature from ASM5
     * @param isMethodReturn true if the parameter type is being returned from a method
     * @return the type of evaluation strategy used
     */
    public static EvaluationStrategies determineEvaluationStrategy(String paramType, boolean isMethodReturn) {
        char evalChar = paramType.charAt(0);
        if ((isArray(evalChar) || isObject(evalChar))) {
            return isMethodReturn ? EvaluationStrategies.BY_SHARING : EvaluationStrategies.BY_REFERENCE;
        } else return EvaluationStrategies.BY_VALUE;
    }

    public static boolean isPrimitive(char c) {
        return PRIMITIVES.contains(c);
    }

    public static boolean isObject(char c) {
        return c == 'L';
    }

    public static boolean isArray(char c) {
        return c == '[';
    }

}
