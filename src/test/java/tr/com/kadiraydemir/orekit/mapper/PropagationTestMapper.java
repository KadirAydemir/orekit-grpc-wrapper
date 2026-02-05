package tr.com.kadiraydemir.orekit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import tr.com.kadiraydemir.orekit.grpc.PropagationModel;
import tr.com.kadiraydemir.orekit.grpc.IntegratorType;
import tr.com.kadiraydemir.orekit.grpc.ReferenceFrame;
import tr.com.kadiraydemir.orekit.model.PropagateRequest;
import tr.com.kadiraydemir.orekit.model.TLEPropagateRequest;
import tr.com.kadiraydemir.orekit.model.PropagationModelType;
import tr.com.kadiraydemir.orekit.model.ReferenceFrameType;

@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PropagationTestMapper {
    PropagateRequest toDTO(tr.com.kadiraydemir.orekit.grpc.PropagateRequest source);
    TLEPropagateRequest toDTO(tr.com.kadiraydemir.orekit.grpc.TLEPropagateRequest source);

    @ValueMapping(source = "UNRECOGNIZED", target = "AUTO")
    PropagationModelType map(PropagationModel source);

    @ValueMapping(source = "UNRECOGNIZED", target = "DORMAND_PRINCE_853")
    tr.com.kadiraydemir.orekit.model.IntegratorType map(IntegratorType source);

    @ValueMapping(source = "UNRECOGNIZED", target = "TEME")
    ReferenceFrameType map(ReferenceFrame source);
}
