package org.mcnative.tools.registry.codegenerator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

//From https://piratemc.com/minecraft-json-id-list/ a little bit modified in legacy-item-ids.txt
public class LegacyMcNativeCodeGenerator {

    public static void main(String[] args) throws IOException {

        String filePath = Objects.requireNonNull(LegacyMcNativeCodeGenerator.class.getClassLoader().getResource("legacy-item-ids.txt")).getPath();

        try (InputStream resource = new FileInputStream(filePath)) {
            StringBuilder codeBuilder = new StringBuilder()
                    .append("private static void ").append("add").append("Legacy").append("ProtocolVersions").append("() {");
            new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).lines().forEach(line -> {
                String[] raw = line.split("#");
                String fullId = raw[0];
                String id;
                String subId;
                if(fullId.contains(":")) {
                    String[] idAndSubId = fullId.split(":");
                    id = idAndSubId[0];
                    subId = idAndSubId[1];
                } else {
                    id = fullId;
                    subId = "0";
                }
                String rawName = raw[1];
                int index = rawName.lastIndexOf(" (");
                rawName = rawName.substring(0, index);
                String name = rawName.toUpperCase().replace(" ", "_");

                String code = "\n    "+name+".addLegacyProtocolIds(new LegacyMaterialProtocolId("+id+", "+subId+"));";
                codeBuilder.append(code);
            });
            codeBuilder.append("\n}");
            System.out.println(codeBuilder.toString());
        }
    }
}
