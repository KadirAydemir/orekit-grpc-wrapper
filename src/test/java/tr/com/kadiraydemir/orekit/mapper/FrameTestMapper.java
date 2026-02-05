package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FrameTestMapper {
    @ValueMapping(source = "UNRECOGNIZED", target = "TEME")
    ReferenceFrameType map(ReferenceFrame source);
}
