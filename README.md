# j2GraPL
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/DavidBakerEffendi/j2GraPL.svg?branch=develop)](https://travis-ci.org/DavidBakerEffendi/j2GraPL)
[![codecov](https://codecov.io/gh/DavidBakerEffendi/j2GraPL/branch/develop/graph/badge.svg)](https://codecov.io/gh/DavidBakerEffendi/j2GraPL)

Converts a Java program, JAR or class file into a code-property graph and inserts it into a graph database to be 
analysed with various program analysis algorithms. The driver used to communicate with each graph database is 
[GraPLHook4j](https://github.com/DavidBakerEffendi/GraPLHook4j).

## Features

j2GraPL is currently under development. It has the following capabilities:
* Project an intraprocedural AST of a JVM program using JVM bytecode:
    - Package/Class/Method hierarchy
    - Variable assignments
    - Arithmetic
    - If-else bodies
* Can project to all graph databases currently supported by [GraPLHook4j](https://github.com/DavidBakerEffendi/GraPLHook4j).
* Currently accepts source code or class files (or directories containing either).

## Building from Source

In order to use j2GraPL, one needs to also make use of GraPLHook4j to interface with a given graph database.
Right now, `lib` contains the latest stable version of GraPLHook4j. This will be the case until the GraPL project can be
hosted on a Maven repository or similar.

```shell script
git clone https://github.com/DavidBakerEffendi/j2GraPL.git
cd j2GraPL
mvn package
```
This will build `target/j2GraPL-X.X.X.jar` which can then be imported into your local project. E.g.
```mxml
<dependency>
  <groupId>za.ac.sun.grapl</groupId>
  <artifactId>j2GraPL</artifactId>
  <version>X.X.X</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/j2GraPL-X.X.X.jar</systemPath>
</dependency>
``` 
To add the GraPL driver one can do the following. This can either be built from source or extracted from `lib` under 
this repository.

## Quickstart

For a quick and simply in-memory graph projection of a Java program:
```java
import za.ac.sun.grapl.Cannon;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import java.io.File;
import java.io.IOException;

public class GraPLDemo {

    public static void main(String[] args) throws IOException {
        TinkerGraphHook hook = new TinkerGraphHook.TinkerGraphHookBuilder("/tmp/grapl/j2grapl_demo.xml")
                                            .createNewGraph(true)
                                            .build();
        // Attach the hook to the cannon
        Cannon cannon = new Cannon(hook);
        File f = new File("./Example.java"); // or new File("./Example.class")
        // Load the cannon with source files, class files, or a directory containing either
        cannon.load(f);
        // Fire the loaded files to be projected the graph database
        cannon.fire();
        // For the TinkerGraph hook, we can export this graph using the format and 
        // directory specified in the constructor
        hook.exportCurrentGraph();
    }

}
```
This exported file can then be visualized using tools such as [Cytoscape](https://cytoscape.org/).