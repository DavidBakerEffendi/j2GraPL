package za.ac.sun.grapl.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.enums.EvaluationStrategies;
import za.ac.sun.grapl.domain.enums.ModifierTypes;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("INTEGER", ASMParserUtil.getShortName("I"));
        assertEquals("BOOLEAN", ASMParserUtil.getShortName("Z"));
        assertEquals("[INTEGER", ASMParserUtil.getShortName("[I"));
        assertEquals("String", ASMParserUtil.getShortName("Ljava/util/String"));
        assertEquals("[Double", ASMParserUtil.getShortName("[Ljava/util/Double"));
        assertEquals("[[LONG", ASMParserUtil.getShortName("[[J"));
    }

}
