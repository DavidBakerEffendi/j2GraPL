package za.ac.sun.grapl.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.enums.Equality;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.ModifierTypes;
import za.ac.sun.grapl.domain.enums.Operators;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class ASMParserUtilTest {

    @Test
    public void testObtainingParameters() {
        assertEquals(Collections.singletonList("I"), ASMParserUtil.obtainParameters("(I)V"));
        assertEquals(Arrays.asList("I", "B"), ASMParserUtil.obtainParameters("(IB)V"));
        assertEquals(Collections.emptyList(), ASMParserUtil.obtainParameters("()Ljava/util/String;"));
        assertEquals(Collections.singletonList("Ljava/util/String"), ASMParserUtil.obtainParameters("(Ljava/util/String;)V"));
        assertEquals(Arrays.asList("Ljava/util/String", "J"), ASMParserUtil.obtainParameters("(Ljava/util/String;J)B"));
        assertEquals(Arrays.asList("[Ljava/util/String", "[B"), ASMParserUtil.obtainParameters("([Ljava/util/String;[B)I"));
    }

    @Test
    public void testObtainMethodReturnType() {
        assertEquals("V", ASMParserUtil.obtainMethodReturnType("()V"));
        assertEquals("Ljava/util/String", ASMParserUtil.obtainMethodReturnType("(IIB)Ljava/util/String;"));
        assertEquals("[[B", ASMParserUtil.obtainMethodReturnType("()[[B"));
        assertEquals("[Ljava/util/Double", ASMParserUtil.obtainMethodReturnType("(IIB)[Ljava/util/Double;"));
    }

    @Test
    public void testDetermineEvaluationStrategy() {
        assertEquals(ASMParserUtil.determineEvaluationStrategy("I", true), EvaluationStrategies.BY_VALUE);
        assertEquals(ASMParserUtil.determineEvaluationStrategy("B", false), EvaluationStrategies.BY_VALUE);
        assertEquals(ASMParserUtil.determineEvaluationStrategy("[I", true), EvaluationStrategies.BY_SHARING);
        assertEquals(ASMParserUtil.determineEvaluationStrategy("Ljava/util/String", true), EvaluationStrategies.BY_SHARING);
        assertEquals(ASMParserUtil.determineEvaluationStrategy("[I", false), EvaluationStrategies.BY_REFERENCE);
        assertEquals(ASMParserUtil.determineEvaluationStrategy("Ljava/util/String", false), EvaluationStrategies.BY_REFERENCE);
    }

    @Test
    public void testDetermineModifiers() {
        assertEquals(ASMParserUtil.determineModifiers(Opcodes.ACC_ABSTRACT, "<init>"),
                EnumSet.of(ModifierTypes.CONSTRUCTOR, ModifierTypes.ABSTRACT, ModifierTypes.VIRTUAL));
        assertEquals(ASMParserUtil.determineModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC, "test"),
                EnumSet.of(ModifierTypes.STATIC, ModifierTypes.PUBLIC));
        assertEquals(ASMParserUtil.determineModifiers(Opcodes.ACC_PROTECTED, "test"),
                EnumSet.of(ModifierTypes.VIRTUAL, ModifierTypes.PROTECTED));
        assertEquals(ASMParserUtil.determineModifiers(Opcodes.ACC_NATIVE + Opcodes.ACC_FINAL, "test"),
                EnumSet.of(ModifierTypes.NATIVE));
    }

    @Test
    public void testShortNameReturn() {
        assertEquals("INTEGER", ASMParserUtil.getReadableType("I"));
        assertEquals("BOOLEAN", ASMParserUtil.getReadableType("Z"));
        assertEquals("[INTEGER", ASMParserUtil.getReadableType("[I"));
        assertEquals("String", ASMParserUtil.getReadableType("Ljava/util/String"));
        assertEquals("[Double", ASMParserUtil.getReadableType("[Ljava/util/Double"));
        assertEquals("[[LONG", ASMParserUtil.getReadableType("[[J"));
    }

    @Test
    public void testIsOperator() {
        assertTrue(ASMParserUtil.isOperator("IADD"));
        assertFalse(ASMParserUtil.isOperator("IAD"));
        assertFalse(ASMParserUtil.isOperator("DSUBB"));
        assertFalse(ASMParserUtil.isOperator("JDIV"));
        assertTrue(ASMParserUtil.isOperator("FREM"));
        assertTrue(ASMParserUtil.isOperator("LMUL"));
    }

    @Test
    public void testIsStore() {
        assertTrue(ASMParserUtil.isStore("ISTORE"));
        assertFalse(ASMParserUtil.isStore("ILOAD"));
        assertFalse(ASMParserUtil.isStore("DSTOR"));
        assertFalse(ASMParserUtil.isStore("JSTORE"));
        assertTrue(ASMParserUtil.isStore("FSTORE"));
        assertTrue(ASMParserUtil.isStore("LSTORE"));
        assertTrue(ASMParserUtil.isStore("ASTORE"));
    }

    @Test
    public void testIsLoad() {
        assertTrue(ASMParserUtil.isLoad("ILOAD"));
        assertFalse(ASMParserUtil.isLoad("ISTORE"));
        assertFalse(ASMParserUtil.isLoad("DLOA"));
        assertFalse(ASMParserUtil.isLoad("JLOAD"));
        assertTrue(ASMParserUtil.isLoad("FLOAD"));
        assertTrue(ASMParserUtil.isLoad("LLOAD"));
        assertTrue(ASMParserUtil.isLoad("ALOAD"));
    }

    @Test
    public void testIsConstant() {
        assertTrue(ASMParserUtil.isConstant("ACONST_NULL"));
        assertTrue(ASMParserUtil.isConstant("ICONST_0"));
        assertTrue(ASMParserUtil.isConstant("FCONST_2"));
        assertTrue(ASMParserUtil.isConstant("DCONST_3"));
        assertFalse(ASMParserUtil.isConstant("JCONST_3"));
    }

    @Test
    public void testStackOperationType() {
        assertEquals("INTEGER", ASMParserUtil.getStackOperationType("ILOAD"));
        assertEquals("OBJECT", ASMParserUtil.getStackOperationType("ASTORE"));
        assertEquals("LONG", ASMParserUtil.getStackOperationType("LLOAD"));
        assertEquals("UNKNOWN", ASMParserUtil.getStackOperationType("[LOAD"));
        assertEquals("UNKNOWN", ASMParserUtil.getStackOperationType("JSTORE"));
        assertEquals("UNKNOWN", ASMParserUtil.getStackOperationType("IITEST"));
        assertEquals("UNKNOWN", ASMParserUtil.getStackOperationType("LSTOREL"));
    }

    @Test
    public void testOperatorType() {
        assertEquals("INTEGER", ASMParserUtil.getOperatorType("IADD"));
        assertEquals("OBJECT", ASMParserUtil.getOperatorType("ASUB"));
        assertEquals("LONG", ASMParserUtil.getOperatorType("LADD"));
        assertEquals("UNKNOWN", ASMParserUtil.getOperatorType("[DIV"));
        assertEquals("UNKNOWN", ASMParserUtil.getOperatorType("JMUL"));
        assertEquals("UNKNOWN", ASMParserUtil.getOperatorType("IITEST"));
        assertEquals("UNKNOWN", ASMParserUtil.getOperatorType("LDIVL"));
        assertEquals("LONG", ASMParserUtil.getOperatorType("LOR"));
        assertEquals("INTEGER", ASMParserUtil.getOperatorType("IAND"));
    }

    @Test
    public void testParseOperator() {
        assertEquals(Operators.IADD, ASMParserUtil.parseOperator("IADD"));
        assertEquals(Operators.LADD, ASMParserUtil.parseOperator("LADD"));
        assertEquals(Operators.DADD, ASMParserUtil.parseOperator("DADD"));
        assertEquals(Operators.FADD, ASMParserUtil.parseOperator("FADD"));
        assertEquals(Operators.FOR, ASMParserUtil.parseOperator("FOR"));
    }

    @Test
    public void testIsJumpStatement() {
        assertTrue(ASMParserUtil.isJumpStatement("IF_ICMPEQ"));
        assertTrue(ASMParserUtil.isJumpStatement("IFNE"));
        assertTrue(ASMParserUtil.isJumpStatement("IFNONNULL"));
        assertTrue(ASMParserUtil.isJumpStatement("LOOKUPSWITCH"));
        assertFalse(ASMParserUtil.isJumpStatement("IF"));
        assertFalse(ASMParserUtil.isJumpStatement(null));
    }

    @Test
    public void testParseEquality() {
        assertEquals(Equality.EQ, ASMParserUtil.parseEquality("IF_ICMPEQ"));
        assertEquals(Equality.EQ, ASMParserUtil.parseEquality("IFNULL"));
        assertEquals(Equality.NE, ASMParserUtil.parseEquality("IFNONNULL"));
        assertEquals(Equality.LE, ASMParserUtil.parseEquality("IF_ICMPLE"));
        assertEquals(Equality.UNKNOWN, ASMParserUtil.parseEquality("GOTO"));
    }

}
