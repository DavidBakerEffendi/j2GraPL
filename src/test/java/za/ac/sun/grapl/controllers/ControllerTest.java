package za.ac.sun.grapl.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ControllerTest {

    @Test
    public void testNullHook() {
        assertThrows(NullPointerException.class, () -> ASTController.getInstance().checkHook());
    }

}
