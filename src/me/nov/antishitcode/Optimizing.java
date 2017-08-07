package me.nov.antishitcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;

import me.nov.antishitcode.utils.InstructionUtils;

public class Optimizing implements Opcodes {

  private Map<String, ClassNode> classes;
  private boolean success;
  private List<Integer> jumps = Arrays.asList(IFEQ, IFNE, IFGT, IFLT, IFGE, IFLE);

  public Optimizing(Map<String, ClassNode> classes) {
    this.classes = classes;
    this.success = false;
  }

  public boolean isSuccess() {
    return success;
  }

  public void start() {
    try {
      for (ClassNode cn : classes.values()) {
        for (MethodNode mn : cn.methods) {
          for (AbstractInsnNode ain : mn.instructions.toArray()) {
            if (ain.getType() == AbstractInsnNode.JUMP_INSN) {
              fixJumpInsn(mn, (JumpInsnNode) ain);
            }
          }
          for (TryCatchBlockNode tcbn : new ArrayList<>(mn.tryCatchBlocks)) {
            AbstractInsnNode handler = tcbn.handler;
            while (handler != null && (handler.getType() == AbstractInsnNode.LABEL || handler.getType() == AbstractInsnNode.FRAME
                || handler.getType() == AbstractInsnNode.LINE || handler.getType() == AbstractInsnNode.INSN)) {
              if (handler.getOpcode() == ATHROW) {
                mn.tryCatchBlocks.remove(tcbn);
                break;
              }
              handler = handler.getNext();
            }
          }
          removeDeadCode(cn.name, mn);
        }
      }
      success = true;
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void fixJumpInsn(MethodNode mn, JumpInsnNode ain) {
    int opc = ain.getOpcode();
    if (jumps.contains(opc) && InstructionUtils.isNumber(ain.getPrevious())) {
      int previous = InstructionUtils.getIntValue(ain.getPrevious());
      if (opc == IFGE) {
        if (previous >= 0) {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.set(ain, new JumpInsnNode(GOTO, ain.label));
        } else {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.remove(ain);
        }
      }
      if (opc == IFGT) {
        if (previous > 0) {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.set(ain, new JumpInsnNode(GOTO, ain.label));
        } else {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.remove(ain);
        }
      }
      if (opc == IFLE) {
        if (previous <= 0) {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.set(ain, new JumpInsnNode(GOTO, ain.label));
        } else {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.remove(ain);
        }
      }
      if (opc == IFLT) {
        if (previous < 0) {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.set(ain, new JumpInsnNode(GOTO, ain.label));
        } else {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.remove(ain);
        }
      }
      if (opc == IFEQ) {
        if (previous == 0) {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.set(ain, new JumpInsnNode(GOTO, ain.label));
        } else {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.remove(ain);
        }
      }
      if (opc == IFNE) {
        if (previous != 0) {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.set(ain, new JumpInsnNode(GOTO, ain.label));
        } else {
          mn.instructions.remove(ain.getPrevious());
          mn.instructions.remove(ain);
        }
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void removeDeadCode(String className, MethodNode method) {
    try {
      Analyzer analyzer = new Analyzer(new BasicInterpreter());
      analyzer.analyze(className, method);
      Frame[] frames = analyzer.getFrames();
      AbstractInsnNode[] insns = method.instructions.toArray();
      for (int i = 0; i < frames.length; i++) {
        AbstractInsnNode insn = insns[i];
        if (frames[i] == null && insn.getType() != AbstractInsnNode.LABEL) {
          method.instructions.remove(insn);
          insns[i] = null;
        }
      }
    } catch (AnalyzerException e) {
    }
  }
}
