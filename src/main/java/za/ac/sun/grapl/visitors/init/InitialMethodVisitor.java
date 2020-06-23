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
package za.ac.sun.grapl.visitors.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;
import za.ac.sun.grapl.controllers.MethodInfoController;
import za.ac.sun.grapl.visitors.OpStackMethodVisitor;

public final class InitialMethodVisitor extends OpStackMethodVisitor implements Opcodes {

    private final static Logger logger = LogManager.getLogger();

    private Label currentLabel;
    private final MethodInfoController methodInfo;

    public InitialMethodVisitor(final MethodVisitor mv, final MethodInfoController methodInfo) {
        super(mv, methodInfo);
        this.methodInfo = methodInfo;
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " (visitInsn)");
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + operand + " (visitIntInsn)");
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        logger.debug("\t" + ASMifier.OPCODES[opcode] + " -> " + var + " (visitVarInsn)");
        methodInfo.addVariable(var);
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + label + " (visitJumpInsn)");
        methodInfo.addJump(ASMifier.OPCODES[opcode], label, currentLabel);
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        logger.debug("\t  VAR: " + var + " INC: " + increment + " (visitIincInsn)");
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitLabel(Label label) {
        logger.debug("");
        logger.debug("\t" + label + " (label)");
        currentLabel = label;
        super.visitLabel(label);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        logger.debug("\t" + ASMifier.OPCODES[opcode] + owner + " " + name + " " + descriptor + " (visitFieldInsn)");
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        logger.debug("\tDEBUG INFO: " + descriptor + " " + name + " -> (" + start + "; " + end + ") (visitLocalVariable)");
        methodInfo.addVarDebugInfo(index, name, descriptor, start, end);
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        logger.debug("\t  " + line + " " + start + " (visitLineNumber)");
        if (Integer.valueOf(-1).equals(methodInfo.getLineNumber())) methodInfo.setLineNumber(line - 1);

        methodInfo.addLabel(line, start);
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitLdcInsn(Object value) {
        logger.debug("\t  " + value + " (visitLdcInsn)");
        super.visitLdcInsn(value);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        logger.debug("\t  " + name + " INC: " + descriptor + " " + bootstrapMethodHandle + " (visitInvokeDynamicInsn)");
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

//    @Override
//    public void visitParameter(String name, int access) {
//        logger.debug(new StringJoiner(" ")
//                .add("\t ").add(name).add(ASMParserUtil.determineModifiers(access).toString()).add("(visitParameter)"));
//        super.visitParameter(name, access);
//    }
//
//    @Override
//    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//        StringJoiner sj = new StringJoiner(" ")
//                .add("\t ").add(String.valueOf(min)).add(String.valueOf(max)).add(dflt.toString());
//        if (labels.length > 0) {
//            sj.add("LABELS [");
//            for (Label label : labels) {
//                sj.add(label.toString());
//            }
//            sj.add("]");
//        }
//        logger.debug(sj.add("(visitTableSwitchInsn)"));
//        super.visitTableSwitchInsn(min, max, dflt, labels);
//    }
//
//    @Override
//    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//        StringJoiner sj = new StringJoiner(" ")
//                .add("\t ").add(dflt.toString());
//        if (keys.length > 0) {
//            sj.add("KEYS [");
//            for (int k : keys) {
//                sj.add(String.valueOf(k));
//            }
//            sj.add("]");
//        }
//        if (labels.length > 0) {
//            sj.add("LABELS [");
//            for (Label label : labels) {
//                sj.add(label.toString());
//            }
//            sj.add("]");
//        }
//        logger.debug(sj.add("(visitTableSwitchInsn)"));
//        super.visitLookupSwitchInsn(dflt, keys, labels);
//    }
//
//    @Override
//    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//        logger.debug(new StringJoiner(" ")
//                .add("\t ").add(start.toString()).add(end.toString())
//                .add(handler.toString()).add(type).add("(visitTryCatchBlock)"));
//        super.visitTryCatchBlock(start, end, handler, type);
//    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + type + " (visitTypeInsn)");
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + owner + " " + name + " " + desc + " (visitMethodInsn)");
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitEnd() {
        logger.debug("\t}");
    }
}