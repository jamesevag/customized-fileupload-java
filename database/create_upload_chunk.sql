CREATE TABLE public.upload_chunk
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_index       INT         NOT NULL,
    chunk_data        OID       NOT NULL,
    status            VARCHAR(50) NOT NULL,
    failure_reason    VARCHAR(1024),
    upload_session_id UUID        NOT NULL,
    CONSTRAINT fk_upload_chunk_session
        FOREIGN KEY (upload_session_id)
            REFERENCES public.upload_session (id)
            ON DELETE CASCADE
);


CREATE INDEX idx_upload_chunk_session_index
    ON public.upload_chunk (upload_session_id, chunk_index);
