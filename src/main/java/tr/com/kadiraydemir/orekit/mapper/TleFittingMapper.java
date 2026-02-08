package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.model.PositionMeasurement;
import tr.com.kadiraydemir.orekit.model.TleFittingRequest;

/**
 * Mapper for TLE fitting operations between gRPC and domain models.
 */
@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface TleFittingMapper {

        /**
         * Converts a gRPC FitTLERequest to domain model.
         */
        @Mapping(target = "initialTleLine1", source = "initialTleLine1")
        @Mapping(target = "initialTleLine2", source = "initialTleLine2")
        @Mapping(target = "satelliteName", source = "satelliteName")
        @Mapping(target = "satelliteNumber", source = "satelliteNumber")
        @Mapping(target = "internationalDesignator", source = "internationalDesignator")
        @Mapping(target = "measurements", source = "measurementsList")
        @Mapping(target = "convergenceThreshold", source = "convergenceThreshold")
        @Mapping(target = "maxIterations", source = "maxIterations")
        @Mapping(target = "inputFrame", source = "inputFrame")
        TleFittingRequest toDomain(tr.com.kadiraydemir.orekit.grpc.FitTLERequest grpcRequest);

        /**
         * Converts gRPC InputReferenceFrame to ReferenceFrameType.
         */
        default tr.com.kadiraydemir.orekit.model.ReferenceFrameType map(
                        tr.com.kadiraydemir.orekit.grpc.InputReferenceFrame source) {
                if (source == null) {
                        return tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME;
                }
                switch (source) {
                        case GCRF_FRAME:
                                return tr.com.kadiraydemir.orekit.model.ReferenceFrameType.GCRF;
                        case EME2000_FRAME:
                                return tr.com.kadiraydemir.orekit.model.ReferenceFrameType.EME2000;
                        case ITRF_FRAME:
                                return tr.com.kadiraydemir.orekit.model.ReferenceFrameType.ITRF;
                        case TEME_FRAME:
                        default:
                                return tr.com.kadiraydemir.orekit.model.ReferenceFrameType.TEME;
                }
        }

        /**
         * Converts a gRPC PositionMeasurement to domain model.
         */
        @Mapping(target = "timestamp", source = "timestamp")
        @Mapping(target = "positionX", source = "positionX")
        @Mapping(target = "positionY", source = "positionY")
        @Mapping(target = "positionZ", source = "positionZ")
        @Mapping(target = "weight", source = "weight")
        @Mapping(target = "sigma", source = "sigma")
        PositionMeasurement toDomain(tr.com.kadiraydemir.orekit.grpc.PositionMeasurement grpcMeasurement);
}
