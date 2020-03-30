/*
 * Copyright 2020 David Baker Effendi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.ac.sun.grapl.visitors.ast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.models.vertices.FileVertex;
import za.ac.sun.grapl.domain.models.vertices.NamespaceBlockVertex;
import za.ac.sun.grapl.hooks.IHook;

import java.util.Objects;

public class ASTClassVisitor extends ClassVisitor implements Opcodes {

    final static Logger logger = LogManager.getLogger();

    final IHook hook;
    private String classPath;
    private String className;
    private String namespace;
    private int order;
    private FileVertex fv;
    private ASTMethodVisitor astMv;

    public ASTClassVisitor(IHook hook, ClassVisitor cv) {
        super(ASM5, cv);
        this.hook = hook;
        this.order = 0;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (name.lastIndexOf('/') != -1) {
            this.className = name.substring(name.lastIndexOf('/') + 1);
            this.namespace = name.substring(0, name.lastIndexOf('/'));
        } else {
            this.className = name;
            this.namespace = "";
        }
        this.classPath = name.replaceAll("/", ".");
        this.namespace = namespace.replaceAll("/", ".");

        // Build NAMESPACE_BLOCK if packages are present
        NamespaceBlockVertex nbv = null;
        if (!this.namespace.isEmpty()) {
            // Populate namespace block chain
            String[] namespaceList = this.namespace.split("\\.");
            nbv = new NamespaceBlockVertex(
                    namespaceList[namespaceList.length - 1],
                    this.namespace, order++);
            if (namespaceList.length > 1) {
                this.populateNamespaceChain(namespaceList);
            } else {
                this.hook.createVertex(nbv);
            }
        }

        fv = new FileVertex(this.className, order++);
        // Create FILE
        this.hook.createVertex(fv);

        // Join FILE and NAMESPACE_BLOCK if namespace is present
        if (!Objects.isNull(nbv)) {
            this.hook.joinFileVertexTo(fv, nbv);
        }

        // TODO: Could create MEMBER vertex from here to declare member classes

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        order = astMv == null ? order : astMv.getOrder();
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        astMv = new ASTMethodVisitor(mv, hook, access, name, classPath, descriptor, fv);
        astMv.setOrder(order);
        return astMv;
    }

    private void populateNamespaceChain(String[] namespaceList) {
        NamespaceBlockVertex prevNamespaceBlock =
                new NamespaceBlockVertex(namespaceList[namespaceList.length - 1], this.namespace, order++);
        StringBuilder namespaceBuilder = new StringBuilder();
        for (int i = namespaceList.length - 2; i >= 0; i--) {
            namespaceBuilder.append(namespaceList[i]);
            NamespaceBlockVertex currNamespaceBlock =
                    new NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++);
            this.hook.joinNamespaceBlocks(currNamespaceBlock, prevNamespaceBlock);
            prevNamespaceBlock = currNamespaceBlock;
            namespaceBuilder.append(".");
        }
    }

    public ASTClassVisitor order(int order) {
        this.order = order;
        return this;
    }

}
