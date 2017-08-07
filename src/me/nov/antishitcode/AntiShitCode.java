package me.nov.antishitcode;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.util.JarUtils;

public class AntiShitCode {

  public static void main(String[] args) throws IOException {
    File input = new File("input.jar");
    Map<String, ClassNode> classes = JarUtils.loadClasses(input);
    Map<String, byte[]> out = JarUtils.loadNonClassEntries(input);

    Optimizing de = new Optimizing(classes);
    de.start();

    if (de.isSuccess()) {
      for (ClassNode cn : classes.values()) {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        out.put(cn.name, cw.toByteArray());
      }
      JarUtils.saveAsJar(out, "output.jar");
    } else {
      System.err.println("Finished without success.");
    }
  }

}
