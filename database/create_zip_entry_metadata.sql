CREATE TABLE public.zip_entry_metadata
(
    id                BIGSERIAL PRIMARY KEY,
    path              VARCHAR(255),
    directory         BOOLEAN,
    size              BIGINT,
    compressed_size   BIGINT,
    encoding          VARCHAR(50),
    status            VARCHAR(50) NOT NULL,
    failure_reason    VARCHAR(1024),
    upload_session_id UUID        NOT NULL,
    CONSTRAINT fk_zip_entry_upload_session
        FOREIGN KEY (upload_session_id)
            REFERENCES public.upload_session (id)
            ON DELETE CASCADE
);
