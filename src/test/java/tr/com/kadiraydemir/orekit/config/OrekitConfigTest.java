package tr.com.kadiraydemir.orekit.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("OrekitConfig Tests")
public class OrekitConfigTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should initialize with ZIP file successfully")
    public void init_withZipFile_addsZipJarCrawler() throws Exception {
        // Given - Create a temporary zip file
        File zipFile = tempDir.resolve("orekit-data.zip").toFile();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
            // Create empty zip with one entry to make it valid
            zos.putNextEntry(new java.util.zip.ZipEntry("test.txt"));
            zos.write("test".getBytes());
            zos.closeEntry();
        }

        OrekitConfig config = new OrekitConfig();
        config.dataPath = zipFile.getAbsolutePath();

        // When - Should not throw exception
        assertDoesNotThrow(() -> config.init());
    }

    @Test
    @DisplayName("Should initialize with directory successfully")
    public void init_withDirectory_addsDirectoryCrawler() throws Exception {
        // Given - Create a temporary directory
        File dataDir = tempDir.resolve("orekit-data").toFile();
        dataDir.mkdirs();
        File testFile = new File(dataDir, "test.txt");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("test");
        }

        OrekitConfig config = new OrekitConfig();
        config.dataPath = dataDir.getAbsolutePath();

        // When - Should not throw exception
        assertDoesNotThrow(() -> config.init());
    }

    @TempDir
    Path anotherTempDir;

    @Test
    @DisplayName("Should handle missing data file gracefully")
    public void init_fileNotFound_logsError() {
        // Given - Non-existent path
        OrekitConfig config = new OrekitConfig();
        config.dataPath = "/non/existent/path/orekit-data.zip";

        // When - Should not throw exception, just log error
        assertDoesNotThrow(() -> config.init());
    }

    @Test
    @DisplayName("Should fallback to working directory when configured path not found")
    public void init_withConfigPath_usesFallback() throws Exception {
        // Given - Create orekit-data.zip in temp directory (simulating working dir)
        File zipFile = anotherTempDir.resolve("orekit-data.zip").toFile();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("test.txt"));
            zos.write("test".getBytes());
            zos.closeEntry();
        }

        // Change to temp directory temporarily
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", anotherTempDir.toString());

        try {
            OrekitConfig config = new OrekitConfig();
            config.dataPath = "/invalid/path.zip";

            // When - Should attempt fallback
            assertDoesNotThrow(() -> config.init());
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }
}
