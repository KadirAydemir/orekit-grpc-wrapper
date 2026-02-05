package tr.com.kadiraydemir.orekit.util;

import com.google.protobuf.util.JsonFormat;
import tr.com.kadiraydemir.orekit.grpc.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Client utility to test TLE streaming with generated data
 * 
 * Example usage with grpcurl:
 * 
 * 1. First, generate the test data:
 *    ./mvnw compile exec:java -Dexec.mainClass="tr.com.kadiraydemir.orekit.util.TleGenerator"
 * 
 * 2. Then stream TLEs using the generated file:
 *    cat test_tles_25k.txt | ./stream_tles.sh
 * 
 * Or use this class to create properly formatted gRPC messages.
 */
public class TleStreamTestClient {

    /**
     * Reads TLEs from file and creates TLEStreamRequest messages
     */
    public static List<TLEStreamRequest> createRequestsFromFile(
            String filename,
            String startDate,
            String endDate,
            int positionCount,
            String outputFrame,
            String integrator) throws IOException {

        List<TLEStreamRequest> requests = new ArrayList<>();

        // First message: Config
        TLEStreamConfig config = TLEStreamConfig.newBuilder()
            .setModel(PropagationModel.AUTO)
            .setStartDate(startDate)
            .setEndDate(endDate)
            .setPositionCount(positionCount)
            .setOutputFrame(ReferenceFrame.valueOf(outputFrame))
            .setIntegrator(IntegratorType.valueOf(integrator))
            .build();

        requests.add(TLEStreamRequest.newBuilder()
            .setConfig(config)
            .build());

        // Read TLEs from file
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line1;
            while ((line1 = reader.readLine()) != null) {
                String line2 = reader.readLine();
                if (line2 != null) {
                    TLELines tle = TLELines.newBuilder()
                        .setTleLine1(line1)
                        .setTleLine2(line2)
                        .build();

                    requests.add(TLEStreamRequest.newBuilder()
                        .setTle(tle)
                        .build());
                }
            }
        }

        return requests;
    }

    /**
     * Creates a JSON file with gRPC request format for use with grpcurl
     */
    public static void createGrpcJsonFile(
            String tleFilename,
            String outputFilename,
            String startDate,
            String endDate,
            int positionCount,
            String outputFrame,
            String integrator) throws IOException {

        List<TLEStreamRequest> requests = createRequestsFromFile(
            tleFilename, startDate, endDate, positionCount, outputFrame, integrator);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilename))) {

            for (TLEStreamRequest request : requests) {
                String json = JsonFormat.printer().print(request);
                writer.println(json);
            }
        }

        System.out.println("Created " + requests.size() + " gRPC messages in " + outputFilename);
    }

    public static void main(String[] args) throws IOException {
        // Generate test files for different scenarios

        // Small test: 100 TLEs
        createGrpcJsonFile(
            "test_tles_100.txt",
            "grpc_requests_100.json",
            "2024-02-05T00:00:00Z",
            "2024-02-06T00:00:00Z",
            5,
            "GCRF",
            "DORMAND_PRINCE_853"
        );

        // Medium test: 1K TLEs
        createGrpcJsonFile(
            "test_tles_1k.txt",
            "grpc_requests_1k.json",
            "2024-02-05T00:00:00Z",
            "2024-02-06T00:00:00Z",
            5,
            "GCRF",
            "DORMAND_PRINCE_853"
        );

        // Large test: 25K TLEs
        createGrpcJsonFile(
            "test_tles_25k.txt",
            "grpc_requests_25k.json",
            "2024-02-05T00:00:00Z",
            "2024-02-06T00:00:00Z",
            5,
            "GCRF",
            "DORMAND_PRINCE_853"
        );

        System.out.println("\nTest files created!");
        System.out.println("Use with: grpcurl -d @ -plaintext localhost:9000 \\");
        System.out.println("  tr.com.kadiraydemir.orekit.grpc.OrbitalService/PropagateTLEStream < grpc_requests_100.json");
    }
}
