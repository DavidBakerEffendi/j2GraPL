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
        String className;
        String namespace;
        if (name.lastIndexOf('/') != -1) {
            className = name.substring(name.lastIndexOf('/') + 1);
            namespace = name.substring(0, name.lastIndexOf('/'));
        } else {
            className = name;
            namespace = "";
        }
        this.classPath = name.replaceAll("/", ".");
        namespace = namespace.replaceAll("/", ".");

        // Build NAMESPACE_BLOCK if packages are present
        NamespaceBlockVertex nbv = null;
        if (!namespace.isEmpty()) {
            // Populate namespace block chain
            String[] namespaceList = namespace.split("\\.");
            if (namespaceList.length > 0) nbv = this.populateNamespaceChain(namespaceList);
        }

        fv = new FileVertex(className, order++);
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

    /**
     * Creates a change of namespace block vertices and returns the final one in the chain.
     *
     * @param namespaceList a list of package names
     * @return the final namespace block vertex in the chain (the one associated with the file)
     */
    private NamespaceBlockVertex populateNamespaceChain(String[] namespaceList) {
        NamespaceBlockVertex prevNamespaceBlock = new NamespaceBlockVertex(namespaceList[0], namespaceList[0], order++);
        if (namespaceList.length == 1) return prevNamespaceBlock;

        NamespaceBlockVertex currNamespaceBlock = null;
        StringBuilder namespaceBuilder = new StringBuilder(namespaceList[0]);
        for (int i = 1; i < namespaceList.length; i++) {
            namespaceBuilder.append(".".concat(namespaceList[i]));
            currNamespaceBlock = new NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++);
            this.hook.joinNamespaceBlocks(prevNamespaceBlock, currNamespaceBlock);
            prevNamespaceBlock = currNamespaceBlock;
        }
        return currNamespaceBlock;
    }

    public ASTClassVisitor order(int order) {
        this.order = order;
        return this;
    }

}
