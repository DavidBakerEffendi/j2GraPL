package za.ac.sun.grapl.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import za.ac.sun.grapl.hooks.IHook;

import java.util.Objects;

public abstract class AbstractController {

    final static Logger logger = LogManager.getLogger();

    private IHook hook;

    public void checkHook() {
        if (Objects.isNull(hook)) {
            String errMsg = "Hook not initialized! To set a hook, use " + getClass().getName() + "#setHook.";
            logger.error(errMsg);
            throw new NullPointerException(errMsg);
        }
    }

    public void hook(final IHook hook) {
        this.hook = hook;
    }

    public IHook hook() {
        return hook;
    }

}
