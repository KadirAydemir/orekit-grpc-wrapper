package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import tr.com.kadiraydemir.orekit.grpc.TransformRequest;
import tr.com.kadiraydemir.orekit.grpc.TransformResponse;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;
import tr.com.kadiraydemir.orekit.model.TransformRequestDTO;
import tr.com.kadiraydemir.orekit.model.TransformResult;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface TransformationMapper {

    TransformRequestDTO toDTO(TransformRequest source);

    TransformResponse map(TransformResult source);

    @ValueMapping(source = "UNRECOGNIZED", target = "TEME")
    ReferenceFrameType map(ReferenceFrame source);

    ReferenceFrame map(ReferenceFrameType source);
}
