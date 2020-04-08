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
* Currently accepts source code, class files (or directories containing either), or a JAR file.

## Building from Source

In order to use j2GraPL, one needs to also make use of GraPLHook4j to interface with a given graph database.
Right now, `lib` contains the latest stable version of GraPLHook4j. This will be the case until the GraPL project can be
hosted on a Maven repository or similar.

```shell script
git clone https://github.com/DavidBakerEffendi/j2GraPL.git
cd j2GraPL
./gradlew jar # For main artifact only
./gradlew fatJar # For fat jar with dependencies
```
This will build `target/j2GraPL-X.X.X[-all].jar` and which can then be imported into your local 
project. One can choose the main artifact or fat jar but here is how one can import this into one's Maven or Gradle 
project respectively. E.g.
```mxml
<dependency>
  <groupId>za.ac.sun.grapl</groupId>
  <artifactId>j2GraPL</artifactId>
  <version>X.X.X</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/j2GraPL-X.X.X.jar</systemPath>
</dependency>
``` 
```groovy
repositories {
    // ...
    flatDir {
        dirs 'lib'
    }
}
dependencies {
    // ...
    implementation name: 'j2GraPL-X.X.X'
}
```

## Dependencies

### Packages

The following packages are used by j2GraPL:

* `org.ow2.asm:asm:7.3.1`
* `org.ow2.asm:asm-util:7.3.1`
* `org.apache.logging.log4j:log4j-core:2.8.2`
* `org.apache.logging.log4j:log4j-slf4j-impl:2.8.2`
* `za.ac.sun.grapl:GraPLHook4j` (under `lib`)

It is not recommended to use the fat jar in your project if using a build tool such as Ant, Maven, Gradle, etc. Rather
use the main artifact and add the dependencies manually (in your `pom.xml`, `build.gradle`, etc.). 

### Java Support

The following versions of Java are officially supported:
* OpenJDK 8
* OpenJDK 9
* OpenJDK 10
* OpenJDK 11

## Quickstart

We use this directory as the base for the following short tutorial - no build tools required. First, we need a Java program to analyze. Here is an
example of a file we can create:
```java
public class Example {

	public static void main(String[] args) {
		int a = 1;
		int b = 2;
		if (a > b) {
			a = b + 1;
		} else {
			b -= a + 1;
		}
	}

}
```

For a quick and simple in-memory graph projection of a Java program:
```java
import za.ac.sun.grapl.Cannon;
import za.ac.sun.grapl.hooks.TinkerGraphHook;
import java.io.File;
import java.io.IOException;

public class GraPLDemo {

    public static void main(String[] args) throws IOException {
        TinkerGraphHook hook = new TinkerGraphHook.TinkerGraphHookBuilder("./j2grapl_demo.xml")
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

To compile both of these, we can use the `build/libs/j2GraPL-X.X.X-all.jar` with 
`lib/GraPLHook4j-X.X.X-jar-with-dependencies.jar`. This can be combined as:
```bash
javac -cp ".:build/libs/j2GraPL-X.X.X-all.jar:lib/GraPLHook4j-X.X.X-jar-with-dependencies.jar:" *.java
java -cp ".:build/libs/j2GraPL-X.X.X-all.jar:lib/GraPLHook4j-X.X.X-jar-with-dependencies.jar:" GraPLDemo
```

This exported file can then be visualized using tools such as [Cytoscape](https://cytoscape.org/). Using Cytoscape and 
the tree layout, the graph should look something like this:

![Example.java Graph](https://github.com/DavidBakerEffendi/j2GraPL/blob/media/graphs/GraPLDemo.png?raw=true)

## Logging

All logging can be configured under `src/main/resources/log4j2.properties`. By default, all logs can be found under 
`/tmp/grapl`.
