package tr.com.kadiraydemir.orekit.grpc.transformation;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tr.com.kadiraydemir.orekit.grpc.*;
import tr.com.kadiraydemir.orekit.mapper.TransformationMapper;
import tr.com.kadiraydemir.orekit.model.TransformResult;
import tr.com.kadiraydemir.orekit.service.transformation.TransformationService;
import tr.com.kadiraydemir.orekit.service.transformation.TransformationServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransformationGrpcServiceUnitTest {

    @Mock
    TransformationService transformationService;

    @Mock
    TransformationMapper transformationMapper;

    @Mock
    ExecutorService propagationExecutor;

    @InjectMocks
    TransformationGrpcService grpcService;

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
    public void transform_success() {
        TransformRequest request = TransformRequest.newBuilder().build();
        tr.com.kadiraydemir.orekit.model.TransformRequest dtoRequest = new tr.com.kadiraydemir.orekit.model.TransformRequest(
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME,
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.ITRF,
                "2024-01-01T00:00:00Z",
                0, 0, 0, 0, 0, 0
        );
        TransformResult dtoResult = new TransformResult(
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME.name(),
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.ITRF.name(),
                "2024-01-01T00:00:00Z",
                10, 20, 30, 0, 0, 0
        );
        TransformResponse response = TransformResponse.newBuilder().setX(10).build();

        when(transformationMapper.toDTO(request)).thenReturn(dtoRequest);
        when(transformationService.transform(dtoRequest)).thenReturn(dtoResult);
        when(transformationMapper.map(dtoResult)).thenReturn(response);

        StreamObserver<TransformResponse> observer = mock(StreamObserver.class);
        grpcService.transform(request, observer);

        verify(observer).onNext(response);
        verify(observer).onCompleted();
    }

    @Test
    public void transform_failure() {
        TransformRequest request = TransformRequest.newBuilder().build();
        tr.com.kadiraydemir.orekit.model.TransformRequest dtoRequest = new tr.com.kadiraydemir.orekit.model.TransformRequest(
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME,
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.ITRF,
                "2024-01-01T00:00:00Z",
                0, 0, 0, 0, 0, 0
        );

        when(transformationMapper.toDTO(request)).thenReturn(dtoRequest);
        when(transformationService.transform(dtoRequest)).thenThrow(new RuntimeException("Transform failed"));

        StreamObserver<TransformResponse> observer = mock(StreamObserver.class);
        grpcService.transform(request, observer);

        verify(observer).onError(any(RuntimeException.class));
    }

    @Test
    public void batchTransform_partialFailure() {
        StateVector sv1 = StateVector.newBuilder().setX(1).build();
        StateVector sv2 = StateVector.newBuilder().setX(2).build();
        BatchTransformRequest request = BatchTransformRequest.newBuilder()
                .addStateVectors(sv1)
                .addStateVectors(sv2)
                .setSourceFrame(ReferenceFrame.TEME)
                .setTargetFrame(ReferenceFrame.ITRF)
                .build();

        // We need to match the calls inside processSingleStateVector
        // It creates a TransformRequest for each StateVector

        // Use any() for simplicity or match specific args
        when(transformationMapper.toDTO(any(TransformRequest.class))).thenReturn(
                new tr.com.kadiraydemir.orekit.model.TransformRequest(
                        tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME,
                        tr.com.kadiraydemir.orekit.model.ReferenceFrameType.ITRF,
                        null, 0, 0, 0, 0, 0, 0)
        );

        // First call succeeds
        TransformResult result1 = new TransformResult(
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME.name(),
                tr.com.kadiraydemir.orekit.model.ReferenceFrameType.ITRF.name(),
                "2024-01-01T00:00:00Z",
                10, 0, 0, 0, 0, 0
        );
        TransformResponse response1 = TransformResponse.newBuilder().setX(10).build();

        // Second call fails (throws exception)
        when(transformationService.transform(any()))
                .thenReturn(result1)
                .thenThrow(new RuntimeException("Batch item failed"));

        when(transformationMapper.map(result1)).thenReturn(response1);

        StreamObserver<BatchTransformResponse> observer = mock(StreamObserver.class);
        grpcService.batchTransform(request, observer);

        // Mutiny streaming is async, but our executor is synchronous.
        // However, Mutiny might use other threads for merge?
        // No, merge uses the subscription thread if not specified otherwise.
        // But runSubscriptionOn(propagationExecutor) is used.

        // Verify that we got a response
        // The service implementation accumulates results into a list and sends ONE BatchTransformResponse
        // But if one item fails, processSingleStateVector catches exception and returns error response

        verify(observer).onNext(any(BatchTransformResponse.class));
        verify(observer).onCompleted();
    }
}
