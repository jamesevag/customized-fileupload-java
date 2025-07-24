CREATE TABLE public.upload_chunk (
                                     id uuid NOT NULL,
                                     chunk_index int4 NOT NULL,
                                     chunk_data oid NOT NULL,
                                     upload_session_id uuid NOT NULL,
                                     CONSTRAINT upload_chunk_pkey PRIMARY KEY (id),
                                     CONSTRAINT fk_upload_session FOREIGN KEY (upload_session_id) REFERENCES public.upload_session(id) ON DELETE CASCADE
);

CREATE INDEX idx_upload_chunk_session_index ON public.upload_chunk USING btree (upload_session_id, chunk_index);