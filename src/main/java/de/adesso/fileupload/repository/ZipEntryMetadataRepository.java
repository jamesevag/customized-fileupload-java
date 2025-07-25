package de.adesso.fileupload.repository;

import de.adesso.fileupload.entity.ZipEntryMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZipEntryMetadataRepository extends JpaRepository<ZipEntryMetadata, Long> {

}
