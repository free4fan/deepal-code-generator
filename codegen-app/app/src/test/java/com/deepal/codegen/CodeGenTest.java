package com.deepal.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CodeGenTest {

    private static final List<String[]> FIXTURES = new ArrayList<>();

    @BeforeClass
    public static void loadFixtures() throws IOException {
        try (InputStream in = CodeGenTest.class.getResourceAsStream("/test-fixtures/fixtures.txt");
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains(",")) {
                    continue;
                }
                String vin = line.split(",", 2)[0];
                String expected = line.split(",", 2)[1];
                FIXTURES.add(new String[]{vin, expected});
            }
        }
        assertTrue("fixtures.txt loaded", !FIXTURES.isEmpty());
    }

    @Test
    public void fixturesMatchOriginalScript() {
        for (String[] f : FIXTURES) {
            assertEquals("VIN " + f[0], f[1], CodeGen.genCode(f[0]));
        }
    }

    @Test
    public void readmeExample() {
        assertEquals("*#14036425#*", CodeGen.genCode("LS6A3E2R5PA123456"));
    }

    @Test
    public void onlyLastEightUsed() {
        assertEquals(CodeGen.genCode("XXXXXXXXLS6A3E2R5PA123456"),
                CodeGen.genCode("LS6A3E2R5PA123456"));
    }

    @Test
    public void lowercaseNormalized() {
        assertEquals(CodeGen.genCode("wxyz1234"), CodeGen.genCode("WXYZ1234"));
    }

    @Test
    public void shortVinThrows() {
        try {
            CodeGen.genCode("12345");
            fail("expected exception for short VIN");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void unacceptableCharDetected() {
        assertEquals(3, CodeGen.indexOfUnacceptable("ABC!EFGH1"));
        assertEquals(-1, CodeGen.indexOfUnacceptable("LS6A3E2R5PA123456"));
        assertEquals(1, CodeGen.indexOfUnacceptable("aob3"));
    }

    @Test
    public void rejectsIoqAndShortVins() {
        assertTrue(CodeGen.indexOfUnacceptable("LS6A3E2R5PA1QOI12") >= 0);
        assertTrue(CodeGen.indexOfUnacceptable("9999QQII8888OO00000") >= 0);
        try {
            CodeGen.genCode("SHORT");
            fail("expected exception for VIN shorter than 8 chars");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
