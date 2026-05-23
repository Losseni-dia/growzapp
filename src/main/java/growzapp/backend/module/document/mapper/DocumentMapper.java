package growzapp.backend.module.document.mapper;

import growzapp.backend.module.document.dto.DocumentDTO;
import growzapp.backend.module.document.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "url", expression = "java(document.getUrl())")
    DocumentDTO toDocumentDto(Document document);

    List<DocumentDTO> toDocumentDtoList(List<Document> documents);
}
