package za.ac.sun.grapl.domain.meta

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import za.ac.sun.grapl.controllers.MethodInfoController

class MethodInfoTest {
    lateinit var testModel: MethodInfoController

    @BeforeEach
    fun setUp() {
        testModel = MethodInfoController("Test", "za.ac.sun.grapl", 1)
    }

    @Test
    fun equalsTest() {
        val testModel1 = MethodInfoController("Test", "za.ac.sun.grapl", 1)
        assertTrue(testModel == testModel1)
        assertTrue(testModel.hashCode() == testModel1.hashCode())
        val testModel2 = MethodInfoController("Test1", "za.ac.sun.grapl", 1)
        assertFalse(testModel == testModel2)
        assertFalse(testModel.hashCode() == testModel2.hashCode())
        val testModel3 = MethodInfoController("Test", "za.ac.sun.grapl.test", 1)
        assertFalse(testModel == testModel3)
        assertFalse(testModel.hashCode() == testModel3.hashCode())
        val testModel4 = MethodInfoController("Test", "za.ac.sun.grapl", 2)
        assertFalse(testModel == testModel4)
        assertFalse(testModel.hashCode() == testModel4.hashCode())
        assertFalse(testModel.equals("Test"))
    }

    @Test
    fun toStringTest() {
        assertTrue("-1: [PUBLIC, VIRTUAL] Test za.ac.sun.grapl" == testModel.toString())
        testModel.lineNumber = 2
        assertTrue("2: [PUBLIC, VIRTUAL] Test za.ac.sun.grapl" == testModel.toString())
    }
}