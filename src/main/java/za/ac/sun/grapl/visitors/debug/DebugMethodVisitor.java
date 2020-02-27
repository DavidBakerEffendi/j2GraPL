/**
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
package za.ac.sun.grapl.visitors.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.ASMifier;

import java.util.StringJoiner;

class DebugMethodVisitor extends MethodVisitor implements Opcodes {

    final static Logger logger = LogManager.getLogger();

    public DebugMethodVisitor(final MethodVisitor mv) {
        super(ASM5, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitLabel(Label label) {
        logger.debug(new StringJoiner(" ").add("\t ").add(label.toString()).add("(label)"));
        super.visitLabel(label);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        logger.debug(new StringJoiner(" ")
                .add("\t ").add(ASMifier.OPCODES[opcode]).add(owner).add(name).add(descriptor).add("(visitFieldInsn)"));
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        logger.debug(new StringJoiner(" ")
                .add("\t ").add(ASMifier.OPCODES[opcode]).add("->").add(String.valueOf(var)).add("visitVarInsn"));
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        logger.debug(new StringJoiner(" ")
                .add("\t ").add(descriptor).add(name)
                .add("-> (").add(start.toString()).add(";").add(end.toString()).add(") (visitLocalVariable)"));
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        logger.debug(new StringJoiner(" ")
                .add("\t ").add(String.valueOf(line)).add(String.valueOf(start)).add("(visitLineNumber)"));
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitInsn(int opcode) {
        logger.debug(new StringJoiner(" ").add("\t ").add(ASMifier.OPCODES[opcode]).add("(visitInsn)"));
        super.visitInsn(opcode);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        logger.debug(new StringJoiner("\t ")
                .add(ASMifier.OPCODES[opcode]).add(label.toString()).add("(visitJumpInsn)"));
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        logger.debug(new StringJoiner(" ").add("\t ").add(value.toString()).add("(visitLdcInsn)"));
        super.visitLdcInsn(value);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        logger.debug(new StringJoiner(" ")
                .add("\t ").add(ASMifier.OPCODES[opcode]).add(type).add("(visitTypeInsn)"));
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        logger.debug(new StringJoiner(" ")
                .add("\t ").add(ASMifier.OPCODES[opcode]).add(owner).add(name).add(desc).add("(visitMethodInsn)"));
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitEnd() {
        logger.debug("\t}");
    }
}