package za.ac.sun.grapl.visitors.ast;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import za.ac.sun.grapl.domain.models.vertices.FileVertex;
import za.ac.sun.grapl.domain.models.vertices.NamespaceBlockVertex;
import za.ac.sun.grapl.hooks.IHook;

public class ASTClassVisitor extends ClassVisitor implements Opcodes {

    final static Logger logger = LogManager.getLogger();
    final IHook hook;
    private String classPath;
    private String className;
    private int order;

    public ASTClassVisitor(IHook hook) {
        super(ASM5);
        this.hook = hook;
        this.order = 0;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (name.lastIndexOf('/') != -1) {
            this.className = name.substring(name.lastIndexOf('/') + 1);
        } else {
            this.className = name;
        }
        this.classPath = name.replaceAll("/", ".");

        this.hook.createVertex(new NamespaceBlockVertex(this.className, name, order));
        this.hook.createVertex(new FileVertex(this.className, order++));

        // TODO: Could create MEMBER vertex from here to declare member classes

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        ASTMethodVisitor astMv = new ASTMethodVisitor(mv, hook,  access, name, classPath, descriptor);
        astMv.setOrder(order);
        return astMv;
    }
}
