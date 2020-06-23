package za.ac.sun.grapl.domain.meta

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClassInfoTest {

    lateinit var testModel: ClassInfo

    @BeforeEach
    fun setUp() {
        testModel = ClassInfo("Test", "za.ac.sun.grapl", 1)
    }

    @Test
    fun equalsTest() {
        val testModel1 = ClassInfo("Test", "za.ac.sun.grapl", 1)
        assertTrue(testModel == testModel1)
        assertTrue(testModel.hashCode() == testModel1.hashCode())
        val testModel2 = ClassInfo("Test1", "za.ac.sun.grapl", 1)
        assertFalse(testModel == testModel2)
        assertFalse(testModel.hashCode() == testModel2.hashCode())
        val testModel3 = ClassInfo("Test", "za.ac.grapl", 1)
        assertFalse(testModel == testModel3)
        assertFalse(testModel.hashCode() == testModel3.hashCode())
        assertFalse(testModel.equals("Test"))
    }
}