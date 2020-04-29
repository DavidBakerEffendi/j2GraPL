/*
 * Copyright 2020 David Baker Effendi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.ac.sun.grapl.util;

import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.enums.*;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class ASMParserUtil implements Opcodes {

    public static final Map<Character, String> PRIMITIVES;
    public static final Set<String> OPERANDS;
    public static final Set<String> NULLARY_JUMPS;
    public static final Set<String> UNARY_JUMPS;
    public static final Set<String> BINARY_JUMPS;

    /**
     * Given a method signature, returns a list of all the parameters separated into a list.
     *
     * @param signature the raw method signature from ASM5
     * @return a list of the parameters
     */
    public static List<String> obtainParameters(final String signature) {
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
     * @return a list of the parameters.
     */
    public static String obtainMethodReturnType(final String signature) {
        return signature.substring(signature.lastIndexOf(')') + 1).replaceAll(";", "");
    }

    /**
     * Given a parameter signature and context of the parameter, determines the evaluation strategy used.
     * TODO: Confirm if these assumptions are true
     *
     * @param paramType      the parameter signature from ASM5
     * @param isMethodReturn true if the parameter type is from a method
     * @return the type of evaluation strategy used
     */
    public static EvaluationStrategies determineEvaluationStrategy(final String paramType, final boolean isMethodReturn) {
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
    public static EnumSet<ModifierTypes> determineModifiers(final int access, final String name) {
        final EnumSet<ModifierTypes> modifiers = EnumSet.of(ModifierTypes.VIRTUAL);
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

    public static EnumSet<ModifierTypes> determineModifiers(final int access) {
        return determineModifiers(access, null);
    }

    public static String getReadableType(final char rawType) {
        return getReadableType(String.valueOf(rawType));
    }

    /**
     * Returns the "readable" name of a given raw name from ASM. For example I -> Integer and
     * java/util/String -> String.
     *
     * @param rawType the unprocessed type string.
     * @return a more "readable" variant of the type.
     */
    public static String getReadableType(final String rawType) {
        final StringBuilder sb = new StringBuilder();
        final char[] sigArr = rawType.toCharArray();
        IntStream.range(0, sigArr.length).mapToObj(i -> sigArr[i]).forEach(c -> {
            if (isPrimitive(c) && sb.indexOf("L") == -1) {
                sb.append(c);
            } else if (isArray(c) || isObject(c)) sb.append(c);
        });

        if (rawType.contains("L")) {
            sb.delete(sb.indexOf("L"), sb.length());
            if (rawType.contains("/")) {
                sb.append(rawType.substring(rawType.lastIndexOf("/") + 1));
            }
        } else {
            int oldLength = sb.length();
            sb.append(convertAllPrimitivesToName(sb.toString()));
            sb.delete(0, oldLength);
        }

        return sb.toString();
    }

    /**
     * Given a stack operation, returns the type.
     *
     * @param operation an xSTORE or xLOAD operation.
     * @return the type of the STORE or LOAD operation. If the operation is invalid, will return "UNKNOWN".
     */
    public static String getStackOperationType(final String operation) {
        if (!operation.contains("STORE") && !operation.contains("LOAD")) return "UNKNOWN";
        if (operation.length() != 6 && operation.length() != 5) return "UNKNOWN";
        return stackType(operation.charAt(0));
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
        operands.add("OR");
        operands.add("XOR");
        operands.add("AND");
        operands.add("SHR");
        operands.add("SHL");
        operands.add("USHR");
        OPERANDS = Collections.unmodifiableSet(operands);
        HashSet<String> nullaryJumps = new HashSet<>();
        HashSet<String> unaryJumps = new HashSet<>();
        HashSet<String> binaryJumps = new HashSet<>();
        // Nullary jumps jump without removing items off the stack
        nullaryJumps.add("GOTO");
        nullaryJumps.add("TABLESWITCH");
        nullaryJumps.add("LOOKUPSWITCH");
        // Unary jumps pop one parameter off the stack
        unaryJumps.add("IFEQ");
        unaryJumps.add("IFNE");
        unaryJumps.add("IFLT");
        unaryJumps.add("IFGE");
        unaryJumps.add("IFGT");
        unaryJumps.add("IFLE");
        unaryJumps.add("IFNULL");
        unaryJumps.add("IFNONNULL");
        // Binary jumps pop two parameters off the stack
        binaryJumps.add("IF_ICMPEQ");
        binaryJumps.add("IF_ICMPNE");
        binaryJumps.add("IF_ICMPLT");
        binaryJumps.add("IF_ICMPGE");
        binaryJumps.add("IF_ICMPGT");
        binaryJumps.add("IF_ICMPLE");
        binaryJumps.add("IF_ACMPEQ");
        binaryJumps.add("IF_ACMPNE");
        NULLARY_JUMPS = Collections.unmodifiableSet(nullaryJumps);
        UNARY_JUMPS = Collections.unmodifiableSet(unaryJumps);
        BINARY_JUMPS = Collections.unmodifiableSet(binaryJumps);
    }

    private static String stackType(final char type) {
        if (type == 'A') return "OBJECT";
        if (type == 'L') return "LONG";
        if (type == 'J') return "UNKNOWN";
        if (type == '[') return "UNKNOWN";
        return PRIMITIVES.getOrDefault(type, "UNKNOWN");
    }

    /**
     * Checks if the given character is associated with a primitive or not according to Section 2.1.3 of the ASM docs.
     *
     * @param c the character e.g. I, D, F, etc.
     * @return true if the character is associated with a primitive, false if otherwise.
     */
    public static boolean isPrimitive(final char c) {
        return PRIMITIVES.containsKey(c);
    }

    /**
     * Checks if the given character is associated an object or not according to Section 2.1.3 of the ASM docs.
     *
     * @param c the character e.g. L
     * @return true if the character is associated with an object, false if otherwise.
     */
    public static boolean isObject(final char c) {
        return c == 'L';
    }

    /**
     * Checks if the given character is associated an array or not according to Section 2.1.3 of the ASM docs.
     *
     * @param c the character e.g. [
     * @return true if the character is associated with an array, false if otherwise.
     */
    public static boolean isArray(final char c) {
        return c == '[';
    }

    /**
     * Converts all primitive characters in a signature to their descriptive names e.g. I -> INTEGER.
     *
     * @param signature the type signature to parse and insert descriptive names into.
     * @return the type signature with all primitive codes converted to their descriptive names.
     */
    private static String convertAllPrimitivesToName(final String signature) {
        final StringBuilder sb = new StringBuilder();
        final char[] sigArr = signature.toCharArray();
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
     * Given an arithmetic operator, returns the type.
     *
     * @param operation an arithmetic operator.
     * @return the type of the operator. If the operator is invalid, will return "UNKNOWN".
     */
    public static String getOperatorType(final String operation) {
        if (operation == null) return "UNKNOWN";
        if (!OPERANDS.contains(operation.substring(1))) return "UNKNOWN";
        return stackType(operation.charAt(0));
    }

    /**
     * From the ASM docs: The ILOAD, LLOAD, FLOAD, DLOAD, and ALOAD instructions read a local variable and push its
     * value on the operand stack. They take as argument the index i of the local variable that must be read.
     *
     * @param line the possible LOAD instruction.
     * @return true if the line is a LOAD instruction, false if otherwise.
     */
    public static boolean isLoad(final String line) {
        if (line == null) return false;
        if (line.length() != 5) return false;
        final char type = line.charAt(0);
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
    public static boolean isStore(final String line) {
        if (line == null) return false;
        if (line.length() != 6) return false;
        final char type = line.charAt(0);
        if (type != 'I' && type != 'L' && type != 'F' && type != 'D' && type != 'A') return false;
        return "STORE".contains(line.substring(1));
    }

    /**
     * From the ASM docs: xADD, xSUB, xMUL, xDIV and xREM correspond to the +,
     * -, *, / and % operations, where x is either I, L, F or D.
     * <p>
     * The logic operators only over I and L. These are SHL, SHR, USHR, AND, OR, and XOR.
     *
     * @param line the possible operand instruction.
     * @return true if the line is an operand instruction, false if otherwise.
     */
    public static boolean isOperator(final String line) {
        if (line == null) return false;
        if (line.length() > 5 || line.length() < 3) return false;
        final char type = line.charAt(0);
        if (type != 'I' && type != 'L' && type != 'F' && type != 'D') return false;
        if (line.contains("SHL") || line.contains("SHR") || line.contains("USHR") ||
                line.contains("AND") || line.contains("OR") || line.contains("XOR")) {
            if (type != 'I' && type != 'L') return false;
        }
        return OPERANDS.contains(line.substring(1));
    }

    /**
     * Determines if the given line is a constant of the pattern xCONST_n found in the
     * {@link org.objectweb.asm.MethodVisitor#visitInsn} hook.
     *
     * @param line the string to evaluate.
     * @return true if line represents a constant, false if otherwise.
     */
    public static boolean isConstant(final String line) {
        if (line == null) return false;
        if (line.length() < 6) return false;
        if ("ACONST_NULL".equals(line)) return true;
        final char type = line.charAt(0);
        return line.contains("CONST_") && (type == 'I' || type == 'F' || type == 'D' || type == 'L') && line.length() > 7;
    }

    /**
     * Parses an operator to determine which Operator enum is associated with it.
     *
     * @param line an operator String e.g. IADD.
     * @return the  {@link za.ac.sun.grapl.domain.enums.Operators } enum associated with the given operator string.
     */
    public static Operators parseOperator(final String line) {
        return Operators.valueOf(line);
    }

    /**
     * Determines if the given string is a jump statement.
     *
     * @param line the opcode.
     * @return true if the given string is a jump statement, false if otherwise.
     */
    public static boolean isJumpStatement(final String line) {
        return (NULLARY_JUMPS.contains(line) || UNARY_JUMPS.contains(line) || BINARY_JUMPS.contains(line));
    }

    /**
     * Parses the jump statement equality and returns the opposite.
     *
     * @param jumpStatement the string of a jump statement e.g. IF_ICMPGE.
     * @return the {@link Equality} of the opposite jump statement, UNKNOWN if it could not be determined.
     */
    public static Equality parseAndFlipEquality(final String jumpStatement) {
        final Equality original = parseEquality(jumpStatement);
        switch (original) {
            case EQ:
                return Equality.NE;
            case NE:
                return Equality.EQ;
            case LT:
                return Equality.GE;
            case GE:
                return Equality.LT;
            case GT:
                return Equality.LE;
            case LE:
                return Equality.GT;
            default:
                return Equality.UNKNOWN;
        }
    }

    /**
     * Parses the equality of the given jump statement.
     *
     * @param jumpStatement the string of a jump statement e.g. IF_ICMPGE.
     * @return the {@link Equality} of the jump statement, UNKNOWN if it could not be determined.
     */
    public static Equality parseEquality(final String jumpStatement) {
        if (UNARY_JUMPS.contains(jumpStatement)) {
            final String eq = jumpStatement.substring(2);
            if ("NULL".equals(eq)) return Equality.EQ;
            else if ("NONNULL".equals(eq)) return Equality.NE;
            return Equality.valueOf(eq);
        } else if (BINARY_JUMPS.contains(jumpStatement)) {
            final String eq = jumpStatement.substring(7);
            return Equality.valueOf(eq);
        }
        return Equality.UNKNOWN;
    }

    /**
     * Determines the type of the given binary jump.
     *
     * @param line the binary jump.
     * @return INTEGER or OBJECT of the binary jump, UNKNOWN if the input is invalid.
     */
    public static String getBinaryJumpType(final String line) {
        if (line == null || !BINARY_JUMPS.contains(line)) return "UNKNOWN";
        if (line.charAt(3) == 'I') return "INTEGER";
        else if (line.charAt(3) == 'A') return "OBJECT";
        else return "UNKNOWN";
    }

    /**
     * Parses the jump operation and returns its {@link JumpAssociations} used in control flow construction.
     *
     * @param jumpOp the jump operation e.g. IF_ICMPLE, GOTO, etc.
     * @return the jump association of the jump operation, null if there is no association.
     */
    public static JumpAssociations parseJumpAssociation(final String jumpOp) {
        if (ASMParserUtil.BINARY_JUMPS.contains(jumpOp)) {
            return JumpAssociations.IF_CMP;
        } else if (ASMParserUtil.NULLARY_JUMPS.contains(jumpOp)) {
            return JumpAssociations.JUMP;
        }
        return null;
    }

    public static <K, V> Stream<K> keys(Map<K, V> map, V value) {
        return map
                .entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey);
    }

}
