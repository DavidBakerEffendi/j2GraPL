package za.ac.sun.grapl.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.ModifierTypes;

import java.util.*;
import java.util.stream.IntStream;

public class ASMParserUtil implements Opcodes {

    final static Logger logger = LogManager.getLogger();

    public static final Map<Character, String> PRIMITIVES;
    public static final Set<String> OPERANDS;

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
    public static String obtainMethodReturnType(String signature) {
        return signature.substring(signature.lastIndexOf(')') + 1).replaceAll(";", "");
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
        final char evalChar = paramType.charAt(0);
        if ((isArray(evalChar) || isObject(evalChar))) {
            return isMethodReturn ? EvaluationStrategies.BY_SHARING : EvaluationStrategies.BY_REFERENCE;
        } else return EvaluationStrategies.BY_VALUE;
    }

    /**
     * Given the ASM5 access parameter and method name, determines the modifier types.
     * <p>
     * In Java, all non-static methods are by default "virtual functions." Only methods marked with the keyword final,
     * which cannot be overridden, along with private methods, which are not inherited, are non-virtual.
     *
     * @param access ASM5 access parameter obtained from visitClass and visitMethod.
     * @param name   name of the method obtained from visitClass and visitMethod.
     * @return an EnumSet of the applicable modifier types.
     */
    public static EnumSet<ModifierTypes> determineModifiers(int access, String name) {
        EnumSet<ModifierTypes> modifiers = EnumSet.of(ModifierTypes.VIRTUAL);
        if ("<init>".equals(name)) modifiers.add(ModifierTypes.CONSTRUCTOR);
        for (int remaining = access, bit; remaining != 0; remaining -= bit) {
            bit = Integer.lowestOneBit(remaining);
            switch (bit) {
                case Opcodes.ACC_STATIC:
                    modifiers.add(ModifierTypes.STATIC);
                    modifiers.remove(ModifierTypes.VIRTUAL);
                    break;
                case Opcodes.ACC_PUBLIC:
                    modifiers.add(ModifierTypes.PUBLIC);
                    break;
                case Opcodes.ACC_PRIVATE:
                    modifiers.add(ModifierTypes.PRIVATE);
                    modifiers.remove(ModifierTypes.VIRTUAL);
                    break;
                case Opcodes.ACC_PROTECTED:
                    modifiers.add(ModifierTypes.PROTECTED);
                    break;
                case Opcodes.ACC_NATIVE:
                    modifiers.add(ModifierTypes.NATIVE);
                    break;
                case Opcodes.ACC_ABSTRACT:
                    modifiers.add(ModifierTypes.ABSTRACT);
                    break;
                case Opcodes.ACC_FINAL:
                    modifiers.remove(ModifierTypes.VIRTUAL);
                    break;
            }
        }
        return modifiers;
    }

    static {
        Map<Character, String> primitives = new HashMap<>();
        primitives.put('Z', "BOOLEAN");
        primitives.put('C', "CHARACTER");
        primitives.put('B', "BYTE");
        primitives.put('S', "SHORT");
        primitives.put('I', "INTEGER");
        primitives.put('F', "FLOAT");
        primitives.put('J', "LONG");
        primitives.put('D', "DOUBLE");
        primitives.put('V', "VOID");
        PRIMITIVES = Collections.unmodifiableMap(primitives);
        HashSet<String> operands = new HashSet<>();
        operands.add("ADD");
        operands.add("SUB");
        operands.add("MUL");
        operands.add("DIV");
        operands.add("REM");
        OPERANDS = Collections.unmodifiableSet(operands);
    }

    public static String getShortName(String longName) {
        final StringBuilder sb = new StringBuilder();
        final char[] sigArr = longName.toCharArray();
        IntStream.range(0, sigArr.length).mapToObj(i -> sigArr[i]).forEach(c -> {
            if (isPrimitive(c) && sb.indexOf("L") == -1) {
                sb.append(c);
            } else if (isArray(c) || isObject(c)) sb.append(c);
        });

        if (longName.contains("L")) {
            sb.delete(sb.indexOf("L"), sb.length());
            if (longName.contains("/")) {
                sb.append(longName.substring(longName.lastIndexOf("/") + 1));
            }
        } else {
            int oldLength = sb.length();
            sb.append(convertAllPrimitivesToName(sb.toString()));
            sb.delete(0, oldLength);
        }

        return sb.toString();
    }

    public static boolean isPrimitive(char c) {
        return PRIMITIVES.containsKey(c);
    }

    public static boolean isObject(char c) {
        return c == 'L';
    }

    public static boolean isArray(char c) {
        return c == '[';
    }

    private static String convertAllPrimitivesToName(String signature) {
        StringBuilder sb = new StringBuilder();
        char[] sigArr = signature.toCharArray();
        IntStream.range(0, sigArr.length).mapToObj(i -> sigArr[i]).forEach(c -> {
            if (isPrimitive(c)) {
                sb.append(PRIMITIVES.get(c));
            } else {
                sb.append(c);
            }
        });
        return sb.toString();
    }

    /**
     * From the ASM docs: xADD, xSUB, xMUL, xDIV and xREM correspond to the +,
     * -, *, / and % operations, where x is either I, L, F or D.
     *
     * @param line the possible operand instruction.
     * @return true if the line is an operand instruction, false if otherwise.
     */
    public static boolean isOperand(String line) {
        if (line.length() != 4) return false;
        char type = line.charAt(0);
        if (type != 'I' && type != 'L' && type != 'F' && type != 'D') return false;
        return OPERANDS.contains(line.substring(1));
    }

    /**
     * From the ASM docs: The ILOAD, LLOAD, FLOAD, DLOAD, and ALOAD instructions read a local variable and push its
     * value on the operand stack. They take as argument the index i of the local variable that must be read.
     *
     * @param line the possible LOAD instruction.
     * @return true if the line is a LOAD instruction, false if otherwise.
     */
    public static boolean isLoad(String line) {
        if (line.length() != 5) return false;
        char type = line.charAt(0);
        if (type != 'I' && type != 'L' && type != 'F' && type != 'D' && type != 'A') return false;
        return "LOAD".contains(line.substring(1));
    }

    /**
     * From the ASM docs: ISTORE, LSTORE, FSTORE, DSTORE and ASTORE instructions pop a value from the operand stack
     * and store it in a local variable designated by its index i.
     *
     * @param line the possible STORE instruction.
     * @return true if the line is a STORE instruction, false if otherwise.
     */
    public static boolean isStore(String line) {
        if (line.length() != 6) return false;
        char type = line.charAt(0);
        if (type != 'I' && type != 'L' && type != 'F' && type != 'D' && type != 'A') return false;
        return "STORE".contains(line.substring(1));
    }
}
