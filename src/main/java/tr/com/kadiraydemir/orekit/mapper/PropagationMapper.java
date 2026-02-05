package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import tr.com.kadiraydemir.orekit.grpc.PositionPoint;
import tr.com.kadiraydemir.orekit.grpc.PropagateResponse;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateResponse;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.PropagateRequest;
import tr.com.kadiraydemir.orekit.model.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.model.PropagateTLEListRequest;
import tr.com.kadiraydemir.orekit.model.TleData;
import tr.com.kadiraydemir.orekit.model.TleIndexedResult;
import tr.com.kadiraydemir.orekit.model.TleResult;
import tr.com.kadiraydemir.orekit.grpc.PropagationModel;
import tr.com.kadiraydemir.orekit.model.PropagationModelType;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PropagationMapper {

    PropagateResponse map(OrbitResult source);

    @Mapping(target = "positionsList", source = "positions")
    TLEPropagateResponse map(TleResult source);

    PositionPoint map(TleResult.PositionPointResult source);

    PropagateRequest toDTO(tr.com.kadiraydemir.orekit.grpc.PropagateRequest source);

    TLEPropagateRequest toDTO(tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest source);

    @Mapping(target = "tles", source = "tlesList")
    PropagateTLEListRequest toDTO(tr.com.kadiraydemir.orekit.grpc.PropagateTLEListRequest source);

    TleData map(tr.com.kadiraydemir.orekit.grpc.TLE source);

    @Mapping(target = "positionsList", source = "positions")
    tr.com.kadiraydemir.orekit.grpc.TLEListPropagateResponse map(TleIndexedResult source);

    @ValueMapping(source = "UNRECOGNIZED", target = "AUTO")
    PropagationModelType map(PropagationModel source);

    @ValueMapping(source = "UNRECOGNIZED", target = "DORMAND_PRINCE_853")
    tr.com.kadiraydemir.orekit.model.IntegratorType map(IntegratorType source);

    @ValueMapping(source = "UNRECOGNIZED", target = "TEME")
    ReferenceFrameType map(ReferenceFrame source);
}
