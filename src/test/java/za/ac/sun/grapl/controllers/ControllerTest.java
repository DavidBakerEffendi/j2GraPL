package za.ac.sun.grapl.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ControllerTest {

    @Test
    public void testNullHook() {
        ASTController.Companion.getInstance().hook(null);
        assertThrows(NullPointerException.class, () -> ASTController.Companion.getInstance().checkHook());
    }

}
