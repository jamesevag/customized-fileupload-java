package com.example.upload.repository;

import com.example.upload.model.ZipEntryMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZipEntryMetadataRepository extends JpaRepository<ZipEntryMetadata, Long> {

}
