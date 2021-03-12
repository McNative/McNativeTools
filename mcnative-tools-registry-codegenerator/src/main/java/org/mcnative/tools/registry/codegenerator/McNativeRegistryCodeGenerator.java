package org.mcnative.tools.registry.codegenerator;

import net.pretronic.libraries.document.Document;
import net.pretronic.libraries.document.entry.DocumentEntry;
import net.pretronic.libraries.document.type.DocumentFileType;
import net.pretronic.libraries.utility.io.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class McNativeRegistryCodeGenerator {

    public static void main(String[] args) {
        System.out.println("");
        System.out.println("    __  ___       _   __        __   _            ");
        System.out.println("   /  |/  /_____ / | / /____ _ / /_ (_)_   __ ___ ");
        System.out.println("  / /|_/ // ___//  |/ // __ `// __// /| | / // _ \\");
        System.out.println(" / /  / // /__ / /|  // /_/ // /_ / / | |/ //  __/");
        System.out.println("/_/  /_/ \\___//_/ |_/ \\__,_/ \\__//_/  |___/ \\___/");
        System.out.println("                           Registry Code Generator");
        System.out.println("");
        if(args.length < 2) {
            showHelp();
            return;
        }
        boolean all = args.length == 3 && args[2].equalsIgnoreCase("--all");

        String minecraftProtocolVersion = args[0];
        if(!minecraftProtocolVersion.startsWith("--protocolVersion=")) {
            showHelp();
            return;
        }
        minecraftProtocolVersion = minecraftProtocolVersion.split("=")[1].replace(".", "_");
        String methodName = "add"+minecraftProtocolVersion+"ProtocolVersions";

        String option = args[1];
        if(option.startsWith("--jar=")) {
            String jarFile = option.split("=")[1];
            generateRegistryFile(jarFile, methodName, all, minecraftProtocolVersion);
        } else if(option.startsWith("--json=")) {
            String jsonFile = option.split("=")[1];
            File location = new File(jsonFile);
            generateCodeFromJsonFile(location, methodName, all, minecraftProtocolVersion);
        } else {
            showHelp();
        }
    }

    private static void generateRegistryFile(String jarFile, String methodName, boolean all, String minecraftProtocolVersion) {
        Process process;
        try {
            process = Runtime.getRuntime().exec("java -cp "+jarFile+" net.minecraft.data.Main --reports");
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        File jsonFile = new File("generated/reports/items.json");
        if(!jsonFile.exists()) {
            jsonFile = new File("generated/reports/registries.json");
        }
        generateCodeFromJsonFile(jsonFile, methodName, all, minecraftProtocolVersion);
        FileUtil.deleteDirectory("generated/");
        FileUtil.deleteDirectory("logs/");
    }

    private static void generateCodeFromJsonFile(File location, String methodName, boolean all, String minecraftProtocolVersion) {
        if(!location.exists() || !location.isFile()) {
            System.err.println("Given location " + location.getName() + " is not a valid file");
            return;
        }
        try {
            StringBuilder constantsBuilder = null;
            if(all) constantsBuilder = new StringBuilder();

            StringBuilder protocolVersionBuilder = new StringBuilder();
            protocolVersionBuilder.append("private static void ").append(methodName).append("() {");

            Document document = DocumentFileType.JSON.getReader().read(location);
            Document materials = document;
            DocumentEntry first = document.getFirst();
            if(first.toDocument().contains("entries")) {
                materials = document.getDocument("minecraft:item").getDocument("entries");
            }

            for (DocumentEntry documentEntry : materials) {
                String protocolVersion = documentEntry.toDocument().getString("protocol_id");
                generateMaterialProtocolVersion(protocolVersionBuilder, constantsBuilder, "JE_"+minecraftProtocolVersion,documentEntry.getKey(),
                        protocolVersion);
            }

            protocolVersionBuilder.append("\n}");


            StringBuilder soundBuilder = new StringBuilder();
            for (DocumentEntry entries : document.getDocument("minecraft:sound_event").getDocument("entries")) {
                soundBuilder.append("\n");
                generateSound(soundBuilder, entries.getKey());
            }

            createCodeOutputFile(protocolVersionBuilder, new File("output_protocolVersion_"+minecraftProtocolVersion+".txt"));
            if(constantsBuilder != null) {
                createCodeOutputFile(constantsBuilder, new File("output_constants_"+minecraftProtocolVersion+".txt"));
            }
            createCodeOutputFile(soundBuilder, new File("output_sounds_" + minecraftProtocolVersion+".txt"));
        } catch (Exception exception) {
            System.err.println("Can't read json registry from " + location.getName());
            exception.printStackTrace();
        }
    }

    private static void createCodeOutputFile(StringBuilder codeBuilder, File output) throws IOException {
        output.createNewFile();
        FileWriter fileWriter = new FileWriter(output);
        fileWriter.write(codeBuilder.toString());
        fileWriter.close();
        System.out.println("Successfully generated protocol versions code to file " + output.getName());
    }

    private static void generateMaterialProtocolVersion(StringBuilder protocolVersionBuilder, StringBuilder constantsBuilder, String minecraftProtocolVersion,
                                                        String material, String protocolVersion) {
        String key = material.split(":")[1].toUpperCase();
        protocolVersionBuilder.append("\n").append("    ").append(key).append(".getProtocolIds().put(MinecraftProtocolVersion.").append(minecraftProtocolVersion)
                .append(", new DefaultMaterialProtocolId(").append(protocolVersion).append("));");
        if(constantsBuilder != null) {
            if(constantsBuilder.length() > 0) {
                constantsBuilder.append("\n");
            }
            constantsBuilder.append("public static final Material ").append(key).append(" = create(\"").append(key).append("\").buildAndRegister();");
        }
    }

    private static void generateSound(StringBuilder soundBuilder, String rawName) {
        String name = rawName.replace("minecraft:", "").toUpperCase().replace(".", "_");
        soundBuilder.append("public static final String ").append(name).append(" = \"").append(rawName).append("\";");
    }

    private static void showHelp() {
        System.out.println("Options");
        System.out.println("--jar=yourServer.jar | Input server.jar file");
        System.out.println("--json=yourRegistry.json | Input registry.json file");
    }
}
