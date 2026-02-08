package tr.com.kadiraydemir.orekit.grpc.eclipse;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.EclipseMapper;
import tr.com.kadiraydemir.orekit.model.EclipseResult;
import tr.com.kadiraydemir.orekit.service.eclipse.EclipseService;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EclipseGrpcServiceUnitTest {

    @Mock
    EclipseService eclipseService;

    @Mock
    EclipseMapper eclipseMapper;

    @Mock
    ExecutorService propagationExecutor;

    @InjectMocks
    EclipseGrpcService grpcService;

    @BeforeEach
    public void setup() {
        // Mock executor to run tasks immediately
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(propagationExecutor).execute(any(Runnable.class));
    }

    @Test
    public void calculateEclipses_success() {
        EclipseRequest request = EclipseRequest.newBuilder().build();
        tr.com.kadiraydemir.orekit.model.EclipseRequest dtoRequest = new tr.com.kadiraydemir.orekit.model.EclipseRequest(
                "line1", "line2", "start", "end"
        );
        EclipseResult dtoResult = new EclipseResult(12345, null);
        EclipseResponse response = EclipseResponse.newBuilder().setNoradId(12345).build();

        when(eclipseMapper.toDTO(request)).thenReturn(dtoRequest);
        when(eclipseService.calculateEclipses(dtoRequest)).thenReturn(dtoResult);
        when(eclipseMapper.map(dtoResult)).thenReturn(response);

        StreamObserver<EclipseResponse> observer = mock(StreamObserver.class);
        grpcService.calculateEclipses(request, observer);

        verify(observer).onNext(response);
        verify(observer).onCompleted();
    }

    @Test
    public void calculateEclipses_failure() {
        EclipseRequest request = EclipseRequest.newBuilder().build();
        tr.com.kadiraydemir.orekit.model.EclipseRequest dtoRequest = new tr.com.kadiraydemir.orekit.model.EclipseRequest(
                "line1", "line2", "start", "end"
        );

        when(eclipseMapper.toDTO(request)).thenReturn(dtoRequest);
        when(eclipseService.calculateEclipses(dtoRequest)).thenThrow(new RuntimeException("Eclipse error"));

        StreamObserver<EclipseResponse> observer = mock(StreamObserver.class);
        grpcService.calculateEclipses(request, observer);

        verify(observer).onError(any(RuntimeException.class));
    }

    @Test
    public void batchCalculateEclipses_partialFailure() {
        TLEPair tle1 = TLEPair.newBuilder().setLine1("1 25544U...").setLine2("2 25544...").build();
        BatchEclipseRequest request = BatchEclipseRequest.newBuilder()
                .addTles(tle1)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-02T00:00:00Z")
                .build();

        // Mock mapper call inside processSingleTle
        // The code constructs EclipseRequest manually:
        // EclipseRequest grpcRequest = EclipseRequest.newBuilder()...
        // Then calls eclipseMapper.toDTO(grpcRequest)

        when(eclipseMapper.toDTO(any(EclipseRequest.class))).thenReturn(
                new tr.com.kadiraydemir.orekit.model.EclipseRequest("1 25544U...", "2 25544...", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z")
        );

        // Service throws exception
        when(eclipseService.calculateEclipses(any())).thenThrow(new RuntimeException("Batch item failed"));

        StreamObserver<BatchEclipseResponse> observer = mock(StreamObserver.class);
        grpcService.batchCalculateEclipses(request, observer);

        verify(observer).onNext(any(BatchEclipseResponse.class));
        verify(observer).onCompleted();
    }

    @Test
    public void batchCalculateEclipses_nullResult() {
        TLEPair tle1 = TLEPair.newBuilder().setLine1("1 25544U...").setLine2("2 25544...").build();
        BatchEclipseRequest request = BatchEclipseRequest.newBuilder()
                .addTles(tle1)
                .setStartDateIso("invalid-date") // To trigger date parse error fallback
                .setEndDateIso("invalid-date")
                .build();

        when(eclipseMapper.toDTO(any(EclipseRequest.class))).thenReturn(
                new tr.com.kadiraydemir.orekit.model.EclipseRequest("1 25544U...", "2 25544...", "invalid-date", "invalid-date")
        );

        // Service returns null
        when(eclipseService.calculateEclipses(any())).thenReturn(null);

        StreamObserver<BatchEclipseResponse> observer = mock(StreamObserver.class);
        grpcService.batchCalculateEclipses(request, observer);

        verify(observer).onNext(any(BatchEclipseResponse.class));
        verify(observer).onCompleted();
    }
}
