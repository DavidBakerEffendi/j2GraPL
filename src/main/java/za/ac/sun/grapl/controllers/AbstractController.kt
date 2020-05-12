package za.ac.sun.grapl.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import za.ac.sun.grapl.hooks.IHook

abstract class AbstractController {
    lateinit var hook: IHook

    companion object {
        val logger: Logger = LogManager.getLogger()
    }

}