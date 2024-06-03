package org.example;

import org.example.wag.world.GlobalWorld;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        GlobalWorld world = GlobalWorld.generate("playground.txt");
        System.out.println(world.displaySubjects());
        System.out.println("---\n" + world.displayVectorSpace());
        System.out.println("---\n" + world.displayGraphSpace());
    }
}
