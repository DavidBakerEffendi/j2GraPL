package za.ac.sun.grapl.visitors.debug;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DebugClassVisitor extends ClassVisitor implements Opcodes {

    final static Logger logger = Logger.getLogger(DebugClassVisitor.class);
    String className;

    public DebugClassVisitor() {
        super(ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        logger.debug("");
        logger.debug("public class " + name + " extends " + superName + " {");
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        logger.debug("");
        logger.debug("}");
        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        logger.debug("");
        String acc = basicAccessDecoder(access);
        logger.debug("\t" + acc + " " + name + descriptor + " {");
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new DebugMethodVisitor(mv);
    }

    private String basicAccessDecoder(int access) {
        if (access == ACC_PUBLIC) return "public";
        else if (access == ACC_PRIVATE) return "private";
        else if (access == ACC_PROTECTED) return "protected";
        else if (access == ACC_STATIC + ACC_PUBLIC) return "public static";
        else if (access == ACC_STATIC + ACC_PRIVATE) return "private static";
        else if (access == ACC_STATIC + ACC_PROTECTED) return "protected static";
        else logger.debug("Unknown access: " + access);

        return "";
    }

}
