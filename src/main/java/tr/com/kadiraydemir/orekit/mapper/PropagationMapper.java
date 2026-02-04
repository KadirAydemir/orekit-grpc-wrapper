package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tr.com.kadiraydemir.orekit.grpc.PositionPoint;
import tr.com.kadiraydemir.orekit.grpc.PropagateResponse;
import tr.com.kadiraydemir.orekit.grpc.TLEPropagateResponse;
import tr.com.kadiraydemir.orekit.model.OrbitResult;
import tr.com.kadiraydemir.orekit.model.TleResult;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PropagationMapper {

    PropagateResponse map(OrbitResult source);

    @Mapping(target = "positionsList", source = "positions")
    TLEPropagateResponse map(TleResult source);

    PositionPoint map(TleResult.PositionPointResult source);
}
