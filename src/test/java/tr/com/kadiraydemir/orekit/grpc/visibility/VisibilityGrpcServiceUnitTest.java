package tr.com.kadiraydemir.orekit.grpc.visibility;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.VisibilityMapper;
import tr.com.kadiraydemir.orekit.model.VisibilityResult;
import tr.com.kadiraydemir.orekit.service.visibility.VisibilityService;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VisibilityGrpcServiceUnitTest {

    @Mock
    VisibilityService visibilityService;

    @Mock
    VisibilityMapper visibilityMapper;

    @Mock
    ExecutorService propagationExecutor;

    @InjectMocks
    VisibilityGrpcService grpcService;

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
    public void getAccessIntervals_success() {
        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder().build();
        tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest dtoRequest = new tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest(
                "line1", "line2", "start", "end", null, 0.0
        );
        VisibilityResult dtoResult = new VisibilityResult("sat", "station", null);
        AccessIntervalsResponse response = AccessIntervalsResponse.newBuilder().setSatelliteName("sat").build();

        when(visibilityMapper.toDTO(request)).thenReturn(dtoRequest);
        when(visibilityService.getAccessIntervals(dtoRequest)).thenReturn(dtoResult);
        when(visibilityMapper.map(dtoResult)).thenReturn(response);

        StreamObserver<AccessIntervalsResponse> observer = mock(StreamObserver.class);
        grpcService.getAccessIntervals(request, observer);

        verify(observer).onNext(response);
        verify(observer).onCompleted();
    }

    @Test
    public void getAccessIntervals_failure() {
        AccessIntervalsRequest request = AccessIntervalsRequest.newBuilder().build();
        tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest dtoRequest = new tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest(
                "line1", "line2", "start", "end", null, 0.0
        );

        when(visibilityMapper.toDTO(request)).thenReturn(dtoRequest);
        when(visibilityService.getAccessIntervals(dtoRequest)).thenThrow(new RuntimeException("Visibility error"));

        StreamObserver<AccessIntervalsResponse> observer = mock(StreamObserver.class);
        grpcService.getAccessIntervals(request, observer);

        verify(observer).onError(any(RuntimeException.class));
    }

    @Test
    public void batchGetAccessIntervals_partialFailure() {
        TLELines tle1 = TLELines.newBuilder().setTleLine1("1 25544U...").setTleLine2("2 25544...").build();
        BatchAccessIntervalsRequest request = BatchAccessIntervalsRequest.newBuilder()
                .addTles(tle1)
                .setStartDateIso("2024-01-01T00:00:00Z")
                .setEndDateIso("2024-01-02T00:00:00Z")
                .setMinElevationDegrees(10.0)
                .build();

        when(visibilityMapper.toDTO(any(AccessIntervalsRequest.class))).thenReturn(
                new tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest("1 25544U...", "2 25544...", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z", null, 10.0)
        );

        // Service throws exception
        when(visibilityService.getAccessIntervals(any())).thenThrow(new RuntimeException("Batch item failed"));

        StreamObserver<BatchAccessIntervalsResponse> observer = mock(StreamObserver.class);
        grpcService.batchGetAccessIntervals(request, observer);

        verify(observer).onNext(any(BatchAccessIntervalsResponse.class));
        verify(observer).onCompleted();
    }

    @Test
    public void batchGetAccessIntervals_nullResult() {
        TLELines tle1 = TLELines.newBuilder().setTleLine1("1 25544U...").setTleLine2("2 25544...").build();
        BatchAccessIntervalsRequest request = BatchAccessIntervalsRequest.newBuilder()
                .addTles(tle1)
                .setStartDateIso("invalid-date")
                .setEndDateIso("invalid-date")
                .build();

        when(visibilityMapper.toDTO(any(AccessIntervalsRequest.class))).thenReturn(
                new tr.com.kadiraydemir.orekit.model.AccessIntervalsRequest("1 25544U...", "2 25544...", "invalid-date", "invalid-date", null, 0.0)
        );

        // Service returns null
        when(visibilityService.getAccessIntervals(any())).thenReturn(null);

        StreamObserver<BatchAccessIntervalsResponse> observer = mock(StreamObserver.class);
        grpcService.batchGetAccessIntervals(request, observer);

        verify(observer).onNext(any(BatchAccessIntervalsResponse.class));
        verify(observer).onCompleted();
    }
}
