package tr.com.kadiraydemir.orekit.config;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.ZipJarCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@ApplicationScoped
@Startup
public class OrekitConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OrekitConfig.class);

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "orekit.data.path", defaultValue = "orekit-data.zip")
    String dataPath;

    @PostConstruct
    public void init() {
        LOG.info("Initializing Orekit Data from: {}", dataPath);
        File orekitData = new File(dataPath);
        if (!orekitData.exists()) {
            LOG.warn("Data at {} not found, trying fallback to 'orekit-data.zip' in working directory", dataPath);
            orekitData = new File("orekit-data.zip");
        }

        if (!orekitData.exists()) {
            // fallback to folder
            orekitData = new File("orekit-data");
        }

        if (orekitData.exists()) {
            LOG.info("Found orekit-data at: {}", orekitData.getAbsolutePath());
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            if (orekitData.isFile()
                    && (orekitData.getName().endsWith(".zip") || orekitData.getName().endsWith(".jar"))) {
                manager.addProvider(new ZipJarCrawler(orekitData));
            } else {
                manager.addProvider(new DirectoryCrawler(orekitData));
            }
        } else {
            LOG.error(
                    "No orekit-data found! Orekit functionality will likely fail. Please ensure '{}' or 'orekit-data.zip' or 'orekit-data/' folder is in the working directory.",
                    dataPath);
        }
    }
}
